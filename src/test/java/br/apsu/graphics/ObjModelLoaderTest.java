package br.apsu.graphics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObjModelLoaderTest {
    @Test
    void loadsRepresentativeRuntimeMeshesAndFitsRequestedBounds() throws Exception {
        List<String> textures = new ArrayList<>();
        for (String modelName : new String[]{
            "01_adapa_heroi",
            "02_kullullu_boss",
            "18_delfim_abissal_inimigo",
            "scenery/17_piscina_lava_3d"
        }) {
            ObjModelLoader.Model model = ObjModelLoader.load(modelName, 200, 300,
                (material, path, emissive) -> {
                    textures.add(path);
                    assertNotNull(ObjModelLoader.class.getResource(path), "Textura MTL ausente: " + path);
                });
            assertFalse(model.node().getChildren().isEmpty(), modelName + " precisa de faces renderizáveis");
            assertTrue(model.width() > 0 && model.width() <= 200.001, modelName);
            assertTrue(model.height() > 0 && model.height() <= 300.001, modelName);
        }
        assertFalse(textures.isEmpty(), "Os modelos devem exercitar referências de textura MTL");
    }

    @Test
    void missingModelFailsWithActionableError() {
        assertThrows(java.io.IOException.class, () -> ObjModelLoader.load("missing-model", 100, 100));
    }
}
