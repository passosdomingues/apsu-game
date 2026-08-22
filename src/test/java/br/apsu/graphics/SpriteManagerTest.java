package br.apsu.graphics;

import javafx.scene.image.Image;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — SpriteManager")
class SpriteManagerTest {

    @Test
    @DisplayName("Carrega e reutiliza a sequência de nado de Adapa")
    void loadsCachedAnimationSequence() {
        SpriteManager manager = SpriteManager.getInstance();
        Image fallback = manager.getImage("sprites/adapa/01_adapa_heroi.png");

        assertNotNull(fallback, "O sprite estático de fallback deve existir");
        SpriteManager.FrameSeq first = manager.loadSequence(
            "sprites/adapa/01_adapa_heroi", 8.0, fallback);
        SpriteManager.FrameSeq second = manager.loadSequence(
            "sprites/adapa/01_adapa_heroi", 8.0, fallback);

        assertEquals(8, first.frames().size());
        assertSame(first.frames(), second.frames(), "Os frames devem ser reutilizados do cache");
        assertNotNull(first.get(0.25));
    }

    @Test
    @DisplayName("Diretório inexistente mantém fallback e nunca retorna frame inválido")
    void missingSequenceUsesFallbackSafely() {
        SpriteManager manager = SpriteManager.getInstance();
        Image fallback = manager.getImage("sprites/adapa/01_adapa_heroi.png");

        SpriteManager.FrameSeq sequence = manager.loadSequence("sprites/testes/inexistente", 8.0, fallback);

        assertEquals(1, sequence.frames().size());
        assertSame(fallback, sequence.get(3.0));
    }

    @Test
    @DisplayName("Transição de nado mistura frames vizinhos em vez de cortar a textura")
    void blendsAdjacentFramesSmoothly() {
        SpriteManager.FrameSeq sequence = SpriteManager.getInstance().loadSequence(
            "sprites/adapa/01_adapa_heroi", 8.0, null);

        SpriteManager.FrameBlend blend = sequence.blend(0.0625); // metade entre frame 1 e 2 a 8 FPS
        assertNotNull(blend.current());
        assertNotNull(blend.next());
        assertEquals(0.5, blend.nextAlpha(), 0.001);
        assertNotSame(blend.current(), blend.next());
    }
}
