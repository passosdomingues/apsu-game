package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.core.GameRenderer;
import br.apsu.model.boss.BossEntity;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.environment.Difficulty;
import br.apsu.model.environment.Projectile;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Renderizador mestre desacoplado.
 * Sprint 5: Bug B1 corrigido (aspecto estável no menu), novas fases P3-P5,
 * visualização de correntes, zonas de pressão, obstáculos vulcânicos.
 */
public class RenderEngine implements GameRenderer {

    private final SpriteManager spriteManager = SpriteManager.getInstance();
    private final Runtime3DLayer runtime3DLayer = new Runtime3DLayer();
    private final LightingEngine lightingEngine = new LightingEngine();
    private final UIRenderer uiRenderer = new UIRenderer();
    private final Image[][] phaseBackgroundLayers = new Image[5][3];
    private final Image[] legacyPhaseBackgrounds = new Image[5];
    private final boolean[] legacyPhaseBackgroundLoaded = new boolean[5];
    private final Paint[] phaseBackgroundGradients = new Paint[5];
    private static final String[][] PHASE_BACKGROUND_COLORS = {
        {"#08192e", "#0e3050", "#144870"},
        {"#040c16", "#081826", "#0a2234"},
        {"#060012", "#110028", "#1a0040"},
        {"#1a0500", "#380800", "#5a0a00"},
        {"#080018", "#150030", "#200050"}
    };
    private boolean reducedEffects = true;

    private static Color color(String web) {
        return Palette.color(web);
    }

    private static final Color MENU_BUBBLE_COLOR = Color.rgb(80, 180, 255);
    private static final Color ABYSS_BUBBLE_COLOR = Color.rgb(80, 160, 255);
    private static final Color SHADOW_COLOR = Color.rgb(2, 8, 14, 0.40);
    private static final Color HERO_BUBBLE_FILL = Color.rgb(160, 240, 255, 0.85);
    private static final Color HERO_BUBBLE_STROKE = Color.rgb(0, 200, 255, 0.7);
    private static final Color HERO_BUBBLE_HIGHLIGHT = Color.rgb(255, 255, 255, 0.5);
    private static final Color HERO_BUBBLE_TRAIL = Color.rgb(150, 225, 245, 0.35);
    private static final Color ENEMY_BULLET_FILL = Color.web("#ff2a4b");
    private static final Color ENEMY_BULLET_HALO = Color.rgb(255, 80, 80, 0.4);
    private static final LinearGradient SEAFLOOR_GRADIENT = new LinearGradient(0, 730, 0, 768,
        false, CycleMethod.NO_CYCLE,
        new Stop(0, Color.rgb(10, 45, 22, 0)), new Stop(1, Color.rgb(10, 45, 22, 0.95)));

    public RenderEngine() {
        lightingEngine.setReducedEffects(true);
        String[] layers = {"fundo", "meio", "frente"};
        for (int phase = 1; phase <= phaseBackgroundLayers.length; phase++) {
            String base = "backgrounds/phase-" + phase;
            String[] colors = PHASE_BACKGROUND_COLORS[phase - 1];
            phaseBackgroundGradients[phase - 1] = new LinearGradient(0, 0, 0, 1, true,
                CycleMethod.NO_CYCLE, new Stop(0, color(colors[0])),
                new Stop(0.5, color(colors[1])), new Stop(1, color(colors[2])));
            for (int layer = 0; layer < layers.length; layer++) {
                phaseBackgroundLayers[phase - 1][layer] = spriteManager.getImage(base + "-" + layers[layer] + ".png");
            }
        }
    }

    /** Qualidade dinâmica: o Canvas continua no thread JavaFX, mas os efeitos
     * mais caros cedem espaço para a jogabilidade quando há queda de FPS. */
    public void setReducedEffects(boolean reducedEffects) {
        this.reducedEffects = reducedEffects;
        lightingEngine.setReducedEffects(reducedEffects);
    }

    public boolean isReducedEffects() { return reducedEffects; }
    public javafx.scene.Node get3DView() { return runtime3DLayer.view(); }
    public void setViewportScale(double scale) { runtime3DLayer.setViewportScale(scale); }

    public void render(GraphicsContext gc, GameContext ctx, double width, double height) {
        gc.clearRect(0, 0, width, height);
        double t = ctx.getNanoTime() / 1_000_000_000.0;
        double shakeX = ctx.getCamera().getShakeX(ctx.getNanoTime());
        double shakeY = ctx.getCamera().getShakeY(ctx.getNanoTime());
        runtime3DLayer.update(ctx, t, shakeX, shakeY);

        // Shake é só uma transformação visual: coordenadas de mundo, colisões
        // e HUD continuam calculados normalmente pelo GameContext.
        gc.save();
        gc.translate(shakeX, shakeY);
        switch (ctx.getState()) {
            case MENU     -> drawMenu(gc, ctx, width, height, t);
            case DIALOGUE -> drawDialogue(gc, ctx, width, height, t);
            case P1       -> drawP1(gc, ctx, width, height, t);
            case P2       -> drawP2(gc, ctx, width, height, t);
            case P3       -> drawP3(gc, ctx, width, height, t);
            case P4       -> drawP4(gc, ctx, width, height, t);
            case P5       -> drawP5(gc, ctx, width, height, t);
            case WIN      -> drawEnd(gc, ctx, width, height, true);
            case OVER     -> drawEnd(gc, ctx, width, height, false);
        }
        gc.restore();

        if (ctx.isOverlayActive()) {
            uiRenderer.drawDialogueOverlay(gc, width, height, t, ctx.getOverlayName(),
                ctx.getOverlayLines(), ctx.getOverlayIdx(), ctx.getOverlayCharsShown());
        }
    }

    // =========================================================
    // B1-FIX — MENU: aspecto estável, sem oscilação de largura
    // =========================================================
    private void drawMenu(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        drawGradientBackground(gc, width, height, Palette.GRADIENT_MENU);

        // Bolhas decorativas de fundo
        for (int i = 0; i < 22; i++) {
            double bx = (i * 87 + Math.sin(timeSeconds * 0.5 + i) * 38 + width * 6) % width;
            double by = (height - (timeSeconds * (16 + i % 7) + i * 68) % (height + 90));
            gc.setGlobalAlpha(0.09 + Math.sin(timeSeconds + i) * 0.03);
            gc.setFill(MENU_BUBBLE_COLOR);
            gc.fillOval(bx, by, 6 + (i % 5) * 10, 6 + (i % 5) * 10);
        }
        gc.setGlobalAlpha(1.0);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        gc.setFont(UiFonts.TITLE_64);
        gc.setFill(Palette.BLUE_TITLE_SHADOW); gc.fillText("AS AGUAS DE APSU", width / 2.0 + 3, 105);
        gc.setFill(Palette.GOLD); gc.fillText("AS AGUAS DE APSU", width / 2.0, 102);

        gc.setFont(UiFonts.SUBTITLE_22);
        gc.setFill(Palette.BLUE_SUBTITLE);
        gc.fillText("A Lenda dos Apkallu", width / 2.0, 150);

        // A malha 3D de Adapa aparece na camada sincronizada ao Canvas.
        HeroType hero = ctx.getHeroType();
        final double ph = 210;
        final double pw = 112;

        double px = width * 0.82 - pw / 2.0, py = 150;

        drawShadow(gc, px + pw / 2, py + ph - 5, pw * 0.8, 14);
        lightingEngine.drawBioluminescentHalo(gc, px + pw/2, py + ph/2, pw, hero.getAura());

        // Card Glassmorphic de Atributos
        gc.setFill(Palette.CARD_BG);
        gc.fillRoundRect(px - 35, py + ph + 15, pw + 70, 145, 12, 12);
        gc.setStroke(hero.getAuraStroke()); gc.setLineWidth(1.5);
        gc.strokeRoundRect(px - 35, py + ph + 15, pw + 70, 145, 12, 12);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(UiFonts.LABEL_BOLD_14);
        gc.setFill(Palette.GOLD);
        gc.fillText(hero.getName(), px - 25, py + ph + 35);

        drawStatBar(gc, "Velocidade",  hero.getMaxSpeed() / 16.5, px - 25, py + ph + 48,  pw + 50, Palette.STAT_SPEED, Palette.STAT_SPEED_STROKE);
        drawStatBar(gc, "Agilidade",   hero.getAccel()    / 3.2,  px - 25, py + ph + 72,  pw + 50, Palette.STAT_AGILITY, Palette.STAT_AGILITY_STROKE);
        drawStatBar(gc, "Resistência", (hero.getDrag() - 0.85) / 0.12, px - 25, py + ph + 96, pw + 50, Palette.STAT_RESIST, Palette.STAT_RESIST_STROKE);
        drawStatBar(gc, "Poder Bolha", hero.getPwr()      / 1.6,  px - 25, py + ph + 120, pw + 50, Palette.STAT_BUBBLE, Palette.STAT_BUBBLE_STROKE);

        // Menu principal: poucas ações visíveis e navegação por teclado explícita.
        gc.setTextAlign(TextAlignment.CENTER);
        String[] opts = {"DIFICULDADE     ▾     " + ctx.getDifficulty().getLabel(), "▶  INICIAR JOGO"};
        double[] ys = {400, 500};
        for (int i = 0; i < opts.length; i++) {
            boolean sel = (i == ctx.getMenuSel());
            if (sel) {
                gc.setFill(Palette.MENU_SELECTED_BG);
                gc.fillRoundRect(width / 2.0 - 280, ys[i] - 31, 560, 62, 16, 16);
                gc.setStroke(Palette.GOLD); gc.setLineWidth(2);
                gc.strokeRoundRect(width / 2.0 - 280, ys[i] - 31, 560, 62, 16, 16);
            }
            gc.setFont(sel ? UiFonts.OPT_SEL_28 : UiFonts.OPT_UNSEL_22);
            gc.setFill(sel ? Palette.GOLD : Palette.MENU_UNSELECTED);
            gc.fillText(opts[i], width / 2.0, ys[i]);
        }

        if (ctx.isDifficultyMenuOpen()) {
            double panelX = width / 2.0 - 235, panelY = 270, panelW = 470, panelH = 320;
            gc.setFill(Palette.DIFF_PANEL_BG);
            gc.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);
            gc.setStroke(Palette.DIFF_PANEL_STROKE); gc.setLineWidth(2);
            gc.strokeRoundRect(panelX, panelY, panelW, panelH, 20, 20);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(UiFonts.SUBTITLE_22); gc.setFill(Palette.GOLD);
            gc.fillText("ESCOLHA A DIFICULDADE", width / 2.0, panelY + 34);
            Difficulty[] modes = Difficulty.values();
            for (int i = 0; i < modes.length; i++) {
                double rowY = panelY + 78 + i * 45;
                boolean selected = i == ctx.getDifficultyMenuSel();
                if (selected) {
                    gc.setFill(Palette.DIFF_ROW_SELECTED_BG);
                    gc.fillRoundRect(panelX + 18, rowY - 19, panelW - 36, 38, 12, 12);
                }
                gc.setFont(selected ? UiFonts.ROW_SEL_19 : UiFonts.ROW_UNSEL_19);
                gc.setFill(selected ? Color.WHITE : Palette.MENU_UNSELECTED);
                gc.fillText((selected ? "▶  " : "     ") + modes[i].getLabel(), width / 2.0, rowY);
            }
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFont(UiFonts.TEXT_12); gc.setFill(Palette.DIFF_HINT);
            uiRenderer.wrapText(gc, modes[ctx.getDifficultyMenuSel()].getDescription(),
                panelX + 24, panelY + 220, panelW - 48, 16);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(UiFonts.TEXT_13); gc.setFill(Palette.DIFF_HINT);
            gc.fillText("↑  ↓  navegar     Enter  confirmar     Esc  voltar", width / 2.0, panelY + panelH - 17);
        } else {
            gc.setFont(UiFonts.HINT_15); gc.setFill(Palette.MENU_HINT);
            gc.fillText("↑  ↓  navegar     Enter  selecionar", width / 2.0, 585);
        }
    }

    private void drawStatBar(GraphicsContext gc, String label, double ratio, double x, double y, double width, Color color, Color stroke) {
        gc.setFont(UiFonts.TEXT_11);
        gc.setFill(Palette.STAT_LABEL);
        gc.fillText(label, x, y + 10);
        double barX = x + 70, barW = width - 70, barH = 10;
        gc.setFill(Palette.STAT_BG);
        gc.fillRoundRect(barX, y + 2, barW, barH, 4, 4);
        gc.setFill(color);
        gc.fillRoundRect(barX, y + 2, barW * Math.max(0.1, Math.min(1.0, ratio)), barH, 4, 4);
        gc.setStroke(stroke); gc.setLineWidth(1);
        gc.strokeRoundRect(barX, y + 2, barW, barH, 4, 4);
    }

    private void drawDialogue(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        drawGradientBackground(gc, width, height, Palette.GRADIENT_DIALOGUE);

        gc.setFill(Palette.CARD_BG_DIALOGUE);
        gc.fillRoundRect(240, height / 2.0 - 150, width - 310, 295, 22, 22);
        gc.setStroke(Palette.CARD_STROKE_DIALOGUE); gc.setLineWidth(2);
        gc.strokeRoundRect(240, height / 2.0 - 150, width - 310, 295, 22, 22);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(UiFonts.TITLE_20);
        gc.setFill(Palette.ENKI_NAME);
        gc.fillText("Enki — Senhor das Águas Primordiais:", 265, height / 2.0 - 118);
        gc.setFill(Color.WHITE); gc.setFont(UiFonts.TEXT_20);
        String[] currentDlg = GuardianEntity.ENKI_INTRO_DIALOGUES[
            Math.min(ctx.getDlgPhase(), GuardianEntity.ENKI_INTRO_DIALOGUES.length - 1)];
        if (ctx.getDlgIdx() < currentDlg.length) uiRenderer.wrapText(gc, currentDlg[ctx.getDlgIdx()], 265, height / 2.0 - 76, width - 440, 34);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Palette.GOLD); gc.setFont(UiFonts.HINT_15);
        gc.fillText("[ ESPAÇO / E: Avançar  •  ESC: Pular Diálogo ]", width / 2.0 + 80, height / 2.0 + 136);
    }

    private void drawParallaxBackground(GraphicsContext gc, Image bg, double cameraX, double width, double height, double parallaxFactor) {
        if (bg == null) return;
        double bgH = height;
        double bgW = bg.getWidth() * (bgH / Math.max(1.0, bg.getHeight()));
        if (bgW <= 0) bgW = width;
        double offset = -(cameraX * parallaxFactor) % bgW;
        if (offset > 0) offset -= bgW;
        for (double x = offset; x < width + bgW; x += bgW) {
            gc.drawImage(bg, x, 0, bgW, bgH);
        }
    }

    private void drawPhaseBackground(GraphicsContext gc, GameContext ctx, double width, double height,
                                    int phase, double fallbackFactor) {
        int index = phase - 1;
        gc.setFill(phaseBackgroundGradients[index]);
        gc.fillRect(0, 0, width, height);
        Image background = phaseBackgroundLayers[index][0];
        Image midground = phaseBackgroundLayers[index][1];
        Image foreground = phaseBackgroundLayers[index][2];
        if (background == null) {
            if (!legacyPhaseBackgroundLoaded[index]) {
                legacyPhaseBackgrounds[index] = spriteManager.getImage("backgrounds/phase-" + phase + ".png");
                legacyPhaseBackgroundLoaded[index] = true;
            }
            Image legacy = legacyPhaseBackgrounds[index];
            if (legacy != null) drawParallaxBackground(gc, legacy, ctx.getCamera().getInterpolatedX(ctx.getRenderAlpha()), width, height, fallbackFactor);
            return;
        }
        double alpha = ctx.getRenderAlpha();
        double cameraX = ctx.getCamera().getInterpolatedX(alpha);
        drawParallaxBackground(gc, background, cameraX, width, height, 0.16);
        drawParallaxBackground(gc, midground, cameraX, width, height, 0.42);
        drawParallaxBackground(gc, foreground, cameraX, width, height, 0.72);
    }

    private void drawInterpolatedHero(GraphicsContext gc, GameContext ctx, double timeSeconds, boolean useCamera) {
        double alpha = ctx.getRenderAlpha();
        HeroEntity hero = ctx.getHero();
        double hx = useCamera ? ctx.getCamera().toScreenX(hero.getInterpolatedX(alpha), alpha) : hero.getInterpolatedX(alpha);
        double hy = hero.getInterpolatedY(alpha);
        drawHero(gc, hx, hy, hero, timeSeconds, ctx.getNanoTime());
    }

    // =========================================================
    // FASE 1 — Águas Claras
    // =========================================================
    private void drawP1(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        drawPhaseBackground(gc, ctx, width, height, 1, 0.40);

        lightingEngine.drawSunCaustics(gc, width, height, timeSeconds);

        // Faixa de areia/algas no rodapé
        gc.setFill(SEAFLOOR_GRADIENT);
        gc.fillRect(0, height - 38, width, 38);

        // Cenário
        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, gx, g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        drawPhaseExit(gc, ctx.getCamera().toScreenX(3900), ctx.getTabletsCollected() >= 1);

        for (EnemyEntity e : ctx.getEnemies()) {
            drawVisibleEnemy(gc, ctx, e, width, timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawInterpolatedHero(gc, ctx, timeSeconds, true);
        ctx.getParticleSystem().render(gc);

        uiRenderer.drawHUD(gc, ctx.getHero(), ctx.getTabletsCollected(), "Fase 1 — Águas Claras", ctx.getDifficulty(),
            OceanDepthProfile.forPhase(ctx.getCurrentPhase()));
        uiRenderer.drawMinimap(gc, ctx, width);
        drawAlert(gc, ctx, width, height);
    }

    // =========================================================
    // FASE 2 — Cavernas de Coral
    // =========================================================
    private void drawP2(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        drawPhaseBackground(gc, ctx, width, height, 2, 0.40);
        gc.setFill(Palette.PHASE2_DIM);
        gc.fillRect(0, 0, width, height);

        // Galeão e Baú
        double csx = ctx.getCamera().toScreenX(ctx.getChestWX());
        if (csx > -300 && csx < width + 300) {
            boolean open = ctx.isChestOpen();

            if (open) {
                lightingEngine.drawBioluminescentHalo(gc, csx + 10, ctx.getChestWY(), 95, Color.GOLD);
            } else if (ctx.canOpenChest()) {
                gc.setFill(Palette.CHEST_PROMPT_BG);
                gc.fillRoundRect(csx - 70, ctx.getChestWY() - 62, 140, 24, 8, 8);
                gc.setFill(Palette.GOLD); gc.setFont(UiFonts.TEXT_BOLD_12);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText("[ E / ESPAÇO: ABRIR BAÚ ]", csx, ctx.getChestWY() - 46);
            }
        }

        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, gx, g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        drawPhaseExit(gc, ctx.getCamera().toScreenX(3900), ctx.getTabletsCollected() >= 2);

        for (EnemyEntity e : ctx.getEnemies()) {
            drawVisibleEnemy(gc, ctx, e, width, timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawInterpolatedHero(gc, ctx, timeSeconds, true);
        ctx.getParticleSystem().render(gc);

        uiRenderer.drawHUD(gc, ctx.getHero(), ctx.getTabletsCollected(), "Fase 2 — Cavernas de Coral", ctx.getDifficulty(),
            OceanDepthProfile.forPhase(ctx.getCurrentPhase()));
        uiRenderer.drawMinimap(gc, ctx, width);
        drawAlert(gc, ctx, width, height);
    }

    // =========================================================
    // FASE 3 — Correntes Abissais
    // =========================================================
    private void drawP3(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        drawPhaseBackground(gc, ctx, width, height, 3, 0.35);

        // === REDESIGN 2026-08-20: BOLHAS ASCENDENTES DE FUNDO EM CAMADAS ===
        // 12 bolhas (era 18) sem strokeOval por frame — preenchimento simples.
        for (int bi = 0; bi < 12; bi++) {
            double bProgress = ((timeSeconds * (0.06 + (bi % 5) * 0.014) + bi * 0.072) % 1.0);
            double bx = (bi * 83 + Math.sin(timeSeconds * 0.4 + bi) * 28 + width * 0.7) % width;
            double by = height - bProgress * (height + 60);
            double br = 2.5 + (bi % 4) * 2.2;
            double alpha = Math.min(bProgress * 3, 1.0) * (1.0 - bProgress) * 0.25;
            gc.setGlobalAlpha(alpha);
            gc.setFill(ABYSS_BUBBLE_COLOR);
            gc.fillOval(bx - br, by - br, br * 2, br * 2);
            // strokeOval removido — impacto visual negligível, custo alto
        }
        gc.setGlobalAlpha(1.0);

        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, gx, g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        drawPhaseExit(gc, ctx.getCamera().toScreenX(3900), ctx.getTabletsCollected() >= 3);

        for (EnemyEntity e : ctx.getEnemies()) {
            drawVisibleEnemy(gc, ctx, e, width, timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawInterpolatedHero(gc, ctx, timeSeconds, true);
        ctx.getParticleSystem().render(gc);

        uiRenderer.drawHUD(gc, ctx.getHero(), ctx.getTabletsCollected(), "Fase 3 — Correntes Abissais", ctx.getDifficulty(),
            OceanDepthProfile.forPhase(ctx.getCurrentPhase()));
        uiRenderer.drawMinimap(gc, ctx, width);
        drawAlert(gc, ctx, width, height);
    }

    // =========================================================
    // FASE 4 — Abismo Vulcânico
    // =========================================================
    private void drawP4(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        drawPhaseBackground(gc, ctx, width, height, 4, 0.30);

        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, gx, g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        drawPhaseExit(gc, ctx.getCamera().toScreenX(3900), ctx.getTabletsCollected() >= 4);

        for (EnemyEntity e : ctx.getEnemies()) {
            drawVisibleEnemy(gc, ctx, e, width, timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawInterpolatedHero(gc, ctx, timeSeconds, true);
        ctx.getParticleSystem().render(gc);

        uiRenderer.drawHUD(gc, ctx.getHero(), ctx.getTabletsCollected(), "Fase 4 — Abismo Vulcânico", ctx.getDifficulty(),
            OceanDepthProfile.forPhase(ctx.getCurrentPhase()));
        uiRenderer.drawMinimap(gc, ctx, width);
        drawAlert(gc, ctx, width, height);
    }

    // =========================================================
    // FASE 5 — Templo Final de Apsu (arena boss)
    // =========================================================
    private void drawP5(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        drawPhaseBackground(gc, ctx, width, height, 5, 0.24);

        for (GuardianEntity g : ctx.getGuardians()) {
            drawGuardian(gc, g.getWorldX(), g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        if (ctx.getBoss() != null) {
            drawBoss(gc, ctx.getBoss(), timeSeconds, width);
        }

        drawProjectiles(gc, ctx);
        drawInterpolatedHero(gc, ctx, timeSeconds, false);
        ctx.getParticleSystem().render(gc);

        uiRenderer.drawHUD(gc, ctx.getHero(), ctx.getTabletsCollected(), "Fase 5 — Templo de Apsu", ctx.getDifficulty(),
            OceanDepthProfile.forPhase(ctx.getCurrentPhase()));
        uiRenderer.drawMinimap(gc, ctx, width);
        drawAlert(gc, ctx, width, height);
    }

    // =========================================================
    // ELEMENTOS VISUAIS AMBIENTAIS
    // =========================================================

    // =========================================================
    // HERÓI
    // =========================================================
    private void drawHero(GraphicsContext gc, double x, double y, HeroEntity hero,
                          double timeSeconds, long currentNanoTime) {
        final double targetW = 112;
        final double targetH = 205;

        double centerX = x + HeroEntity.HW / 2.0;
        double centerY = y + HeroEntity.HH / 2.0;
        drawShadow(gc, centerX, centerY + targetH * 0.44, targetW * 0.48, 10);
        lightingEngine.drawBioluminescentHalo(gc, centerX, centerY,
            targetW * 0.32, hero.getType().getAura());
    }

    // =========================================================
    // INIMIGOS
    // =========================================================
    private void drawVisibleEnemy(GraphicsContext gc, GameContext ctx, EnemyEntity enemy,
                                  double width, double timeSeconds) {
        if (!enemy.isAlive()) return;
        double x = ctx.getCamera().toScreenX(enemy.getWorldX());
        var eType = enemy.getType();
        if (x + eType.getWidth() < -120 || x > width + 120) return;
        double y = enemy.getCurrentY();
        double ew = eType.getWidth();
        double eh = eType.getHeight();

        drawShadow(gc, x + ew / 2, y + eh - 2, ew * 0.65, 9);
    }

    // =========================================================
    // BOSS
    // =========================================================
    private void drawBoss(GraphicsContext gc, BossEntity boss, double timeSeconds, double width) {
        drawShadow(gc, boss.getX() + BossEntity.BW / 2, boss.getY() + BossEntity.BH - 5,
            BossEntity.RENDER_W * 0.10, 16);

        // Aviso de transição fica preso acima do boss; não desenha aura/anel
        // pulsante em volta do modelo, que confundia o sinal de ataque.
        if (boss.isTelegraphing((long) (timeSeconds * 1_000_000_000L))) {
            double cueX = boss.getX() + BossEntity.BW / 2.0 - 74;
            if (cueX > width - 320) cueX -= 300;
            double cueY = Math.max(94, boss.getY() - 42);
            gc.setFill(Palette.BOSS_TELEGRAPH_BG);
            gc.fillRoundRect(cueX, cueY, 148, 26, 8, 8);
            gc.setStroke(Palette.BOSS_TELEGRAPH_STROKE);
            gc.setLineWidth(2);
            gc.strokeRoundRect(cueX, cueY, 148, 26, 8, 8);
            gc.setFill(Palette.BOSS_TELEGRAPH_TEXT);
            gc.setFont(UiFonts.TEXT_BOLD_12);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("ATAQUE · " + boss.getCombatPhase().getLabel(),
                boss.getX() + BossEntity.BW / 2.0, cueY + 17);
        }

        // Barra de vida do boss
        double hpRatio = (double) boss.getHp() / boss.getMaxHp();
        double barW = 360;
        double barX = (width - barW) / 2.0;
        double barY = 30;
        gc.setFill(Palette.BOSS_HP_BG);
        gc.fillRoundRect(barX, barY, barW, 10, 4, 4);
        gc.setFill(hpRatio > 0.5 ? Palette.BOSS_HP_HIGH : Palette.BOSS_HP_LOW);
        gc.fillRoundRect(barX, barY, barW * hpRatio, 10, 4, 4);
        gc.setStroke(Palette.BOSS_HP_STROKE); gc.setLineWidth(1.5);
        gc.strokeRoundRect(barX, barY, barW, 10, 4, 4);
        gc.setFont(UiFonts.TEXT_BOLD_11);
        gc.setFill(Palette.GOLD_BORDER);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(boss.getCombatPhase().getLabel(), width / 2.0, barY - 5);
    }

    private void drawGuardian(GraphicsContext gc, double x, double y, int type, boolean prompt, double timeSeconds) {
        lightingEngine.drawBioluminescentHalo(gc, x + GuardianEntity.GW / 2, y + GuardianEntity.GH / 2,
            GuardianEntity.GW * 0.9, Palette.GUARDIAN_HALO);
        drawShadow(gc, x + GuardianEntity.GW / 2, y + GuardianEntity.GH - 4, GuardianEntity.GW * 0.7, 16);

        if (prompt) {
            gc.setFill(Palette.GUARDIAN_PROMPT_BG);
            gc.fillRoundRect(x + GuardianEntity.GW / 2 - 85, y - 32, 170, 26, 8, 8);
            gc.setStroke(Palette.GOLD); gc.setLineWidth(1.5);
            gc.strokeRoundRect(x + GuardianEntity.GW / 2 - 85, y - 32, 170, 26, 8, 8);
            gc.setFill(Palette.GOLD); gc.setFont(UiFonts.HINT_BOLD_13);
            gc.setTextAlign(TextAlignment.CENTER); gc.fillText("[ E / ESPAÇO: CONVERSAR ]", x + GuardianEntity.GW / 2, y - 15);
        }
    }

    /** O texto instrui a descida; a ruína que marca a rota é uma malha 3D. */
    private void drawPhaseExit(GraphicsContext gc, double worldX, boolean unlocked) {
        Color signal = unlocked ? Palette.EXIT_UNLOCKED : Palette.EXIT_LOCKED;
        gc.setFont(UiFonts.TEXT_BOLD_11);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(signal);
        gc.fillText(unlocked ? "DESCIDA LIBERADA" : "RUÍNAS ANTIGAS", worldX, 520);
    }

    /** B3-FIX: projéteis renderizados convertendo coordenadas de mundo para tela com interpolação. */
    private void drawProjectiles(GraphicsContext gc, GameContext ctx) {
        boolean isP5 = (ctx.getState() == GameContext.State.P5);
        double alpha = ctx.getRenderAlpha();
        for (Projectile p : ctx.getProjectiles()) {
            // Converte para coordenadas de tela interpoladas
            double px = p.getInterpolatedX(alpha);
            double py = p.getInterpolatedY(alpha);
            double screenX = (isP5 || p.isArena()) ? px : ctx.getCamera().toScreenX(px, alpha);
            double screenY = py;

            if (p.getType() == Projectile.Type.HERO_BUBBLE) {
                double direction = Math.signum(p.getVx());
                gc.setFill(HERO_BUBBLE_TRAIL);
                gc.fillOval(screenX - direction * 14 - 3, screenY + 3, 6, 6);
                gc.fillOval(screenX - direction * 25 - 2, screenY - 4, 4, 4);
                gc.setFill(HERO_BUBBLE_FILL);
                gc.fillOval(screenX - 8, screenY - 8, 16, 16);
                gc.setStroke(HERO_BUBBLE_STROKE); gc.setLineWidth(1.5);
                gc.strokeOval(screenX - 8, screenY - 8, 16, 16);
                gc.setFill(HERO_BUBBLE_HIGHLIGHT);
                gc.fillOval(screenX - 5, screenY - 5, 5, 5); // highlight
            } else {
                gc.setFill(ENEMY_BULLET_FILL);
                gc.fillOval(screenX - 6, screenY - 6, 12, 12);
                gc.setFill(ENEMY_BULLET_HALO);
                gc.fillOval(screenX - 10, screenY - 10, 20, 20); // halo
            }
        }
    }

    private void drawAlert(GraphicsContext gc, GameContext ctx, double width, double height) {
        if (ctx.getAlertMessage().isEmpty()) return;
        long el = ctx.getNanoTime() - ctx.getAlertTime();
        if (el > 3_800_000_000L) return;
        double alpha = Math.min(1.0, (3_800_000_000L - el) / 700_000_000.0);
        gc.setFill(Palette.rgb(0, 6, 20, alpha * 0.92));
        gc.fillRoundRect(width / 2.0 - 410, height - 98, 820, 66, 14, 14);
        gc.setStroke(Palette.GOLD); gc.setLineWidth(1.5);
        gc.strokeRoundRect(width / 2.0 - 410, height - 98, 820, 66, 14, 14);
        gc.setFill(Palette.rgb(255, 215, 0, alpha));
        gc.setFont(UiFonts.ALERT_18);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(ctx.getAlertMessage(), width / 2.0, height - 60);
    }

    private void drawEnd(GraphicsContext gc, GameContext ctx, double width, double height, boolean win) {
        drawGradientBackground(gc, width, height, win ? Palette.GRADIENT_WIN : Palette.GRADIENT_OVER);
        ctx.getParticleSystem().render(gc);
        gc.setTextAlign(TextAlignment.CENTER); gc.setTextBaseline(VPos.CENTER);
        gc.setFont(UiFonts.TITLE_80);
        gc.setFill(win ? Palette.WIN_GOLD : Palette.OVER_RED);
        gc.fillText(win ? "VITORIA" : "GAME OVER", width / 2.0, 210);
        gc.setFont(UiFonts.TEXT_26);
        gc.setFill(win ? Palette.WIN_TEXT : Palette.OVER_TEXT);
        gc.fillText(win ? "Adapa purificou o oceano Apsu!" : "Adapa caiu nas profundezas...", width / 2.0, 320);
        gc.setFont(UiFonts.SUBTITLE_22);
        gc.setFill(Palette.GOLD);
        gc.fillText("Tabuletas: " + ctx.getTabletsCollected() + " / " + ctx.getTotalTablets(), width / 2.0, 390);
        gc.setFill(Palette.END_HERO); gc.setFont(UiFonts.TEXT_16);
        gc.fillText("Herói: " + ctx.getHeroType().getName() + "  |  Dificuldade: " + ctx.getDifficulty().getLabel(), width / 2.0, 430);
        gc.setFill(Palette.GOLD); gc.setFont(UiFonts.SUBTITLE_22);
        gc.fillText("[ ESPAÇO / R / ENTER — Menu Principal ]", width / 2.0, height - 80);
    }

    private void drawGradientBackground(GraphicsContext gc, double width, double height, LinearGradient gradient) {
        gc.setFill(gradient);
        gc.fillRect(0, 0, width, height);
    }

    private void drawShadow(GraphicsContext gc, double cx, double cy, double rw, double rh) {
        gc.setFill(SHADOW_COLOR);
        gc.fillOval(cx - rw / 2, cy - rh / 2, rw, rh);
    }
}
