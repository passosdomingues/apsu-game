package br.apsu.tools;

import br.apsu.core.GameContext;
import br.apsu.core.SaveManager;
import br.apsu.core.events.EventBus;
import br.apsu.graphics.RenderEngine;
import br.apsu.model.environment.Projectile;
import br.apsu.model.guardian.GuardianEntity;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Ferramenta de Captura e Inspecao Visual das Fases")
public class PhaseInspectionCaptureTest {

    private static final int W = 1366;
    private static final int H = 768;
    private static final Path OUT_DIR = Path.of(".claude/capturas/inspecao");

    @BeforeAll
    static void initJavaFX() throws Exception {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Ja inicializado
        }
        Files.createDirectories(OUT_DIR);
    }

    private static void saveNode(StackPane root, Canvas canvas, String filename) throws Exception {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.BLACK);

        WritableImage fxImage;
        try {
            fxImage = root.snapshot(params, null);
        } catch (Exception e) {
            fxImage = canvas.snapshot(params, null);
        }

        int width = (int) fxImage.getWidth();
        int height = (int) fxImage.getHeight();
        PixelReader pr = fxImage.getPixelReader();
        BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bImage.setRGB(x, y, pr.getArgb(x, y));
            }
        }
        File targetFile = OUT_DIR.resolve(filename).toFile();
        ImageIO.write(bImage, "png", targetFile);
        System.out.println("Screenshot gravado: " + targetFile.getAbsolutePath());
    }

    @Test
    @DisplayName("Capturar todos os pontos-chave de todas as fases")
    void captureAllPhasesKeyMoments() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Canvas canvas = new Canvas(W, H);
                GraphicsContext gc = canvas.getGraphicsContext2D();
                Canvas uiCanvas = new Canvas(W, H);
                uiCanvas.setMouseTransparent(true);
                GraphicsContext uiGc = uiCanvas.getGraphicsContext2D();

                RenderEngine renderer = new RenderEngine();
                StackPane root = new StackPane(canvas, renderer.get3DView(), uiCanvas);
                root.setAlignment(Pos.CENTER);
                root.setStyle("-fx-background-color: black;");
                Scene scene = new Scene(root, W, H, true, SceneAntialiasing.DISABLED);
                scene.setFill(Color.BLACK);

                EventBus bus = new EventBus();
                GameContext context = new GameContext(new SaveManager(Path.of("target/test-capture-save.json")), bus);

                // 1. Menu Principal
                context.update(1_000_000_000L);
                renderer.render(gc, uiGc, context, W, H);
                saveNode(root, canvas, "01_menu_principal.png");

                // 2. Fase 1: Início
                context.startP1();
                advance(context, renderer, gc, uiGc, 60);
                saveNode(root, canvas, "02_fase_1_inicio.png");

                // 3. Fase 1: Ecolocalização acionada (ondas expansivas de sonar)
                context.getEchoController().trigger();
                advance(context, renderer, gc, uiGc, 30); // 0.5s após disparo
                saveNode(root, canvas, "03_fase_1_ecolocalizacao.png");

                // 4. Fase 1: Enki e diálogo mitológico
                for (GuardianEntity g : context.getGuardians()) {
                    context.getHero().setX(g.getWorldX() - 80);
                    context.getHero().setY(g.getWorldY());
                    context.openOverlay(g.getType(), g.getName(), g.getDialogue(), null);
                    break;
                }
                advance(context, renderer, gc, uiGc, 30);
                saveNode(root, canvas, "04_fase_1_enki_dialogo.png");
                context.advanceOverlay();
                context.advanceOverlay();
                context.advanceOverlay();

                // 5. Fase 1: Próximo ao Portal
                context.getHero().setX(3800);
                advance(context, renderer, gc, uiGc, 30);
                saveNode(root, canvas, "05_fase_1_portal_selado.png");

                // 6. Fase 2: Cavernas de Coral e Naufrágio
                context.startP2();
                advance(context, renderer, gc, uiGc, 60);
                saveNode(root, canvas, "06_fase_2_caverna_coral.png");

                // 7. Fase 2: Diálogo com Enki Eremita
                for (GuardianEntity g : context.getGuardians()) {
                    context.getHero().setX(g.getWorldX() - 80);
                    context.getHero().setY(g.getWorldY());
                    context.openOverlay(g.getType(), g.getName(), g.getDialogue(), null);
                    break;
                }
                advance(context, renderer, gc, uiGc, 30);
                saveNode(root, canvas, "07_fase_2_enki_dialogo.png");
                context.advanceOverlay();
                context.advanceOverlay();
                context.advanceOverlay();

                // 8. Fase 3: Correntes Abissais
                context.startP3();
                advance(context, renderer, gc, uiGc, 60);
                saveNode(root, canvas, "08_fase_3_correntes_abissais.png");

                // 9. Fase 4: Abismo Vulcânico (Início e Fundo)
                context.startP4();
                advance(context, renderer, gc, uiGc, 60);
                saveNode(root, canvas, "09_fase_4_abismo_vulcanico.png");

                // 10. Fase 4: Projéteis com Bola de Lava 3D
                context.getHero().setX(1800);
                context.getProjectiles().add(new Projectile(Projectile.Type.ENEMY_MALIGN, 1950, 400, -200, 0, 1.0, false));
                advance(context, renderer, gc, uiGc, 20);
                saveNode(root, canvas, "10_fase_4_projeteis_lava_3d.png");

                // 11. Fase 5: Templo de Apsu e Boss Kullullû com Telegraph
                context.startP5();
                advance(context, renderer, gc, uiGc, 60);
                if (context.getBoss() != null) {
                    context.getBoss().takeDamage(3); // Ativa Fusão Vulcânica / Fase 2
                }
                advance(context, renderer, gc, uiGc, 40);
                saveNode(root, canvas, "11_fase_5_boss_kullullu.png");

            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        assertNull(errorRef.get(), "Ocorreu erro durante a captura visual: " + errorRef.get());

        assertTrue(Files.exists(OUT_DIR.resolve("01_menu_principal.png")));
        assertTrue(Files.exists(OUT_DIR.resolve("04_fase_1_enki_dialogo.png")));
        assertTrue(Files.exists(OUT_DIR.resolve("09_fase_4_abismo_vulcanico.png")));
        assertTrue(Files.exists(OUT_DIR.resolve("11_fase_5_boss_kullullu.png")));
    }

    private void advance(GameContext context, RenderEngine renderer, GraphicsContext gc, GraphicsContext uiGc, int frames) {
        long stepNanos = 1_000_000_000L / 60L;
        for (int i = 0; i < frames; i++) {
            long now = context.getNanoTime() + stepNanos;
            context.update(now);
            renderer.render(gc, uiGc, context, W, H);
        }
    }
}
