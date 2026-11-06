package br.apsu.graphics;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — Palette")
class PaletteTest {

    @Test
    @DisplayName("Cores e gradientes essenciais devem ser validos e nao nulos")
    void testEssentialColorsAndGradientsNotNull() {
        assertNotNull(Palette.GOLD);
        assertNotNull(Palette.GOLD_LIGHT);
        assertNotNull(Palette.CARD_BG);
        assertNotNull(Palette.HUD_BG);
        assertNotNull(Palette.HUD_HP_HIGH);
        assertNotNull(Palette.HUD_HP_HIGH_STROKE);
        assertNotNull(Palette.RADAR_BG);
        assertNotNull(Palette.GRADIENT_MENU);
        assertNotNull(Palette.GRADIENT_DIALOGUE);
        assertNotNull(Palette.GRADIENT_WIN);
        assertNotNull(Palette.GRADIENT_OVER);
    }

    @Test
    @DisplayName("color() deve cachear e retornar a mesma instancia")
    void testColorCache() {
        Color c1 = Palette.color("#ffd700");
        Color c2 = Palette.color("#ffd700");
        assertSame(c1, c2, "Deve retornar a mesma instancia cacheada");
    }

    @Test
    @DisplayName("rgb() deve cachear e retornar a mesma instancia")
    void testRgbCache() {
        Color c1 = Palette.rgb(10, 20, 30, 0.5);
        Color c2 = Palette.rgb(10, 20, 30, 0.5);
        assertSame(c1, c2, "Deve retornar a mesma instancia cacheada");
    }
}
