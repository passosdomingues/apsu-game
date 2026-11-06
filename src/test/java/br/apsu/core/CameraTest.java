package br.apsu.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade - Camera")
public class CameraTest {

    @Test
    @DisplayName("Rolagem amortecida da câmera e limites de mapa [0, 4000]")
    void testCameraTrackingAndClamping() {
        Camera camera = new Camera(1366, 4000);

        assertEquals(0, camera.getX());

        // Target hero no início do mapa
        camera.update(100);
        assertEquals(0, camera.getX(), "Câmera não deve rolar abaixo de 0");

        // Target hero no meio do mapa (segue com amortecimento)
        camera.update(2000);
        assertTrue(camera.getX() > 0, "Câmera deve acompanhar o herói");
        assertTrue(camera.getX() < 4000 - 1366, "Câmera deve respeitar a largura do mundo");

        // Target hero no final do mapa converge suavemente para o limite
        for (int i = 0; i < 60; i++) {
            camera.update(4500);
        }
        assertEquals(4000 - 1366, camera.getX(), 1.0, "Câmera deve travar no limite do mundo (4000 - 1366 = 2634)");
    }

    @Test
    @DisplayName("Interpolação de render subpixel e snap da câmera")
    void testCameraInterpolationAndSnap() {
        Camera camera = new Camera(1366, 4000);
        camera.snap(1000);
        double startX = camera.getX();

        camera.update(1500, 1.0 / 60.0);
        double currentX = camera.getX();
        assertTrue(currentX > startX);

        double mid = camera.getInterpolatedX(0.5);
        assertEquals(startX + (currentX - startX) * 0.5, mid, 0.0001);
        assertEquals(Math.round(mid), camera.getRenderX(0.5));
    }

    @Test
    @DisplayName("Tremor visual decai e não altera a posição de mundo da câmera")
    void shakeDecaysWithoutMovingWorldCamera() {
        Camera camera = new Camera(1366, 4000);
        camera.snap(2000);
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
