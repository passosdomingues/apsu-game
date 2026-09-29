package br.apsu.core;

import br.apsu.model.boss.BossEntity;
import br.apsu.audio.AudioCue;
import br.apsu.core.events.EventBus;
import br.apsu.core.events.GameEvent;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.enemy.EnemyType;
import br.apsu.model.environment.Difficulty;
import br.apsu.model.environment.Projectile;
import br.apsu.model.environment.OceanDepthProfile;
import br.apsu.model.environment.SceneryElement;
import br.apsu.model.hero.HeroType;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Integração — GameContext & Fluxo de Jogo Completo (5 Fases)")
public class GameContextIntegrationTest {

    private GameContext context;
    private EventBus eventBus;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        context = new GameContext(new SaveManager(tempDir.resolve("savegame.json")), eventBus);
        context.reset();
    }

    @Test
    @DisplayName("Fluxo completo de Inicialização e Transição de 5 Fases (MENU -> P1 -> P2 -> P3 -> P4 -> P5)")
    void testGamePhaseFlow() {
        assertEquals(GameContext.State.MENU, context.getState());

        context.transitionToPhase(1);
        assertEquals(GameContext.State.DIALOGUE, context.getState());
        assertEquals(0, context.getDlgPhase());

        context.startP1();
        assertEquals(GameContext.State.P1, context.getState());
        assertTrue(context.getEnemies().size() > 0);
        assertEquals(1, context.getGuardians().size());

        context.startP2();
        assertEquals(GameContext.State.P2, context.getState());
        assertTrue(context.getSceneryElements().size() > 0, "Fase 2 deve possuir obstáculos de coral e gêiseres");

        context.startP3();
        assertEquals(GameContext.State.P3, context.getState());
        assertTrue(context.getSceneryElements().size() > 0, "Fase 3 deve possuir correntes de água e zonas de pressão");

        context.startP4();
        assertEquals(GameContext.State.P4, context.getState());
        assertTrue(context.getSceneryElements().size() > 0, "Fase 4 deve possuir lagoas de lava e obstáculos vulcânicos");

        context.startP5();
        assertEquals(GameContext.State.P5, context.getState());
        assertNotNull(context.getBoss(), "Fase 5 (Boss Final) deve spawnar o Boss Kullullû");
    }

    @Test
    @DisplayName("Integração de Colisão: Disparo de bolhas do Herói DESTRÓI inimigos comuns")
    void testHeroBubblesDestroyCommonEnemies() {
        List<AudioCue> cues = new ArrayList<>();
        eventBus.subscribe(GameEvent.Type.SOUND_REQUESTED,
            event -> cues.add((AudioCue) event.payload()));
        context.startP1();
        context.getHero().setHasBubblePower(true);

        EnemyEntity targetEnemy = new EnemyEntity(EnemyType.PEIXE, 400, 300, 0, 0);
        context.getEnemies().clear();
        context.getEnemies().add(targetEnemy);

        // Projétil em coordenadas de MUNDO (B3-FIX)
        Projectile bubble = new Projectile(Projectile.Type.HERO_BUBBLE, targetEnemy.getWorldX() + 10, targetEnemy.getCurrentY() + 10, 10, 0, 1.0, false);
        context.getProjectiles().add(bubble);

        context.update(1_000_000_000L);

        assertFalse(targetEnemy.isAlive(), "Bolhas do herói devem DESTRUIR inimigos comuns ao colidir");
        assertTrue(cues.contains(AudioCue.ENEMY_DEFEATED));
        assertTrue(cues.contains(AudioCue.ENEMY_DAMAGED), "O acerto precisa publicar o evento de impacto próprio");
    }

    @Test
    @DisplayName("Navegação e confirmação do menu publicam sons diferentes")
    void menuNavigationAndConfirmationPublishDistinctCues() {
        List<AudioCue> cues = new ArrayList<>();
        eventBus.subscribe(GameEvent.Type.SOUND_REQUESTED,
            event -> cues.add((AudioCue) event.payload()));

        context.onKey(KeyCode.UP);
        context.setMenuSel(2);
        context.onKey(KeyCode.ENTER);

        assertTrue(cues.contains(AudioCue.MENU_NAVIGATE));
        assertTrue(cues.contains(AudioCue.MENU_CONFIRM));
    }

    @Test
    @DisplayName("Integração de Batalha com o Boss na Fase 5: Dano do herói e Vitória ao zerar HP do Boss")
    void testBossBattleAndVictory() {
        context.startP5();
        BossEntity boss = context.getBoss();
        assertNotNull(boss, "Boss deve ser instanciado na Fase 5");

        boss.takeDamage(boss.getHp() - 1); // reduz o setup; o golpe final usa a colisão real
        List<AudioCue> cues = new ArrayList<>();
        List<Boolean> bossMusic = new ArrayList<>();
        eventBus.subscribe(GameEvent.Type.SOUND_REQUESTED,
            event -> cues.add((AudioCue) event.payload()));
        eventBus.subscribe(GameEvent.Type.BOSS_MUSIC_CHANGED,
            event -> bossMusic.add((Boolean) event.payload()));

        context.getProjectiles().add(new Projectile(Projectile.Type.HERO_BUBBLE,
            boss.getX() + 10, boss.getY() + 10, 0, 0, 1, true));
        context.update(0);

        assertTrue(boss.isDead(), "O projétil deve zerar a vida do boss por colisão");
        assertEquals(GameContext.State.WIN, context.getState());
        assertTrue(cues.contains(AudioCue.BOSS_DAMAGED));
        assertTrue(cues.contains(AudioCue.VICTORY));
        assertEquals(List.of(false), bossMusic, "A trilha de boss deve encerrar na vitória");
    }

    @Test
    @DisplayName("Eventos de áudio acompanham profundidade e entrada na arena do boss")
    void phaseChangesPublishDepthAndBossMusicState() {
        List<OceanDepthProfile> depths = new ArrayList<>();
        List<Boolean> bossMusic = new ArrayList<>();
        eventBus.subscribe(GameEvent.Type.OCEAN_DEPTH_CHANGED,
            event -> depths.add((OceanDepthProfile) event.payload()));
        eventBus.subscribe(GameEvent.Type.BOSS_MUSIC_CHANGED,
            event -> bossMusic.add((Boolean) event.payload()));

        context.startP1();
        context.startP2();
        context.startP3();
        context.startP4();
        context.startP5();

        assertEquals(List.of(OceanDepthProfile.COASTAL, OceanDepthProfile.DEEP_REEF,
            OceanDepthProfile.ABYSSAL_PLAIN, OceanDepthProfile.HYDROTHERMAL_VENT,
            OceanDepthProfile.HADAL_TRENCH), depths);
        assertEquals(List.of(false, false, false, false, true), bossMusic);
    }

    @Test
    @DisplayName("Corrente e zona de pressão aplicam fatores do ambiente da fase")
    void phaseThreeEnvironmentalZonesExposeTheirActiveForces() {
        context.startP3();
        context.getHero().setX(950);
        context.getHero().setY(200);
        context.update(1_000_000_000L);
        assertTrue(context.isInCurrent());
        assertEquals(-2.2, context.getCurrentFx());

        context.startP3();
        context.getHero().setX(1_550);
        context.getHero().setY(600);
        context.update(2_000_000_000L);
        assertTrue(context.isInPressureZone());
        assertEquals(0.90, context.getPressureBuoyMult(), 0.001);
    }

    @Test
    @DisplayName("Interação com guardião abre diálogo e concede exatamente uma tabuleta ao concluir")
    void guardianDialogueAwardsTabletOnlyAfterCompletion() {
        context.startP1();
        var guardian = context.getGuardians().get(0);
        context.getHero().setX(guardian.getWorldX());
        context.getHero().setY(guardian.getWorldY());
        context.getInputManager().registerKeyPress(KeyCode.E);
        context.update(1_000_000_000L);

        assertTrue(context.isOverlayActive());
        assertEquals(0, context.getTabletsCollected());
        for (int i = 0; i < 64 && context.isOverlayActive(); i++) context.advanceOverlay();

        assertFalse(context.isOverlayActive());
        assertEquals(1, context.getTabletsCollected());
    }

    @Test
    @DisplayName("Troca de Dificuldades e seleção de Herói via Teclado")
    void testMenuKeyboardNavigation() {
        assertEquals(Difficulty.MEDIO, context.getDifficulty());

        context.onKey(KeyCode.UP);
        context.onKey(KeyCode.UP);
        context.onKey(KeyCode.LEFT);
        assertEquals(Difficulty.FACIL, context.getDifficulty());

        context.onKey(KeyCode.DOWN);
        context.onKey(KeyCode.RIGHT);
        assertEquals(HeroType.ABISSAL, context.getHeroType());
    }

    @Test
    @DisplayName("Abertura do Baú e Despertar do Poder das Bolhas na Fase 2")
    void testChestOpeningAndBubblePower() {
        context.startP2();
        assertFalse(context.getHero().hasBubblePower());

        context.getHero().setX(context.getChestWX());
        context.getHero().setY(context.getChestWY());
        context.getInputManager().registerKeyPress(KeyCode.E);

        context.update(2_000_000_000L);
        assertTrue(context.isChestOpen(), "O baú deve ter sido aberto com o comando E");
        assertTrue(context.getHero().hasBubblePower(), "O poder das bolhas deve ser ativado após abrir o baú");
    }

    @Test
    @DisplayName("Aura do herói não deve contar como colisão com coral")
    void auraDoesNotCauseCoralDamage() {
        context.startP2();
        SceneryElement topCoral = context.getSceneryElements().stream()
            .filter(e -> e.getType() == SceneryElement.Type.CORAL && e.getWorldY() == 0)
            .findFirst()
            .orElseThrow();

        // O sprite ainda alcança o coral, mas o núcleo de colisão termina
        // exatamente antes dele: transparência/aura não pode causar dano.
        context.getHero().setX(topCoral.getWorldX() - 60);
        context.getHero().setY(60);
        double hpBefore = context.getHero().getHp();
        context.update(1_000_000_000L);

        assertEquals(hpBefore, context.getHero().getHp(), 0.001);
    }

    @Test
    @DisplayName("Preferências persistidas são restauradas em uma nova sessão")
    void restoresPersistedPreferences() {
        context.cycleDiff(+1);
        context.cycleHero(+1);

        GameContext restored = new GameContext(new SaveManager(tempDir.resolve("savegame.json")));

        assertEquals(Difficulty.DIFICIL, restored.getDifficulty());
        assertEquals(HeroType.ABISSAL, restored.getHeroType());
    }

    @Test
    @DisplayName("Push-out de obstáculos (MTV) ejeta o herói para fora de blocos sólidos")
    void pushHeroOutOfObstacleEjectsHeroFromSolidObstacle() {
        context.startP2();
        // Posicionar herói sobreposto com uma rocha (por exemplo, ox=500, oy=100, ow=100, oh=100)
        double rockX = 500, rockY = 100, rockW = 100, rockH = 100;
        context.getHero().setX(rockX + 10);
        context.getHero().setY(rockY + 10);

        context.pushHeroOutOfObstacle(rockX, rockY, rockW, rockH);

        // O centro de colisão do herói (hx = hero.getX() + 16, hy = hero.getY() + 32)
        // não deve mais sobrepor a rocha [rockX, rockX + rockW] x [rockY, rockY + rockH].
        double hx = context.getHero().getX() + 24;
        double hy = context.getHero().getY() + 50;
        double hw = 24;
        double hh = 80;

        assertFalse(context.rectsHit(hx, hy, hw, hh, rockX, rockY, rockW, rockH),
            "Após o push-out, a caixa de colisão do herói deve estar completamente fora da rocha");
    }
}
