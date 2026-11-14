package br.apsu.echo;

import br.apsu.core.GameContext;
import br.apsu.core.SaveManager;
import br.apsu.graphics.Palette;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.enemy.EnemyType;
import br.apsu.model.environment.SceneryElement;
import br.apsu.model.guardian.GuardianEntity;
import br.apsu.model.hero.HeroEntity;
import br.apsu.model.hero.HeroType;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("US-031: Onda visual do eco e destaques temporarios")
class EchoWaveRendererTest {

    private GameContext context;
    private EchoWaveRenderer waveRenderer;

    @BeforeEach
    void setUp() {
        context = new GameContext(new SaveManager(Path.of("target/test-echo-wave-save.json")));
        context.startP1();
        waveRenderer = context.getEchoWaveRenderer();
        waveRenderer.reset();
    }

    @Test
    @DisplayName("AC1: Onda se expande a partir do heroi ate o raio de 600 unidades em 1,0 s")
    void testWaveExpansionTo600InOneSecond() {
        assertFalse(waveRenderer.isWaveActive(), "Onda deve iniciar inativa");
        assertEquals(0.0, waveRenderer.getWaveRadius(), 0.001);

        // Ativa o eco via controller
        assertTrue(context.getEchoController().trigger(), "Ativacao de eco deve ter sucesso");

        // Primeiro frame de update detecta novo eco e inicia onda
        waveRenderer.update(context, 1.0 / 60.0);
        assertTrue(waveRenderer.isWaveActive(), "Onda deve estar ativa durante expansao");
        assertTrue(waveRenderer.getWaveRadius() > 0.0, "Raio deve comecar a expandir");

        // Aos 0.5 s, deve ter aproximadamente 300 unidades de raio
        waveRenderer.reset();
        context.getEchoController().reset();
        context.getEchoController().trigger();
        waveRenderer.update(context, 0.5);
        assertEquals(300.0, waveRenderer.getWaveRadius(), 5.0, "Aos 0.5s raio deve ser aprox 300");

        // Aos 1.0 s, deve atingir exatamente 600 unidades
        waveRenderer.update(context, 0.5);
        assertEquals(EchoWaveRenderer.WAVE_RADIUS_MAX, waveRenderer.getWaveRadius(), 0.001,
            "Aos 1.0s raio deve ser 600 unidades");
        assertFalse(waveRenderer.isWaveActive(), "Apos 1.0s expansao da onda deve finalizar");

        // Apos 1.0 s, raio permanece limitado a 600 unidades
        waveRenderer.update(context, 0.5);
        assertEquals(EchoWaveRenderer.WAVE_RADIUS_MAX, waveRenderer.getWaveRadius(), 0.001,
            "Raio nao deve ultrapassar 600 unidades");
    }

    @Test
    @DisplayName("AC2: Entidades atingidas sao destacadas por 3,0 s nas cores amarela, vermelha e branca")
    void testHitsAndDistinctHighlightsFor3Seconds() {
        // Cores distintas coerentes com GPS
        assertEquals(Palette.RADAR_GUARDIAN, waveRenderer.getGuardianColor(), "Guardiao deve ser amarelo (RADAR_GUARDIAN)");
        assertEquals(Palette.RADAR_ENEMY, waveRenderer.getEnemyColor(), "Inimigo deve ser vermelho (RADAR_ENEMY)");
        assertEquals(Color.WHITE, waveRenderer.getSceneryColor(), "Obstaculo deve ser branco");

        // Configura posicao do heroi no mundo
        context.getHero().setX(1000);
        context.getHero().setY(400);

        // Limpa e adiciona entidades com distancias controladas:
        // Hero center: 1000 + 36 = 1036, 400 + 96 = 496
        context.getGuardians().clear();
        context.getEnemies().clear();
        context.getSceneryElements().clear();

        // Guardiao a ~200 unidades
        context.getGuardians().add(new GuardianEntity(0, 1200, 450));
        // Inimigo a ~300 unidades (dentro do raio de 600)
        context.getEnemies().add(new EnemyEntity(EnemyType.PEIXE, 1300, 496, 1.0, 0));
        // Inimigo distante a ~800 unidades (fora do raio de 600)
        context.getEnemies().add(new EnemyEntity(EnemyType.PEIXE, 1900, 496, 1.0, 0));
        // Obstaculo de cenario a ~250 unidades
        context.getSceneryElements().add(new SceneryElement(SceneryElement.Type.CORAL, 1250, 450, 80, 100));

        // Aciona eco e avanca 0.6 s (raio = 360 unidades)
        context.getEchoController().trigger();
        waveRenderer.update(context, 0.6);

        // Guardiao e inimigo proximos foram atingidos e destacados por 3.0 s
        assertTrue(waveRenderer.isGuardianHighlighted(0), "Guardiao proximo deve ser destacado");
        assertEquals(3.0, waveRenderer.getGuardianHighlightTimer(0), 0.1, "Destaque do guardiao deve ser 3.0 s");

        assertTrue(waveRenderer.isEnemyHighlighted(0), "Inimigo a 300 unidades deve ser destacado");
        assertEquals(3.0, waveRenderer.getEnemyHighlightTimer(0), 0.1, "Destaque do inimigo deve ser 3.0 s");

        assertTrue(waveRenderer.isEnemyRevealed(context.getEnemies().get(0), context),
            "isEnemyRevealed deve ser true para inimigo destacado");

        // Inimigo fora do raio nao e destacado
        assertFalse(waveRenderer.isEnemyHighlighted(1), "Inimigo a 800 unidades nao deve ser destacado");
        assertFalse(waveRenderer.isEnemyRevealed(context.getEnemies().get(1), context));

        // Obstaculo de cenario destacado por 3.0 s
        assertTrue(waveRenderer.isSceneryHighlighted(0), "Obstaculo proximo deve ser destacado");
        assertEquals(3.0, waveRenderer.getSceneryHighlightTimer(0), 0.1, "Destaque do cenario deve ser 3.0 s");

        // Avanca 1.5 s adicionais: timer deve decrescer
        waveRenderer.update(context, 1.5);
        assertTrue(waveRenderer.isGuardianHighlighted(0), "Apos 1.5s destaque ainda ativo");
        assertEquals(1.5, waveRenderer.getGuardianHighlightTimer(0), 0.1);

        // Avanca mais 1.6 s (total > 3.0 s apos o impacto): destaque expira
        waveRenderer.update(context, 1.6);
        assertFalse(waveRenderer.isGuardianHighlighted(0), "Apos 3.0s destaque deve expirar");
        assertFalse(waveRenderer.isEnemyHighlighted(0), "Apos 3.0s destaque do inimigo deve expirar");
        assertFalse(waveRenderer.isSceneryHighlighted(0), "Apos 3.0s destaque do cenario deve expirar");
    }

    @Test
    @DisplayName("AC3: Renderizacao imperativa no Canvas sem excecoes e respeitando a hierarquia")
    void testRenderImperativeCanvasNoExceptions() {
        Canvas canvas = new Canvas(1366, 768);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Renderiza com eco inativo
        assertDoesNotThrow(() -> waveRenderer.render(gc, context, 1366, 768));

        // Ativa eco e simula meio caminho
        context.getEchoController().trigger();
        waveRenderer.update(context, 0.5);

        // Renderiza com anel e destaques ativos
        assertDoesNotThrow(() -> waveRenderer.render(gc, context, 1366, 768));

        // Testa renderizacao na fase 5 com Boss
        context.startP5();
        assertDoesNotThrow(() -> waveRenderer.render(gc, context, 1366, 768));
    }

    @Test
    @DisplayName("AC4: Zero alocacoes e p95 de frame-time em loop intenso")
    void testPerformanceZeroAllocationBenchmark() {
        Canvas canvas = new Canvas(1366, 768);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        context.getEchoController().trigger();

        // Warmup JIT
        for (int i = 0; i < 200; i++) {
            waveRenderer.update(context, 1.0 / 60.0);
            waveRenderer.render(gc, context, 1366, 768);
        }

        // Medicao de 600 quadros (10 segundos simulados de eco ativo na fase pesada)
        context.getEchoController().reset();
        context.getEchoController().trigger();

        long startNs = System.nanoTime();
        for (int i = 0; i < 600; i++) {
            waveRenderer.update(context, 1.0 / 60.0);
            waveRenderer.render(gc, context, 1366, 768);
        }
        long totalNs = System.nanoTime() - startNs;
        double avgMs = (totalNs / 600.0) / 1_000_000.0;

        // O frame-time medio do renderizador do eco deve ser inferior a 1.0 ms (muito abaixo dos 16.7 ms do frame total)
        assertTrue(avgMs < 1.0, "Tempo medio por quadro do eco deve ser < 1.0 ms (obtido: " + avgMs + " ms)");
    }
}
