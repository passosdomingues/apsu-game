package br.apsu.graphics;

import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — LightingEngine")
public class LightingEngineTest {

    @Test
    @DisplayName("Alternância e estado de efeitos reduzidos para performance")
    void testReducedEffectsState() {
        LightingEngine engine = new LightingEngine();

        // Por padrão reducedEffects é false
        engine.setReducedEffects(true);
        // Garante que o método não lança exceção ao alterar modo de performance
        assertDoesNotThrow(() -> engine.setReducedEffects(false));
    }

    @Test
    @DisplayName("Causticas e halos respeitam os modos de qualidade")
    void drawsCausticsAndCachedHalosInBothQualityModes() {
        LightingEngine engine = new LightingEngine();
        var graphics = new Canvas(1366, 768).getGraphicsContext2D();

        assertDoesNotThrow(() -> engine.drawSunCaustics(graphics, 1366, 768, 3.5));
        assertDoesNotThrow(() -> engine.drawBioluminescentHalo(graphics, 100, 120, 40, Color.CYAN));
        engine.setReducedEffects(true);
        assertDoesNotThrow(() -> engine.drawSunCaustics(graphics, 1366, 768, 4.5));
        assertDoesNotThrow(() -> engine.drawBioluminescentHalo(graphics, 100, 120, 40, null));
    }
}
