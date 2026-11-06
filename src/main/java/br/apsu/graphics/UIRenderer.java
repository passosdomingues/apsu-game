package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.environment.Difficulty;
import br.apsu.model.environment.OceanDepthProfile;
import br.apsu.model.guardian.GuardianEntity;
import br.apsu.model.hero.HeroEntity;
import br.apsu.model.hero.HeroType;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Renderizador desacoplado de UI, HUD, diálogos glassmorphism e radar.
 * Sem emojis — toda indicação visual usa formas desenhadas ou texto ASCII.
 */
public class UIRenderer {
    private static Color color(String web) {
        return Palette.color(web);
    }

    private static Color rgb(int red, int green, int blue, double alpha) {
        return Palette.rgb(red, green, blue, alpha);
    }

    public void drawHUD(GraphicsContext gc, HeroEntity hero, int tablets, String phaseName, Difficulty diff) {
        drawHUD(gc, hero, tablets, phaseName, diff, OceanDepthProfile.COASTAL);
    }

    public void drawHUD(GraphicsContext gc, HeroEntity hero, int tablets, String phaseName, Difficulty diff,
                        OceanDepthProfile depth) {
        gc.setFill(Palette.HUD_BG);
        gc.fillRoundRect(8, 8, 440, 136, 12, 12);
        gc.setStroke(Palette.HUD_STROKE); gc.setLineWidth(1.5);
        gc.strokeRoundRect(8, 8, 440, 136, 12, 12);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(UiFonts.LABEL_BOLD_14);
        gc.setFill(Palette.HUD_LABEL);
        gc.fillText("VIDA", 22, 32);

        // HP desenhado como barras retangulares — sem emoji
        int maxHearts = (int) Math.ceil(hero.getMaxHP());
        double hpRatio = Math.max(0, hero.getHp()) / Math.max(1, hero.getMaxHP());
        double barTotalW = maxHearts * 24;
        double barX = 65, barY = 20, barH = 14;
        // Fundo
        gc.setFill(Palette.HUD_HP_BG);
        gc.fillRoundRect(barX, barY, barTotalW, barH, 4, 4);
        // Preenchimento colorido por threshold
        Color hpColor = hpRatio > 0.6 ? Palette.HUD_HP_HIGH
                      : hpRatio > 0.3 ? Palette.HUD_HP_MED
                      : Palette.HUD_HP_LOW;
        gc.setFill(hpColor);
        gc.fillRoundRect(barX + 1, barY + 1, (barTotalW - 2) * hpRatio, barH - 2, 3, 3);
        // Divisórias por ponto de vida
        gc.setStroke(Palette.HUD_HP_DIVIDER); gc.setLineWidth(1);
        for (int i = 1; i < maxHearts; i++) {
            double lx = barX + i * 24;
            gc.strokeLine(lx, barY + 1, lx, barY + barH - 1);
        }
        // Borda
        Color borderStroke = hero.isInvulnerable() ? Palette.HUD_HP_INVULN_STROKE
                           : hpRatio > 0.6 ? Palette.HUD_HP_HIGH_STROKE
                           : hpRatio > 0.3 ? Palette.HUD_HP_MED_STROKE
                           : Palette.HUD_HP_LOW_STROKE;
        gc.setStroke(borderStroke);
        gc.setLineWidth(hero.isInvulnerable() ? 2 : 1);
        gc.strokeRoundRect(barX, barY, barTotalW, barH, 4, 4);
        // Décimos tornam impactos leves visíveis sem arredondá-los para cima.
        gc.setFont(UiFonts.TEXT_11);
        gc.setFill(Palette.HUD_HP_TEXT);
        int hpTenths = (int) Math.round(Math.max(0, hero.getHp()) * 10);
        String hpText = (hpTenths / 10) + "." + (hpTenths % 10) + " / " + (int) Math.round(hero.getMaxHP());
        gc.fillText(hpText,
            barX + barTotalW + 6, barY + 11);
        if (hero.isInvulnerable()) {
            gc.setFill(Palette.HUD_DAMAGE_TEXT);
            gc.setFont(UiFonts.DAMAGE_BOLD_9);
            gc.fillText("DANO", 270, barY + 11);
        }

        // Tabuletas
        gc.setFont(UiFonts.LABEL_BOLD_14);
        gc.setFill(Palette.HUD_LABEL);
        gc.fillText("TABULETAS", 22, 56);
        // Ícones de tabuleta como pequenos quadrados dourados
        for (int i = 0; i < 5; i++) {
            boolean has = i < tablets;
            gc.setFill(has ? Palette.GOLD : Palette.HUD_TABLET_SLOT);
            gc.fillRoundRect(120 + i * 20, 44, 15, 14, 3, 3);
            if (has) {
                gc.setStroke(Palette.GOLD_LIGHT); gc.setLineWidth(1);
                gc.strokeRoundRect(120 + i * 20, 44, 15, 14, 3, 3);
            }
        }

        // Poder de bolhas
        boolean hasBubble = hero.hasBubblePower();
        gc.setFont(UiFonts.LABEL_BOLD_14);
        gc.setFill(Palette.HUD_LABEL);
        gc.fillText("PODER", 240, 56);
        gc.setFill(hasBubble ? Palette.HUD_POWER_ACTIVE : Palette.HUD_POWER_LOCKED);
        gc.fillText(hasBubble ? "[ATIVO]" : "[BLOQUEADO]", 292, 56);

        // Fase e herói
        gc.setFont(UiFonts.TEXT_13);
        gc.setFill(Palette.HUD_PHASE_INFO);
        gc.fillText(phaseName + " [" + diff.getLabel() + "]", 22, 80);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(hero.getType().getAura());
        gc.fillText(hero.getType().getName(), 438, 80);
        gc.setTextAlign(TextAlignment.LEFT);

        gc.setFont(UiFonts.TEXT_10);
        gc.setFill(Palette.HUD_DEPTH_INFO);
        gc.fillText(depth.getLabel() + " · " + depth.getDepthMeters() + " m · " + depth.getPressureBar() + " bar",
            22, 98);

        if (hasBubble) {
            gc.setFill(Palette.HUD_BUBBLE_MODE); gc.setFont(UiFonts.TEXT_11);
            gc.fillText(hero.isMultipleShotMode() ? "Bolhas: múltiplas [M]" : "Bolha: direta [M]", 22, 112);
        }

        gc.setFont(UiFonts.TEXT_11);
        gc.setFill(Palette.HUD_KEYS_HINT);
        gc.fillText("WASD / SETAS: NADAR   ·   E: INTERAGIR   ·   ESPAÇO: BOLHAS", 22, 130);
    }

    /** Radar compacto: posição de inimigos e guardiões sem revelar detalhes. */
    public void drawMinimap(GraphicsContext gc, GameContext context, double canvasWidth) {
        final double mapW = 230, mapH = 70, mapX = canvasWidth - mapW - 18, mapY = 18;
        final double worldWidth = context.getState() == GameContext.State.P5 ? 1366 : 4000;

        gc.setFill(Palette.RADAR_BG);
        gc.fillRoundRect(mapX, mapY, mapW, mapH, 10, 10);
        gc.setStroke(Palette.RADAR_STROKE); gc.setLineWidth(1.3);
        gc.strokeRoundRect(mapX, mapY, mapW, mapH, 10, 10);
        gc.setFill(Palette.RADAR_TITLE); gc.setFont(UiFonts.RADAR_BOLD_10);
        gc.fillText("RADAR", mapX + 8, mapY + 13);

        for (EnemyEntity enemy : context.getEnemies()) {
            if (!enemy.isAlive()) continue;
            gc.setFill(Palette.RADAR_ENEMY);
            gc.fillOval(worldToMapX(enemy.getWorldX(), worldWidth, mapX, mapW) - 2, mapY + mapH / 2 - 2, 4, 4);
        }
        for (GuardianEntity guardian : context.getGuardians()) {
            gc.setFill(Palette.RADAR_GUARDIAN);
            gc.fillOval(worldToMapX(guardian.getWorldX(), worldWidth, mapX, mapW) - 3, mapY + mapH / 2 - 3, 6, 6);
        }
        if (context.getBoss() != null && !context.getBoss().isDead()) {
            gc.setFill(Palette.RADAR_BOSS);
            gc.fillOval(worldToMapX(context.getBoss().getX(), worldWidth, mapX, mapW) - 4, mapY + mapH / 2 - 4, 8, 8);
        }

        gc.setFill(context.getHero().getType().getAura());
        double heroX = worldToMapX(context.getHero().getX(), worldWidth, mapX, mapW);
        gc.fillPolygon(new double[]{heroX - 4, heroX + 4, heroX},
                       new double[]{mapY + mapH / 2 + 5, mapY + mapH / 2 + 5, mapY + mapH / 2 - 5}, 3);
    }

    static double worldToMapX(double worldX, double worldWidth, double mapX, double mapWidth) {
        if (worldWidth <= 0) return mapX;
        double normalized = Math.max(0, Math.min(1, worldX / worldWidth));
        return mapX + normalized * mapWidth;
    }

    public void drawDialogueOverlay(GraphicsContext gc, double canvasW, double canvasH, double timeSeconds,
                                  String name, String[] lines, int lineIdx, int charsShown) {
        double boxW = canvasW * 0.84, boxH = 165;
        double boxX = (canvasW - boxW) / 2, boxY = canvasH - boxH - 25;

        gc.setFill(Palette.DIALOGUE_OVERLAY_BG);
        gc.fillRoundRect(boxX, boxY, boxW, boxH, 18, 18);
        gc.setStroke(Palette.GOLD); gc.setLineWidth(2.2);
        gc.strokeRoundRect(boxX, boxY, boxW, boxH, 18, 18);

        double textX = boxX + 30;
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(UiFonts.TITLE_20);
        gc.setFill(Palette.GOLD);
        if (name != null) gc.fillText(name, textX, boxY + 32);

        if (lines != null && lineIdx < lines.length) {
            String fullText = lines[lineIdx];
            String shown = fullText.substring(0, Math.min(charsShown, fullText.length()));
            gc.setFont(UiFonts.TEXT_19);
            gc.setFill(Color.WHITE);
            wrapText(gc, shown, textX, boxY + 64, boxW - 60, 30);
        }

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(UiFonts.HINT_BOLD_13);
        gc.setFill(Palette.DIALOGUE_OVERLAY_HINT);
        gc.fillText("[ E / ESPACO / ENTER: Avancar  |  ESC: Pular ]", canvasW / 2.0, boxY + boxH - 14);

        if (lines != null) {
            gc.setFill(Palette.GOLD);
            gc.fillText((lineIdx + 1) + " / " + lines.length, canvasW / 2.0 + 280, boxY + boxH - 14);
        }
    }

    public void wrapText(GraphicsContext gc, String txt, double x, double y, double maxWidth, double lineHeight) {
        if (txt == null || txt.isEmpty()) return;
        String[] words = txt.split(" "); StringBuilder line = new StringBuilder(); double ly = y;
        for (String w : words) {
            String test = line.length() > 0 ? line + " " + w : w;
            if (test.length() * 11.0 > maxWidth) {
                if (line.length() > 0) gc.fillText(line.toString(), x, ly);
                line = new StringBuilder(w); ly += lineHeight;
            } else {
                if (line.length() > 0) line.append(" ");
                line.append(w);
            }
        }
        if (line.length() > 0) gc.fillText(line.toString(), x, ly);
    }
}
