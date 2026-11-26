package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.core.SaveManager;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.enemy.EnemyType;
import br.apsu.model.environment.Difficulty;
import br.apsu.model.environment.Projectile;
import br.apsu.model.particle.ParticleSystem;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("APSU-P17: Viewport Culling e Otimização de Renderização em Tempo Real")
class ViewportCullingTest {

    private GameContext context;

    @BeforeAll
    static void initJavaFX() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @BeforeEach
    void setUp() {
        context = new GameContext(new SaveManager(Path.of("target/test-culling-save.json")));
        context.startP1();
    }

    @Test
    @DisplayName("Inimigos muito além da viewport não disparam projéteis")
    void distantEnemiesDoNotShoot() {
        context.setDifficulty(Difficulty.DIFICIL);
        context.getProjectiles().clear();
        context.getEnemies().clear();

        // Inimigo muito distante (3500px quando câmera está em 0)
        EnemyEntity distantEnemy = new EnemyEntity(EnemyType.PEIXE, 3500, 300, 1.0, 50);
        context.getEnemies().add(distantEnemy);

        // Simula vários frames avançando no tempo
        long t = System.nanoTime();
        for (int i = 0; i < 60; i++) {
            context.update(t + i * 20_000_000L);
        }

        // Não deve ter criado nenhum projétil porque o inimigo está fora da tela
        assertTrue(context.getProjectiles().isEmpty(),
            "Inimigos fora da viewport não devem disparar projéteis");
    }

    @Test
    @DisplayName("ParticleSystem ignora partículas fora da tela sem exceção")
    void particleSystemCullsOffscreenParticles() {
        ParticleSystem ps = new ParticleSystem();
        ps.addBurst(-500, -500, Color.CYAN); // Fora da tela à esquerda/topo
        ps.addBurst(2000, 1000, Color.GOLD); // Fora da tela à direita/baixo
        ps.addBurst(500, 300, Color.RED);    // Dentro da tela

        Canvas canvas = new Canvas(1366, 768);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        assertDoesNotThrow(() -> ps.render(gc), "Renderização com partículas fora da tela não deve lançar exceção");
    }

    @Test
    @DisplayName("RenderEngine renderiza sem erros com entidades fora da tela")
    void renderEngineHandlesOffscreenEntitiesGracefully() {
        RenderEngine engine = new RenderEngine();
        Canvas canvas = new Canvas(1366, 768);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fase 1 com entidades espalhadas por 4000px de mundo
        assertDoesNotThrow(() -> engine.render(gc, context, 1366, 768));

        // Fase 4 pesada com múltiplos corais, lava e inimigos
        context.startP4();
        assertDoesNotThrow(() -> engine.render(gc, context, 1366, 768));
    }
}
