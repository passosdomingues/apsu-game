package br.apsu.model;

import br.apsu.model.particle.ParticleSystem;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}
