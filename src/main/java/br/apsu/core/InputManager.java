package br.apsu.core;

import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;

/**
 * Gerenciador desacoplado de entrada do teclado.
 */
public class InputManager {
    private final Set<KeyCode> activeKeys = new HashSet<>();

    public void registerKeyPress(KeyCode code) {
        activeKeys.add(code);
    }

    public void registerKeyRelease(KeyCode code) {
        activeKeys.remove(code);
    }

    public boolean isPressed(KeyCode... codes) {
        for (KeyCode c : codes) {
            if (activeKeys.contains(c)) return true;
        }
        return false;
    }

    public boolean isInteractPressed() {
        return isPressed(KeyCode.E, KeyCode.SPACE, KeyCode.ENTER, KeyCode.F);
    }

    public void clearKeys() {
        activeKeys.clear();
    }

    public Set<KeyCode> getActiveKeys() {
        return activeKeys;
    }
}
