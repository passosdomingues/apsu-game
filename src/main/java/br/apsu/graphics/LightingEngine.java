package br.apsu.graphics;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * Motor de iluminação dinâmica e cáusticas de alta performance.
 * Utiliza cache de cores com alpha pre-calculados para zerar alocações por frame.
 */
public class LightingEngine {
    private boolean reducedEffects;
    private static final Color CAUSTIC_BLUE = Color.rgb(140, 210, 255, 0.04);
    private final double[] causticX = new double[4];
    private final double[] causticY = new double[4];

    private final Map<Color, Color> outerAlphaCache = new HashMap<>();
    private final Map<Color, Color> innerAlphaCache = new HashMap<>();

    public void setReducedEffects(boolean reducedEffects) {
        this.reducedEffects = reducedEffects;
    }

    public void drawSunCaustics(GraphicsContext gc, double width, double height, double timeSeconds) {
        int bands = reducedEffects ? 3 : 6;
        gc.setFill(CAUSTIC_BLUE);
        for (int i = 0; i < bands; i++) {
            double sx = 60 + i * 220 + Math.sin(timeSeconds * 0.3 + i) * 22;
            double wave = Math.sin(timeSeconds * 0.5 + i * 0.7) * 15;
            causticX[0] = sx + wave;
            causticX[1] = sx + 60 + wave;
            causticX[2] = sx + 280;
            causticX[3] = sx + 220;
            causticY[0] = causticY[1] = 0;
            causticY[2] = causticY[3] = height;
            gc.fillPolygon(causticX, causticY, 4);
        }
    }

    /**
     * Halo bioluminescente suave com cores cacheadas (zero alocação).
     */
    public void drawBioluminescentHalo(GraphicsContext gc, double cx, double cy, double radius, Color glowColor) {
        if (reducedEffects || glowColor == null) return;

        Color outer = outerAlphaCache.computeIfAbsent(glowColor, c -> c.deriveColor(0, 1, 1, 0.13));
        Color inner = innerAlphaCache.computeIfAbsent(glowColor, c -> c.deriveColor(0, 1, 1, 0.22));

        // Anel externo difuso
        gc.setFill(outer);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        // Núcleo mais brilhante
        double inR = radius * 0.55;
        gc.setFill(inner);
        gc.fillOval(cx - inR, cy - inR, inR * 2, inR * 2);
    }

}
