package br.apsu.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveDifficultyTest {
    @Test
    void easesPressureForAPlayerTakingRepeatedDamage() {
        AdaptiveDifficulty adaptive = new AdaptiveDifficulty();
        adaptive.recordDamage();
        adaptive.recordDamage();
        assertTrue(adaptive.getAdjustment(15) < 0);
    }

    @Test
    void increasesPressureForConsistentMasteryAndResetsPerPhase() {
        AdaptiveDifficulty adaptive = new AdaptiveDifficulty();
        for (int i = 0; i < 5; i++) adaptive.recordEnemyDefeated();
        assertTrue(adaptive.getAdjustment(30) > 0);
        adaptive.beginPhase();
        assertEquals(0, adaptive.getDamageEvents());
        assertEquals(0, adaptive.getDefeatedEnemies());
    }
}
