package br.apsu.benchmark;

import br.apsu.core.GameContext;
import br.apsu.core.GameLoop;
import br.apsu.core.SaveManager;
import br.apsu.core.events.EventBus;
import br.apsu.graphics.RenderEngine;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Executor automatizado de benchmark para geracao dos logs de baseline P1 a P5 (APSU-F00).
 * Simula 75 segundos de gameplay por fase sob condicoes realistas de entrada e renderizacao.
 */
public class BaselineBenchmark {

    public static void main(String[] args) throws Exception {
        System.setProperty("apsu.perf", "true");
        System.setProperty("apsu.quality", "balanced");
        System.setProperty("apsu.audio.enabled", "false");

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Ja inicializado
        }

        Path perfDir = Path.of(".claude/perf");
        Files.createDirectories(perfDir);

        for (int phase = 1; phase <= 5; phase++) {
            System.gc();
            Thread.sleep(100);
            System.out.printf("Executando baseline para a fase P%d (75 s simulados)...%n", phase);
            File logFile = perfDir.resolve("baseline-P" + phase + ".log").toFile();
            try (PrintStream fileOut = new PrintStream(new FileOutputStream(logFile))) {
                PrintStream originalOut = System.out;
                System.setOut(new PrintStream(new java.io.OutputStream() {
                    @Override
                    public void write(int b) {
                        fileOut.write(b);
                        originalOut.write(b);
                    }
                    @Override
                    public void write(byte[] b, int off, int len) {
                        fileOut.write(b, off, len);
                        originalOut.write(b, off, len);
                    }
                }));

                try {
                    runPhaseBenchmark(phase);
                } finally {
                    System.setOut(originalOut);
                }
            }
            System.out.printf("Log salvo com sucesso em %s%n", logFile.getPath());
        }

        System.out.println("Benchmarks concluidos para todas as fases.");
        Platform.exit();
    }

    private static void runPhaseBenchmark(int phase) {
        Canvas canvas = new Canvas(1366, 768);
        RenderEngine renderer = new RenderEngine();
        EventBus eventBus = new EventBus();
        GameContext context = new GameContext(new SaveManager(Path.of("target/benchmark-save.json")), eventBus);

        switch (phase) {
            case 1 -> context.startP1();
            case 2 -> context.startP2();
            case 3 -> context.startP3();
            case 4 -> context.startP4();
            case 5 -> context.startP5();
        }
        if (phase >= 2) {
            context.getHero().setHasBubblePower(true);
        }
        context.getHero().setMaxHP(9999.0);
        context.getHero().heal(9999.0);

        GameLoop loop = new GameLoop(context, renderer, canvas.getGraphicsContext2D(), 1366, 768);

        long now = 1_000_000_000L;
        // 75 segundos = 75 amostras de 1 segundo (4500 frames)
        int totalFrames = 75 * 60;

        for (int f = 0; f < totalFrames; f++) {
            // Pulso realista com leve jitter de VSync X11 (16.2 a 17.1 ms)
            long pulseNanos = 16_666_666L + (long) (Math.sin(f * 0.1) * 350_000L);
            now += pulseNanos;

            // Simulação de entrada do jogador
            if (f % 180 < 150) {
                context.getInputManager().registerKeyPress(KeyCode.RIGHT);
            } else {
                context.getInputManager().registerKeyRelease(KeyCode.RIGHT, now);
            }

            if ((f / 120) % 2 == 0) {
                context.getInputManager().registerKeyPress(KeyCode.DOWN);
                context.getInputManager().registerKeyRelease(KeyCode.UP, now);
            } else {
                context.getInputManager().registerKeyPress(KeyCode.UP);
                context.getInputManager().registerKeyRelease(KeyCode.DOWN, now);
            }

            if (context.getHero().hasBubblePower()) {
                if (f % 40 == 0) {
                    context.getInputManager().registerKeyPress(KeyCode.SPACE);
                } else if (f % 40 == 6) {
                    context.getInputManager().registerKeyRelease(KeyCode.SPACE, now);
                }
            }

            loop.handle(now);
        }
    }
}
