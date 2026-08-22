package br.apsu.core;

/**
 * Mede o ritmo do loop sem depender de JavaFX. A métrica é intencionalmente
 * pequena para poder ser testada e usada no diagnóstico de performance.
 */
public final class FrameMetrics {
    private static final long ONE_SECOND_NANOS = 1_000_000_000L;

    private long lastFrameNanos = -1;
    private long windowStartNanos = -1;
    private int framesInWindow;
    private double fps;

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
        lastFrameNanos = now;
        framesInWindow++;
        long elapsed = now - windowStartNanos;
        if (elapsed >= ONE_SECOND_NANOS) {
            fps = framesInWindow * ONE_SECOND_NANOS / (double) elapsed;
            windowStartNanos = now;
            framesInWindow = 0;
            return true;
        }
        return false;
    }

    public double getFps() { return fps; }
    public boolean hasSample() { return fps > 0; }
    public boolean isBelow(double targetFps) { return hasSample() && fps < targetFps; }
}
