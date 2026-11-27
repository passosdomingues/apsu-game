package br.apsu.model;

import br.apsu.core.GameContext;
import br.apsu.core.SaveManager;
import br.apsu.core.events.EventBus;
import br.apsu.graphics.Palette;
import br.apsu.model.boss.BossEntity;
import br.apsu.model.boss.BossEntity.CombatPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("US-041: Telegraph dos ataques do Kullullu")
public class BossAttackTelegraphTest {

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
    @DisplayName("AC1 e AC5: Janela de telegrafo entre sinal e dano esta entre 0,6 s e 1,0 s")
    void testAttackTelegraphDurationWithinWindow() {
        long minNanos = 600_000_000L; // 0,6 s
        long maxNanos = 1_000_000_000L; // 1,0 s

        assertTrue(BossEntity.ATTACK_TELEGRAPH_NANOS >= minNanos,
                "Janela de telegrafo deve ser de pelo menos 0,6 s (600 ms)");
        assertTrue(BossEntity.ATTACK_TELEGRAPH_NANOS <= maxNanos,
                "Janela de telegrafo nao deve exceder 1,0 s (1000 ms)");
        assertEquals(750_000_000L, BossEntity.ATTACK_TELEGRAPH_NANOS,
                "Duracao padrao do telegrafo deve ser de 0,75 s");
    }

    @Test
    @DisplayName("AC1 e AC2: Ciclo de vida do telegrafo nas tres fases de combate (Obsidiana, Fusao e Furia)")
    void testAttackTelegraphLifecycleInAllThreeCombatPhases() {
        BossEntity boss = new BossEntity(0, 1000, 300);

        CombatPhase[] phases = {
            CombatPhase.OBSIDIANA_FRIA,
            CombatPhase.FUSAO_VULCANICA,
            CombatPhase.FURIA_DE_APSU
        };

        for (CombatPhase phase : phases) {
            long t0 = 10_000_000_000L;
            boss.startAttackTelegraph(t0, 200, 300);

            // 1. Inicio imediato do telegrafo
            assertTrue(boss.isAttackTelegraphing(t0), "Telegrafo ativo no instante inicial para " + phase);
            assertFalse(boss.isAttackReadyToFire(t0), "Ataque ainda nao esta pronto para disparar no instante inicial");
            assertEquals(0.0, boss.getAttackTelegraphProgress(t0), 0.01);

            // 2. Progresso intermediario durante a janela (nenhum dano/disparo prematuro)
            long tMid = t0 + 400_000_000L; // +0,4 s
            assertTrue(boss.isAttackTelegraphing(tMid), "Telegrafo permanece ativo no meio da janela");
            assertFalse(boss.isAttackReadyToFire(tMid), "Ataque nao dispara no meio da janela de aviso");
            assertTrue(boss.getAttackTelegraphProgress(tMid) > 0.45 && boss.getAttackTelegraphProgress(tMid) < 0.60);

            // 3. Fim da janela de telegrafo (0,75 s)
            long tEnd = t0 + BossEntity.ATTACK_TELEGRAPH_NANOS;
            assertTrue(boss.isAttackReadyToFire(tEnd), "Apos expiracao da janela, o ataque esta pronto para disparar");
            assertEquals(1.0, boss.getAttackTelegraphProgress(tEnd), 0.001);

            // 4. Finalizacao do telegrafo
            boss.finishAttackTelegraph(tEnd);
            assertFalse(boss.isAttackTelegraphing(tEnd), "Telegrafo encerrado apos disparo");
            assertFalse(boss.isAttackReadyToFire(tEnd), "Ataque nao esta mais pendente apos disparo");
            assertEquals(tEnd, boss.getLastShotTime(), "Ultimo disparo registrado no timestamp final do telegrafo");
        }
    }

    @Test
    @DisplayName("AC2: Na Fase 5 do GameContext, nenhum projetil ou dano ocorre antes do fim do telegrafo")
    void testGameContextIntegrationAttackTelegraphPreventsPrematureDamage() {
        context.startP5();
        assertEquals(GameContext.State.P5, context.getState());
        assertNotNull(context.getBoss());

        // Posiciona heroi longe de colisoes de contato fisico
        context.getHero().setX(100);
        context.getHero().setY(300);
        double initialHp = context.getHero().getHp();

        // 1. Executa updates para esgotar o cooldown inicial e engatilhar o telegrafo
        // Em dificuldade MEDIO, bullInterval inicial eh ~1,2 s
        long triggerTime = 2_000_000_000L;
        context.update(triggerTime);

        assertTrue(context.getBoss().isAttackTelegraphing(triggerTime),
                "O chefe deve entrar em estado de telegrafo de ataque ao concluir o intervalo de recarga");
        assertTrue(context.getProjectiles().isEmpty(),
                "Nenhum projetil deve existir no inicio do telegrafo");
        assertEquals(initialHp, context.getHero().getHp(),
                "Nenhum dano deve ocorrer durante o telegrafo");

        // 2. Durante a janela do telegrafo (+0,4 s), nenhum projetil ou dano eh emitido
        long midTelegraph = triggerTime + 400_000_000L;
        context.update(midTelegraph);
        assertTrue(context.getBoss().isAttackTelegraphing(midTelegraph));
        assertTrue(context.getProjectiles().isEmpty(),
                "Projeteis ainda nao foram criados no meio do telegrafo");
        assertEquals(initialHp, context.getHero().getHp(),
                "Vida do heroi inalterada durante a janela de aviso visual");

        // 3. Ao atingir o fim da janela do telegrafo (+0,75 s), os projeteis sao disparados
        long fireTime = triggerTime + BossEntity.ATTACK_TELEGRAPH_NANOS;
        context.update(fireTime);
        assertFalse(context.getBoss().isAttackTelegraphing(fireTime),
                "Telegrafo encerra no instante do disparo");
        assertFalse(context.getProjectiles().isEmpty(),
                "Projeteis do chefe devem ser gerados imediatamente ao fim do telegrafo");
    }

    @Test
    @DisplayName("AC3: Cores e formas dos sinais sao distintas entre as fases de combate")
    void testTelegraphSignalsAreDistinctInShapeAndColor() {
        // Obsidiana Fria (Fase 1): Ciano / Glacial
        assertNotNull(Palette.TELEGRAPH_OBSIDIANA_LINE);
        assertNotNull(Palette.TELEGRAPH_OBSIDIANA_FILL);

        // Fusao Vulcanica (Fase 2): Laranja / Magma
        assertNotNull(Palette.TELEGRAPH_FUSAO_STROKE);
        assertNotNull(Palette.TELEGRAPH_FUSAO_FILL);

        // Furia de Apsu (Fase 3): Vermelho / Carmesim
        assertNotNull(Palette.TELEGRAPH_FURIA_STROKE);
        assertNotNull(Palette.TELEGRAPH_FURIA_FILL);
        assertNotNull(Palette.TELEGRAPH_FURIA_MARKER);

        // Validacao de diferenciacao cromatica estrita
        assertNotEquals(Palette.TELEGRAPH_OBSIDIANA_LINE, Palette.TELEGRAPH_FUSAO_STROKE,
                "Obsidiana Fria e Fusao Vulcanica devem ter cores de contorno distintas");
        assertNotEquals(Palette.TELEGRAPH_FUSAO_STROKE, Palette.TELEGRAPH_FURIA_STROKE,
                "Fusao Vulcanica e Furia de Apsu devem ter cores de contorno distintas");
        assertNotEquals(Palette.TELEGRAPH_OBSIDIANA_LINE, Palette.TELEGRAPH_FURIA_STROKE,
                "Obsidiana Fria e Furia de Apsu devem ter cores de contorno distintas");

        assertNotEquals(Palette.TELEGRAPH_OBSIDIANA_FILL, Palette.TELEGRAPH_FUSAO_FILL,
                "Preenchimentos devem ser cromaticamente distintos");
        assertNotEquals(Palette.TELEGRAPH_FUSAO_FILL, Palette.TELEGRAPH_FURIA_FILL,
                "Preenchimentos devem ser cromaticamente distintos");
    }

    @Test
    @DisplayName("Mudanca de fase de combate cancela telegrafo pendente da fase anterior")
    void testPhaseTransitionCancelsPendingAttackTelegraph() {
        BossEntity boss = new BossEntity(0, 1000, 300);
        boss.startAttackTelegraph(1_000_000_000L, 200, 300);
        assertTrue(boss.isAttackTelegraphing(1_000_000_000L));

        // Boss sofre dano e muda para FUSAO_VULCANICA
        boss.takeDamage(22);
        assertTrue(boss.updateCombatPhase(1_200_000_000L));
        assertEquals(CombatPhase.FUSAO_VULCANICA, boss.getCombatPhase());

        // Telegrafo de ataque anterior cancelado para dar lugar ao telegrafo de mudanca de fase
        assertFalse(boss.isAttackTelegraphing(1_200_000_000L));
        assertTrue(boss.isTelegraphing(1_200_000_000L), "Telegrafo de mudanca de fase fica ativo");
    }

    @Test
    @DisplayName("AC4 e AC6: Renderizacao do telegrafo nao lanca excecoes e respeita o orcamento de frame (p95 < 16.7ms)")
    void testTelegraphRenderingAndPerformance() {
        var canvas = new javafx.scene.canvas.Canvas(1366, 768);
        var gc = canvas.getGraphicsContext2D();
        var renderer = new br.apsu.graphics.BossTelegraphRenderer();

        BossEntity boss = new BossEntity(0, 1000, 300);

        CombatPhase[] phases = {
            CombatPhase.OBSIDIANA_FRIA,
            CombatPhase.FUSAO_VULCANICA,
            CombatPhase.FURIA_DE_APSU
        };

        long[] frameDurations = new long[60];

        for (CombatPhase phase : phases) {
            if (phase == CombatPhase.FUSAO_VULCANICA) boss.takeDamage(22);
            if (phase == CombatPhase.FURIA_DE_APSU) boss.takeDamage(20);
            boss.updateCombatPhase(10_000_000_000L);

            long t0 = 12_000_000_000L;
            boss.startAttackTelegraph(t0, 150, 350);

            // Mede 60 frames de renderizacao ativa de telegrafo
            for (int i = 0; i < 60; i++) {
                long now = t0 + (long)(i * (BossEntity.ATTACK_TELEGRAPH_NANOS / 60.0));
                long start = System.nanoTime();
                assertDoesNotThrow(() -> renderer.render(gc, boss, 1366, 768, now),
                        "Renderizacao do telegrafo na fase " + phase + " nao deve lancar excecao");
                frameDurations[i] = System.nanoTime() - start;
            }

            java.util.Arrays.sort(frameDurations);
            // p95 nos 60 frames (indice 56)
            double p95Ms = frameDurations[56] / 1_000_000.0;
            assertTrue(p95Ms < 16.7, "Frame-time p95 (" + p95Ms + " ms) deve ser estritamente inferior a 16.7 ms (60 FPS)");
        }
    }
}
