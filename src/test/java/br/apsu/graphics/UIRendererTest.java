package br.apsu.graphics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Testes de Unidade — UIRenderer")
class UIRendererTest {

    @Test
    @DisplayName("Radar converte coordenadas de mundo e respeita seus limites")
    void minimapCoordinateMappingIsClamped() {
        assertEquals(100, UIRenderer.worldToMapX(-10, 4000, 100, 200));
        assertEquals(200, UIRenderer.worldToMapX(2000, 4000, 100, 200));
        assertEquals(300, UIRenderer.worldToMapX(9999, 4000, 100, 200));
    }
}
