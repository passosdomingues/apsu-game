package br.apsu.graphics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade - GraphicsDiagnostics (US-007)")
class GraphicsDiagnosticsTest {

    @Test
    @DisplayName("Formata linha de diagnostico [GFX] com todos os campos")
    void formatDiagnosticContainsAllFields() {
        String line = GraphicsDiagnostics.formatDiagnostic(
            "Linux 6.8.0 (amd64)", "21.0.3+2", true, "ES2Pipeline");

        assertTrue(line.startsWith("[GFX]"));
        assertTrue(line.contains("SO: Linux 6.8.0 (amd64)"));
        assertTrue(line.contains("JavaFX: 21.0.3+2"));
        assertTrue(line.contains("SCENE3D: disponivel"));
        assertTrue(line.contains("Pipeline: ES2Pipeline"));
    }

    @Test
    @DisplayName("Formata linha com SCENE3D indisponivel")
    void formatDiagnosticHandlesUnavailableScene3d() {
        String line = GraphicsDiagnostics.formatDiagnostic(
            "Windows 11 (amd64)", "21.0.3", false, "SWPipeline");

        assertTrue(line.contains("SCENE3D: indisponivel"));
        assertTrue(line.contains("Pipeline: SWPipeline"));
    }

    @Test
    @DisplayName("Tolera campos nulos ou em branco com defaults seguros")
    void formatDiagnosticNullSafe() {
        String line = GraphicsDiagnostics.formatDiagnostic(null, "", false, null);

        assertTrue(line.contains("SO: Desconhecido"));
        assertTrue(line.contains("JavaFX: Desconhecida"));
        assertTrue(line.contains("SCENE3D: indisponivel"));
        assertTrue(line.contains("Pipeline: Desconhecido"));
    }

    @Test
    @DisplayName("Getter e setter de teste para suporte a SCENE3D")
    void scene3dSupportTestToggle() {
        try {
            GraphicsDiagnostics.setScene3dSupportedForTesting(false);
            assertFalse(GraphicsDiagnostics.isScene3dSupported());

            GraphicsDiagnostics.setScene3dSupportedForTesting(true);
            assertTrue(GraphicsDiagnostics.isScene3dSupported());
        } finally {
            GraphicsDiagnostics.setScene3dSupportedForTesting(null);
        }
    }
}
