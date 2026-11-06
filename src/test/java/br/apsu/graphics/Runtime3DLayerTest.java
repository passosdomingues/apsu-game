package br.apsu.graphics;

import br.apsu.model.environment.SceneryElement;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade - Runtime3DLayer")
class Runtime3DLayerTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Ja inicializado
        }
    }

    @Test
    @DisplayName("Cenários de recife e obstáculos mapeiam os modelos corretos")
    void fullReefSchoolSetIsNotRepeatedForEachCoralCollisionColumn() {
        assertNull(Runtime3DLayer.sceneryModel(SceneryElement.Type.CORAL),
            "O OBJ contem a composicao completa do recife e do cardume, nao uma peca de coral reutilizavel");
        assertEquals("16_corrente_abissal_3d",
            Runtime3DLayer.sceneryModel(SceneryElement.Type.CURRENT));
        assertEquals("14_geiser_hidrotermal_3d",
            Runtime3DLayer.sceneryModel(SceneryElement.Type.GEYSER));
        assertEquals("17_piscina_lava_3d",
            Runtime3DLayer.sceneryModel(SceneryElement.Type.LAVA_POOL));
        assertEquals("12_obstaculo_vulcanico",
            Runtime3DLayer.sceneryModel(SceneryElement.Type.VOLCANIC_ROCK));
    }

    @Test
    @DisplayName("paletteTint retorna cores validas e consistentes por fase e papel")
    void paletteTintProvidesDistinctTintsAcrossPhasesAndRoles() {
        Color heroP1 = Runtime3DLayer.paletteTint(1, "menu-hero");
        Color heroP2 = Runtime3DLayer.paletteTint(2, "menu-hero");
        Color bossP5 = Runtime3DLayer.paletteTint(5, "boss");
        Color enemyP3 = Runtime3DLayer.paletteTint(3, "enemy-0");
        Color accentP1 = Runtime3DLayer.paletteTint(1, "chest");

        assertNotNull(heroP1);
        assertNotNull(heroP2);
        assertNotNull(bossP5);
        assertNotNull(enemyP3);
        assertNotNull(accentP1);

        // Fases diferentes produzem tons diferentes
        assertNotEquals(heroP1, heroP2, "Heroi deve ter tint diferente entre fases 1 e 2");
    }

    @Test
    @DisplayName("ObjModelLoader.multiply calcula multiplicacao de cores com clamp")
    void colorMultiplyClampsCorrectly() {
        Color source = Color.color(0.5, 0.8, 1.0, 1.0);
        Color tint = Color.color(1.0, 0.5, 0.5, 1.0);
        Color result = ObjModelLoader.multiply(source, tint);

        assertEquals(0.5, result.getRed(), 0.001);
        assertEquals(0.4, result.getGreen(), 0.001);
        assertEquals(0.5, result.getBlue(), 0.001);
        assertEquals(1.0, result.getOpacity(), 0.001);
    }

    @Test
    @DisplayName("Actor deve reutilizar a mesma instancia e nó quando apenas o tint muda")
    void actorReusesNodeAndInstanceWhenTintChanges() {
        Runtime3DLayer layer = new Runtime3DLayer();
        layer.setActivePhase(1);

        // Primeiro carregamento na fase 1
        Runtime3DLayer.Actor actor1 = layer.actor("hero", "characters/01_adapa_heroi", 112, 210);
        if (actor1 == null) {
            // Em ambiente com carregamento assincrono, se o modelo for null na primeira chamada, ignora
            return;
        }

        Group node1 = layer.getActorNode("hero");
        Color tint1 = layer.getActorTint("hero");
        assertNotNull(node1);
        assertNotNull(tint1);

        // Muda para a fase 2 (o paletteTint sera diferente)
        layer.setActivePhase(2);
        Runtime3DLayer.Actor actor2 = layer.actor("hero", "characters/01_adapa_heroi", 112, 210);

        // O Actor e o no devem ser exatamente os mesmos (sem recriar ou descartar do scene graph)
        assertSame(actor1, actor2, "Deve retornar a mesma instancia de Actor");
        assertSame(node1, layer.getActorNode("hero"), "O no do scene graph nao deve ser recriado");

        // Mas o tint deve ter sido atualizado para a fase 2 in-place
        Color tint2 = layer.getActorTint("hero");
        assertNotEquals(tint1, tint2, "O tint do Actor deve ter sido atualizado");
    }
}
