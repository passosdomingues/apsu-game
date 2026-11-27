package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.core.InputManager;
import br.apsu.core.SaveManager;
import br.apsu.core.events.EventBus;
import br.apsu.tutorial.TutorialController;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade - VirtualKeyboardOverlay")
class VirtualKeyboardOverlayTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Ja inicializado
        }
    }

    @Test
    @DisplayName("VirtualKeyboardOverlay instancia com todas as teclas obrigatorias mapeadas")
    void overlayInstantiatesWithRequiredKeys() {
        VirtualKeyboardOverlay overlay = new VirtualKeyboardOverlay();
        assertNotNull(overlay);

        Canvas canvas = new Canvas(1366, 768);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        InputManager input = new InputManager();

        // Testa o registro e ativação de teclas especiais (X: skip, P: pause, ENTER, ESC)
        input.registerKeyPress(KeyCode.X);
        input.registerKeyPress(KeyCode.P);
        input.registerKeyPress(KeyCode.ENTER);
        input.registerKeyPress(KeyCode.ESCAPE);

        EventBus bus = new EventBus();
        GameContext context = new GameContext(new SaveManager(Path.of("target/test-kbd-save2.json")), bus);
        context.startP1();

        assertDoesNotThrow(() -> {
            overlay.renderGameplayOverlay(gc, input, context.getTutorialController(), context, 1366, 768, 2.0);
        });
    }

    @Test
    @DisplayName("Renderiza overlay de gameplay no Canvas sem exceções")
    void rendersGameplayOverlayWithoutExceptions() {
        VirtualKeyboardOverlay overlay = new VirtualKeyboardOverlay();
        Canvas canvas = new Canvas(1366, 768);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        InputManager input = new InputManager();
        input.registerKeyPress(KeyCode.W);
        input.registerKeyPress(KeyCode.SPACE);

        EventBus bus = new EventBus();
        GameContext context = new GameContext(new SaveManager(Path.of("target/test-kbd-save.json")), bus);
        context.startP1();

        assertDoesNotThrow(() -> {
            overlay.renderGameplayOverlay(gc, input, context.getTutorialController(), context, 1366, 768, 1.5);
        });
    }

    @Test
    @DisplayName("Renderiza showcase e tutorial do Menu Principal sem exceções")
    void rendersMenuShowcaseWithoutExceptions() {
        VirtualKeyboardOverlay overlay = new VirtualKeyboardOverlay();
        Canvas canvas = new Canvas(1366, 768);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        assertDoesNotThrow(() -> {
            // Testa vários momentos do ciclo de animação
            overlay.renderMenuShowcase(gc, 36, 175, 345, 490, 0.5);
            overlay.renderMenuShowcase(gc, 36, 175, 345, 490, 4.0);
            overlay.renderMenuShowcase(gc, 36, 175, 345, 490, 8.0);
            overlay.renderMenuShowcase(gc, 36, 175, 345, 490, 12.0);
            overlay.renderMenuShowcase(gc, 36, 175, 345, 490, 16.0);
        });
    }
}
