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
            "characters/01_adapa_heroi",
            "02_kullullu_boss",
            "scenery/07_navio_naufragado_elemento-cenario",
            "18_delfim_abissal_inimigo",
            "scenery/17_piscina_lava_3d"
        }) {
            ObjModelLoader.Model model = ObjModelLoader.load(modelName, 200, 300,
                (material, path) -> {
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
    void centersBlenderDepthOriginSoPerspectiveDoesNotDisplaceModels() throws Exception {
        ObjModelLoader.Model ship = ObjModelLoader.load("scenery/07_navio_naufragado_elemento-cenario",
            620, 360, (material, path) -> {});
        float minDepth = Float.POSITIVE_INFINITY;
        float maxDepth = Float.NEGATIVE_INFINITY;
        for (var child : ship.node().getChildren()) {
            var mesh = (javafx.scene.shape.TriangleMesh) ((javafx.scene.shape.MeshView) child).getMesh();
            for (int i = 2; i < mesh.getPoints().size(); i += 3) {
                minDepth = Math.min(minDepth, mesh.getPoints().get(i));
                maxDepth = Math.max(maxDepth, mesh.getPoints().get(i));
            }
        }
        assertEquals(0, (minDepth + maxDepth) / 2.0, 0.001,
            "O eixo de profundidade deve ficar centrado na origem do ator");
    }

    @Test
    void diffuseTexturePathsAreNotReusedAsEmissionMaps() throws Exception {
        for (String modelName : new String[]{
            "characters/01_adapa_heroi",
            "characters/02_kullullu_boss",
            "characters/04_peixe_sombrio_inimigo",
            "scenery/06_bau_tesouro_elemento-cenario",
            "scenery/07_navio_naufragado_elemento-cenario"
        }) {
            int[] textureCount = {0};
            ObjModelLoader.load(modelName, 620, 360, (material, path) -> {
                textureCount[0]++;
                assertNotNull(ObjModelLoader.class.getResource(path), modelName + ": mapa MTL ausente " + path);
                assertNull(material.getSelfIlluminationMap(), modelName
                    + ": mapa diffuse não deve virar emissão e apagar o sombreamento");
            });
            assertTrue(textureCount[0] > 0, modelName + " precisa ter texturas diffuse no MTL");
        }
    }

    @Test
    void runtimeWreckDoesNotContainItsDuplicateDecorativeChest() throws Exception {
        try (var stream = ObjModelLoader.class.getResourceAsStream(
            "/models/scenery/07_navio_naufragado_elemento-cenario/07_navio_naufragado_elemento-cenario.obj")) {
            assertNotNull(stream);
            String obj = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertFalse(obj.contains("o corpo_bau"));
            assertFalse(obj.contains("o Topo_bau"));
            assertFalse(obj.contains("o Cube.001"));
        }
        assertNotNull(ObjModelLoader.class.getResource(
            "/models/scenery/06_bau_tesouro_elemento-cenario/06_bau_tesouro_elemento-cenario.obj"),
            "O baú 3D independente precisa ser o único renderizado no naufrágio");
    }

    @Test
    void modelInstancesShareMeshesInsteadOfRebuildingGeometry() throws Exception {
        ObjModelLoader.Model template = ObjModelLoader.load("18_delfim_abissal_inimigo", 1, 1,
            (material, path) -> {});
        ObjModelLoader.Model first = ObjModelLoader.instance(template, 100, 120, javafx.scene.paint.Color.rgb(255, 225, 200));
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
            assertNotSame(source.getMaterial(), firstView.getMaterial(),
                "Cada instância pode receber a paleta da fase sem mutar o template compartilhado");
            var sourceMaterial = (javafx.scene.paint.PhongMaterial) source.getMaterial();
            var firstMaterial = (javafx.scene.paint.PhongMaterial) firstView.getMaterial();
            var secondMaterial = (javafx.scene.paint.PhongMaterial) secondView.getMaterial();
            assertSame(sourceMaterial.getDiffuseMap(), firstMaterial.getDiffuseMap());
            assertSame(sourceMaterial.getDiffuseMap(), secondMaterial.getDiffuseMap());
            var mesh = (javafx.scene.shape.TriangleMesh) source.getMesh();
            assertTrue(mesh.getPoints().size() / 3 < mesh.getFaces().size() / 9 * 3,
                "A malha deve reaproveitar vértices compartilhados entre triângulos");
        }
        assertTrue(first.width() <= 100.001 && first.height() <= 120.001);
        assertTrue(second.width() <= 160.001 && second.height() <= 90.001);
    }

    @Test
    void phasePalettesSeparateWarmFocalAccentsFromCoolCharacterSupport() {
        var hero = Runtime3DLayer.paletteTint(2, "hero-swim");
        var treasure = Runtime3DLayer.paletteTint(2, "chest");
        var enemyInP2 = Runtime3DLayer.paletteTint(2, "enemy-0");
        var enemyInP4 = Runtime3DLayer.paletteTint(4, "enemy-0");
        assertNotEquals(hero, treasure, "O baú deve formar o acento quente da exploração");
        assertNotEquals(enemyInP2, enemyInP4, "Inimigos recebem correção de cor conforme o bioma");
        assertTrue(treasure.getRed() > treasure.getBlue(), "O acento deve permanecer quente e legível");
    }
}
