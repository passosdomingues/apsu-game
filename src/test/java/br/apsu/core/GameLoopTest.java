package br.apsu.core;

import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameLoopTest {
    private static final long STEP_NANOS = 1_000_000_000L / 60L;

    @Test
    void fixedStepSimulationAccumulatesElapsedTimeAndAlwaysRenders() {
        GameContext context = new GameContext(new SaveManager(java.nio.file.Path.of("target/loop-test-save.json")));
        RecordingRenderer renderer = new RecordingRenderer();
        GameLoop loop = new GameLoop(context, renderer, null, 1366, 768);

        loop.handle(2_000_000_000L);
        assertEquals(2_000_000_000L, context.getNanoTime());
        loop.handle(2_000_000_000L + STEP_NANOS * 2);

        assertEquals(2_000_000_000L + STEP_NANOS * 2, context.getNanoTime());
        assertEquals(2, renderer.renders);
        assertEquals(1366, renderer.lastWidth);
        assertEquals(768, renderer.lastHeight);
    }

    @Test
    void slowFramesCapCatchupAndEnableReducedEffectsOnce() {
        GameContext context = new GameContext(new SaveManager(java.nio.file.Path.of("target/loop-test-save.json")));
        RecordingRenderer renderer = new RecordingRenderer();
        GameLoop loop = new GameLoop(context, renderer, null, 1366, 768);

        loop.handle(0);
        loop.handle(1_000_000_000L);

        assertEquals(STEP_NANOS * 4, context.getNanoTime(), "No frame pode simular mais de quatro passos atrasados");
        assertTrue(renderer.reducedEffects);
        assertEquals(1, renderer.qualityChanges);
        assertTrue(context.getParticleSystem().isReducedEffects());
        assertEquals(2, renderer.renders);

        loop.handle(1_000_000_000L + STEP_NANOS);
        assertEquals(STEP_NANOS * 5, context.getNanoTime(),
            "Atraso descartado não deve ficar acumulado e congelar frames seguintes");
        assertEquals(3, renderer.renders);
    }

    @Test
    void performanceProfileStartsWithFullEffects() {
        String previousQuality = System.getProperty("apsu.quality");
        try {
            System.setProperty("apsu.quality", "high");
            GameContext context = new GameContext(new SaveManager(java.nio.file.Path.of("target/loop-test-save.json")));
            RecordingRenderer renderer = new RecordingRenderer();
            new GameLoop(context, renderer, null, 1366, 768);

            assertFalse(renderer.reducedEffects);
            assertFalse(context.getParticleSystem().isReducedEffects());
            assertEquals(1, renderer.qualityChanges);
        } finally {
            if (previousQuality == null) System.clearProperty("apsu.quality");
            else System.setProperty("apsu.quality", previousQuality);
        }
    }

    private static final class RecordingRenderer implements GameRenderer {
        private boolean reducedEffects;
        private int qualityChanges;
        private int renders;
        private double lastWidth;
        private double lastHeight;
        @Override public boolean isReducedEffects() { return reducedEffects; }
        @Override public void setReducedEffects(boolean reducedEffects) {
            this.reducedEffects = reducedEffects;
            qualityChanges++;
        }
        @Override public void render(GraphicsContext graphics, GameContext context, double width, double height) {
            renders++;
            lastWidth = width;
            lastHeight = height;
        }
    }
}
