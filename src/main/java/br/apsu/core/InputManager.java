package br.apsu.core;

import javafx.scene.input.KeyCode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Gerenciador desacoplado de entrada do teclado com anti-jitter de repetição X11.
 */
public class InputManager {
    public static final long RELEASE_GRACE_NANOS = 25_000_000L; // 25 ms

    private final Set<KeyCode> activeKeys = new HashSet<>();
    private final Map<KeyCode, Long> pendingReleases = new HashMap<>();

    public void registerKeyPress(KeyCode code) {
        if (code == null) return;
        activeKeys.add(code);
        pendingReleases.remove(code);
    }

    public void registerKeyRelease(KeyCode code, long nowNanos) {
        if (code == null) return;
        if (activeKeys.contains(code)) {
            pendingReleases.put(code, nowNanos + RELEASE_GRACE_NANOS);
        }
    }

    public void registerKeyRelease(KeyCode code) {
        registerKeyRelease(code, System.nanoTime());
    }

    public void tick(long nowNanos) {
        if (pendingReleases.isEmpty()) return;
        Iterator<Map.Entry<KeyCode, Long>> it = pendingReleases.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<KeyCode, Long> entry = it.next();
            if (nowNanos >= entry.getValue()) {
                activeKeys.remove(entry.getKey());
                it.remove();
            }
        }
    }

    public boolean isPressed(KeyCode... codes) {
        for (KeyCode c : codes) {
            if (activeKeys.contains(c)) return true;
        }
        return false;
    }

    public double axisX() {
        boolean left = isPressed(KeyCode.LEFT, KeyCode.A);
        boolean right = isPressed(KeyCode.RIGHT, KeyCode.D);
        if (left && right) return 0.0;
        if (left) return -1.0;
        if (right) return 1.0;
        return 0.0;
    }

    public double axisY() {
        boolean up = isPressed(KeyCode.UP, KeyCode.W);
        boolean down = isPressed(KeyCode.DOWN, KeyCode.S);
        if (up && down) return 0.0;
        if (up) return -1.0;
        if (down) return 1.0;
        return 0.0;
    }

    public boolean isInteractPressed() {
        return isPressed(KeyCode.E, KeyCode.SPACE, KeyCode.ENTER, KeyCode.F);
    }

    public void clearKeys() {
        activeKeys.clear();
        pendingReleases.clear();
    }

    public Set<KeyCode> getActiveKeys() {
        return activeKeys;
    }
}
