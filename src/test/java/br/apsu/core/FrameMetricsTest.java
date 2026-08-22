package br.apsu.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameMetricsTest {
    @Test
    void calculatesFpsFromAOneSecondWindow() {
        FrameMetrics metrics = new FrameMetrics();
        metrics.recordFrame(0);
        for (int i = 1; i <= 60; i++) {
            metrics.recordFrame(i * 16_666_667L);
        }

        assertTrue(metrics.hasSample());
        assertEquals(60.0, metrics.getFps(), 1.0);
        assertFalse(metrics.isBelow(55.0));
    }
}
