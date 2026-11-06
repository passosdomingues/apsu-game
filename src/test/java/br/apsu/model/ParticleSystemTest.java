package br.apsu.model;

import br.apsu.model.particle.ParticleSystem;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade - ParticleSystem")
class ParticleSystemTest {

    @Test
    void reducedAndFullQualityRespectTheirBudgetsAndExpireParticles() {
        ParticleSystem particles = new ParticleSystem();
        particles.addBurst(10, 20, Color.CYAN);
        assertEquals(7, particles.size());
        particles.update();
        assertEquals(7, particles.size());

        particles.setReducedEffects(false);
        particles.addBurst(10, 20, Color.GOLD);
        assertEquals(21, particles.size());
        for (int i = 0; i < 41; i++) particles.update();
        assertEquals(0, particles.size());
    }

    @Test
    void bubblesAndBurstsNeverExceedParticleBudgetAndClearResetsIt() {
        ParticleSystem particles = new ParticleSystem();
        for (int i = 0; i < 30; i++) particles.addBurst(i, i, Color.BLUE);
        assertEquals(140, particles.size());
        particles.addBubble(1, 2, 3, 4, Color.WHITE);
        assertEquals(140, particles.size());

        particles.clear();
        particles.addBubble(1, 2, 3, 4, Color.WHITE);
        assertEquals(1, particles.size());
        assertDoesNotThrow(() -> particles.render(new Canvas(100, 100).getGraphicsContext2D()));
    }

    @Test
    void fullQualityAllowsMoreParticlesThenCapsAtItsHigherBudget() {
        ParticleSystem particles = new ParticleSystem();
        particles.setReducedEffects(false);
        for (int i = 0; i < 30; i++) particles.addBurst(i, i, Color.RED);
        assertEquals(320, particles.size());
    }

    @Test
    @DisplayName("Pool deve reutilizar as mesmas instancias de Particle entre rajadas consecutivas")
    void poolReusesSameInstancesBetweenBursts() {
        ParticleSystem particles = new ParticleSystem();
        particles.addBurst(100, 200, Color.CYAN);
        assertEquals(7, particles.size());

        // Guarda as referencias das 7 particulas alocadas no pool
        ParticleSystem.Particle[] firstBatch = new ParticleSystem.Particle[7];
        for (int i = 0; i < 7; i++) {
            firstBatch[i] = particles.getParticle(i);
            assertNotNull(firstBatch[i]);
            assertEquals(100, firstBatch[i].getX(), 0.001);
            assertEquals(200, firstBatch[i].getY(), 0.001);
        }

        // Expira todas as particulas
        for (int i = 0; i < 45; i++) {
            particles.update();
        }
        assertEquals(0, particles.size(), "Todas as particulas devem ter expirado");

        // Nova rajada em outra posicao
        particles.addBurst(300, 400, Color.GOLD);
        assertEquals(7, particles.size());

        // As referencias devem ser EXATAMENTE as mesmas (sem alocacao de novos objetos Particle)
        for (int i = 0; i < 7; i++) {
            ParticleSystem.Particle reused = particles.getParticle(i);
            assertSame(firstBatch[i], reused, "Slot " + i + " deve reutilizar a mesma instancia do pool");
            assertEquals(300, reused.getX(), 0.001, "Posicao x deve ter sido reiniciada");
            assertEquals(400, reused.getY(), 0.001, "Posicao y deve ter sido reiniciada");
        }
    }

    @Test
    @DisplayName("Render de particula com alpha zero ou maximo nao lanca excecao")
    void renderHandlesEdgeAlphaWithoutThrowing() {
        ParticleSystem particles = new ParticleSystem();
        particles.addBubble(10, 10, 0, 0, Color.MAGENTA);
        Canvas canvas = new Canvas(100, 100);
        assertDoesNotThrow(() -> particles.render(canvas.getGraphicsContext2D()));
    }
}
