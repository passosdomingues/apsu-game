package br.apsu.graphics;

import br.apsu.model.environment.SceneryElement;
import javafx.scene.canvas.GraphicsContext;

/**
 * Renderizador procedural 2D para obstaculos de coral na Fase 2 (US-001).
 * Desenha estalactites (descendo do teto, y=0) e estalagmites (subindo do fundo).
 *
 * Criterios de aceitacao:
 * - Tolerancia de no maximo 8 px entre a caixa de colisao AABB e os limites visuais desenhados.
 * - Zero alocacoes por quadro no hot path a 60 FPS (Regra 11).
 */
public final class CoralRenderer {

    // Arrays pre-alocados para reutilizacao sem alocacao no hot path
    private static final double[] X_POINTS = new double[7];
    private static final double[] Y_POINTS = new double[7];

    private CoralRenderer() {}

    /**
     * Renderiza um elemento de coral no canvas.
     *
     * @param gc contexto grafico do Canvas 2D
     * @param elem elemento de cenario do tipo CORAL
     * @param screenX coordenada X ja convertida para a tela
     * @param viewHeight altura da tela/viewport (768)
     */
    public static void renderCoral(GraphicsContext gc, SceneryElement elem, double screenX, double viewHeight) {
        if (elem == null || elem.getType() != SceneryElement.Type.CORAL) {
            return;
        }

        double w = elem.getWidth();
        double h = elem.getHeight();
        double y = elem.getWorldY();

        boolean isStalactite = (y <= 1.0); // pendurada no topo

        if (isStalactite) {
            // Estalactite: base larga no teto (y=0), ponta afinando para baixo (y=h)
            X_POINTS[0] = screenX;              Y_POINTS[0] = 0;
            X_POINTS[1] = screenX + w * 0.15;   Y_POINTS[1] = h * 0.35;
            X_POINTS[2] = screenX + w * 0.30;   Y_POINTS[2] = h * 0.70;
            X_POINTS[3] = screenX + w * 0.50;   Y_POINTS[3] = h; // ponta inferior
            X_POINTS[4] = screenX + w * 0.70;   Y_POINTS[4] = h * 0.70;
            X_POINTS[5] = screenX + w * 0.85;   Y_POINTS[5] = h * 0.35;
            X_POINTS[6] = screenX + w;          Y_POINTS[6] = 0;
        } else {
            // Estalagmite: base larga no fundo (y+h), ponta subindo (y)
            X_POINTS[0] = screenX;              Y_POINTS[0] = y + h;
            X_POINTS[1] = screenX + w * 0.15;   Y_POINTS[1] = y + h * 0.65;
            X_POINTS[2] = screenX + w * 0.30;   Y_POINTS[2] = y + h * 0.30;
            X_POINTS[3] = screenX + w * 0.50;   Y_POINTS[3] = y; // ponta superior
            X_POINTS[4] = screenX + w * 0.70;   Y_POINTS[4] = y + h * 0.30;
            X_POINTS[5] = screenX + w * 0.85;   Y_POINTS[5] = y + h * 0.65;
            X_POINTS[6] = screenX + w;          Y_POINTS[6] = y + h;
        }

        // 1. Sombra base
        gc.setFill(Palette.CORAL_SHADOW);
        gc.fillPolygon(X_POINTS, Y_POINTS, 7);

        // 2. Corpo do coral (ligeiramente inset)
        gc.setFill(Palette.CORAL_BASE);
        gc.fillRoundRect(screenX + 2, y + 2, Math.max(1, w - 4), Math.max(1, h - 4), 8, 8);

        // 3. Destaque de luz / textura de coral
        gc.setStroke(Palette.CORAL_LIGHT);
        gc.setLineWidth(2.0);
        gc.strokePolygon(X_POINTS, Y_POINTS, 7);

        // 4. Detalhe luminoso da ponta
        gc.setFill(Palette.CORAL_TIP);
        if (isStalactite) {
            gc.fillOval(screenX + w * 0.5 - 4, h - 8, 8, 8);
        } else {
            gc.fillOval(screenX + w * 0.5 - 4, y, 8, 8);
        }
    }
}
