package br.apsu.core;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade - InputManager")
public class InputManagerTest {

    private InputManager inputManager;

    @BeforeEach
    void setUp() {
        inputManager = new InputManager();
    }

    @Test
    @DisplayName("Registro e remoção de teclas com tolerância a repetição (grace period)")
    void testKeyPressAndReleaseWithGracePeriod() {
        long t0 = 1_000_000_000L;
        assertFalse(inputManager.isPressed(KeyCode.W));

        inputManager.registerKeyPress(KeyCode.W);
        assertTrue(inputManager.isPressed(KeyCode.W));

        // Release agendado
        inputManager.registerKeyRelease(KeyCode.W, t0);
        // Dentro da janela de 25 ms, continua marcado como pressionado
        inputManager.tick(t0 + 10_000_000L);
        assertTrue(inputManager.isPressed(KeyCode.W), "Tecla deve permanecer pressionada durante o grace period de 25ms");

        // Após expirar a janela (30 ms depois), a tecla é liberada
        inputManager.tick(t0 + 30_000_000L);
        assertFalse(inputManager.isPressed(KeyCode.W), "Tecla deve ser desativada após expirar a janela de debounce");
    }

    @Test
    @DisplayName("Auto-repeat do X11: press recebido dentro da janela cancela release pendente")
    void testX11AutoRepeatDoesNotDropInput() {
        long t0 = 1_000_000_000L;
        inputManager.registerKeyPress(KeyCode.RIGHT);

        // X11 dispara release seguido de press a cada ~30 ms
        for (int i = 0; i < 5; i++) {
            long tickTime = t0 + i * 30_000_000L;
            inputManager.registerKeyRelease(KeyCode.RIGHT, tickTime);
            inputManager.registerKeyPress(KeyCode.RIGHT); // Novo press imediato do repeat
            inputManager.tick(tickTime + 20_000_000L);
            assertTrue(inputManager.isPressed(KeyCode.RIGHT), "Tecla não pode falhar durante repetição contínua");
        }
    }

    @Test
    @DisplayName("Resolução de eixos: direções opostas simultâneas resultam em neutro")
    void testOpposingAxesResultInNeutral() {
        assertEquals(0.0, inputManager.axisX());
        assertEquals(0.0, inputManager.axisY());

        inputManager.registerKeyPress(KeyCode.RIGHT);
        assertEquals(1.0, inputManager.axisX());

        inputManager.registerKeyPress(KeyCode.LEFT);
        assertEquals(0.0, inputManager.axisX(), "Esquerda e direita juntas devem anular o eixo");

        inputManager.registerKeyPress(KeyCode.UP);
        assertEquals(-1.0, inputManager.axisY());

        inputManager.registerKeyPress(KeyCode.DOWN);
        assertEquals(0.0, inputManager.axisY(), "Cima e baixo juntas devem anular o eixo");
    }

    @Test
    @DisplayName("Detecção de teclas de interação e limpeza total com clearKeys")
    void testInteractKeysAndClear() {
        assertFalse(inputManager.isInteractPressed());

        inputManager.registerKeyPress(KeyCode.E);
        assertTrue(inputManager.isInteractPressed());

        inputManager.registerKeyRelease(KeyCode.E, 1_000_000_000L);
        inputManager.clearKeys();
        assertFalse(inputManager.isInteractPressed(), "clearKeys deve limpar teclas ativas e pendências");

        inputManager.registerKeyPress(KeyCode.SPACE);
        assertTrue(inputManager.isInteractPressed());
    }
}
