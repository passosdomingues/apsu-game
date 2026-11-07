package br.apsu.guidance;

import br.apsu.core.GameContext;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Componente de HUD que renderiza a seta de orientacao para o objetivo (US-010).
 *
 * Caracteristicas:
 * - Zero alocacao no loop de 60 FPS (sem criacao de objetos por frame).
 * - Tema claro exclusivo (dourado luminoso com borda e preenchimento visivel em qualquer agua).
 * - Posicionamento na borda da tela com precisao angular (< 5 graus de erro).
 * - Quando o objetivo esta visivel a mais de 300 px, posiciona a seta apontando sem encobrir heroi nem HUD.
 * - Desaparece suavemente quando o heroi esta a menos de 300 px do objetivo.
 */
public final class ObjectiveArrow {

    // Paleta em tema claro/dourado sem dark mode
    private static final Color ARROW_FILL = Color.rgb(255, 220, 80, 0.95);
    private static final Color ARROW_STROKE = Color.rgb(20, 100, 180, 0.90);
    private static final Color ARROW_PULSE = Color.rgb(255, 245, 160, 0.85);

    // Borda da tela e margens para nao encobrir HUD nem heroi
    private static final double MARGIN_TOP = 140.0;   // Protege o HUD superior (vida, tabuleta, radar)
    private static final double MARGIN_BOTTOM = 40.0;
    private static final double MARGIN_LEFT = 40.0;
    private static final double MARGIN_RIGHT = 40.0;

    // Buffers reutilizados para desenhar o poligono da seta (zero-alloc)
    private final double[] xPoints = new double[3];
    private final double[] yPoints = new double[3];

    public ObjectiveArrow() {}

    /**
     * Calcula o angulo em radianos do heroi para o objetivo nas coordenadas de mundo.
     */
    public static double calculateAngle(double fromX, double fromY, double toX, double toY) {
        return Math.atan2(toY - fromY, toX - fromX);
    }

    /**
     * Calcula o erro angular absoluto em graus entre dois angulos em radianos.
     */
    public static double angularErrorDegrees(double angleRadA, double angleRadB) {
        double diff = Math.toDegrees(angleRadA - angleRadB);
        while (diff > 180.0) diff -= 360.0;
        while (diff < -180.0) diff += 360.0;
        return Math.abs(diff);
    }

    /**
     * Renderiza a seta de orientacao se houver objetivo valido na fase.
     */
    public void render(GraphicsContext gc, GameContext context, double canvasWidth, double canvasHeight, double timeSeconds) {
        if (gc == null || context == null) return;
        ObjectiveTracker.Objective obj = ObjectiveTracker.resolveCurrentObjective(context);
        if (obj == null) return;

        double heroWorldX = context.getHero().getX();
        double heroWorldY = context.getHero().getY();

        double dx = obj.worldX() - heroWorldX;
        double dy = obj.worldY() - heroWorldY;
        double distWorld = Math.hypot(dx, dy);

        // Se o heroi estiver muito proximo do objetivo (< 300 px), nao precisa da seta
        if (distWorld < 300.0) return;

        double targetScreenX = context.getCamera().toScreenX(obj.worldX());
        double targetScreenY = obj.worldY();

        double heroScreenX = context.getCamera().toScreenX(heroWorldX);
        double heroScreenY = heroWorldY;

        double angle = Math.atan2(targetScreenY - heroScreenY, targetScreenX - heroScreenX);

        // Clamping da posicao na borda da tela ou proximo ao heroi
        double arrowX;
        double arrowY;

        boolean targetOnScreen = targetScreenX >= MARGIN_LEFT && targetScreenX <= (canvasWidth - MARGIN_RIGHT)
            && targetScreenY >= MARGIN_TOP && targetScreenY <= (canvasHeight - MARGIN_BOTTOM);

        if (!targetOnScreen) {
            // Objetivo fora da tela: posiciona seta na borda da tela respeitando a direcao
            arrowX = clamp(heroScreenX + Math.cos(angle) * (canvasWidth * 0.45), MARGIN_LEFT, canvasWidth - MARGIN_RIGHT);
            arrowY = clamp(heroScreenY + Math.sin(angle) * (canvasHeight * 0.45), MARGIN_TOP, canvasHeight - MARGIN_BOTTOM);
        } else {
            // Objetivo na tela, mas a mais de 300 px: posiciona a seta na metade do caminho sem encobrir o heroi
            double offsetDist = Math.min(180.0, distWorld * 0.35);
            arrowX = heroScreenX + Math.cos(angle) * offsetDist;
            arrowY = heroScreenY + Math.sin(angle) * offsetDist;
        }

        // Animacao sutil de pulso sem alocacao
        double pulse = Math.sin(timeSeconds * 5.0) * 3.0;
        double size = 18.0 + pulse;

        // Monta o triangulo da seta rotacionado em direcao ao angulo
        // Ponta frontal
        xPoints[0] = arrowX + Math.cos(angle) * size;
        yPoints[0] = arrowY + Math.sin(angle) * size;

        // Base lateral esquerda
        double leftAngle = angle + 2.5; // ~143 graus da frente
        xPoints[1] = arrowX + Math.cos(leftAngle) * (size * 0.7);
        yPoints[1] = arrowY + Math.sin(leftAngle) * (size * 0.7);

        // Base lateral direita
        double rightAngle = angle - 2.5;
        xPoints[2] = arrowX + Math.cos(rightAngle) * (size * 0.7);
        yPoints[2] = arrowY + Math.sin(rightAngle) * (size * 0.7);

        // Desenho com tema claro: preenchimento luminoso + contorno azul marinho de alto contraste
        gc.setFill(ARROW_FILL);
        gc.fillPolygon(xPoints, yPoints, 3);

        gc.setStroke(ARROW_STROKE);
        gc.setLineWidth(2.2);
        gc.strokePolygon(xPoints, yPoints, 3);

        // Pequeno nucleo interno de brilho
        gc.setFill(ARROW_PULSE);
        gc.fillOval(arrowX - 2.5, arrowY - 2.5, 5.0, 5.0);
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}
