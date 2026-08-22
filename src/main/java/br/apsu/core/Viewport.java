package br.apsu.core;

/** Mantém o canvas lógico 1366×768 inteiro em qualquer resolução. */
public record Viewport(double scale, double offsetX, double offsetY) {
    public static Viewport fit(double availableWidth, double availableHeight,
                               double logicalWidth, double logicalHeight) {
        if (availableWidth <= 0 || availableHeight <= 0 || logicalWidth <= 0 || logicalHeight <= 0) {
            return new Viewport(1, 0, 0);
        }
        double scale = Math.min(availableWidth / logicalWidth, availableHeight / logicalHeight);
        return new Viewport(scale,
            (availableWidth - logicalWidth * scale) / 2.0,
            (availableHeight - logicalHeight * scale) / 2.0);
    }
}
