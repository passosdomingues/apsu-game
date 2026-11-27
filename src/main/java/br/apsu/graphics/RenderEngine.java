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
import br.apsu.guidance.ObjectiveTracker;
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
    private final AdaptiveRenderController adaptiveController = new AdaptiveRenderController();
    private final LightingEngine lightingEngine = new LightingEngine();
    private final UIRenderer uiRenderer = new UIRenderer();
    private final BossTelegraphRenderer bossTelegraphRenderer = new BossTelegraphRenderer();
    private final VirtualKeyboardOverlay keyboardOverlay = new VirtualKeyboardOverlay();
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
    // Projéteis de Lava (APSU-B18A: Inimigos e Boss Kullullû)
    private static final Color LAVA_AURA = Color.rgb(255, 60, 0, 0.35);
    private static final Color LAVA_OUTER = Color.rgb(230, 45, 10, 0.95);
    private static final Color LAVA_CORE = Color.rgb(255, 215, 80, 1.0);
    private static final Color LAVA_HOTSPOT = Color.rgb(255, 255, 230, 0.95);
    private static final Color LAVA_TRAIL = Color.rgb(255, 120, 20, 0.45);
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
    @Override
    public void recordFrameSample(br.apsu.core.FrameMetrics.Sample sample) {
        adaptiveController.recordSample(sample);
    }

    private GraphicsContext currentUiGc;

    public javafx.scene.Node get3DView() { return runtime3DLayer.view(); }
    public void setViewportScale(double scale) { runtime3DLayer.setViewportScale(scale); }
    public AdaptiveRenderController getAdaptiveRenderController() { return adaptiveController; }
    public BossTelegraphRenderer getBossTelegraphRenderer() { return bossTelegraphRenderer; }

    @Override
    public void render(GraphicsContext gc, GameContext ctx, double width, double height) {
        render(gc, null, ctx, width, height);
    }

    @Override
    public void render(GraphicsContext backgroundGc, GraphicsContext foregroundGc, GameContext ctx, double width, double height) {
        GraphicsContext gc = backgroundGc;
        this.currentUiGc = (foregroundGc != null) ? foregroundGc : backgroundGc;
        if (foregroundGc != null) {
            foregroundGc.clearRect(0, 0, width, height);
        }
        gc.clearRect(0, 0, width, height);
        double t = ctx.getNanoTime() / 1_000_000_000.0;
        adaptiveController.onPhaseChanged(ctx.getCurrentPhase());
        runtime3DLayer.setSecondaryElementsDisabled(adaptiveController.isSecondaryIn2D());
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
            case PAUSE    -> drawPause(gc, ctx, width, height, t);
            case WIN      -> drawEnd(gc, ctx, width, height, true);
            case OVER     -> drawEnd(gc, ctx, width, height, false);
        }
        gc.restore();

        GraphicsContext targetUi = targetUi(gc);
        if (ctx.isOverlayActive()) {
            uiRenderer.drawDialogueOverlay(targetUi, width, height, t, ctx.getOverlayName(),
                ctx.getOverlayLines(), ctx.getOverlayIdx(), ctx.getOverlayCharsShown());
        } else {
            uiRenderer.drawObjectiveArrow(targetUi, ctx, width, height, t);
            GameContext.State s = ctx.getState();
            if (s == GameContext.State.P1 || s == GameContext.State.P2 || s == GameContext.State.P3 || s == GameContext.State.P4) {
                uiRenderer.drawEchoPrompt(targetUi, ctx, width, height, t);
            }
        }

        GameContext.State state = ctx.getState();
        if (state == GameContext.State.P1 || state == GameContext.State.P2
            || state == GameContext.State.P3 || state == GameContext.State.P4
            || state == GameContext.State.P5) {
            keyboardOverlay.renderGameplayOverlay(targetUi, ctx.getInputManager(),
                ctx.getTutorialController(), ctx, width, height, t);
        }
    }

    private GraphicsContext targetUi(GraphicsContext fallback) {
        return (currentUiGc != null) ? currentUiGc : fallback;
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

        // APSU-U18B / APSU-U18C: Painel interativo de tutorial e demonstração com teclado virtual
        keyboardOverlay.renderMenuShowcase(gc, 36, 175, 345, 490, timeSeconds);

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

        // Menu principal dinâmico (APSU-M19E)
        gc.setTextAlign(TextAlignment.CENTER);
        boolean canCont = ctx.canContinue();
        String[] opts;
        double[] ys;
        if (canCont) {
            int contPhase = Math.min(5, ctx.getPhaseDone() + 1);
            opts = new String[]{
                "▶  CONTINUAR  [FASE " + contPhase + "]",
                "✦  NOVO JOGO",
                "DIFICULDADE     ▾     " + ctx.getDifficulty().getLabel()
            };
            ys = new double[]{380, 460, 540};
        } else {
            opts = new String[]{
                "DIFICULDADE     ▾     " + ctx.getDifficulty().getLabel(),
                "▶  INICIAR JOGO"
            };
            ys = new double[]{400, 500};
        }

        for (int i = 0; i < opts.length; i++) {
            boolean sel = (i == ctx.getMenuSel());
            if (sel) {
                gc.setFill(Palette.MENU_SELECTED_BG);
                gc.fillRoundRect(width / 2.0 - 280, ys[i] - 28, 560, 56, 16, 16);
                gc.setStroke(Palette.GOLD); gc.setLineWidth(2);
                gc.strokeRoundRect(width / 2.0 - 280, ys[i] - 28, 560, 56, 16, 16);
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
        gc.fillText("[ ESPAÇO / E: Avançar  •  X: Pular Diálogo ]", width / 2.0 + 80, height / 2.0 + 136);
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
            drawGuardian(gc, ctx, g, gx, g.getWorldY(), g.isPromptVisible(), timeSeconds);
        }

        drawPhaseExit(gc, ctx.getCamera().toScreenX(3900), ctx.getTabletsCollected() >= 1);

        for (EnemyEntity e : ctx.getEnemies()) {
            drawVisibleEnemy(gc, ctx, e, width, timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawSacredTablet(gc, ctx, timeSeconds);
        ctx.getEchoWaveRenderer().render(gc, ctx, width, height);
        drawInterpolatedHero(gc, ctx, timeSeconds, true);
        ctx.getParticleSystem().render(gc);

        GraphicsContext ui = targetUi(gc);
        ObjectiveTracker.Objective objP1 = ObjectiveTracker.resolveCurrentObjective(ctx);
        String objTextP1 = objP1 != null ? objP1.description() : null;
        uiRenderer.drawHUD(ui, ctx.getHero(), ctx.getTabletsCollected(), "Fase 1 — Águas Claras", ctx.getDifficulty(),
            OceanDepthProfile.forPhase(ctx.getCurrentPhase()), 0.0, objTextP1, ctx.getEchoController());
        uiRenderer.drawMinimap(ui, ctx, width);
        drawAlert(ui, ctx, width, height);
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
            } else {
                if (ctx.getTabletsCollected() < 2) {
                    double tabY = ctx.getChestWY() - 45 + Math.sin(timeSeconds * 3.5) * 8;
                    lightingEngine.drawTabletGlow(gc, csx, tabY, 28.0, timeSeconds);
                }
                if (ctx.isChestPromptVisible()) {
                    gc.setFill(Palette.CHEST_PROMPT_BG);
                    gc.fillRoundRect(csx - 70, ctx.getChestWY() - 62, 140, 24, 8, 8);
                    gc.setFill(Palette.GOLD); gc.setFont(UiFonts.TEXT_BOLD_12);
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText("[ E / ESPAÇO: ABRIR BAÚ ]", csx, ctx.getChestWY() - 46);
                }
            }
        }

        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, ctx, g, gx, g.getWorldY(), g.isPromptVisible(), timeSeconds);
        }

        drawPhaseExit(gc, ctx.getCamera().toScreenX(3900), ctx.getTabletsCollected() >= 2);

        for (EnemyEntity e : ctx.getEnemies()) {
            drawVisibleEnemy(gc, ctx, e, width, timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawSacredTablet(gc, ctx, timeSeconds);
        ctx.getEchoWaveRenderer().render(gc, ctx, width, height);
        drawInterpolatedHero(gc, ctx, timeSeconds, true);
        ctx.getParticleSystem().render(gc);

        double unlockHighlight = 0.0;
        if (ctx.isChestOpen() && ctx.getChestTime() > 0) {
            long elapsed = ctx.getNanoTime() - ctx.getChestTime();
            if (elapsed < 2_000_000_000L) {
                unlockHighlight = 1.0 - (elapsed / 2_000_000_000.0);
            }
        }
        GraphicsContext ui = targetUi(gc);
        ObjectiveTracker.Objective objP2 = ObjectiveTracker.resolveCurrentObjective(ctx);
        String objTextP2 = objP2 != null ? objP2.description() : null;
        uiRenderer.drawHUD(ui, ctx.getHero(), ctx.getTabletsCollected(), "Fase 2 — Cavernas de Coral", ctx.getDifficulty(),
            OceanDepthProfile.forPhase(ctx.getCurrentPhase()), unlockHighlight, objTextP2, ctx.getEchoController());
        uiRenderer.drawMinimap(ui, ctx, width);
        drawAlert(ui, ctx, width, height);
    }

    // =========================================================
    // FASE 3 — Correntes Abissais
    // =========================================================
    private void drawP3(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        drawPhaseBackground(gc, ctx, width, height, 3, 0.35);

        // === REDESIGN 2026-08-20: BOLHAS ASCENDENTES DE FUNDO EM CAMADAS ===
        // Reduzido para 6 bolhas em modo reducedEffects para preservar performance
        int bubbleCount = reducedEffects ? 6 : 12;
        for (int bi = 0; bi < bubbleCount; bi++) {
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
            drawGuardian(gc, ctx, g, gx, g.getWorldY(), g.isPromptVisible(), timeSeconds);
        }

        drawPhaseExit(gc, ctx.getCamera().toScreenX(3900), ctx.getTabletsCollected() >= 3);

        for (EnemyEntity e : ctx.getEnemies()) {
            drawVisibleEnemy(gc, ctx, e, width, timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawSacredTablet(gc, ctx, timeSeconds);
        ctx.getEchoWaveRenderer().render(gc, ctx, width, height);
        drawInterpolatedHero(gc, ctx, timeSeconds, true);
        ctx.getParticleSystem().render(gc);

        GraphicsContext ui = targetUi(gc);
        ObjectiveTracker.Objective objP3 = ObjectiveTracker.resolveCurrentObjective(ctx);
        String objTextP3 = objP3 != null ? objP3.description() : null;
        uiRenderer.drawHUD(ui, ctx.getHero(), ctx.getTabletsCollected(), "Fase 3 — Correntes Abissais", ctx.getDifficulty(),
            OceanDepthProfile.forPhase(ctx.getCurrentPhase()), 0.0, objTextP3, ctx.getEchoController());
        uiRenderer.drawMinimap(ui, ctx, width);
        drawAlert(ui, ctx, width, height);
    }

    // =========================================================
    // FASE 4 — Abismo Vulcânico
    // =========================================================
    private void drawP4(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        drawPhaseBackground(gc, ctx, width, height, 4, 0.30);

        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, ctx, g, gx, g.getWorldY(), g.isPromptVisible(), timeSeconds);
        }

        drawPhaseExit(gc, ctx.getCamera().toScreenX(3900), ctx.getTabletsCollected() >= 4);

        for (EnemyEntity e : ctx.getEnemies()) {
            drawVisibleEnemy(gc, ctx, e, width, timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawSacredTablet(gc, ctx, timeSeconds);
        ctx.getEchoWaveRenderer().render(gc, ctx, width, height);
        drawInterpolatedHero(gc, ctx, timeSeconds, true);
        ctx.getParticleSystem().render(gc);

        GraphicsContext ui = targetUi(gc);
        ObjectiveTracker.Objective objP4 = ObjectiveTracker.resolveCurrentObjective(ctx);
        String objTextP4 = objP4 != null ? objP4.description() : null;
        uiRenderer.drawHUD(ui, ctx.getHero(), ctx.getTabletsCollected(), "Fase 4 — Abismo Vulcânico", ctx.getDifficulty(),
            OceanDepthProfile.forPhase(ctx.getCurrentPhase()), 0.0, objTextP4, ctx.getEchoController());
        uiRenderer.drawMinimap(ui, ctx, width);
        drawAlert(ui, ctx, width, height);
    }

    // =========================================================
    // FASE 5 — Templo Final de Apsu (arena boss)
    // =========================================================
    private void drawP5(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        drawPhaseBackground(gc, ctx, width, height, 5, 0.24);

        for (GuardianEntity g : ctx.getGuardians()) {
            drawGuardian(gc, ctx, g, g.getWorldX(), g.getWorldY(), g.isPromptVisible(), timeSeconds);
        }

        if (ctx.getBoss() != null) {
            bossTelegraphRenderer.render(gc, ctx.getBoss(), width, height, ctx.getNanoTime());
            drawBoss(gc, ctx.getBoss(), timeSeconds, width);
        }

        drawProjectiles(gc, ctx);
        drawSacredTablet(gc, ctx, timeSeconds);
        ctx.getEchoWaveRenderer().render(gc, ctx, width, height);
        drawInterpolatedHero(gc, ctx, timeSeconds, false);
        ctx.getParticleSystem().render(gc);

        GraphicsContext ui = targetUi(gc);
        ObjectiveTracker.Objective objP5 = ObjectiveTracker.resolveCurrentObjective(ctx);
        String objTextP5 = objP5 != null ? objP5.description() : null;
        uiRenderer.drawHUD(ui, ctx.getHero(), ctx.getTabletsCollected(), "Fase 5 — Templo de Apsu", ctx.getDifficulty(),
            OceanDepthProfile.forPhase(ctx.getCurrentPhase()), 0.0, objTextP5, ctx.getEchoController());
        uiRenderer.drawMinimap(ui, ctx, width);
        drawAlert(ui, ctx, width, height);
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

        if (hero.isDying()) {
            double prog = hero.getDeathProgress();
            gc.save();
            gc.setGlobalAlpha(Math.max(0.15, 1.0 - prog * 0.75));
            drawShadow(gc, centerX, centerY + targetH * 0.44, targetW * 0.48 * (1.0 - prog * 0.5), 10);
            lightingEngine.drawBioluminescentHalo(gc, centerX, centerY, targetW * 0.32 * (1.0 - prog * 0.8), hero.getType().getAura());
            if (!adaptiveController.isHeroIn3D()) {
                Character2DFallbackRenderer.renderHero(gc, null, hero, x, y, timeSeconds);
            }
            gc.restore();
            return;
        }

        drawShadow(gc, centerX, centerY + targetH * 0.44, targetW * 0.48, 10);
        lightingEngine.drawBioluminescentHalo(gc, centerX, centerY,
            targetW * 0.32, hero.getType().getAura());

        // US-008 / US-009: Fallback 2D para heroi no Canvas quando Scene3D indisponivel ou forcado em 2D
        if (!adaptiveController.isHeroIn3D()) {
            Character2DFallbackRenderer.renderHero(gc, null, hero, x, y, timeSeconds);
        }
    }

    // =========================================================
    // INIMIGOS
    // =========================================================
    private void drawVisibleEnemy(GraphicsContext gc, GameContext ctx, EnemyEntity enemy,
                                  double width, double timeSeconds) {
        if (!enemy.isAlive() && !enemy.isDying()) return;
        double x = ctx.getCamera().toScreenX(enemy.getWorldX());
        var eType = enemy.getType();
        if (x + eType.getWidth() < -120 || x > width + 120) return;
        double y = enemy.getCurrentY();
        double ew = eType.getWidth();
        double eh = eType.getHeight();

        gc.save();
        if (enemy.isDying()) {
            double prog = enemy.getDeathProgress();
            gc.setGlobalAlpha(Math.max(0.0, 1.0 - prog));
            gc.translate(x + ew / 2.0, y + eh / 2.0);
            gc.rotate(prog * 180.0);
            gc.translate(-(x + ew / 2.0), -(y + eh / 2.0));
        }

        drawShadow(gc, x + ew / 2, y + eh - 2, ew * 0.65, 9);

        // US-008 / US-009: Fallback 2D para inimigos no Canvas quando 2D completo ou modo hibrido
        if (adaptiveController.isSecondaryIn2D()) {
            Character2DFallbackRenderer.renderEnemy(gc, enemy, x, y);
        }
        gc.restore();
    }

    // =========================================================
    // BOSS
    // =========================================================
    private void drawBoss(GraphicsContext gc, BossEntity boss, double timeSeconds, double width) {
        drawShadow(gc, boss.getX() + BossEntity.BW / 2, boss.getY() + BossEntity.BH - 5,
            BossEntity.RENDER_W * 0.10, 16);

        // US-008 / US-009: Fallback 2D para Boss no Canvas quando 2D completo ou modo hibrido
        if (adaptiveController.isSecondaryIn2D()) {
            Character2DFallbackRenderer.renderBoss(gc, boss, timeSeconds);
        }

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

    private void drawSacredTablet(GraphicsContext gc, GameContext ctx, double timeSeconds) {
        br.apsu.model.environment.SacredTabletEntity tablet = ctx.getSacredTablet();
        if (tablet != null && tablet.isRevealed()) {
            double sx = ctx.getCamera().toScreenX(tablet.getWorldX());
            // APSU-P17: culling — ignora tabuleta fora da tela
            if (sx < -80 || sx > 1446) return;
            double sy = tablet.getWorldY() + Math.sin(timeSeconds * 3.5) * 8.0;
            lightingEngine.drawTabletGlow(gc, sx, sy, 36.0, timeSeconds);
            gc.save();
            gc.setGlobalAlpha(tablet.getRevealProgress());
            gc.setFill(Palette.GOLD);
            gc.fillRoundRect(sx - 18, sy - 22, 36, 44, 6, 6);
            gc.setStroke(Palette.GOLD_LIGHT);
            gc.setLineWidth(2.0);
            gc.strokeRoundRect(sx - 18, sy - 22, 36, 44, 6, 6);
            gc.setStroke(Palette.rgb(30, 20, 0, 0.8));
            gc.setLineWidth(1.5);
            gc.strokeLine(sx - 10, sy - 12, sx + 10, sy - 12);
            gc.strokeLine(sx - 8, sy - 4, sx + 8, sy - 4);
            gc.strokeLine(sx - 10, sy + 4, sx + 10, sy + 4);
            gc.strokeLine(sx - 6, sy + 12, sx + 6, sy + 12);
            gc.restore();
        }
    }

    private void drawGuardian(GraphicsContext gc, GameContext ctx, GuardianEntity guardian, double x, double y, boolean prompt, double timeSeconds) {
        // APSU-P17: culling — guardião completamente fora da tela não é desenhado
        if (x + GuardianEntity.GW < -80 || x > 1446) return;

        lightingEngine.drawBioluminescentHalo(gc, x + GuardianEntity.GW / 2, y + GuardianEntity.GH / 2,
            GuardianEntity.GW * 0.9 * guardian.getScaleEnki(), Palette.GUARDIAN_HALO);
        drawShadow(gc, x + GuardianEntity.GW / 2, y + GuardianEntity.GH - 4, GuardianEntity.GW * 0.7 * guardian.getScaleEnki(), 16);

        // US-008 / US-009: Fallback 2D para guardiao no Canvas quando Scene3D indisponivel ou 2D completo
        if (!adaptiveController.isGuardianIn3D()) {
            double prevAlpha = gc.getGlobalAlpha();
            gc.setGlobalAlpha(prevAlpha * Math.max(0.1, guardian.getAlphaEnki()));
            GuardianEntity dummy = new GuardianEntity(guardian.getType(), x, y);
            Character2DFallbackRenderer.renderGuardian(gc, dummy, x, y);
            gc.setGlobalAlpha(prevAlpha);
        }

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
        // APSU-P17: culling — ignora portal de descida fora da tela
        if (worldX < -200 || worldX > 1566) return;
        Color signal = unlocked ? Palette.EXIT_UNLOCKED : Palette.EXIT_LOCKED;
        gc.setFont(UiFonts.TEXT_BOLD_11);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(signal);
        gc.fillText(unlocked ? "DESCIDA LIBERADA" : "RUÍNAS ANTIGAS", worldX, 520);
    }

    /** B3-FIX: projéteis renderizados convertendo coordenadas de mundo para tela com interpolação.
     *  APSU-P17: culling de viewport — só desenha projéteis dentro dos limites da tela. */
    private void drawProjectiles(GraphicsContext gc, GameContext ctx) {
        boolean isP5 = (ctx.getState() == GameContext.State.P5);
        double alpha = ctx.getRenderAlpha();
        for (Projectile p : ctx.getProjectiles()) {
            // Converte para coordenadas de tela interpoladas
            double px = p.getInterpolatedX(alpha);
            double py = p.getInterpolatedY(alpha);
            double screenX = (isP5 || p.isArena()) ? px : ctx.getCamera().toScreenX(px, alpha);
            double screenY = py;

            // APSU-P17: culling — projétil completamente fora da tela não é desenhado
            if (screenX < -32 || screenX > 1398 || screenY < -32 || screenY > 800) continue;

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
            } else if (p.getType() == Projectile.Type.BOSS_BULLET) {
                // APSU-B18A: Projétil vulcânico do Boss Kullullû (esfera colossal de lava incandescente)
                double direction = Math.signum(p.getVx());
                gc.setFill(LAVA_TRAIL);
                gc.fillOval(screenX - direction * 16 - 5, screenY + 2, 10, 10);
                gc.fillOval(screenX - direction * 30 - 3, screenY - 4, 6, 6);
                gc.setFill(LAVA_AURA);
                gc.fillOval(screenX - 18, screenY - 18, 36, 36);
                gc.setFill(LAVA_OUTER);
                gc.fillOval(screenX - 12, screenY - 12, 24, 24);
                gc.setFill(LAVA_CORE);
                gc.fillOval(screenX - 7, screenY - 7, 14, 14);
                gc.setFill(LAVA_HOTSPOT);
                gc.fillOval(screenX - 3, screenY - 3, 6, 6);
            } else {
                // ENEMY_MALIGN: Projétil de lava incandescente dos inimigos
                double direction = Math.signum(p.getVx());
                gc.setFill(LAVA_TRAIL);
                gc.fillOval(screenX - direction * 12 - 2, screenY + 2, 5, 5);
                gc.setFill(ENEMY_BULLET_HALO);
                gc.fillOval(screenX - 11, screenY - 11, 22, 22);
                gc.setFill(LAVA_OUTER);
                gc.fillOval(screenX - 8, screenY - 8, 16, 16);
                gc.setFill(LAVA_CORE);
                gc.fillOval(screenX - 4, screenY - 4, 8, 8);
                gc.setFill(LAVA_HOTSPOT);
                gc.fillOval(screenX - 2, screenY - 2, 4, 4);
            }
        }
    }

    private void drawAlert(GraphicsContext gc, GameContext ctx, double width, double height) {
        if (ctx.isOverlayActive()) return; // AC3: Ocultar alertas enquanto diálogo estiver aberto
        String msg = ctx.getAlertMessage();
        boolean isTutorialHint = msg.isEmpty()
                && ctx.getState() == GameContext.State.P1
                && ctx.getTutorialController() != null && ctx.getTutorialController().isActive();
        if (isTutorialHint) {
            msg = ctx.getTutorialController().getCurrentInstruction();
        }
        if (msg == null || msg.isEmpty()) return;

        double alpha = 1.0;
        if (!isTutorialHint) {
            long el = ctx.getNanoTime() - ctx.getAlertTime();
            if (el > 3_800_000_000L) return;
            alpha = Math.min(1.0, (3_800_000_000L - el) / 700_000_000.0);
        }

        if (isTutorialHint) {
            // Tutorial: pill estilo imersivo no canto inferior esquerdo com ícone de tecla destacado
            drawTutorialPill(gc, msg, alpha, width, height);
        } else {
            // Alerta de fase: banner central padrão
            gc.setFill(Palette.rgb(0, 6, 20, alpha * 0.92));
            gc.fillRoundRect(width / 2.0 - 410, height - 98, 820, 66, 14, 14);
            gc.setStroke(Palette.GOLD); gc.setLineWidth(1.5);
            gc.strokeRoundRect(width / 2.0 - 410, height - 98, 820, 66, 14, 14);
            gc.setFill(Palette.rgb(255, 215, 0, alpha));
            gc.setFont(UiFonts.ALERT_18);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(msg, width / 2.0, height - 60);
        }
    }

    /**
     * Renderiza dica de tutorial como pílula discreta no canto inferior esquerdo.
     * Formato esperado do msg: "[KEY] Descrição curta da ação"
     * A parte [KEY] é renderizada como pílula dourada, o resto em branco.
     */
    private void drawTutorialPill(GraphicsContext gc, String msg, double alpha, double width, double height) {
        // Extrai [KEY] do início da string
        String keyLabel = "";
        String description = msg;
        if (msg.startsWith("[") && msg.contains("]")) {
            int end = msg.indexOf(']');
            keyLabel = msg.substring(1, end);
            description = msg.substring(end + 1).trim();
        }

        double pillX = 24, pillY = height - 72;
        double keyW = keyLabel.isEmpty() ? 0 : (keyLabel.length() * 10.0 + 20);
        double textW = description.length() * 9.0 + 20;
        double totalW = keyW + (keyW > 0 ? 10 : 0) + textW;
        double pillH = 42;

        // Fundo da pílula
        gc.setGlobalAlpha(alpha * 0.90);
        gc.setFill(Palette.rgb(5, 10, 30, 0.95));
        gc.fillRoundRect(pillX, pillY, totalW, pillH, 12, 12);
        gc.setStroke(Palette.rgb(255, 215, 0, 0.60)); gc.setLineWidth(1.5);
        gc.strokeRoundRect(pillX, pillY, totalW, pillH, 12, 12);

        double textY = pillY + pillH / 2.0 + 6;

        if (!keyLabel.isEmpty()) {
            // Pílula de tecla: dourada
            gc.setFill(Palette.rgb(255, 215, 0, 0.95));
            gc.fillRoundRect(pillX + 8, pillY + 7, keyW - 4, pillH - 14, 6, 6);
            gc.setFill(Palette.rgb(5, 10, 30, 1.0));
            gc.setFont(UiFonts.HINT_BOLD_13);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(keyLabel, pillX + 8 + (keyW - 4) / 2.0 - keyLabel.length() * 4.5, textY);
        }

        // Descrição em branco
        gc.setFill(Palette.rgb(230, 235, 255, 0.95));
        gc.setFont(UiFonts.TEXT_19);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(description, pillX + keyW + (keyW > 0 ? 10 : 8), textY);

        gc.setGlobalAlpha(1.0);
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
        gc.fillText("[ R — Reiniciar Fase  |  ESPAÇO / ENTER — Menu Principal ]", width / 2.0, height - 80);
    }

    private void drawPause(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        // Desenha a fase correspondente no fundo
        switch (ctx.getStateBeforePause()) {
            case P1 -> drawP1(gc, ctx, width, height, timeSeconds);
            case P2 -> drawP2(gc, ctx, width, height, timeSeconds);
            case P3 -> drawP3(gc, ctx, width, height, timeSeconds);
            case P4 -> drawP4(gc, ctx, width, height, timeSeconds);
            case P5 -> drawP5(gc, ctx, width, height, timeSeconds);
            default -> drawP1(gc, ctx, width, height, timeSeconds);
        }

        GraphicsContext ui = targetUi(gc);
        // Overlay de escurecimento suave
        ui.setFill(Palette.rgb(2, 10, 26, 0.78));
        ui.fillRect(0, 0, width, height);

        // Painel central de pausa glassmorphic
        double pw = 520, ph = 340;
        double px = (width - pw) / 2.0;
        double py = (height - ph) / 2.0;

        ui.setFill(Palette.rgb(6, 18, 40, 0.94));
        ui.fillRoundRect(px, py, pw, ph, 18, 18);
        ui.setStroke(Palette.GOLD);
        ui.setLineWidth(2.2);
        ui.strokeRoundRect(px, py, pw, ph, 18, 18);

        ui.setTextAlign(TextAlignment.CENTER);
        ui.setTextBaseline(VPos.CENTER);
        ui.setFont(UiFonts.TITLE_64);
        ui.setFill(Palette.GOLD);
        ui.fillText("PAUSA", width / 2.0, py + 55);

        ui.setFont(UiFonts.TEXT_BOLD_12);
        ui.setFill(Palette.rgb(140, 210, 255, 0.95));
        ui.fillText("Fase " + ctx.getCurrentPhase() + " — " + getPhaseTitle(ctx.getCurrentPhase()), width / 2.0, py + 95);

        String[] pauseOpts = {
            "▶  CONTINUAR  [ESC / P]",
            "↺  REINICIAR FASE  [R]",
            "⌂  MENU PRINCIPAL  [M]"
        };
        double startY = py + 155;
        for (int i = 0; i < pauseOpts.length; i++) {
            boolean sel = (i == ctx.getPauseMenuSel());
            double optY = startY + i * 54;
            if (sel) {
                ui.setFill(Palette.MENU_SELECTED_BG);
                ui.fillRoundRect(px + 36, optY - 22, pw - 72, 44, 12, 12);
                ui.setStroke(Palette.GOLD);
                ui.setLineWidth(1.8);
                ui.strokeRoundRect(px + 36, optY - 22, pw - 72, 44, 12, 12);
            }
            ui.setFont(sel ? UiFonts.SUBTITLE_22 : UiFonts.TEXT_16);
            ui.setFill(sel ? Palette.GOLD : Palette.MENU_UNSELECTED);
            ui.fillText(pauseOpts[i], width / 2.0, optY);
        }
    }

    private String getPhaseTitle(int phase) {
        return switch (phase) {
            case 1 -> "Águas Claras";
            case 2 -> "Cavernas de Coral";
            case 3 -> "Correntes Abissais";
            case 4 -> "Abismo Vulcânico";
            case 5 -> "Templo de Apsu";
            default -> "Águas de Apsu";
        };
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
