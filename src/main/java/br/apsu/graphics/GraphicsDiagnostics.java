package br.apsu.graphics;

import java.util.Locale;

/**
 * Diagnostico do subsistema grafico JavaFX e suporte a 3D (US-007).
 */
public final class GraphicsDiagnostics {

    private static volatile Boolean scene3dSupported = null;
    private static volatile String lastDiagnosticLine = "";

    private GraphicsDiagnostics() {}

    /**
     * Formata a linha de diagnostico [GFX] de forma deterministica, testavel sem JavaFX.
     */
    public static String formatDiagnostic(String os, String jfxVersion, boolean scene3d, String pipeline) {
        String safeOs = (os == null || os.isBlank()) ? "Desconhecido" : os.trim();
        String safeJfx = (jfxVersion == null || jfxVersion.isBlank()) ? "Desconhecida" : jfxVersion.trim();
        String safePipe = (pipeline == null || pipeline.isBlank()) ? "Desconhecido" : pipeline.trim();
        return String.format(Locale.ROOT,
            "[GFX] SO: %s | JavaFX: %s | SCENE3D: %s | Pipeline: %s",
            safeOs, safeJfx, scene3d ? "disponivel" : "indisponivel", safePipe);
    }

    /**
     * Detecta o ambiente real de execucao, loga a linha [GFX] e memoriza o suporte a 3D.
     */
    public static synchronized void probeAndLog() {
        String os = System.getProperty("os.name") + " " + System.getProperty("os.version")
            + " (" + System.getProperty("os.arch") + ")";
        String jfxVersion = System.getProperty("javafx.runtime.version",
            System.getProperty("javafx.version", "21.0.3"));

        boolean supported = false;
        try {
            supported = javafx.application.Platform.isSupported(javafx.application.ConditionalFeature.SCENE3D);
        } catch (Throwable ignored) {
            supported = false;
        }
        scene3dSupported = supported;

        String pipeline = resolvePipeline();
        lastDiagnosticLine = formatDiagnostic(os, jfxVersion, supported, pipeline);
        System.out.println(lastDiagnosticLine);
    }

    private static String resolvePipeline() {
        try {
            Class<?> pipelineClass = Class.forName("com.sun.prism.GraphicsPipeline");
            Object pipe = pipelineClass.getMethod("getPipeline").invoke(null);
            if (pipe != null) {
                return pipe.getClass().getSimpleName();
            }
        } catch (Throwable ignored) {
            // Ignorado em ambientes restritos
        }
        String order = System.getProperty("prism.order");
        if (order != null && !order.isBlank()) {
            return "configurado (" + order + ")";
        }
        return "padrao";
    }

    public static boolean isScene3dSupported() {
        // Opcao -Dapsu.render=2d forca o modo 2D Canvas (US-008)
        String renderMode = System.getProperty("apsu.render");
        if ("2d".equalsIgnoreCase(renderMode)) {
            return false;
        }

        if (scene3dSupported == null) {
            try {
                scene3dSupported = javafx.application.Platform.isSupported(javafx.application.ConditionalFeature.SCENE3D);
            } catch (Throwable ignored) {
                scene3dSupported = false;
            }
        }
        return scene3dSupported != null && scene3dSupported;
    }

    public static void setScene3dSupportedForTesting(Boolean supported) {
        scene3dSupported = supported;
    }

    public static String getLastDiagnosticLine() {
        return lastDiagnosticLine;
    }
}
