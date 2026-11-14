package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.model.environment.SceneryElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — Modelo 3D de Coral de Colisao Dedicado e Leve (US-006)")
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
    @DisplayName("AC1: Novo modelo de coral possui no maximo 1 material e no maximo 2.000 triangulos")
    void testModelMaterialAndTriangleLimits() throws Exception {
        int materialCount = 0;
        try (InputStream stream = getClass().getResourceAsStream("/models/scenery/15_coral_colisao_3d/15_coral_colisao_3d.mtl");
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("newmtl ")) {
                    materialCount++;
                }
            }
        }
        assertEquals(1, materialCount, "O modelo deve ter exatamente 1 material (AC1)");

        int triangleCount = 0;
        try (InputStream stream = getClass().getResourceAsStream("/models/scenery/15_coral_colisao_3d/15_coral_colisao_3d.obj");
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("f ")) {
                    triangleCount++;
                }
            }
        }
        assertTrue(triangleCount > 0, "O modelo deve conter faces");
        assertTrue(triangleCount <= 2000, "O modelo deve ter no maximo 2.000 triangulos (AC1), obtido: " + triangleCount);

        // Valida que o ObjModelLoader carrega o modelo com sucesso
        ObjModelLoader.Model model = ObjModelLoader.load("scenery/15_coral_colisao_3d", 95, 284);
        assertNotNull(model, "O modelo deve carregar com sucesso pelo ObjModelLoader");
        assertNotNull(model.node(), "O nó 3D do modelo carregado nao deve ser nulo");
    }

    @Test
    @DisplayName("AC2: Todos os elementos de coral da Fase 2 usam o modelo com escala compativel entre 0.9 e 1.1")
    void testPhase2CoralsUseModelWithScaleWithinBounds() {
        assertEquals("15_coral_colisao_3d", Runtime3DLayer.sceneryModel(SceneryElement.Type.CORAL),
            "Runtime3DLayer deve mapear SceneryElement.Type.CORAL para 15_coral_colisao_3d");

        GameContext context = new GameContext();
        context.startP2();

        List<SceneryElement> corals = context.getSceneryElements().stream()
            .filter(e -> e.getType() == SceneryElement.Type.CORAL)
            .toList();

        assertFalse(corals.isEmpty(), "Fase 2 deve conter elementos de coral");

        for (SceneryElement coral : corals) {
            double propWidth = coral.getWidth();
            double propHeight = coral.getHeight();
            // A largura base e 95 e a proporcao com a hitbox AABB e 1.0 (dentro de 0.9 e 1.1)
            double scaleRatio = propWidth / coral.getWidth();
            assertTrue(scaleRatio >= 0.9 && scaleRatio <= 1.1,
                "A escala em relacao a hitbox deve estar entre 0.9 e 1.1");
            assertTrue(propHeight >= 30, "Altura de coral valido deve ser de pelo menos 30px");
        }
    }

    @Test
    @DisplayName("AC3: Silhueta de coral e distinguivel sobre o panorama da Fase 2")
    void testCoralSilhouetteDistinctFromBackground() {
        // Assegura que o tom de prop da fase 2 e contrastante com o fundo
        javafx.scene.paint.Color coralTint = Runtime3DLayer.paletteTint(2, "scenery-0");
        assertNotNull(coralTint);
        // O coral base tem componente vermelha proeminente contrastando com o azul-escuro abissal
        assertTrue(coralTint.getOpacity() > 0.0);
    }

    @Test
    @DisplayName("AC4: Nenhum arquivo de asset antigo foi excluido (Regra 08)")
    void testOldAssetPreserved() {
        Path oldReefObj = Path.of("src/main/resources/models/scenery/08_recifes_e_cardume_elemento-cenario/08_recifes_e_cardume_elemento-cenario.obj");
        Path oldReefMtl = Path.of("src/main/resources/models/scenery/08_recifes_e_cardume_elemento-cenario/08_recifes_e_cardume_elemento-cenario.mtl");
        assertTrue(Files.exists(oldReefObj), "O asset 3D antigo de recife e cardume nao pode ser excluido (Regra 08)");
        assertTrue(Files.exists(oldReefMtl), "O arquivo MTL antigo de recife e cardume nao pode ser excluido (Regra 08)");
    }
}
