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
import br.apsu.model.hero.HeroEntity;
import br.apsu.model.guardian.GuardianEntity;
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
        assertEquals(GameContext.State.P1, context.getState());
        assertEquals(0, context.getDlgPhase());
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
        assertEquals(0, context.getBoss().getVariant(), "A fase usa apenas o Kullullû principal");
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
        targetEnemy.takeDamage(targetEnemy.getHp() - 1); // deixa com 1 HP para testar impacto e destruição
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
        assertTrue(cues.contains(AudioCue.BOSS_DAMAGED));
        assertEquals(List.of(false), bossMusic, "A trilha de boss deve encerrar na derrota");
        assertNotNull(context.getSacredTablet(), "A 5ª Tabuleta Sagrada emerge no altar ao derrotar Kullullû");

        // Herói aproxima-se e resgata a 5ª Tabuleta Sagrada no altar
        context.getHero().resetPosition(context.getSacredTablet().getWorldX(), context.getSacredTablet().getWorldY());
        context.update(1_000_000L);

        assertEquals(GameContext.State.WIN, context.getState());
        assertTrue(cues.contains(AudioCue.VICTORY));
        assertEquals(5, context.getTabletsCollected(), "Adapa reúne todas as 5 tabuletas sagradas");
    }

    @Test
    @DisplayName("Projétil do boss reduz vida e ativa feedback de dano na arena final")
    void bossProjectileDamagesHeroInFinalArena() {
        context.setDifficulty(Difficulty.FACIL);
        context.startP5();
        context.getHero().resetPosition(80, 280);
        double hpBefore = context.getHero().getHp();
        Projectile bullet = new Projectile(Projectile.Type.BOSS_BULLET,
            context.getHero().getX() + 32, context.getHero().getY() + 90,
            0, 0, 0.5, true);
        context.getProjectiles().add(bullet);

        context.update(1_000_000_000L);

        assertEquals(hpBefore - 0.5, context.getHero().getHp(), 0.001,
            "O HUD precisa refletir dano fracionário do projétil do boss");
        assertTrue(context.getHero().isInvulnerable(), "Um acerto deve iniciar a janela de recuperação");
        assertFalse(context.getProjectiles().contains(bullet), "O projétil que acertou deve ser consumido");
    }

    @Test
    @DisplayName("APSU-B19A: Boss Kullullû causa dano de contato real no modo Fácil sem bloqueio de tutorial")
    void bossContactDamagesHeroInFacilModeInFinalArena() {
        context.setDifficulty(Difficulty.FACIL);
        context.getHero().heal(Difficulty.FACIL.getInitialHP());
        context.startP5();
        // Posiciona herói colidindo com o boss
        context.getHero().resetPosition(context.getBoss().getX() + 20, context.getBoss().getY() + 20);
        double hpBefore = context.getHero().getHp();
        assertEquals(5.0, hpBefore, 0.001, "No modo fácil o herói inicia com 5.0 HP");

        context.update(1_000_000_000L);

        assertTrue(context.getHero().getHp() < hpBefore,
            "O boss deve causar dano real no modo fácil (sem ser bloqueado por filtro de 50% HP do tutorial)");
        assertTrue(context.getHero().isInvulnerable(), "Herói entra em invulnerabilidade temporária após dano");
    }

    @Test
    @DisplayName("Colisão de contato com inimigo reduz a vida mesmo sem bolha ativa")
    void enemyContactDamagesHeroWithoutActiveBubble() {
        context.setDifficulty(Difficulty.MEDIO);
        context.startP1();
        context.getProjectiles().clear();
        context.getEnemies().clear();
        context.getHero().setX(100);
        context.getHero().setY(300);
        EnemyEntity contact = new EnemyEntity(EnemyType.PEIXE, 110, 310, 0, 0);
        context.getEnemies().add(contact);
        double hpBefore = context.getHero().getHp();

        context.update(1_000_000_000L);

        assertTrue(context.getHero().getHp() < hpBefore,
            "O índice de colisão deve ser atualizado mesmo quando o jogador não dispara");
        assertTrue(context.getHero().isInvulnerable());
        assertFalse(contact.isAlive(), "O contato deve afastar o inimigo que atingiu o herói");
    }

    @Test
    @DisplayName("Arena final posiciona o boss maior dentro da área visível")
    void finalBossStartsLeftAndAboveItsPreviousPosition() {
        context.startP5();
        BossEntity boss = context.getBoss();

        assertEquals(1126, boss.getX() + BossEntity.BW / 2.0, 0.001);
        assertEquals(254, boss.getY() + BossEntity.BH / 2.0, 0.001);
        assertEquals(3800, BossEntity.RENDER_W);
        assertEquals(4600, BossEntity.RENDER_H);
    }

    @Test
    @DisplayName("Golfinho abissal fica na fase 4 e a arena final mantém o duelo limpo")
    void dolphinMovesToPhaseFourAndBossArenaHasNoExtraActorsOrCurrents() {
        context.startP3();
        context.startP4();
        context.startP5();
        assertTrue(context.getEnemies().isEmpty());
        assertTrue(context.getGuardians().isEmpty(), "O encontro final não deve ter Enki na arena");
        assertTrue(context.getSceneryElements().stream().noneMatch(e ->
            e.getType() == SceneryElement.Type.CURRENT || e.getType() == SceneryElement.Type.PRESSURE_ZONE));
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
        context.revealAndCollectSacredTablet();
        assertEquals(1, context.getTabletsCollected());
        assertTrue(context.getCutsceneController().isActive());
        assertEquals("Saida da Fase 1", context.getCutsceneController().getActiveRequest().label());
        assertEquals(3900.0, context.getCutsceneController().getActiveRequest().targetWorldX(), 0.001);
        var objective = br.apsu.guidance.ObjectiveTracker.resolveCurrentObjective(context);
        assertNotNull(objective);
        assertEquals(3900.0, objective.worldX(), 0.001);
    }

    @Test
    @DisplayName("US-024: receber tabuleta nas fases 1 a 4 foca a saída e atualiza o objetivo")
    void tabletDialogueFocusesPhaseExitAndUpdatesObjective() {
        assertExitIntroductionForPhase(1, context::startP1, 1_000_000_000L);
        assertExitIntroductionForPhase(2, context::startP2, 2_000_000_000L);
        assertExitIntroductionForPhase(3, context::startP3, 3_000_000_000L);
        assertExitIntroductionForPhase(4, context::startP4, 4_000_000_000L);
    }

    private void assertExitIntroductionForPhase(int phase, Runnable startPhase, long timestamp) {
        startPhase.run();
        if (phase == 2) context.getHero().setHasBubblePower(true);
        GuardianEntity guardian = context.getGuardians().get(0);
        context.getHero().setX(guardian.getWorldX());
        context.getHero().setY(guardian.getWorldY());
        context.getInputManager().registerKeyPress(KeyCode.E);
        context.update(timestamp);
        assertTrue(context.isOverlayActive(), "O diálogo do guardião da fase " + phase + " deve abrir");

        // Limpa a apresentação de chegada antes de validar o foco da saída.
        context.getCutsceneController().stop();
        for (int frame = 0; frame < 64 && context.isOverlayActive(); frame++) context.advanceOverlay();
        context.revealAndCollectSacredTablet();

        assertEquals(phase, context.getTabletsCollected());
        assertTrue(context.getCutsceneController().isActive(), "A saída deve ser apresentada após a tabuleta");
        assertEquals("Saida da Fase " + phase, context.getCutsceneController().getActiveRequest().label());
        assertEquals(3900.0, context.getCutsceneController().getActiveRequest().targetWorldX(), 0.001);
        var objective = br.apsu.guidance.ObjectiveTracker.resolveCurrentObjective(context);
        assertNotNull(objective);
        assertEquals(3900.0, objective.worldX(), 0.001);
        assertEquals("Siga para o portal", objective.description());

        context.onKey(KeyCode.X);
        for (int frame = 1; frame <= 18; frame++) context.update(timestamp + frame * 16_666_666L);
        assertFalse(context.getCutsceneController().isActive(), "Pular deve devolver o foco em até 0,3 s");
    }

    @Test
    @DisplayName("Menu abre o submenu de dificuldade e mantém Adapa principal")
    void testMenuKeyboardNavigation() {
        assertEquals(Difficulty.MEDIO, context.getDifficulty());

        context.onKey(KeyCode.ENTER);
        assertTrue(context.isDifficultyMenuOpen());
        context.onKey(KeyCode.UP);
        context.onKey(KeyCode.ENTER);
        assertEquals(Difficulty.FACIL, context.getDifficulty());
        assertFalse(context.isDifficultyMenuOpen());
        context.onKey(KeyCode.ENTER);
        context.onKey(KeyCode.DOWN);
        context.onKey(KeyCode.DOWN);
        context.onKey(KeyCode.ENTER);
        assertEquals(Difficulty.DIFICIL, context.getDifficulty());
        context.cycleHero(+1);
        assertEquals(HeroType.GUARDIAO, context.getHeroType());
    }

    @Test
    @DisplayName("Abertura do Baú e Despertar do Poder das Bolhas na Fase 2")
    void testChestOpeningAndBubblePower() {
        context.startP2();
        assertFalse(context.getHero().hasBubblePower());

        context.getHero().setY(context.getChestWY() - HeroEntity.HH / 2.0);
        context.getHero().setX(context.getChestWX() - HeroEntity.HW - 70);
        assertTrue(context.canOpenChest(), "A área de interação deve alcançar o baú pelo lado esquerdo");
        context.getHero().setX(context.getChestWX() + 70);
        assertTrue(context.canOpenChest(), "A área de interação deve alcançar o baú pelo lado direito");
        context.getHero().setX(context.getChestWX() - HeroEntity.HW - 170);
        assertFalse(context.canOpenChest(), "O prompt não deve aparecer fora do alcance do jogador");

        context.getHero().setX(context.getChestWX());
        context.getHero().setY(context.getChestWY());
        context.getInputManager().registerKeyPress(KeyCode.E);

        context.update(2_000_000_000L);
        assertTrue(context.isChestOpen(), "O baú deve ter sido aberto com o comando E");
        assertTrue(context.getHero().hasBubblePower(), "O poder das bolhas deve ser ativado após abrir o baú");
    }

    @Test
    @DisplayName("Fase 2 nao deve conter obstaculos de coral (removidos)")
    void phase2HasNoCoralObstacles() {
        context.startP2();
        boolean hasAny = context.getSceneryElements().stream()
            .anyMatch(e -> e.getWorldX() >= 500 && e.getWorldX() <= 3200
                && e.getWorldY() == 0);
        // A Fase 2 nao deve conter mais estalactites de coral
        assertFalse(hasAny, "Fase 2 nao deve conter obstaculos de coral no teto");
    }

    @Test
    @DisplayName("Preferências persistidas são restauradas em uma nova sessão")
    void restoresPersistedPreferences() {
        context.cycleDiff(+1);
        context.cycleHero(+1);

        GameContext restored = new GameContext(new SaveManager(tempDir.resolve("savegame.json")));

        assertEquals(Difficulty.DIFICIL, restored.getDifficulty());
        assertEquals(HeroType.GUARDIAO, restored.getHeroType());
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

        // O centro de colisão do herói (hx = hero.getX() + 20, hy = hero.getY() + 44)
        // não deve mais sobrepor a rocha [rockX, rockX + rockW] x [rockY, rockY + rockH].
        double hx = context.getHero().getX() + 20;
        double hy = context.getHero().getY() + 44;
        double hw = 28;
        double hh = 100;

        assertFalse(context.rectsHit(hx, hy, hw, hh, rockX, rockY, rockW, rockH),
            "Após o push-out, a caixa de colisão do herói deve estar completamente fora da rocha");
    }

    @Test
    @DisplayName("Colisão estável: herói pressionado contra obstáculo não vibra (AC2)")
    void heroPressedAgainstObstacleDoesNotVibrate() {
        context.startP2();
        double rockX = 500, rockY = 200, rockW = 100, rockH = 300;
        var hero = context.getHero();
        hero.setX(430);
        hero.setY(250);

        // Pressionar seta para a direita contra o obstáculo por 180 ticks (3 segundos)
        double expectedX = rockX - 28 - 20; // ox - hw - 20
        double maxDeltaAfterContact = 0.0;
        boolean contacted = false;

        for (int i = 0; i < 180; i++) {
            hero.updatePhysics(false, true, false, false, 0, 768, 0, 1.0, 0, 0, i * 16_666_666L);
            context.pushHeroOutOfObstacle(rockX, rockY, rockW, rockH);

            if (Math.abs(hero.getX() - expectedX) < 1.0) {
                contacted = true;
                double delta = Math.abs(hero.getX() - expectedX);
                if (delta > maxDeltaAfterContact) maxDeltaAfterContact = delta;
            }
        }

        assertTrue(contacted, "O herói deve ter alcançado o obstáculo");
        assertTrue(maxDeltaAfterContact <= 0.1,
            "A variação de posição contra a parede deve ser no máximo 0.1 px (foi " + maxDeltaAfterContact + ")");
    }

    @Test
    @DisplayName("Deslizar na parede: herói pressionado contra obstáculo desliza no eixo livre (AC3)")
    void heroSlidesAlongObstacleWhenSecondAxisPressed() {
        context.startP2();
        double rockX = 500, rockY = 0, rockW = 100, rockH = 768;
        var hero = context.getHero();
        double expectedX = rockX - 28 - 20;
        hero.setX(expectedX);
        hero.setY(150);

        // Pressionar direita (contra a parede) e para baixo (deslizar)
        double initialY = hero.getY();
        for (int i = 0; i < 20; i++) {
            hero.updatePhysics(false, true, false, true, 0, 768, 0, 1.0, 0, 0, i * 16_666_666L);
            context.pushHeroOutOfObstacle(rockX, rockY, rockW, rockH);
            assertEquals(expectedX, hero.getX(), 0.01,
                "Herói deve permanecer encostado na parede no eixo X a cada tick enquanto desliza");
        }

        assertTrue(hero.getY() > initialY + 25.0, "Herói deve ter deslizado para baixo no eixo Y livre");
    }

    @Test
    @DisplayName("Fase 1 exibe aviso de Modo 2D quando SCENE3D estiver indisponível (US-007 AC2)")
    void startP1Shows2DModeAlertWhenScene3DUnsupported() {
        try {
            br.apsu.graphics.GraphicsDiagnostics.setScene3dSupportedForTesting(false);
            context.startP1();
            assertEquals("Modo 2D: 3D indisponível neste equipamento", context.getAlertMessage(),
                "Deve alertar o jogador quando SCENE3D não for suportado pelo pipeline");
        } finally {
            br.apsu.graphics.GraphicsDiagnostics.setScene3dSupportedForTesting(null);
        }
    }

    @Test
    @DisplayName("Cutscene via EventBus: câmera foca no alvo e herói não sofre dano durante a cena (US-011 AC3)")
    void cutsceneCameraFocusAndHeroImmunity() {
        context.startP1();
        double initialHeroX = context.getHero().getX();
        double initialHp = context.getHero().getHp();

        // Dispara cutscene via EventBus
        eventBus.publish(new br.apsu.core.events.GameEvent(
            br.apsu.core.events.GameEvent.Type.CUTSCENE_REQUESTED,
            new br.apsu.cutscene.CutsceneRequest(1800.0, 1.0, "Elemento Importante"),
            0, 0
        ));

        assertTrue(context.getCutsceneController().isActive(), "Cutscene deve estar ativa");

        // Spawna um inimigo diretamente em cima do herói
        EnemyEntity enemy = new EnemyEntity(EnemyType.PEIXE, initialHeroX, context.getHero().getY(), 0, 0);
        context.getEnemies().clear();
        context.getEnemies().add(enemy);

        // Roda múltiplos frames durante a cutscene
        for (int i = 0; i < 30; i++) {
            context.update((i + 1) * 16_666_666L);
        }

        assertEquals(initialHp, context.getHero().getHp(), 0.001,
            "Herói não deve sofrer dano durante a cutscene");

        // Pula com tecla ESPAÇO
        context.onKey(KeyCode.SPACE);

        // Processa o retorno da câmera (PAN_BACK concluído)
        for (int i = 30; i < 60; i++) {
            context.update((i + 1) * 16_666_666L);
        }

        assertFalse(context.getCutsceneController().isActive(), "Cutscene deve ter terminado");
        assertEquals(initialHeroX, context.getHero().getX(), 1.0, "Herói retoma sem salto de posição");
    }

    @Test
    @DisplayName("Fase 1 dispara cutscene de foco do primeiro guardiao por 2.0 s (US-012 AC1)")
    void p1InitialDialogueEndTriggersGuardianCutscene() {
        context.transitionToPhase(1); // Entra diretamente na Fase 1 sem tela de diálogo intermediária
        assertEquals(GameContext.State.P1, context.getState());
        assertTrue(context.getCutsceneController().isActive(),
            "Cutscene de foco no primeiro guardião deve iniciar na entrada da Fase 1");
        assertEquals("Primeiro Guardiao", context.getCutsceneController().getActiveRequest().label());
        assertEquals(br.apsu.cutscene.PhaseCutsceneConfig.P1_INTRO_DURATION, context.getCutsceneController().getActiveRequest().holdDurationSeconds(), 0.001);

        double guardianCenterX = context.getGuardians().get(0).getWorldX() + br.apsu.model.guardian.GuardianEntity.GW / 2.0;
        assertEquals(guardianCenterX, context.getCutsceneController().getActiveRequest().targetWorldX(), 0.001);
    }

    private void advanceThroughP1IntroDialogue() {
        context.transitionToPhase(1);
    }

    @Test
    @DisplayName("Cutscene do guardião toca uma única vez por partida e redispara após reset (US-012 AC3)")
    void p1GuardianCutscenePlaysOncePerGameAndReplaysOnReset() {
        advanceThroughP1IntroDialogue();
        assertTrue(context.getCutsceneController().isActive());

        // Consome toda a cutscene
        for (int i = 0; i < 400 && context.getCutsceneController().isActive(); i++) {
            context.update((i + 1) * 16_666_666L);
        }
        assertFalse(context.getCutsceneController().isActive());

        // Avançar novamente diálogo sem reset() não deve redisparar a cutscene
        advanceThroughP1IntroDialogue();
        assertFalse(context.getCutsceneController().isActive(),
            "Cutscene não deve redisparar na mesma partida");

        // Reinicia a partida
        context.reset();
        assertFalse(context.getCutsceneController().isActive());

        advanceThroughP1IntroDialogue();
        assertTrue(context.getCutsceneController().isActive(),
            "Cutscene deve redisparar uma vez após reset da partida");
    }

    @Test
    @DisplayName("Pular a cutscene do guardião conclui foco e mantém seta de orientação visível (US-012 AC2, AC4)")
    void p1GuardianCutsceneSkipKeepsObjectiveArrowAndHeroImmunity() {
        advanceThroughP1IntroDialogue();
        assertTrue(context.getCutsceneController().isActive());

        // Seta de objetivo deve estar apontando para o guardião
        br.apsu.guidance.ObjectiveTracker.Objective obj = br.apsu.guidance.ObjectiveTracker.resolveCurrentObjective(context);
        assertNotNull(obj, "Objetivo não deve ser nulo");
        assertEquals("Fale com o guardião", obj.description());

        // Inimigo próximo colidindo não causa dano durante a cutscene (AC4)
        double initialHeroX = context.getHero().getX();
        context.getEnemies().clear();
        context.getEnemies().add(new EnemyEntity(EnemyType.PEIXE, initialHeroX, context.getHero().getY(), 0, 0));
        double hpBefore = context.getHero().getHp();
        context.update(16_666_666L);
        assertEquals(hpBefore, context.getHero().getHp(), 0.001, "Herói imune a dano durante cutscene do guardião");

        // Pula com X
        context.onKey(KeyCode.X);

        // Aguarda retorno rápido (<= 0.3 s = ~20 frames)
        for (int i = 0; i < 25; i++) {
            context.update((i + 1) * 16_666_666L);
        }
        assertFalse(context.getCutsceneController().isActive(), "Cutscene deve finalizar após skip");

        // Após skip, seta continua perfeitamente apontando para o guardião
        br.apsu.guidance.ObjectiveTracker.Objective objAfter = br.apsu.guidance.ObjectiveTracker.resolveCurrentObjective(context);
        assertNotNull(objAfter);
        assertEquals("Fale com o guardião", objAfter.description());
    }

    @Test
    @DisplayName("US-023: guardiões das fases 2 a 4 são apresentados uma vez, com foco pulável e aviso normal")
    void laterPhaseGuardiansAreIntroducedOnceWhenApproached() {
        assertGuardianIntroduction(2, context::startP2, 1_000_000_000L);
        assertGuardianIntroduction(3, context::startP3, 2_000_000_000L);
        assertGuardianIntroduction(4, context::startP4, 3_000_000_000L);
    }

    private void assertGuardianIntroduction(int phase, Runnable startPhase, long firstTimestamp) {
        startPhase.run();
        GuardianEntity guardian = context.getGuardians().get(0);
        double guardianCenterX = guardian.getWorldX() + GuardianEntity.GW / 2.0;
        double guardianCenterY = guardian.getWorldY() + GuardianEntity.GH / 2.0;

        context.getHero().setX(guardianCenterX - 800.01 - HeroEntity.HW / 2.0);
        context.getHero().setY(guardianCenterY - HeroEntity.HH / 2.0);
        context.update(firstTimestamp);
        assertFalse(context.getCutsceneController().isActive(), "O foco não deve iniciar além de 800 unidades");

        context.getHero().setX(guardianCenterX - 790 - HeroEntity.HW / 2.0);
        context.update(firstTimestamp + 16_666_666L);

        assertTrue(context.getCutsceneController().isActive(), "Fase " + phase + " deve iniciar o foco a até 800 unidades");
        assertEquals(guardian.getName(), context.getCutsceneController().getActiveRequest().label());
        assertEquals(br.apsu.cutscene.PhaseCutsceneConfig.guardianIntroDuration(phase), context.getCutsceneController().getActiveRequest().holdDurationSeconds(), 0.001);
        assertEquals(guardianCenterX, context.getCutsceneController().getActiveRequest().targetWorldX(), 0.001);

        context.onKey(KeyCode.X);
        for (int tick = 1; tick <= 18; tick++) {
            context.update(firstTimestamp + (tick + 1) * 16_666_666L);
        }
        assertFalse(context.getCutsceneController().isActive(), "Pulo deve encerrar o foco em até 0,3 s");

        context.getHero().setX(guardianCenterX - 850 - HeroEntity.HW / 2.0);
        context.update(firstTimestamp + 20 * 16_666_666L);

        // O aviso contextual de interação continua funcionando quando o herói chega perto.
        context.getHero().setX(guardianCenterX - 100 - HeroEntity.HW / 2.0);
        context.getHero().setY(guardianCenterY - HeroEntity.HH / 2.0);
        context.update(firstTimestamp + 21 * 16_666_666L);
        assertTrue(guardian.isPromptVisible(), "O aviso de interação deve aparecer após a apresentação");
        assertFalse(context.getCutsceneController().isActive(), "A apresentação não deve repetir na mesma fase");
    }

    @Test
    @DisplayName("US-013: Aviso contextual de interação com guardião possui histerese anti-pisca (150 in, 180 out)")
    void guardianInteractionPromptHysteresis() {
        context.startP1();
        var guardian = context.getGuardians().get(0);
        double targetCenterX = guardian.getWorldX() + GuardianEntity.GW / 2.0;
        double targetCenterY = guardian.getWorldY() + GuardianEntity.GH / 2.0;

        // Herói longe (> 200) -> aviso não visível
        context.getHero().setX(targetCenterX - 250 - HeroEntity.HW / 2.0);
        context.getHero().setY(targetCenterY - HeroEntity.HH / 2.0);
        context.update(16_666_666L);
        assertFalse(guardian.isPromptVisible(), "Aviso não deve aparecer longe do guardião");

        // Herói aproxima para 140 (< 150) -> aviso aparece
        context.getHero().setX(targetCenterX - 140 - HeroEntity.HW / 2.0);
        context.update(33_333_333L);
        assertTrue(guardian.isPromptVisible(), "Aviso deve aparecer ao entrar no raio de 150 unidades");

        // Herói afasta para 165 (entre 150 e 180 - zona de histerese) -> aviso continua visível sem piscar
        context.getHero().setX(targetCenterX - 165 - HeroEntity.HW / 2.0);
        context.update(50_000_000L);
        assertTrue(guardian.isPromptVisible(), "Aviso deve permanecer ativo na zona de histerese (165 unidades)");

        // Herói afasta para 190 (> 180) -> aviso desaparece
        context.getHero().setX(targetCenterX - 190 - HeroEntity.HW / 2.0);
        context.update(66_666_666L);
        assertFalse(guardian.isPromptVisible(), "Aviso deve sumir ao afastar além de 180 unidades");

        // Retorna para 165 -> ainda não reaparece (precisa cruzar < 150)
        context.getHero().setX(targetCenterX - 165 - HeroEntity.HW / 2.0);
        context.update(83_333_333L);
        assertFalse(guardian.isPromptVisible(), "Aviso não deve reaparecer ao entrar na histerese vindo de fora");
    }

    @Test
    @DisplayName("US-013: Aviso contextual do baú possui histerese (150 in, 180 out)")
    void chestInteractionPromptHysteresis() {
        context.startP2();
        double chestX = context.getChestWX();
        double chestY = context.getChestWY();

        // Herói longe (> 200) -> prompt do baú inativo
        context.getHero().setX(chestX - 250 - HeroEntity.HW / 2.0);
        context.getHero().setY(chestY - HeroEntity.HH / 2.0);
        assertFalse(context.isChestPromptVisible(), "Prompt do baú não deve aparecer a 250 unidades");

        // Herói aproxima para 130 (< 150) -> prompt do baú ativo
        context.getHero().setX(chestX - 130 - HeroEntity.HW / 2.0);
        assertTrue(context.isChestPromptVisible(), "Prompt do baú deve aparecer a 130 unidades (< 150)");

        // Herói afasta para 170 (histerese) -> prompt continua ativo
        context.getHero().setX(chestX - 170 - HeroEntity.HW / 2.0);
        assertTrue(context.isChestPromptVisible(), "Prompt do baú deve permanecer ativo a 170 unidades");

        // Herói afasta para 195 (> 180) -> prompt desativa
        context.getHero().setX(chestX - 195 - HeroEntity.HW / 2.0);
        assertFalse(context.isChestPromptVisible(), "Prompt do baú deve desativar além de 180 unidades");
    }

    @Test
    @DisplayName("US-013: Guardião já contactado não exibe mais o aviso e iniciar diálogo com Espaço não solta bolha")
    void contactedGuardianNeverShowsPromptAndSpaceSuppressesBubble() {
        context.startP1();
        var guardian = context.getGuardians().get(0);
        context.getHero().setHasBubblePower(true);
        context.getHero().setX(guardian.getWorldX());
        context.getHero().setY(guardian.getWorldY());

        int initialProjectiles = context.getProjectiles().size();

        // Inicia diálogo com Espaço
        context.getInputManager().registerKeyPress(KeyCode.SPACE);
        context.update(100_000_000L);

        // Diálogo deve abrir
        assertTrue(context.isOverlayActive(), "Diálogo com guardião deve abrir com tecla Espaço");
        // Nenhuma bolha deve ter sido disparada
        assertEquals(initialProjectiles, context.getProjectiles().size(),
            "Iniciar diálogo com Espaço não deve disparar projétil de bolha (AC3)");

        // Conclui diálogo
        for (int i = 0; i < 64 && context.isOverlayActive(); i++) context.advanceOverlay();
        assertFalse(context.isOverlayActive());
        assertTrue(guardian.isContacted());

        // Atualiza frame perto do guardião
        context.update(150_000_000L);
        assertFalse(guardian.isPromptVisible(),
            "Guardião já contactado nunca deve voltar a exibir aviso de interação (AC4)");
    }

    @Test
    @DisplayName("US-021: Cutscene foca o baú na Fase 2 se fechado, seta aponta para o baú e skip encerra em <= 0.3s")
    void phaseTwoChestCutsceneAndObjectiveFlow() {
        context.reset();
        context.transitionToPhase(2);

        assertEquals(GameContext.State.P2, context.getState());
        assertEquals(0, context.getProjectiles().size());
        assertTrue(context.getCutsceneController().isActive(), "Cutscene do baú deve iniciar na Fase 2");
        assertEquals("Bau Naufragio", context.getCutsceneController().getActiveRequest().label());
        assertEquals(context.getChestWX(), context.getCutsceneController().getActiveRequest().targetWorldX(), 0.001);

        // A seta aponta para o baú
        var obj = br.apsu.guidance.ObjectiveTracker.resolveCurrentObjective(context);
        assertNotNull(obj);
        assertEquals(context.getChestWX(), obj.worldX(), 0.001);
        assertEquals("Encontre o naufrágio e abra o baú", obj.description());

        // Pula cutscene
        context.onKey(KeyCode.SPACE);

        // Em <= 0.3 s (<= 20 frames a 60fps), o foco termina
        for (int i = 0; i < 20; i++) {
            context.update((i + 1) * 16_666_666L);
        }
        assertFalse(context.getCutsceneController().isActive(), "Cutscene deve terminar após skip em <= 0.3s");

        // Se a fase 2 for recarregada com o baú já aberto ou poder já obtido, a cutscene não repete
        context.getHero().setHasBubblePower(true);
        context.transitionToPhase(2);
        context.onKey(KeyCode.SPACE);
        assertFalse(context.getCutsceneController().isActive(), "Cutscene do baú não deve repetir se o poder já foi obtido");
    }

    @Test
    @DisplayName("US-022: Abertura do baú publica CHEST_OPENED, exibe 'Espaço para atacar' uma vez e não reaplica se já aberto")
    void chestOpenedEventAndAttackHintFlow() {
        context.startP2();

        List<GameEvent> chestEvents = new ArrayList<>();
        context.getEventBus().subscribe(GameEvent.Type.CHEST_OPENED, chestEvents::add);

        // Posiciona herói sobre o baú
        context.getHero().setX(context.getChestWX());
        context.getHero().setY(context.getChestWY());
        context.getInputManager().registerKeyPress(KeyCode.E);

        // Atualiza frame
        context.update(2_000_000_000L);

        assertTrue(context.isChestOpen(), "Baú deve estar aberto");
        assertTrue(context.getHero().hasBubblePower(), "Poder das bolhas deve estar ativo");

        // Evento publicado exatamente 1 vez com dados de contrato
        assertEquals(1, chestEvents.size(), "Evento CHEST_OPENED deve ser publicado exatamente uma vez");
        GameEvent event = chestEvents.get(0);
        assertEquals(GameEvent.Type.CHEST_OPENED, event.type());
        assertEquals("Poder das Bolhas Desperto", event.payload());
        assertEquals(context.getChestWX(), event.worldX(), 0.001);
        assertEquals(context.getChestWY(), event.worldY(), 0.001);

        // Alerta exibido com a dica
        assertEquals("Espaço para atacar", context.getAlertMessage(),
            "A dica 'Espaço para atacar' deve ser exibida ao desbloquear o poder");

        // Segunda interação com baú já aberto não publica novo evento nem reaplica
        context.update(2_016_666_666L);
        context.getInputManager().registerKeyPress(KeyCode.E);
        context.update(2_033_333_333L);

        assertEquals(1, chestEvents.size(), "Nenhum evento adicional deve ser publicado após o baú já estar aberto");
    }

    @Test
    @DisplayName("US-014: Objetivo atual dinâmico no HUD transiciona corretamente entre as fases e eventos")
    void dynamicHudObjectiveTransitions() {
        context.reset();

        // 1. Fase 1 antes da conversa -> "Fale com o guardião"
        context.startP1();
        var obj1 = br.apsu.guidance.ObjectiveTracker.resolveCurrentObjective(context);
        assertNotNull(obj1);
        assertEquals("Fale com o guardião", obj1.description());

        // 2. Conclui conversa e recebe tabuleta -> muda para "Siga para o portal" em até 1s
        var guardian = context.getGuardians().get(0);
        context.getHero().setX(guardian.getWorldX());
        context.getHero().setY(guardian.getWorldY());
        context.getInputManager().registerKeyPress(KeyCode.E);
        context.update(1_000_000_000L);
        context.onKey(KeyCode.X); // fecha diálogo
        context.revealAndCollectSacredTablet(); // coleta tabuleta
        assertEquals(1, context.getTabletsCollected());
        var objAfterTablet = br.apsu.guidance.ObjectiveTracker.resolveCurrentObjective(context);
        assertNotNull(objAfterTablet);
        assertEquals("Siga para o portal", objAfterTablet.description());

        // 3. Fase 2 com o poder das bolhas bloqueado -> "Encontre o naufrágio e abra o baú"
        context.startP2();
        assertFalse(context.getHero().hasBubblePower());
        assertFalse(context.isChestOpen());
        var objP2 = br.apsu.guidance.ObjectiveTracker.resolveCurrentObjective(context);
        assertNotNull(objP2);
        assertEquals("Encontre o naufrágio e abra o baú", objP2.description());

        // Ao abrir o baú, objetivo da fase 2 passa para o guardião da fase 2 ("Fale com o guardião")
        context.getHero().setX(context.getChestWX());
        context.getHero().setY(context.getChestWY());
        context.getInputManager().registerKeyPress(KeyCode.E);
        context.update(2_000_000_000L);
        assertTrue(context.isChestOpen());

        // Ao abrir o baú, a fase 2 não possui seta no guardião (apenas na fase 1)
        var objP2AfterChest = br.apsu.guidance.ObjectiveTracker.resolveCurrentObjective(context);
        assertNull(objP2AfterChest, "Fase 2 não possui seta para o guardião");
    }

    @Test
    @DisplayName("US-020: Tutorial guiado na Fase 1, proteção de vida a 50%, avanço com Esc e conclusão")
    void testPhase1GuidedTutorialIntegration() {
        context.startP1();
        assertTrue(context.getTutorialController().isActive(), "Tutorial deve iniciar ativo na Fase 1");
        assertEquals(br.apsu.tutorial.TutorialStep.SWIM_DOWN, context.getTutorialController().getCurrentStep());

        // Dano durante o tutorial não reduz vida abaixo de 50%
        double maxHp = context.getHero().getMaxHP();
        context.getHero().applyDamage(maxHp * 0.9, 1_000_000_000L);
        // Usando hurtHero indiretamente via dano filtrado
        double capped = context.getTutorialController().filterHeroDamage(context.getHero().getHp(), maxHp, 10.0);
        assertTrue(capped >= maxHp * 0.50, "Vida nao deve cair abaixo de 50%");

        // X pula o tutorial
        context.onKey(KeyCode.X);
        assertFalse(context.getTutorialController().isActive(), "X deve concluir/pular o tutorial");
        assertTrue(context.getTutorialController().isSkipped());

        // Reiniciar jogo restaura o tutorial
        context.reset();
        context.startP1();
        assertTrue(context.getTutorialController().isActive(), "Reiniciar jogo deve restaurar tutorial ativo");
    }

    @Test
    @DisplayName("US-002: Dano por contato com Kullullû (contato único, contínuo e invulnerabilidade)")
    void testBossContactDamageAndInvulnerability() {
        context.startP5();
        assertNotNull(context.getBoss());
        assertFalse(context.getBoss().isDead());

        java.util.concurrent.atomic.AtomicInteger damagedEvents = new java.util.concurrent.atomic.AtomicInteger(0);
        context.getEventBus().subscribe(br.apsu.core.events.GameEvent.Type.HERO_DAMAGED, e -> damagedEvents.incrementAndGet());

        // 1. Contato único com o Boss
        context.getHero().setX(context.getBoss().getX() + 20);
        context.getHero().setY(context.getBoss().getY() + 20);
        double hpBefore = context.getHero().getHp();

        context.update(1_000_000_000L); // 1 tick de contato
        assertTrue(context.getHero().getHp() < hpBefore, "Herói deve sofrer dano por contato com o Boss");
        assertTrue(context.getHero().isInvulnerable(), "Herói deve ficar invulnerável após o dano");
        assertEquals(1, damagedEvents.get(), "Evento HERO_DAMAGED publicado exatamente uma vez no contato");

        // 2. Contato contínuo durante o período de invulnerabilidade (nenhum dano extra aplicado)
        double hpDuringInvuln = context.getHero().getHp();
        context.update(1_500_000_000L); // 0.5s depois (ainda invulnerável)
        assertEquals(hpDuringInvuln, context.getHero().getHp(), "Nenhum dano adicional deve ser aplicado durante invulnerabilidade");
        assertEquals(1, damagedEvents.get(), "Nenhum evento adicional emitido durante invulnerabilidade");

        // 3. Após fim da invulnerabilidade (1.25s), novo dano é aplicado
        context.update(2_300_000_000L); // 1.3s após o primeiro dano
        assertTrue(context.getHero().getHp() < hpDuringInvuln, "Novo dano deve ser aplicado após o fim da invulnerabilidade");
        assertEquals(2, damagedEvents.get(), "Novo evento HERO_DAMAGED emitido após expiração da invulnerabilidade");
    }

    @Test
    @DisplayName("APSU-M19D: Menu de pausa congela simulação e retoma com ESC/P")
    void testPauseMenuToggleAndResume() {
        context.startP2();
        assertEquals(GameContext.State.P2, context.getState());

        // Pressiona ESC para pausar
        context.onKey(KeyCode.ESCAPE);
        assertEquals(GameContext.State.PAUSE, context.getState());
        assertEquals(GameContext.State.P2, context.getStateBeforePause());

        // Simulação travada durante pausa
        double heroXBefore = context.getHero().getX();
        context.getHero().setVx(10.0);
        context.update(2_000_000_000L);
        assertEquals(heroXBefore, context.getHero().getX(), 0.001, "Herói não se move enquanto o jogo estiver pausado");

        // Pressiona P para despausar
        context.onKey(KeyCode.P);
        assertEquals(GameContext.State.P2, context.getState());
    }

    @Test
    @DisplayName("APSU-M19D: Tecla R reinicia a fase instantaneamente com vida e poderes")
    void testInstantRestartWithKeyR() {
        context.startP3();
        context.getHero().applyDamage(2.0, 1000L);
        assertTrue(context.getHero().getHp() < 4.0);

        context.onKey(KeyCode.R);
        assertEquals(GameContext.State.P3, context.getState());
        assertEquals(Difficulty.MEDIO.getInitialHP(), context.getHero().getHp(), 0.001, "Vida restaurada no reinício");
        assertTrue(context.getHero().hasBubblePower(), "Poder de bolha mantido em fases avançadas");
        assertEquals(2, context.getTabletsCollected(), "Tabuletas das fases anteriores mantidas");
    }

    @Test
    @DisplayName("APSU-M19E: Menu principal oferece opção Continuar quando phaseDone > 0")
    void testContinueSystemFromMainMenu() {
        context.setPhaseDone(3); // Desbloqueou até fase 4
        assertTrue(context.canContinue());

        context.onKey(KeyCode.ENTER); // Primeira opção do menu é Continuar Fase 4
        // Transiciona e inicia diálogo / fase 4
        assertEquals(4, context.getCurrentPhase());
        assertTrue(context.getHero().hasBubblePower());
        assertEquals(3, context.getTabletsCollected());
    }

    @Test
    @DisplayName("APSU-G19F: Vitória na Fase 5 requer resgate da 5ª Tabuleta Sagrada após derrotar Kullullû")
    void testFifthSacredTabletEmergedOnBossDefeat() {
        context.startP5();
        assertNull(context.getSacredTablet(), "Antes de derrotar o boss não há tabuleta na arena");

        // Derrota o boss com bolha
        BossEntity boss = context.getBoss();
        boss.takeDamage(boss.getHp() - 1);
        context.getProjectiles().add(new Projectile(Projectile.Type.HERO_BUBBLE,
            boss.getX() + 10, boss.getY() + 10, 0, 0, 1, true));
        context.update(0);

        assertTrue(boss.isDead());
        assertNotNull(context.getSacredTablet(), "5ª Tabuleta Sagrada deve emergir no altar sagrado");
        assertTrue(context.getSacredTablet().isRevealed());
        assertNotEquals(GameContext.State.WIN, context.getState(), "Jogo ainda não venceu até coletar a 5ª relíquia");

        // Coleta a 5ª tabuleta
        context.getHero().resetPosition(context.getSacredTablet().getWorldX(), context.getSacredTablet().getWorldY());
        context.update(1_000_000L);

        assertEquals(GameContext.State.WIN, context.getState());
        assertEquals(5, context.getTabletsCollected(), "5/5 tabuletas coletadas");
    }

    @Test
    @DisplayName("APSU-B19A: Projétil de lava do Boss Kullullû (BOSS_BULLET) causa dano real ao herói em P5")
    void testBossBulletDamageInPhase5() {
        context.startP5();
        assertEquals(GameContext.State.P5, context.getState());
        double initialHp = context.getHero().getHp();
        assertTrue(initialHp > 0);

        // Posiciona projétil de lava sobre o centro de colisão do herói
        double hx = context.getHero().getX() + HeroEntity.HW / 2.0;
        double hy = context.getHero().getY() + HeroEntity.HH / 2.0;
        context.getProjectiles().add(new Projectile(Projectile.Type.BOSS_BULLET, hx, hy, -4.5, 0, 1.2, true));
        context.update(2_000_000_000L); // atualiza com tempo futuro para garantir sem invulnerabilidade prévia

        assertTrue(context.getHero().getHp() < initialHp, "Vida do herói deve diminuir ao ser atingido por bola de lava");
        assertTrue(context.getProjectiles().isEmpty(), "Projétil de lava consumido após o impacto");
    }

    @Test
    @DisplayName("Retornar ao menu principal após vitória e iniciar um novo jogo do zero")
    void testReturnToMenuAfterWinningAndStartNewGame() {
        context.startP5();
        BossEntity boss = context.getBoss();
        boss.takeDamage(boss.getHp() - 1);
        context.getProjectiles().add(new Projectile(Projectile.Type.HERO_BUBBLE,
            boss.getX() + 10, boss.getY() + 10, 0, 0, 1, true));
        context.update(0);
        context.getHero().resetPosition(context.getSacredTablet().getWorldX(), context.getSacredTablet().getWorldY());
        context.update(1_000_000L);
        assertEquals(GameContext.State.WIN, context.getState());

        // Pressiona ESPAÇO para voltar ao menu principal
        context.onKey(KeyCode.SPACE);
        assertEquals(GameContext.State.MENU, context.getState());
        assertFalse(context.canContinue(), "Após zerar o jogo, a opção não deve ser continuar a última fase vencida");

        // No menu principal sem continue: opção 1 é Iniciar Jogo (já selecionada automaticamente no returnToMenu)
        context.onKey(KeyCode.ENTER);

        // Deve iniciar um novo jogo na Fase 1
        assertEquals(1, context.getCurrentPhase());
        assertEquals(GameContext.State.P1, context.getState());
        assertEquals(0, context.getTabletsCollected());
    }
}

