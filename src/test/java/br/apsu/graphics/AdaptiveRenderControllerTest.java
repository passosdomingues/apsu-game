package br.apsu.graphics;

import br.apsu.core.FrameMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade - AdaptiveRenderController (US-009)")
class AdaptiveRenderControllerTest {

    private String originalQuality;
    private String originalRender;

    @BeforeEach
    void setUp() {
        originalQuality = System.getProperty("apsu.quality");
        originalRender = System.getProperty("apsu.render");
        GraphicsDiagnostics.setScene3dSupportedForTesting(true);
    }

    @AfterEach
    void tearDown() {
        if (originalQuality != null) System.setProperty("apsu.quality", originalQuality);
        else System.clearProperty("apsu.quality");

        if (originalRender != null) System.setProperty("apsu.render", originalRender);
        else System.clearProperty("apsu.render");

        GraphicsDiagnostics.setScene3dSupportedForTesting(null);
    }

    @Test
    @DisplayName("Perfil low seleciona modo hibrido por padrao com heroi/guardiao em 3D e secundarios em 2D")
    void lowProfileSelectsHybridModeByDefault() {
        System.clearProperty("apsu.render");
        System.setProperty("apsu.quality", "low");

        AdaptiveRenderController controller = new AdaptiveRenderController();

        assertEquals(AdaptiveRenderController.Mode.HYBRID, controller.getCurrentMode());
        assertTrue(controller.isSecondaryIn2D(), "Inimigos e props devem estar em 2D no perfil low");
        assertTrue(controller.isHeroIn3D(), "Heroi deve continuar em 3D no modo hibrido");
        assertTrue(controller.isGuardianIn3D(), "Guardiao deve continuar em 3D no modo hibrido");
        assertFalse(controller.isEnemyIn3D(), "Inimigos nao devem renderizar em 3D no modo hibrido");
        assertFalse(controller.isBossIn3D(), "Boss nao deve renderizar em 3D no modo hibrido");
    }

    @Test
    @DisplayName("Computador com GPU dedicada (perfil high ou balanced) mantem 3D completo")
    void dedicatedGpuMaintainsFull3D() {
        System.clearProperty("apsu.render");
        System.setProperty("apsu.quality", "high");

        AdaptiveRenderController controller = new AdaptiveRenderController();

        assertEquals(AdaptiveRenderController.Mode.FULL_3D, controller.getCurrentMode());
        assertFalse(controller.isSecondaryIn2D(), "Inimigos e props devem estar em 3D no modo FULL_3D");
        assertTrue(controller.isHeroIn3D());
        assertTrue(controller.isGuardianIn3D());
        assertTrue(controller.isEnemyIn3D());
        assertTrue(controller.isBossIn3D());
    }

    @Test
    @DisplayName("Propriedade -Dapsu.render prevalece sobre a escolha automatica")
    void explicitRenderPropertyPrevailed() {
        System.setProperty("apsu.quality", "high");

        System.setProperty("apsu.render", "hybrid");
        assertEquals(AdaptiveRenderController.Mode.HYBRID, new AdaptiveRenderController().getCurrentMode());

        System.setProperty("apsu.render", "2d");
        assertEquals(AdaptiveRenderController.Mode.FULL_2D, new AdaptiveRenderController().getCurrentMode());

        System.setProperty("apsu.quality", "low");
        System.setProperty("apsu.render", "3d");
        assertEquals(AdaptiveRenderController.Mode.FULL_3D, new AdaptiveRenderController().getCurrentMode());
    }

    @Test
    @DisplayName("Modo 3D migra para hibrido se p95 > 33ms por 10s seguidos")
    void migratesToHybridAfter10SecondsSlowP95() {
        System.clearProperty("apsu.render");
        System.setProperty("apsu.quality", "high");

        AdaptiveRenderController controller = new AdaptiveRenderController();
        assertEquals(AdaptiveRenderController.Mode.FULL_3D, controller.getCurrentMode());

        FrameMetrics.Sample slowSample = new FrameMetrics.Sample(25.0, 25, 20.0, 36.0, 45.0, 20, 10);
        FrameMetrics.Sample normalSample = new FrameMetrics.Sample(60.0, 60, 16.0, 16.5, 17.0, 1, 0);

        // 9 segundos lentos nao devem migrar ainda
        for (int i = 0; i < 9; i++) {
            controller.recordSample(slowSample);
            assertEquals(AdaptiveRenderController.Mode.FULL_3D, controller.getCurrentMode());
            assertFalse(controller.isSecondaryIn2D());
        }

        // 10o segundo lento ativa migracao para hibrido
        controller.recordSample(slowSample);
        assertEquals(AdaptiveRenderController.Mode.HYBRID, controller.getCurrentMode());
        assertTrue(controller.isSecondaryIn2D());

        // Histerese: mesmo voltando a 60 fps (p95 < 16.7 ms), NAO deve voltar ao 3D completo antes de reiniciar fase
        for (int i = 0; i < 5; i++) {
            controller.recordSample(normalSample);
            assertEquals(AdaptiveRenderController.Mode.HYBRID, controller.getCurrentMode(),
                "Histerese deve manter modo hibrido ate a mudanca de fase");
            assertTrue(controller.isSecondaryIn2D());
        }

        // Ao mudar de fase, o modo 3D e restaurado
        controller.onPhaseChanged(2);
        assertEquals(AdaptiveRenderController.Mode.FULL_3D, controller.getCurrentMode(),
            "Nova fase deve restaurar modo 3D completo configurado");
        assertFalse(controller.isSecondaryIn2D());
    }

    @Test
    @DisplayName("Interrupcao de lentidao antes de 10s reseta o contador de segundos lentos")
    void slowStreakResetByFastSecond() {
        System.clearProperty("apsu.render");
        System.setProperty("apsu.quality", "high");

        AdaptiveRenderController controller = new AdaptiveRenderController();

        FrameMetrics.Sample slowSample = new FrameMetrics.Sample(25.0, 25, 20.0, 36.0, 45.0, 20, 10);
        FrameMetrics.Sample fastSample = new FrameMetrics.Sample(55.0, 55, 17.0, 22.0, 28.0, 5, 0);

        for (int i = 0; i < 8; i++) {
            controller.recordSample(slowSample);
        }
        assertEquals(8, controller.getConsecutiveSlowSeconds());

        // Segundo com p95 <= 33ms reseta contador
        controller.recordSample(fastSample);
        assertEquals(0, controller.getConsecutiveSlowSeconds());
        assertEquals(AdaptiveRenderController.Mode.FULL_3D, controller.getCurrentMode());
    }
}
