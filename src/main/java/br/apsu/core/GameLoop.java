package br.apsu.core;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import java.util.Locale;

/**
 * Loop monotônico de animação e atualização VSync 60 FPS.
 */
public class GameLoop extends AnimationTimer {

    private final GameContext context;
    private final GameRenderer renderer;
    private final GraphicsContext gc;
    private final double width, height;
    private final FrameMetrics frameMetrics = new FrameMetrics();
    private static final long STEP_NANOS = 1_000_000_000L / 60L;
    private static final int MAX_STEPS_PER_FRAME = 4;
    private static final double REDUCE_EFFECTS_BELOW_FPS = 50.0;
    private static final boolean PERF_LOG = Boolean.getBoolean("apsu.perf");
    private long previousNow = -1;
    private long simulationNow = -1;
    private long accumulatedNanos;
    private long profileFrames, profileUpdateNanos, profileRenderNanos, profileSteps;

    public GameLoop(GameContext context, GameRenderer renderer, GraphicsContext gc, double width, double height) {
        this.context = context;
        this.renderer = renderer;
        this.gc = gc;
        this.width = width;
        this.height = height;
    }

    @Override
    public void handle(long now) {
        boolean fpsSampleReady = frameMetrics.recordFrame(now);
        if (fpsSampleReady) {
            double fps = frameMetrics.getFps();
            if (PERF_LOG && profileFrames > 0) {
                System.out.printf(Locale.ROOT,
                    "[PERF] %.1f fps | update %.2f ms | render %.2f ms | %.2f sim/frame%n",
                    fps, profileUpdateNanos / (double) profileFrames / 1_000_000.0,
                    profileRenderNanos / (double) profileFrames / 1_000_000.0,
                    profileSteps / (double) profileFrames);
                profileFrames = profileUpdateNanos = profileRenderNanos = profileSteps = 0;
            }
            if (fps < REDUCE_EFFECTS_BELOW_FPS && !renderer.isReducedEffects()) {
                renderer.setReducedEffects(true);
                context.getParticleSystem().setReducedEffects(true);
                System.err.printf("[WARN] FPS %.1f: efeitos reduzidos temporariamente para priorizar controle.%n", fps);
            }
        }
        long updateStart = PERF_LOG ? System.nanoTime() : 0;
        int steps;
        if (previousNow < 0) {
            previousNow = now;
            simulationNow = now;
            context.update(simulationNow);
            steps = 1;
        } else {
            long elapsed = Math.min(now - previousNow, 250_000_000L);
            previousNow = now;
            accumulatedNanos += Math.max(0, elapsed);
            steps = 0;
            while (accumulatedNanos >= STEP_NANOS && steps < MAX_STEPS_PER_FRAME) {
                simulationNow += STEP_NANOS;
                context.update(simulationNow);
                accumulatedNanos -= STEP_NANOS;
                steps++;
            }
            // Nunca deixa um frame lento acumular atraso de controle indefinidamente.
            if (steps == MAX_STEPS_PER_FRAME && accumulatedNanos >= STEP_NANOS) accumulatedNanos = 0;
        }
        if (PERF_LOG) profileUpdateNanos += System.nanoTime() - updateStart;
        long renderStart = PERF_LOG ? System.nanoTime() : 0;
        renderer.render(gc, context, width, height);
        if (PERF_LOG) {
            profileRenderNanos += System.nanoTime() - renderStart;
            profileSteps += steps;
            profileFrames++;
        }
    }

    /** Exposto para telemetria futura e inspeção em execução, sem acoplar ao renderer. */
    public FrameMetrics getFrameMetrics() { return frameMetrics; }
}
