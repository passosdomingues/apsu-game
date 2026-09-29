package br.apsu.core;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;

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
    private long previousNow = -1;
    private long simulationNow = -1;
    private long accumulatedNanos;

    public GameLoop(GameContext context, GameRenderer renderer, GraphicsContext gc, double width, double height) {
        this.context = context;
        this.renderer = renderer;
        this.gc = gc;
        this.width = width;
        this.height = height;
    }

    @Override
    public void handle(long now) {
        if (frameMetrics.recordFrame(now)) {
            double fps = frameMetrics.getFps();
            if (fps < REDUCE_EFFECTS_BELOW_FPS && !renderer.isReducedEffects()) {
                renderer.setReducedEffects(true);
                context.getParticleSystem().setReducedEffects(true);
                System.err.printf("[WARN] FPS %.1f: efeitos reduzidos temporariamente para priorizar controle.%n", fps);
            }
        }
        if (previousNow < 0) {
            previousNow = now;
            simulationNow = now;
            context.update(simulationNow);
        } else {
            long elapsed = Math.min(now - previousNow, 250_000_000L);
            previousNow = now;
            accumulatedNanos += Math.max(0, elapsed);
            int steps = 0;
            while (accumulatedNanos >= STEP_NANOS && steps < MAX_STEPS_PER_FRAME) {
                simulationNow += STEP_NANOS;
                context.update(simulationNow);
                accumulatedNanos -= STEP_NANOS;
                steps++;
            }
            // Nunca deixa um frame lento acumular atraso de controle indefinidamente.
            if (steps == MAX_STEPS_PER_FRAME && accumulatedNanos >= STEP_NANOS) accumulatedNanos = 0;
        }
        renderer.render(gc, context, width, height);
    }

    /** Exposto para telemetria futura e inspeção em execução, sem acoplar ao renderer. */
    public FrameMetrics getFrameMetrics() { return frameMetrics; }
}
