package br.apsu.graphics;

import br.apsu.core.FrameMetrics;
import java.util.Locale;

/**
 * Controlador de modo de renderizacao adaptativo (US-009).
 * Gerencia a alternancia entre 3D completo, Hibrido (Heroi/Guardiao em 3D,
 * inimigos e cenarios secundários em Canvas 2D) e 2D completo.
 */
public final class AdaptiveRenderController {

    public enum Mode {
        FULL_3D,
        HYBRID,
        FULL_2D
    }

    private final Mode initialMode;
    private Mode currentMode;
    private boolean secondaryIn2D;
    private int consecutiveSlowSeconds = 0;
    private int currentPhase = -1;

    public AdaptiveRenderController() {
        this.initialMode = resolveConfiguredMode();
        this.currentMode = this.initialMode;
        this.secondaryIn2D = (currentMode == Mode.HYBRID || currentMode == Mode.FULL_2D);
    }

    /**
     * Resolve o modo de renderizacao com base em flags da JVM e perfil de qualidade.
     * Precedencia:
     * 1. -Dapsu.render=3d | hybrid | 2d
     * 2. Se SCENE3D indisponivel -> FULL_2D
     * 3. Se apsu.quality=low -> HYBRID
     * 4. Caso contrario (GPU dedicada / high / balanced) -> FULL_3D
     */
    public static Mode resolveConfiguredMode() {
        String prop = System.getProperty("apsu.render");
        if (prop != null && !prop.isBlank()) {
            String p = prop.trim().toLowerCase(Locale.ROOT);
            if ("2d".equals(p)) return Mode.FULL_2D;
            if ("hybrid".equals(p) || "hibrido".equals(p)) return Mode.HYBRID;
            if ("3d".equals(p)) return Mode.FULL_3D;
        }

        if (!GraphicsDiagnostics.isScene3dSupported()) {
            return Mode.FULL_2D;
        }

        String quality = System.getProperty("apsu.quality", "balanced");
        if ("low".equalsIgnoreCase(quality)) {
            return Mode.HYBRID;
        }

        return Mode.FULL_3D;
    }

    /**
     * Chamado na mudanca de fase para permitir reset de adaptacao sem oscilacao intra-fase.
     */
    public void onPhaseChanged(int newPhase) {
        if (this.currentPhase != newPhase) {
            this.currentPhase = newPhase;
            // Reseta contador de lentidao para a nova fase
            consecutiveSlowSeconds = 0;
            // Se o modo inicial configurado era FULL_3D, reseta para FULL_3D no inicio da fase
            if (initialMode == Mode.FULL_3D && currentMode != Mode.FULL_3D) {
                currentMode = Mode.FULL_3D;
                secondaryIn2D = false;
                System.out.println("[GFX] Nova fase " + newPhase + ": modo 3D completo restaurado.");
            }
        }
    }

    /**
     * Processa a amostra de frame-time do segundo anterior.
     * Criterio: Se no modo 3D completo p95 > 33 ms por 10 s seguidos, migra para Hibrido (elementos secundarios em 2D).
     * Previne oscilacao: se p95 cair para < 16.7 ms, NAO volta ao 3D completo durante a mesma fase.
     */
    public void recordSample(FrameMetrics.Sample sample) {
        if (sample == null || currentMode != Mode.FULL_3D) {
            return;
        }

        // Amostra valida com frames medidos
        if (sample.frames() > 0 && sample.p95FrameMs() > 33.0) {
            consecutiveSlowSeconds++;
            if (consecutiveSlowSeconds >= 10) {
                downgradeToHybrid();
            }
        } else {
            consecutiveSlowSeconds = 0;
        }
    }

    private void downgradeToHybrid() {
        currentMode = Mode.HYBRID;
        secondaryIn2D = true;
        consecutiveSlowSeconds = 0;
        System.out.println("[GFX] Desempenho critico (p95 > 33ms por 10s): migrando elementos secundarios (inimigos/props) para 2D.");
    }

    public Mode getCurrentMode() {
        return currentMode;
    }

    public boolean isSecondaryIn2D() {
        return secondaryIn2D || currentMode == Mode.FULL_2D;
    }

    public boolean isHeroIn3D() {
        return currentMode != Mode.FULL_2D && GraphicsDiagnostics.isScene3dSupported();
    }

    public boolean isGuardianIn3D() {
        return currentMode != Mode.FULL_2D && GraphicsDiagnostics.isScene3dSupported();
    }

    public boolean isEnemyIn3D() {
        return currentMode == Mode.FULL_3D && !secondaryIn2D;
    }

    public boolean isBossIn3D() {
        return currentMode == Mode.FULL_3D && !secondaryIn2D;
    }

    public int getConsecutiveSlowSeconds() {
        return consecutiveSlowSeconds;
    }
}
