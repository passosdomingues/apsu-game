package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.core.SaveManager;
import br.apsu.model.environment.Difficulty;
import br.apsu.model.environment.OceanDepthProfile;
import br.apsu.model.hero.HeroEntity;
import br.apsu.model.hero.HeroType;
import javafx.scene.canvas.Canvas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("Testes de Unidade — UIRenderer")
class UIRendererTest {

    @Test
    @DisplayName("Desenha HUD e diálogos com estado vazio e completo")
    void drawsHudAndDialogueVariants() {
        var graphics = new Canvas(1366, 768).getGraphicsContext2D();
        var renderer = new UIRenderer();
        var hero = new HeroEntity(HeroType.GUARDIAO, 5);
        var context = new GameContext(new SaveManager(java.nio.file.Path.of("target/test-save.json")));

        assertDoesNotThrow(() -> renderer.drawHUD(graphics, hero, 0, "Fase 1", Difficulty.MEDIO));
        hero.setHasBubblePower(true);
        assertDoesNotThrow(() -> renderer.drawHUD(graphics, hero, 5, "Fossa", Difficulty.DIFICIL,
            OceanDepthProfile.HADAL_TRENCH));
        assertDoesNotThrow(() -> renderer.drawMinimap(graphics, context, 1366));
        context.startP5();
        assertDoesNotThrow(() -> renderer.drawMinimap(graphics, context, 1366));
        assertDoesNotThrow(() -> renderer.drawDialogueOverlay(graphics, 1366, 768, 0,
            null, null, 0, 0));
        assertDoesNotThrow(() -> renderer.drawDialogueOverlay(graphics, 1366, 768, 0,
            "Enki", new String[]{"Uma mensagem longa para validar a quebra de linha."}, 0, 12));
    }

    @Test
    @DisplayName("Radar converte coordenadas de mundo e respeita seus limites")
    void minimapCoordinateMappingIsClamped() {
        assertEquals(100, UIRenderer.worldToMapX(-10, 4000, 100, 200));
        assertEquals(200, UIRenderer.worldToMapX(2000, 4000, 100, 200));
        assertEquals(300, UIRenderer.worldToMapX(9999, 4000, 100, 200));
    }
}
