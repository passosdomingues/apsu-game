package br.apsu.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — Camera")
public class CameraTest {

    @Test
    @DisplayName("Rolagem suave da câmera e limites de mapa [0, 4000]")
    void testCameraTrackingAndClamping() {
        Camera camera = new Camera(1366, 4000);

        assertEquals(0, camera.getX());

        // Target hero no início do mapa
        camera.update(100);
        assertEquals(0, camera.getX(), "Câmera não deve rolar abaixo de 0");

        // Target hero no meio do mapa
        camera.update(2000);
        assertTrue(camera.getX() > 0, "Câmera deve acompanhar o herói");
        assertTrue(camera.getX() < 4000 - 1366, "Câmera deve respeitar a largura do mundo");

        // Target hero no final do mapa
        camera.update(4500);
        assertEquals(4000 - 1366, camera.getX(), "Câmera deve travar no limite do mundo (4000 - 1366 = 2634)");
    }

    @Test
    @DisplayName("Tremor visual decai e não altera a posição de mundo da câmera")
    void shakeDecaysWithoutMovingWorldCamera() {
        Camera camera = new Camera(1366, 4000);
        camera.update(2000);
        double trackedX = camera.getX();

        long startedAt = 1_000_000_000L;
        camera.applyShake(12, 200_000_000L, startedAt);

        assertNotEquals(0.0, camera.getShakeX(startedAt + 40_000_000L), 0.0001);
        assertEquals(trackedX, camera.getX(), "Shake não pode alterar a câmera de mundo");

        camera.updateShake(startedAt + 250_000_000L);
        assertEquals(0.0, camera.getShakeX(startedAt + 250_000_000L), 0.0001);
        assertEquals(0.0, camera.getShakeY(startedAt + 250_000_000L), 0.0001);
    }
}
