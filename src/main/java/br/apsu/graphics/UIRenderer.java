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
import java.util.List;

/**
 * Renderizador desacoplado de UI, HUD, diálogos glassmorphism e radar.
 * Sem emojis — toda indicação visual usa formas desenhadas ou texto ASCII.
 */
public class UIRenderer {
    private final br.apsu.guidance.ObjectiveArrow objectiveArrow = new br.apsu.guidance.ObjectiveArrow();
    private final StringBuilder wrapLineBuffer = new StringBuilder(128);
    private final double[] minimapHeroX = new double[3];
    private final double[] minimapHeroY = new double[3];

    private double keysHintAlpha = 1.0;
    private boolean keysMovementConfirmed = false;
    private int keysHintFrames = 0;

    public double getKeysHintAlpha() { return keysHintAlpha; }
    public void setKeysHintAlpha(double keysHintAlpha) { this.keysHintAlpha = keysHintAlpha; }
    public boolean isKeysMovementConfirmed() { return keysMovementConfirmed; }
    public void setKeysMovementConfirmed(boolean keysMovementConfirmed) { this.keysMovementConfirmed = keysMovementConfirmed; }
    public void resetKeysHint() {
        this.keysHintAlpha = 1.0;
        this.keysMovementConfirmed = false;
        this.keysHintFrames = 0;
    }

    public br.apsu.guidance.ObjectiveArrow getObjectiveArrow() {
        return objectiveArrow;
    }

    private static Color color(String web) {
        return Palette.color(web);
    }

    private static Color rgb(int red, int green, int blue, double alpha) {
        return Palette.rgb(red, green, blue, alpha);
    }

    // Tema claro para indicador do eco e dica (US-033)
    public static final Color ECHO_LIGHT_BG = Color.rgb(240, 246, 252, 0.95);
    public static final Color ECHO_LIGHT_STROKE = Color.rgb(120, 180, 240, 0.90);
    public static final Color ECHO_LIGHT_TEXT = Color.rgb(10, 30, 60);
    public static final Color ECHO_LIGHT_PROGRESS_BG = Color.rgb(215, 230, 248, 0.90);
    public static final Color ECHO_LIGHT_PROGRESS_FILL = Color.rgb(40, 140, 230, 0.90);
    public static final Color ECHO_HINT_BG = Color.rgb(248, 250, 255, 0.95);
    public static final Color ECHO_HINT_STROKE = Color.rgb(140, 195, 255, 0.90);
    public static final Color ECHO_HINT_TEXT = Color.rgb(15, 45, 80);

    public void drawHUD(GraphicsContext gc, HeroEntity hero, int tablets, String phaseName, Difficulty diff) {
        drawHUD(gc, hero, tablets, phaseName, diff, OceanDepthProfile.COASTAL, 0.0, null, null);
    }

    public void drawHUD(GraphicsContext gc, HeroEntity hero, int tablets, String phaseName, Difficulty diff,
                        OceanDepthProfile depth) {
        drawHUD(gc, hero, tablets, phaseName, diff, depth, 0.0, null, null);
    }

    public void drawHUD(GraphicsContext gc, HeroEntity hero, int tablets, String phaseName, Difficulty diff,
                        OceanDepthProfile depth, double unlockHighlightProgress) {
        drawHUD(gc, hero, tablets, phaseName, diff, depth, unlockHighlightProgress, null, null);
    }

    public void drawHUD(GraphicsContext gc, HeroEntity hero, int tablets, String phaseName, Difficulty diff,
                        OceanDepthProfile depth, double unlockHighlightProgress, String objectiveText) {
        drawHUD(gc, hero, tablets, phaseName, diff, depth, unlockHighlightProgress, objectiveText, null);
    }

    public void drawHUD(GraphicsContext gc, HeroEntity hero, int tablets, String phaseName, Difficulty diff,
                        OceanDepthProfile depth, double unlockHighlightProgress, String objectiveText,
                        br.apsu.echo.EchoController echo) {
        // APSU-U02: Transição e recolhimento suave da dica de teclas
        keysHintFrames++;
        if (!keysMovementConfirmed && (Math.abs(hero.getVx()) > 0.1 || Math.abs(hero.getVy()) > 0.1 || keysHintFrames > 300)) {
            keysMovementConfirmed = true;
        }
        if (keysMovementConfirmed && keysHintAlpha > 0.0) {
            keysHintAlpha = Math.max(0.0, keysHintAlpha - 0.015);
        }

        boolean hasObjective = objectiveText != null && !objectiveText.isEmpty();
        double hintExtraH = 16.0 * keysHintAlpha;
        double hudH = 104 + hintExtraH + (hasObjective ? 18 : 0);

        gc.setFill(Palette.HUD_BG);
        gc.fillRoundRect(8, 8, 440, hudH, 12, 12);
        gc.setStroke(Palette.HUD_STROKE); gc.setLineWidth(1.5);
        gc.strokeRoundRect(8, 8, 440, hudH, 12, 12);

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

        // US-033: Indicador de recarga da ecolocalização na linha 1 (sem sobrepor TABULETAS)
        if (echo != null) {
            double echoX = Math.max(230, barX + barTotalW + 55);
            drawEchoIndicator(gc, echo, echoX, barY);
        }

        // Tabuletas e Poder (primeiro plano compacto)
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

        // Poder de bolhas (US-022: troca poder bloqueado por bolhas com destaque animado)
        boolean hasBubble = hero.hasBubblePower();
        gc.setFont(UiFonts.LABEL_BOLD_14);
        gc.setFill(Palette.HUD_LABEL);
        gc.fillText("PODER", 232, 56);

        if (unlockHighlightProgress > 0.0) {
            // Destaque animado de 1 a 2s (pulso dourado/verde ciano)
            double pulse = Math.abs(Math.sin(unlockHighlightProgress * Math.PI * 4));
            Color highlightColor = Palette.rgb(
                (int) (64 + (255 - 64) * pulse),
                (int) (255),
                (int) (136 + (255 - 136) * (1.0 - pulse)),
                0.95
            );
            gc.setFill(highlightColor);
            gc.fillText("[BOLHAS!]", 286, 56);
            gc.setStroke(highlightColor);
            gc.setLineWidth(1.5 + pulse * 1.5);
            gc.strokeRoundRect(282, 42, 76, 18, 4, 4);
        } else {
            gc.setFill(hasBubble ? Palette.HUD_POWER_ACTIVE : Palette.HUD_POWER_LOCKED);
            gc.fillText(hasBubble ? "[BOLHAS]" : "[BLOQUEADO]", 286, 56);
        }

        if (hasBubble) {
            gc.setFill(Palette.HUD_BUBBLE_MODE); gc.setFont(UiFonts.TEXT_11);
            gc.fillText(hero.isMultipleShotMode() ? "Múltiplas [M]" : "Direta [M]", 370, 56);
        }

        // Fase e herói
        gc.setFont(UiFonts.TEXT_13);
        gc.setFill(Palette.HUD_PHASE_INFO);
        gc.fillText(phaseName + " [" + diff.getLabel() + "]", 22, 76);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(hero.getType().getAura());
        gc.fillText(hero.getType().getName(), 438, 76);
        gc.setTextAlign(TextAlignment.LEFT);

        // Badge discreto de profundidade (AC1)
        double badgeX = 22, badgeY = 82, badgeW = 205, badgeH = 15;
        gc.setFill(Palette.HUD_HP_BG);
        gc.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 4, 4);
        gc.setStroke(Palette.HUD_STROKE); gc.setLineWidth(0.8);
        gc.strokeRoundRect(badgeX, badgeY, badgeW, badgeH, 4, 4);
        gc.setFont(UiFonts.TEXT_10);
        gc.setFill(Palette.HUD_DEPTH_INFO);
        gc.fillText(depth.getDepthMeters() + " m  ·  " + depth.getPressureBar() + " bar  (" + depth.getLabel() + ")",
            badgeX + 6, badgeY + 11);

        double cursorY = 98;
        if (keysHintAlpha > 0.01) {
            double prevAlpha = gc.getGlobalAlpha();
            gc.setGlobalAlpha(prevAlpha * keysHintAlpha);
            gc.setFont(UiFonts.TEXT_11);
            gc.setFill(Palette.HUD_KEYS_HINT);
            gc.fillText("WASD / SETAS: NADAR   ·   E: INTERAGIR   ·   ESPAÇO: BOLHAS", 22, cursorY + 13);
            gc.setGlobalAlpha(prevAlpha);
            cursorY += hintExtraH;
        }

        if (hasObjective) {
            gc.setFont(UiFonts.TEXT_BOLD_11);
            gc.setFill(Palette.HUD_OBJECTIVE_LABEL);
            gc.fillText("OBJETIVO:", 22, cursorY + 14);
            gc.setFont(UiFonts.TEXT_11);
            gc.setFill(Palette.HUD_OBJECTIVE_TEXT);
            gc.fillText(objectiveText, 90, cursorY + 14);
        }
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
        gc.fillText("ECOLOCALIZAÇÃO", mapX + 8, mapY + 13);

        List<EnemyEntity> enemies = context.getEnemies();
        for (int i = 0; i < enemies.size(); i++) {
            EnemyEntity enemy = enemies.get(i);
            if (!enemy.isAlive()) continue;
            // US-032: Inimigos so aparecem no GPS se revelados pelo eco
            if (!isEnemyVisibleOnMinimap(i, context)) {
                continue;
            }
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
        minimapHeroX[0] = heroX - 4; minimapHeroX[1] = heroX + 4; minimapHeroX[2] = heroX;
        minimapHeroY[0] = mapY + mapH / 2 + 5; minimapHeroY[1] = mapY + mapH / 2 + 5; minimapHeroY[2] = mapY + mapH / 2 - 5;
        gc.fillPolygon(minimapHeroX, minimapHeroY, 3);
    }

    public boolean isEnemyVisibleOnMinimap(int enemyIndex, GameContext context) {
        if (context == null || enemyIndex < 0 || enemyIndex >= context.getEnemies().size()) return false;
        EnemyEntity enemy = context.getEnemies().get(enemyIndex);
        if (!enemy.isAlive()) return false;
        return context.getEchoWaveRenderer() != null && context.getEchoWaveRenderer().isEnemyHighlighted(enemyIndex);
    }

    public boolean isBossVisibleOnMinimap(GameContext context) {
        return context != null && context.getBoss() != null && !context.getBoss().isDead();
    }

    public boolean isGuardianVisibleOnMinimap(int guardianIndex, GameContext context) {
        return context != null && guardianIndex >= 0 && guardianIndex < context.getGuardians().size();
    }

    /**
     * Desenha o indicador de recarga do eco com icone 'Q' e progresso (US-033).
     * Usa exclusivamente o tema claro para contraste e legibilidade.
     */
    public void drawEchoIndicator(GraphicsContext gc, br.apsu.echo.EchoController echo, double x, double y) {
        if (echo == null) return;

        boolean onCooldown = echo.isOnCooldown();
        double progress = echo.getCooldownProgress();
        double alpha = onCooldown ? 0.45 : 1.0;

        gc.save();
        gc.setGlobalAlpha(alpha);

        // Icone 'Q' no tema claro
        double iconW = 18, iconH = 14;
        gc.setFill(ECHO_LIGHT_BG);
        gc.fillRoundRect(x, y, iconW, iconH, 3, 3);
        gc.setStroke(ECHO_LIGHT_STROKE);
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(x, y, iconW, iconH, 3, 3);

        gc.setFill(ECHO_LIGHT_TEXT);
        gc.setFont(UiFonts.RADAR_BOLD_10);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("Q", x + iconW / 2.0, y + iconH / 2.0);

        // Se em recarga: exibe progresso de 0 a 100%
        if (onCooldown) {
            int pct = (int) Math.round(progress * 100.0);
            gc.setFont(UiFonts.TEXT_10);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(pct + "%", x + iconW + 4, y + iconH / 2.0);
        }

        // Se dica ativa: "Q: eco"
        if (echo.isHintVisible()) {
            double hintX = x + (onCooldown ? iconW + 38 : iconW + 6);
            drawEchoHint(gc, hintX, y);
        }

        gc.restore();
    }

    /**
     * Desenha a dica de tecla no tema claro (US-033).
     */
    public void drawEchoHint(GraphicsContext gc, double x, double y) {
        double hintW = 52, hintH = 14;
        gc.setGlobalAlpha(1.0);
        gc.setFill(ECHO_HINT_BG);
        gc.fillRoundRect(x, y, hintW, hintH, 3, 3);
        gc.setStroke(ECHO_HINT_STROKE);
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(x, y, hintW, hintH, 3, 3);

        gc.setFill(ECHO_HINT_TEXT);
        gc.setFont(UiFonts.TEXT_10);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("Q: eco", x + hintW / 2.0, y + hintH / 2.0);
    }

    /**
     * Dica visual elegante e pulsante da tecla Q sugerida quando o jogador estiver próximo
     * à área indicada pela pista do Enki (APSU-U03 / AC3).
     */
    public void drawEchoPrompt(GraphicsContext gc, GameContext context, double canvasWidth, double canvasHeight, double timeSeconds) {
        if (context == null || context.getHero() == null) return;
        br.apsu.model.environment.SacredTabletEntity tablet = context.getSacredTablet();
        if (tablet == null || tablet.isRevealed() || tablet.isCollected()) return;

        double heroX = context.getHero().getX();
        double heroY = context.getHero().getY();
        double dist = Math.hypot(heroX - tablet.getWorldX(), heroY - tablet.getWorldY());
        if (dist > 480.0) return;

        double screenX = context.getCamera().toScreenX(heroX);
        double screenY = heroY;
        if (screenX < 60 || screenX > canvasWidth - 60 || screenY < 50 || screenY > canvasHeight - 50) return;

        gc.save();
        double pulse = 0.75 + 0.25 * Math.sin(timeSeconds * 5.0);
        gc.setGlobalAlpha(pulse);

        String prompt = "[Q] Ecolocalização";
        double pillW = 140, pillH = 26;
        double pillX = screenX - pillW / 2.0;
        double pillY = screenY - 55.0;

        gc.setFill(Color.rgb(5, 20, 45, 0.85));
        gc.fillRoundRect(pillX, pillY, pillW, pillH, 13, 13);
        gc.setStroke(Color.rgb(70, 200, 255, 0.9));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(pillX, pillY, pillW, pillH, 13, 13);

        gc.setFill(Color.rgb(180, 240, 255));
        gc.setFont(UiFonts.TEXT_BOLD_12);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(prompt, screenX, pillY + pillH / 2.0);

        gc.restore();
    }

    public void drawObjectiveArrow(GraphicsContext gc, GameContext context, double canvasWidth, double canvasHeight, double timeSeconds) {
        objectiveArrow.render(gc, context, canvasWidth, canvasHeight, timeSeconds);
    }

    static double worldToMapX(double worldX, double worldWidth, double mapX, double mapWidth) {
        if (worldWidth <= 0) return mapX;
        double normalized = Math.max(0, Math.min(1, worldX / worldWidth));
        return mapX + normalized * mapWidth;
    }

    public void drawDialogueOverlay(GraphicsContext gc, double canvasW, double canvasH, double timeSeconds,
                                  String name, String[] lines, int lineIdx, int charsShown) {
        // APSU-U01: Elevação e margem vertical para evitar oclusão por algas e corais do chão
        double boxW = canvasW * 0.84, boxH = 165;
        double boxX = (canvasW - boxW) / 2, boxY = canvasH - boxH - 65;

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
        wrapLineBuffer.setLength(0);
        double ly = y;
        int len = txt.length();
        int i = 0;
        while (i < len) {
            while (i < len && txt.charAt(i) == ' ') i++;
            if (i >= len) break;
            int start = i;
            while (i < len && txt.charAt(i) != ' ') i++;
            int wordLen = i - start;

            int neededLen = (wrapLineBuffer.length() > 0 ? wrapLineBuffer.length() + 1 : 0) + wordLen;
            if (neededLen * 11.0 > maxWidth) {
                if (wrapLineBuffer.length() > 0) {
                    gc.fillText(wrapLineBuffer.toString(), x, ly);
                    wrapLineBuffer.setLength(0);
                    ly += lineHeight;
                }
            } else if (wrapLineBuffer.length() > 0) {
                wrapLineBuffer.append(' ');
            }
            wrapLineBuffer.append(txt, start, i);
        }
        if (wrapLineBuffer.length() > 0) {
            gc.fillText(wrapLineBuffer.toString(), x, ly);
            wrapLineBuffer.setLength(0);
        }
    }
}
