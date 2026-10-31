package br.apsu.core;

import java.util.Arrays;

/**
 * Mede o ritmo do loop sem depender de JavaFX. A métrica é intencionalmente
 * pequena para poder ser testada e usada no diagnóstico de performance.
 */
public final class FrameMetrics {
    private static final long ONE_SECOND_NANOS = 1_000_000_000L;
    private static final int MAX_WINDOW_FRAMES = 360;

    public record Sample(double fps, int frames, double p50FrameMs, double p95FrameMs,
                         double worstFrameMs, int framesOver16Ms, int framesOver33Ms) {}

    private long lastFrameNanos = -1;
    private long windowStartNanos = -1;
    private int framesInWindow;
    private double fps;
    private final double[] frameDurationsMs = new double[MAX_WINDOW_FRAMES];
    private int durationCount;
    private Sample lastSample = new Sample(0, 0, 0, 0, 0, 0, 0);

    /**
     * Registra um frame e retorna {@code true} quando uma nova amostra de FPS
     * foi concluída (aproximadamente uma vez por segundo).
     */
    public boolean recordFrame(long now) {
        if (lastFrameNanos < 0) {
            lastFrameNanos = now;
            windowStartNanos = now;
            framesInWindow = 1;
            return false;
        }
        long frameDelta = Math.max(0, now - lastFrameNanos);
        lastFrameNanos = now;
        if (durationCount < frameDurationsMs.length) {
            frameDurationsMs[durationCount++] = frameDelta / 1_000_000.0;
        }
        framesInWindow++;
        long elapsed = now - windowStartNanos;
        if (elapsed >= ONE_SECOND_NANOS) {
            fps = framesInWindow * ONE_SECOND_NANOS / (double) elapsed;
            lastSample = summarizeWindow();
            windowStartNanos = now;
            framesInWindow = 0;
            durationCount = 0;
            return true;
        }
        return false;
    }

    public double getFps() { return fps; }
    public Sample getLastSample() { return lastSample; }
    public boolean hasSample() { return fps > 0; }
    public boolean isBelow(double targetFps) { return hasSample() && fps < targetFps; }

    private Sample summarizeWindow() {
        if (durationCount == 0) return new Sample(fps, framesInWindow, 0, 0, 0, 0, 0);
        double[] sorted = Arrays.copyOf(frameDurationsMs, durationCount);
        Arrays.sort(sorted);
        int over16 = 0, over33 = 0;
        for (int i = 0; i < durationCount; i++) {
            if (frameDurationsMs[i] > 16.67) over16++;
            if (frameDurationsMs[i] > 33.33) over33++;
        }
        return new Sample(fps, framesInWindow,
            percentile(sorted, 0.50), percentile(sorted, 0.95), sorted[sorted.length - 1], over16, over33);
    }

    private static double percentile(double[] sorted, double percentile) {
        int index = (int) Math.ceil(percentile * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }
}
