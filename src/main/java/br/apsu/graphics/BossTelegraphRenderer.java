package br.apsu.graphics;

import br.apsu.model.boss.BossEntity;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.StrokeLineCap;

/**
 * Renderizador de telegrafos visuais dedicados para os ataques de Kullullu (US-041).
 *
 * Garante sinal antecipado e legivel de 0,6 s a 1,0 s antes de cada ataque:
 * - Obsidiana Fria: Cone / leque de mira direcional ciano apontando na trajetoria das bolhas.
 * - Fusao Vulcanica: Anel circular concentrico expansivo laranja ao redor do corpo do chefe.
 * - Furia de Apsu: Colunas verticais translúcidas vermelhas marcando as coordenadas de impacto dos meteoros.
 *
 * Alocacao zero no hot path (Regra 11) e nao obstrui HUD nem a silhueta do heroi (AC4).
 */
public class BossTelegraphRenderer {

    // Buffers pre-alocados para desenho do cone e marcadores sem instanciar arrays por frame (Regra 11)
    private final double[] fanX = new double[24];
    private final double[] fanY = new double[24];
    private final double[] markerX = new double[3];
    private final double[] markerY = new double[3];

    public void render(GraphicsContext gc, BossEntity boss, double width, double height, long nanoTime) {
        if (boss == null || boss.isDead() || !boss.isAttackTelegraphing(nanoTime)) {
            return;
        }

        double progress = boss.getAttackTelegraphProgress(nanoTime);
        double bfx = boss.getX();
        double bfy = boss.getY() + BossEntity.BH / 2.0;

        gc.save();
        gc.setLineCap(StrokeLineCap.ROUND);

        switch (boss.getCombatPhase()) {
            case OBSIDIANA_FRIA -> renderObsidianaTelegraph(gc, boss, bfx, bfy, progress);
            case FUSAO_VULCANICA -> renderFusaoTelegraph(gc, boss, bfx + BossEntity.BW / 2.0, bfy, progress);
            case FURIA_DE_APSU -> renderFuriaTelegraph(gc, boss, height, progress);
        }

        gc.restore();
    }

    /**
     * Obsidiana Fria: Cone / leque direcional ciano que se estende do chefe ate a area do heroi.
     */
    private void renderObsidianaTelegraph(GraphicsContext gc, BossEntity boss, double bfx, double bfy, double progress) {
        double tx = boss.getAttackTargetX() - bfx;
        double ty = boss.getAttackTargetY() - bfy;
        double baseAngle = Math.atan2(ty, tx);
        double spread = BossEntity.SPREAD[Math.min(boss.getVariant(), BossEntity.SPREAD.length - 1)];

        double dist = Math.sqrt(tx * tx + ty * ty);
        // O cone para antes de cobrir o centro do heroi (AC4)
        double range = Math.max(220.0, Math.min(500.0, dist - 50.0));

        double startAngle = baseAngle - spread;
        double endAngle = baseAngle + spread;
        int segments = 16;
        double angleStep = (endAngle - startAngle) / segments;

        fanX[0] = bfx;
        fanY[0] = bfy;
        for (int i = 0; i <= segments; i++) {
            double a = startAngle + i * angleStep;
            fanX[i + 1] = bfx + Math.cos(a) * range;
            fanY[i + 1] = bfy + Math.sin(a) * range;
        }

        // Preenchimento suave do cone
        gc.setFill(Palette.TELEGRAPH_OBSIDIANA_FILL);
        gc.fillPolygon(fanX, fanY, segments + 2);

        // Borda e linhas de mira
        gc.setStroke(Palette.TELEGRAPH_OBSIDIANA_LINE);
        gc.setLineWidth(1.5 + progress * 1.5);
        // Linha central de mira
        gc.strokeLine(bfx, bfy, bfx + Math.cos(baseAngle) * range, bfy + Math.sin(baseAngle) * range);
        // Linhas limites do cone
        gc.strokeLine(bfx, bfy, fanX[1], fanY[1]);
        gc.strokeLine(bfx, bfy, fanX[segments + 1], fanY[segments + 1]);

        // Arco delimitador frontal
        for (int i = 1; i <= segments; i++) {
            gc.strokeLine(fanX[i], fanY[i], fanX[i + 1], fanY[i + 1]);
        }
    }

    /**
     * Fusao Vulcanica: Ondas de choque concentricas pulsantes laranjas em expansao omnidirecional.
     */
    private void renderFusaoTelegraph(GraphicsContext gc, BossEntity boss, double centerX, double centerY, double progress) {
        double maxRadius = 260.0;
        double currentRadius = 40.0 + progress * (maxRadius - 40.0);
        double innerRadius = Math.max(20.0, currentRadius * 0.60);

        // Preenchimento de aviso de explosao
        gc.setFill(Palette.TELEGRAPH_FUSAO_FILL);
        gc.fillOval(centerX - currentRadius, centerY - currentRadius, currentRadius * 2.0, currentRadius * 2.0);

        // Anéis concentricos de aviso
        gc.setStroke(Palette.TELEGRAPH_FUSAO_STROKE);
        gc.setLineWidth(2.5 + Math.sin(progress * Math.PI) * 1.5);
        gc.strokeOval(centerX - currentRadius, centerY - currentRadius, currentRadius * 2.0, currentRadius * 2.0);
        gc.strokeOval(centerX - innerRadius, centerY - innerRadius, innerRadius * 2.0, innerRadius * 2.0);

        // Raios indicadores das 8 direções de disparo circular
        int rays = 8;
        for (int i = 0; i < rays; i++) {
            double angle = (Math.PI * 2 * i) / rays;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            gc.strokeLine(centerX + cos * (currentRadius * 0.55),
                          centerY + sin * (currentRadius * 0.55),
                          centerX + cos * currentRadius,
                          centerY + sin * currentRadius);
        }
    }

    /**
     * Furia de Apsu: Colunas verticais vermelhas indicando a queda exata dos meteoros vulcânicos.
     */
    private void renderFuriaTelegraph(GraphicsContext gc, BossEntity boss, double height, double progress) {
        int count = 5;
        double columnWidth = 44.0;
        // Inicia abaixo da barra do HUD (Y=30..45) para nao cobrir o HUD (AC4)
        double startY = 75.0;
        double columnHeight = height - startY - 30.0;

        for (int i = 0; i < count; i++) {
            double meteorX = 260.0 + Math.floorMod(boss.getAttackSeed() / 100_000_000L + i * 193L, 820L);
            double colLeft = meteorX - columnWidth / 2.0;

            // Coluna de perigo translúcida
            gc.setFill(Palette.TELEGRAPH_FURIA_FILL);
            gc.fillRect(colLeft, startY, columnWidth, columnHeight);

            // Bordas verticais da coluna
            gc.setStroke(Palette.TELEGRAPH_FURIA_STROKE);
            gc.setLineWidth(1.8);
            gc.strokeLine(colLeft, startY, colLeft, startY + columnHeight);
            gc.strokeLine(colLeft + columnWidth, startY, colLeft + columnWidth, startY + columnHeight);

            // Marcador de perigo no topo (triangulo indicador abaixo do HUD)
            gc.setFill(Palette.TELEGRAPH_FURIA_MARKER);
            markerX[0] = meteorX - 10.0;
            markerX[1] = meteorX + 10.0;
            markerX[2] = meteorX;
            markerY[0] = startY + 4.0;
            markerY[1] = startY + 4.0;
            markerY[2] = startY + 16.0;
            gc.fillPolygon(markerX, markerY, 3);

            // Linha indicadora de descida do meteoro sincronizada com o progresso
            double indicatorY = startY + columnHeight * progress;
            gc.setStroke(Palette.TELEGRAPH_FURIA_MARKER);
            gc.setLineWidth(2.5);
            gc.strokeLine(colLeft + 2.0, indicatorY, colLeft + columnWidth - 2.0, indicatorY);
        }
    }
}
