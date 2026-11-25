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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertDoesNotThrow(() -> renderer.drawHUD(graphics, hero, 1, "Fase 1", Difficulty.FACIL,
            OceanDepthProfile.COASTAL, 0.5, "Fale com o guardião"));
        assertDoesNotThrow(() -> renderer.drawMinimap(graphics, context, 1366));
        context.startP5();
        assertDoesNotThrow(() -> renderer.drawMinimap(graphics, context, 1366));
        assertDoesNotThrow(() -> renderer.drawDialogueOverlay(graphics, 1366, 768, 0,
            null, null, 0, 0));
        assertDoesNotThrow(() -> renderer.drawDialogueOverlay(graphics, 1366, 768, 0,
            "Enki", new String[]{"Uma mensagem longa para validar a quebra de linha."}, 0, 12));
    }

    @Test
    @DisplayName("APSU-U02: Dica de teclas realiza fade-out suave apos movimentacao confirmada")
    void keysHintFadesOutOnMovement() {
        var graphics = new Canvas(1366, 768).getGraphicsContext2D();
        var renderer = new UIRenderer();
        var hero = new HeroEntity(HeroType.GUARDIAO, 5);

        assertEquals(1.0, renderer.getKeysHintAlpha(), 0.001);
        // Sem movimento
        hero.setVx(0.0);
        hero.setVy(0.0);
        renderer.drawHUD(graphics, hero, 0, "Fase 1", Difficulty.MEDIO);
        assertEquals(1.0, renderer.getKeysHintAlpha(), 0.001);

        // Heroi se move
        hero.setVx(150.0);
        renderer.drawHUD(graphics, hero, 0, "Fase 1", Difficulty.MEDIO);
        assertTrue(renderer.isKeysMovementConfirmed());
        assertTrue(renderer.getKeysHintAlpha() < 1.0);

        // Varios frames simulam recolhimento completo
        for (int i = 0; i < 70; i++) {
            renderer.drawHUD(graphics, hero, 0, "Fase 1", Difficulty.MEDIO);
        }
        assertEquals(0.0, renderer.getKeysHintAlpha(), 0.001);

        // Reset restaura estado
        renderer.resetKeysHint();
        assertEquals(1.0, renderer.getKeysHintAlpha(), 0.001);
    }

    @Test
    @DisplayName("Radar converte coordenadas de mundo e respeita seus limites")
    void minimapCoordinateMappingIsClamped() {
        assertEquals(100, UIRenderer.worldToMapX(-10, 4000, 100, 200));
        assertEquals(200, UIRenderer.worldToMapX(2000, 4000, 100, 200));
        assertEquals(300, UIRenderer.worldToMapX(9999, 4000, 100, 200));
    }

    @Test
    @DisplayName("Dica orgânica de ecolocalização Q é desenhada quando próximo da tabuleta oculta (APSU-U03)")
    void echoPromptRendersWhenNearHiddenTablet() {
        var graphics = new Canvas(1366, 768).getGraphicsContext2D();
        var renderer = new UIRenderer();
        var context = new GameContext();
        context.startP1();
        // Na Fase 1, tabuleta em 2350, 580
        // Posiciona herói longe
        context.getHero().setX(100);
        context.getHero().setY(100);
        assertDoesNotThrow(() -> renderer.drawEchoPrompt(graphics, context, 1366, 768, 1.0));

        // Posiciona herói perto (a 200 unidades)
        context.getHero().setX(2250);
        context.getHero().setY(550);
        assertDoesNotThrow(() -> renderer.drawEchoPrompt(graphics, context, 1366, 768, 1.0));

        // Após revelada, não desenha prompt
        context.getSacredTablet().reveal();
        assertDoesNotThrow(() -> renderer.drawEchoPrompt(graphics, context, 1366, 768, 1.0));
    }
}
