package br.apsu.core;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — InputManager")
public class InputManagerTest {

    private InputManager inputManager;

    @BeforeEach
    void setUp() {
        inputManager = new InputManager();
    }

    @Test
    @DisplayName("Registro e remoção de teclas pressionadas")
    void testKeyPressAndRelease() {
        assertFalse(inputManager.isPressed(KeyCode.W));
        inputManager.registerKeyPress(KeyCode.W);
        assertTrue(inputManager.isPressed(KeyCode.W));

        inputManager.registerKeyRelease(KeyCode.W);
        assertFalse(inputManager.isPressed(KeyCode.W));
    }

    @Test
    @DisplayName("Detecção de teclas de interação (E, ESPAÇO, ENTER, F)")
    void testInteractKeys() {
        assertFalse(inputManager.isInteractPressed());

        inputManager.registerKeyPress(KeyCode.E);
        assertTrue(inputManager.isInteractPressed());
        inputManager.clearKeys();

        inputManager.registerKeyPress(KeyCode.SPACE);
        assertTrue(inputManager.isInteractPressed());
    }
}
