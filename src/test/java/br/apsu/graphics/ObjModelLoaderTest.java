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

    @Test
    void modelInstancesShareMeshesInsteadOfRebuildingGeometry() throws Exception {
        ObjModelLoader.Model template = ObjModelLoader.load("18_delfim_abissal_inimigo", 1, 1,
            (material, path, emissive) -> {});
        ObjModelLoader.Model first = ObjModelLoader.instance(template, 100, 120);
        ObjModelLoader.Model second = ObjModelLoader.instance(template, 160, 90);

        assertNotSame(first.node(), second.node());
        assertTrue(first.node().getScaleX() > 1 && first.node().getScaleY() > 1,
            "A instância deve preservar a escala aplicada à geometria normalizada");
        assertTrue(second.node().getScaleX() > 1 && second.node().getScaleY() > 1,
            "Cada tamanho solicitado precisa manter sua escala base");
        assertEquals(template.node().getChildren().size(), first.node().getChildren().size());
        for (int i = 0; i < template.node().getChildren().size(); i++) {
            var source = (javafx.scene.shape.MeshView) template.node().getChildren().get(i);
            var firstView = (javafx.scene.shape.MeshView) first.node().getChildren().get(i);
            var secondView = (javafx.scene.shape.MeshView) second.node().getChildren().get(i);
            assertSame(source.getMesh(), firstView.getMesh());
            assertSame(source.getMesh(), secondView.getMesh());
            assertSame(source.getMaterial(), firstView.getMaterial());
            var mesh = (javafx.scene.shape.TriangleMesh) source.getMesh();
            assertTrue(mesh.getPoints().size() / 3 < mesh.getFaces().size() / 9 * 3,
                "A malha deve reaproveitar vértices compartilhados entre triângulos");
        }
        assertTrue(first.width() <= 100.001 && first.height() <= 120.001);
        assertTrue(second.width() <= 160.001 && second.height() <= 90.001);
    }
}
