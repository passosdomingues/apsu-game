package br.apsu.model;

import br.apsu.model.environment.SceneryElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneryElementTest {
    @Test
    void buildersStoreCurrentPressureAndOscillationParameters() {
        SceneryElement current = new SceneryElement(SceneryElement.Type.CURRENT, 20, 30, 40, 50)
            .withCurrent(-2.5, 1.25);
        SceneryElement pressure = new SceneryElement(SceneryElement.Type.PRESSURE_ZONE, 10, 0, 100, 768)
            .withBuoyancy(0.72);
        SceneryElement obstacle = new SceneryElement(SceneryElement.Type.MOVING_OBSTACLE, 100, 200, 20, 30)
            .withOscillation(40, 10, 2, Math.PI / 2);

        assertEquals(-2.5, current.getCurrentVx());
        assertEquals(1.25, current.getCurrentVy());
        assertEquals(0.72, pressure.getBuoyancyMult());
        assertEquals(100, obstacle.getBaseX());
        assertEquals(200, obstacle.getBaseY());
        assertEquals(40, obstacle.getOscAmplX());

        obstacle.updatePosition(0);
        assertEquals(140, obstacle.getWorldX(), 1e-9);
        assertEquals(200 + 10 * Math.cos(Math.PI * 0.65), obstacle.getWorldY(), 1e-9);
    }

    @Test
    void staticElementsStayPutAndCanBeDeactivated() {
        SceneryElement coral = new SceneryElement(SceneryElement.Type.CORAL, 5, 6, 7, 8);
        coral.updatePosition(99);
        assertEquals(5, coral.getWorldX());
        assertEquals(6, coral.getWorldY());
        assertTrue(coral.isActive());
        coral.setActive(false);
        assertFalse(coral.isActive());
    }
}
