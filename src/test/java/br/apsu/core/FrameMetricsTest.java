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

    @Test
    void summarizesFramePacingAndCountsMissedBudgets() {
        FrameMetrics metrics = new FrameMetrics();
        metrics.recordFrame(0);
        for (int i = 1; i <= 60; i++) metrics.recordFrame(i * 20_000_000L);

        FrameMetrics.Sample sample = metrics.getLastSample();
        assertEquals(20.0, sample.p50FrameMs(), 0.01);
        assertEquals(20.0, sample.p95FrameMs(), 0.01);
        assertEquals(20.0, sample.worstFrameMs(), 0.01);
        assertEquals(50, sample.framesOver16Ms());
        assertEquals(0, sample.framesOver33Ms());
    }

    @Test
    void countsSevereStallsSeparatelyFromOrdinarySlowFrames() {
        FrameMetrics metrics = new FrameMetrics();
        metrics.recordFrame(0);
        long now = 0;
        for (int i = 1; i <= 60; i++) {
            now += i == 8 ? 80_000_000L : 16_000_000L;
            metrics.recordFrame(now);
        }

        assertTrue(metrics.getLastSample().worstFrameMs() >= 80.0);
        assertEquals(1, metrics.getLastSample().framesOver33Ms());
    }
}
