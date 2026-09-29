package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.core.GameRenderer;
import br.apsu.model.boss.BossEntity;
import br.apsu.model.enemy.EnemyEntity;
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
    private boolean reducedEffects = true;
    private final Map<String, LinearGradient> backgroundGradients = new HashMap<>();
    private static final Map<String, Color> COLOR_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static final Color MENU_BUBBLE_COLOR = Color.rgb(80, 180, 255);
    private static final Color ABYSS_BUBBLE_COLOR = Color.rgb(80, 160, 255);
    private static final Color SHADOW_COLOR = Color.rgb(2, 8, 14, 0.40);
    private static final LinearGradient SEAFLOOR_GRADIENT = new LinearGradient(0, 730, 0, 768,
        false, CycleMethod.NO_CYCLE,
        new Stop(0, Color.rgb(10, 45, 22, 0)), new Stop(1, Color.rgb(10, 45, 22, 0.95)));

    public RenderEngine() {
        lightingEngine.setReducedEffects(true);
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
        drawGradientBackground(gc, width, height, "#040c1e", "#082040", "#0a3058");

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

        gc.setFont(Font.font("Serif", FontWeight.BOLD, 64));
        gc.setFill(Color.rgb(0, 60, 160, 0.28)); gc.fillText("AS AGUAS DE APSU", width / 2.0 + 3, 105);
        gc.setFill(color("#ffd700")); gc.fillText("AS AGUAS DE APSU", width / 2.0, 102);

        gc.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        gc.setFill(color("#70b8d8"));
        gc.fillText("A Lenda dos Apkallu", width / 2.0, 150);

        // B1-FIX: aspecto calculado da imagem ESTÁTICA (não do frame animado)
        HeroType hero = ctx.getHeroType();
        final double ph = 210;
        double aspect = 0.5;
        final double pw = ph * aspect; // LARGURA ESTÁVEL — não depende do frame animado

        double px = width * 0.82 - pw / 2.0, py = 150;

        drawShadow(gc, px + pw / 2, py + ph - 5, pw * 0.8, 14);
        // O herói do menu também é uma malha 3D, desenhada pela Runtime3DLayer.
        lightingEngine.drawBioluminescentHalo(gc, px + pw/2, py + ph/2, pw, hero.getAura());

        // Card Glassmorphic de Atributos
        gc.setFill(Color.rgb(4, 16, 36, 0.88));
        gc.fillRoundRect(px - 35, py + ph + 15, pw + 70, 145, 12, 12);
        gc.setStroke(color(hero.getAuraHex()).deriveColor(0, 1, 1, 0.6)); gc.setLineWidth(1.5);
        gc.strokeRoundRect(px - 35, py + ph + 15, pw + 70, 145, 12, 12);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        gc.setFill(color("#ffd700"));
        gc.fillText(hero.getName(), px - 25, py + ph + 35);

        drawStatBar(gc, "Velocidade",  hero.getMaxSpeed() / 16.5, px - 25, py + ph + 48,  pw + 50, color("#4db8ff"));
        drawStatBar(gc, "Agilidade",   hero.getAccel()    / 3.2,  px - 25, py + ph + 72,  pw + 50, color("#44cc88"));
        drawStatBar(gc, "Resistência", (hero.getDrag() - 0.85) / 0.12, px - 25, py + ph + 96, pw + 50, color("#ffaa44"));
        drawStatBar(gc, "Poder Bolha", hero.getPwr()      / 1.6,  px - 25, py + ph + 120, pw + 50, color("#e060ff"));

        // Menu 3 Opções
        gc.setTextAlign(TextAlignment.CENTER);
        String[] opts = {
            "  Dificuldade: " + ctx.getDifficulty().getLabel() + "  [A / D]",
            "  Heroi: " + hero.getName() + "  [A / D]",
            "  INICIAR JOGO"
        };
        double[] ys = {380, 455, 545};
        for (int i = 0; i < opts.length; i++) {
            boolean sel = (i == ctx.getMenuSel());
            if (sel) {
                gc.setFill(Color.rgb(0, 100, 220, 0.28));
                gc.fillRoundRect(width / 2.0 - 340, ys[i] - 32, 680, 62, 16, 16);
                gc.setStroke(color("#ffd700")); gc.setLineWidth(2);
                gc.strokeRoundRect(width / 2.0 - 340, ys[i] - 32, 680, 62, 16, 16);
            }
            gc.setFont(Font.font("Serif", sel ? FontWeight.BOLD : FontWeight.NORMAL, sel ? 28 : 22));
            gc.setFill(sel ? color("#ffd700") : color("#78b4cc"));
            gc.fillText(opts[i], width / 2.0, ys[i]);
        }

        gc.setFont(Font.font("Serif", 15));
        gc.setFill(color("#ffd700"));
        gc.fillText(ctx.getDifficulty().getDescription(), width / 2.0, 625);
    }

    private void drawStatBar(GraphicsContext gc, String label, double ratio, double x, double y, double width, Color color) {
        gc.setFont(Font.font("Serif", 11));
        gc.setFill(color("#a0c0e0"));
        gc.fillText(label, x, y + 10);
        double barX = x + 70, barW = width - 70, barH = 10;
        gc.setFill(Color.rgb(8, 24, 48, 0.9));
        gc.fillRoundRect(barX, y + 2, barW, barH, 4, 4);
        gc.setFill(color);
        gc.fillRoundRect(barX, y + 2, barW * Math.max(0.1, Math.min(1.0, ratio)), barH, 4, 4);
        gc.setStroke(color.deriveColor(0, 1, 1, 0.5)); gc.setLineWidth(1);
        gc.strokeRoundRect(barX, y + 2, barW, barH, 4, 4);
    }

    private void drawDialogue(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        drawGradientBackground(gc, width, height, "#030a14", "#081226", "#0a1e3c");

        gc.setFill(Color.rgb(4, 10, 28, 0.94));
        gc.fillRoundRect(240, height / 2.0 - 150, width - 310, 295, 22, 22);
        gc.setStroke(color("#0e3888")); gc.setLineWidth(2);
        gc.strokeRoundRect(240, height / 2.0 - 150, width - 310, 295, 22, 22);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        gc.setFill(color("#60a0e0"));
        gc.fillText("Enki — Senhor das Águas Primordiais:", 265, height / 2.0 - 118);
        gc.setFill(Color.WHITE); gc.setFont(Font.font("Serif", 20));
        String[] currentDlg = GuardianEntity.ENKI_INTRO_DIALOGUES[
            Math.min(ctx.getDlgPhase(), GuardianEntity.ENKI_INTRO_DIALOGUES.length - 1)];
        if (ctx.getDlgIdx() < currentDlg.length) uiRenderer.wrapText(gc, currentDlg[ctx.getDlgIdx()], 265, height / 2.0 - 76, width - 440, 34);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(color("#ffd700")); gc.setFont(Font.font("Serif", 15));
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

    // =========================================================
    // FASE 1 — Águas Claras
    // =========================================================
    private void drawP1(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        Image bg1 = spriteManager.getImage("backgrounds/phase-1.png");
        if (bg1 != null) {
            drawParallaxBackground(gc, bg1, ctx.getCamera().getX(), width, height, 0.40);
        } else {
            drawGradientBackground(gc, width, height, "#08192e", "#0e3050", "#144870");
        }

        lightingEngine.drawSunCaustics(gc, width, height, timeSeconds);

        // Faixa de areia/algas no rodapé
        gc.setFill(SEAFLOOR_GRADIENT);
        gc.fillRect(0, height - 38, width, 38);

        // Cenário
        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, gx, g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        drawPortal(gc, ctx.getCamera().toScreenX(4000 - 100), ctx.getTabletsCollected() >= 1, height, timeSeconds);

        for (EnemyEntity e : ctx.getEnemies()) {
            drawVisibleEnemy(gc, ctx, e, width, timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawHero(gc, ctx.getCamera().toScreenX(ctx.getHero().getX()), ctx.getHero().getY(), ctx.getHero(), timeSeconds);
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
        Image bg2 = spriteManager.getImage("backgrounds/phase-2.png");
        if (bg2 != null) {
            drawParallaxBackground(gc, bg2, ctx.getCamera().getX(), width, height, 0.40);
        } else {
            drawGradientBackground(gc, width, height, "#040c16", "#081826", "#0a2234");
        }


        // Galeão e Baú
        double csx = ctx.getCamera().toScreenX(ctx.getChestWX());
        if (csx > -300 && csx < width + 300) {
            boolean open = ctx.isChestOpen();

            if (open) {
                lightingEngine.drawBioluminescentHalo(gc, csx + 10, ctx.getChestWY(), 95, Color.GOLD);
            } else {
                gc.setFill(Color.rgb(0, 12, 28, 0.88));
                gc.fillRoundRect(csx - 70, ctx.getChestWY() - 62, 140, 24, 8, 8);
                gc.setFill(color("#ffd700")); gc.setFont(Font.font("Serif", FontWeight.BOLD, 12));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText("[ E / ESPAÇO: BAÚ ]", csx, ctx.getChestWY() - 46);
            }
        }

        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, gx, g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        drawPortal(gc, ctx.getCamera().toScreenX(4000 - 100), ctx.getTabletsCollected() >= 2, height, timeSeconds);

        for (EnemyEntity e : ctx.getEnemies()) {
            drawVisibleEnemy(gc, ctx, e, width, timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawHero(gc, ctx.getCamera().toScreenX(ctx.getHero().getX()), ctx.getHero().getY(), ctx.getHero(), timeSeconds);
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
        Image bg3 = spriteManager.getImage("backgrounds/phase-3.png");
        if (bg3 != null) {
            drawParallaxBackground(gc, bg3, ctx.getCamera().getX(), width, height, 0.35);
        } else {
            drawGradientBackground(gc, width, height, "#060012", "#110028", "#1a0040");
        }

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

        drawPortal(gc, ctx.getCamera().toScreenX(4000 - 100), ctx.getTabletsCollected() >= 3, height, timeSeconds);

        for (EnemyEntity e : ctx.getEnemies()) {
            drawVisibleEnemy(gc, ctx, e, width, timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawHero(gc, ctx.getCamera().toScreenX(ctx.getHero().getX()), ctx.getHero().getY(), ctx.getHero(), timeSeconds);
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
        Image bg4 = spriteManager.getImage("backgrounds/phase-4.png");
        if (bg4 != null) {
            drawParallaxBackground(gc, bg4, ctx.getCamera().getX(), width, height, 0.30);
        } else {
            drawGradientBackground(gc, width, height, "#1a0500", "#380800", "#5a0a00");
        }

        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, gx, g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        drawPortal(gc, ctx.getCamera().toScreenX(4000 - 100), ctx.getTabletsCollected() >= 4, height, timeSeconds);

        for (EnemyEntity e : ctx.getEnemies()) {
            drawVisibleEnemy(gc, ctx, e, width, timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawHero(gc, ctx.getCamera().toScreenX(ctx.getHero().getX()), ctx.getHero().getY(), ctx.getHero(), timeSeconds);
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
        Image bg5 = spriteManager.getImage("backgrounds/phase-5.png");
        if (bg5 != null) {
            drawParallaxBackground(gc, bg5, ctx.getCamera().getX(), width, height, 0.24);
        } else {
            drawGradientBackground(gc, width, height, "#080018", "#150030", "#200050");
        }

        for (GuardianEntity g : ctx.getGuardians()) {
            drawGuardian(gc, g.getWorldX(), g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        if (ctx.getBoss() != null) {
            drawBoss(gc, ctx.getBoss(), timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawHero(gc, ctx.getHero().getX(), ctx.getHero().getY(), ctx.getHero(), timeSeconds);
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
    private void drawHero(GraphicsContext gc, double x, double y, HeroEntity hero, double timeSeconds) {
        if (hero.isInvulnerable()) {
            gc.setGlobalAlpha(0.45);
        }

        boolean shooting = hero.isShooting();
        final double targetH = HeroEntity.HH;
        double targetW = 120;

        double attackX = shooting ? hero.getAttackOffsetX() : 0;
        double attackY = shooting ? hero.getAttackOffsetY() : 0;
        x += attackX;
        y += attackY;
        drawShadow(gc, x + HeroEntity.HW / 2, y + HeroEntity.HH - 3, targetW * 0.55, 12);

        // A pose 3D é renderizada pela Runtime3DLayer.

        lightingEngine.drawBioluminescentHalo(gc, x + HeroEntity.HW/2, y + HeroEntity.HH/2,
            targetW * 0.32, hero.getType().getAura());
        if (shooting) {
            drawVariantAttackCue(gc, x, y, hero, timeSeconds);
        }
        if (hero.isInvulnerable()) {
            gc.setGlobalAlpha(1.0);
        }
    }

    /** Feedback de ataque para skins sem sequência Blender dedicada. */
    private void drawVariantAttackCue(GraphicsContext gc, double x, double y, HeroEntity hero, double timeSeconds) {
        double direction = hero.isFacingRight() ? 1 : -1;
        double originX = x + HeroEntity.HW / 2 + direction * 34;
        double originY = y + HeroEntity.HH * 0.42;
        double pulse = 0.65 + Math.sin(timeSeconds * 28) * 0.25;
        gc.save();
        gc.setStroke(hero.getType().getAura().deriveColor(0, 1, 1, pulse));
        gc.setLineWidth(4);
        gc.strokeLine(originX - direction * 18, originY + 16, originX + direction * 38, originY - 18);
        gc.setFill(Color.rgb(210, 250, 255, pulse));
        gc.fillOval(originX + direction * 30 - 7, originY - 25, 14, 14);
        gc.restore();
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
        // Inimigos 3D são mantidos fora da camada Canvas.
    }

    // =========================================================
    // BOSS
    // =========================================================
    private void drawBoss(GraphicsContext gc, BossEntity boss, double timeSeconds) {
        drawShadow(gc, boss.getX() + BossEntity.BW / 2, boss.getY() + BossEntity.BH - 5, BossEntity.BW * 0.7, 22);
        // O modelo 3D do boss é desenhado pela Runtime3DLayer.

        // Telegrafo antes da troca de estágio: contraste alto e duração curta,
        // para ser lido sem encobrir o sprite ou o HUD.
        if (boss.isTelegraphing((long) (timeSeconds * 1_000_000_000L))) {
            double pulse = 0.45 + 0.35 * Math.sin(timeSeconds * 28);
            gc.setStroke(Color.rgb(255, 190, 40, pulse));
            gc.setLineWidth(5);
            gc.strokeOval(boss.getX() - 14, boss.getY() - 14, BossEntity.BW + 28, BossEntity.BH + 28);
        }

        // Barra de vida do boss
        double hpRatio = (double) boss.getHp() / boss.getMaxHp();
        double barW = BossEntity.BW * 0.9;
        double barX = boss.getX() + BossEntity.BW * 0.05;
        double barY = boss.getY() - 18;
        gc.setFill(Color.rgb(20, 0, 0, 0.8));
        gc.fillRoundRect(barX, barY, barW, 10, 4, 4);
        gc.setFill(hpRatio > 0.5 ? color("#ff4444") : color("#ff8800"));
        gc.fillRoundRect(barX, barY, barW * hpRatio, 10, 4, 4);
        gc.setStroke(Color.rgb(255, 60, 60, 0.7)); gc.setLineWidth(1.5);
        gc.strokeRoundRect(barX, barY, barW, 10, 4, 4);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 11));
        gc.setFill(color("#ffd27a"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(boss.getCombatPhase().getLabel(), boss.getX() + BossEntity.BW / 2, barY - 5);
    }

    private void drawGuardian(GraphicsContext gc, double x, double y, int type, boolean prompt, double timeSeconds) {
        lightingEngine.drawBioluminescentHalo(gc, x + GuardianEntity.GW / 2, y + GuardianEntity.GH / 2,
            GuardianEntity.GW * 0.9, color("#40e0ff"));
        drawShadow(gc, x + GuardianEntity.GW / 2, y + GuardianEntity.GH - 4, GuardianEntity.GW * 0.7, 16);

        if (prompt) {
            gc.setFill(Color.rgb(0, 8, 24, 0.90));
            gc.fillRoundRect(x + GuardianEntity.GW / 2 - 85, y - 32, 170, 26, 8, 8);
            gc.setStroke(color("#ffd700")); gc.setLineWidth(1.5);
            gc.strokeRoundRect(x + GuardianEntity.GW / 2 - 85, y - 32, 170, 26, 8, 8);
            gc.setFill(color("#ffd700")); gc.setFont(Font.font("Serif", FontWeight.BOLD, 13));
            gc.setTextAlign(TextAlignment.CENTER); gc.fillText("[ E / ESPAÇO: CONVERSAR ]", x + GuardianEntity.GW / 2, y - 15);
        }
    }

    private void drawPortal(GraphicsContext gc, double px, boolean unlocked, double height, double timeSeconds) {
        double centerX = px + 8;
        double centerY = height / 2.0 - 16;
        // A geometria do portal vem da malha 3D da Runtime3DLayer.
        double pulse = 0.88 + Math.sin(timeSeconds * 2.4) * 0.12;
        lightingEngine.drawBioluminescentHalo(gc, centerX, centerY, 104 * pulse,
            unlocked ? color("#28d9ff") : color("#d94a5e"));

        // Indicador de estado compacto; a geometria do portal vem do sprite 3D.
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(unlocked ? color("#75ecff") : color("#ff8290"));
        gc.fillText(unlocked ? "PORTAL ABERTO" : "PORTAL SELADO", centerX, centerY + 84);
    }

    /** B3-FIX: projéteis renderizados convertendo coordenadas de mundo para tela. */
    private void drawProjectiles(GraphicsContext gc, GameContext ctx) {
        boolean isP5 = (ctx.getState() == GameContext.State.P5);
        for (Projectile p : ctx.getProjectiles()) {
            // Converte para coordenadas de tela (para P1-P4 com câmera; P5 já está em tela)
            double screenX = (isP5 || p.isArena()) ? p.getX() : ctx.getCamera().toScreenX(p.getX());
            double screenY = p.getY();

            if (p.getType() == Projectile.Type.HERO_BUBBLE) {
                // Bolha bonita com brilho
                gc.setFill(Color.rgb(160, 240, 255, 0.85));
                gc.fillOval(screenX - 8, screenY - 8, 16, 16);
                gc.setStroke(Color.rgb(0, 200, 255, 0.7)); gc.setLineWidth(1.5);
                gc.strokeOval(screenX - 8, screenY - 8, 16, 16);
                gc.setFill(Color.rgb(255, 255, 255, 0.5));
                gc.fillOval(screenX - 5, screenY - 5, 5, 5); // highlight
            } else {
                gc.setFill(color("#ff2a4b"));
                gc.fillOval(screenX - 6, screenY - 6, 12, 12);
                gc.setFill(Color.rgb(255, 80, 80, 0.4));
                gc.fillOval(screenX - 10, screenY - 10, 20, 20); // halo
            }
        }
    }

    private void drawAlert(GraphicsContext gc, GameContext ctx, double width, double height) {
        if (ctx.getAlertMessage().isEmpty()) return;
        long el = ctx.getNanoTime() - ctx.getAlertTime();
        if (el > 3_800_000_000L) return;
        double alpha = Math.min(1.0, (3_800_000_000L - el) / 700_000_000.0);
        gc.setFill(Color.rgb(0, 6, 20, alpha * 0.92));
        gc.fillRoundRect(width / 2.0 - 410, height - 98, 820, 66, 14, 14);
        gc.setStroke(color("#ffd700")); gc.setLineWidth(1.5);
        gc.strokeRoundRect(width / 2.0 - 410, height - 98, 820, 66, 14, 14);
        gc.setFill(Color.rgb(255, 215, 0, alpha));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(ctx.getAlertMessage(), width / 2.0, height - 60);
    }

    private void drawEnd(GraphicsContext gc, GameContext ctx, double width, double height, boolean win) {
        drawGradientBackground(gc, width, height,
            win ? "#041408" : "#140408", win ? "#0a2d10" : "#2a0810", win ? "#103a1a" : "#400a16");
        ctx.getParticleSystem().render(gc);
        gc.setTextAlign(TextAlignment.CENTER); gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 80));
        gc.setFill(win ? color("#ffd700") : color("#ff2828"));
        gc.fillText(win ? "VITORIA" : "GAME OVER", width / 2.0, 210);
        gc.setFont(Font.font("Serif", 26));
        gc.setFill(win ? color("#88ffaa") : color("#ff9090"));
        gc.fillText(win ? "Adapa purificou o oceano Apsu!" : "Adapa caiu nas profundezas...", width / 2.0, 320);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        gc.setFill(color("#ffd700"));
        gc.fillText("Tabuletas: " + ctx.getTabletsCollected() + " / " + ctx.getTotalTablets(), width / 2.0, 390);
        gc.setFill(color("#80b8d0")); gc.setFont(Font.font("Serif", 16));
        gc.fillText("Herói: " + ctx.getHeroType().getName() + "  |  Dificuldade: " + ctx.getDifficulty().getLabel(), width / 2.0, 430);
        gc.setFill(color("#ffd700")); gc.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        gc.fillText("[ ESPAÇO / R / ENTER — Menu Principal ]", width / 2.0, height - 80);
    }

    private void drawGradientBackground(GraphicsContext gc, double width, double height, String top, String mid, String bot) {
        String key = top + '|' + mid + '|' + bot;
        LinearGradient gradient = backgroundGradients.computeIfAbsent(key, ignored ->
            new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, color(top)), new Stop(0.5, color(mid)), new Stop(1, color(bot))));
        gc.setFill(gradient);
        gc.fillRect(0, 0, width, height);
    }

    private static Color color(String web) {
        return COLOR_CACHE.computeIfAbsent(web, Color::web);
    }

    private void drawShadow(GraphicsContext gc, double cx, double cy, double rw, double rh) {
        gc.setFill(SHADOW_COLOR);
        gc.fillOval(cx - rw / 2, cy - rh / 2, rw, rh);
    }
}
