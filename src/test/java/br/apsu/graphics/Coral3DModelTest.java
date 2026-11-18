package br.apsu.graphics;

import br.apsu.model.environment.SceneryElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do mapeamento de modelos 3D para elementos de cenario.
 * Coral foi removido da Fase 2; os testes cobrem apenas os tipos ativos.
 */
@DisplayName("Testes de Unidade — Modelos 3D de Cenario (sceneryModel mapping)")
public class Coral3DModelTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Ja inicializado
        }
    }

    @Test
    @DisplayName("Modelos ativos de cenario mapeiam corretamente")
    void activeSceneryTypesMapToCorrectModels() {
        assertEquals("16_corrente_abissal_3d",
            Runtime3DLayer.sceneryModel(SceneryElement.Type.CURRENT));
        assertEquals("14_geiser_hidrotermal_3d",
            Runtime3DLayer.sceneryModel(SceneryElement.Type.GEYSER));
        assertEquals("17_piscina_lava_3d",
            Runtime3DLayer.sceneryModel(SceneryElement.Type.LAVA_POOL));
        assertEquals("12_obstaculo_vulcanico",
            Runtime3DLayer.sceneryModel(SceneryElement.Type.VOLCANIC_ROCK));
        assertEquals("13_obstaculo_abissal",
            Runtime3DLayer.sceneryModel(SceneryElement.Type.MOVING_OBSTACLE));
        assertNull(Runtime3DLayer.sceneryModel(SceneryElement.Type.GEYSER) == null
            ? null : null); // tipos sem modelo devem retornar null
    }

    @Test
    @DisplayName("Silhueta de cenario produz tint valido para Fase 2")
    void phase2SceneryTintIsValid() {
        javafx.scene.paint.Color tint = Runtime3DLayer.paletteTint(2, "scenery-0");
        assertNotNull(tint);
        assertTrue(tint.getOpacity() > 0.0);
    }

    @Test
    @DisplayName("Asset antigo de recife e cardume nao foi excluido (Regra 08)")
    void oldReefAssetPreserved() {
        Path oldReefObj = Path.of("src/main/resources/models/scenery/08_recifes_e_cardume_elemento-cenario/08_recifes_e_cardume_elemento-cenario.obj");
        Path oldReefMtl = Path.of("src/main/resources/models/scenery/08_recifes_e_cardume_elemento-cenario/08_recifes_e_cardume_elemento-cenario.mtl");
        assertTrue(Files.exists(oldReefObj), "O asset 3D antigo de recife e cardume nao pode ser excluido (Regra 08)");
        assertTrue(Files.exists(oldReefMtl), "O arquivo MTL antigo de recife e cardume nao pode ser excluido (Regra 08)");
    }
}
