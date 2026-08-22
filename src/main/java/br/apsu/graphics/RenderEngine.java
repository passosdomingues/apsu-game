package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.model.boss.BossEntity;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.environment.Projectile;
import br.apsu.model.environment.SceneryElement;
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
 * Renderizador mestre desacoplado.
 * Sprint 5: Bug B1 corrigido (aspecto estável no menu), novas fases P3-P5,
 * visualização de correntes, zonas de pressão, obstáculos vulcânicos.
 */
public class RenderEngine {

    private final SpriteManager spriteManager = SpriteManager.getInstance();
    private final LightingEngine lightingEngine = new LightingEngine();
    private final UIRenderer uiRenderer = new UIRenderer();
    private boolean reducedEffects;

    // Gradients e Cores estáticos para evitar alocações a cada frame (GC zero)
    private static final LinearGradient LAVA_FOOTER_GRADIENT = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0, Color.rgb(255, 80, 0, 0)),
        new Stop(0.5, Color.rgb(255, 80, 0, 0.35)),
        new Stop(1, Color.rgb(220, 30, 0, 0.92)));

    private static final LinearGradient LAVA_POOL_GRADIENT = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0, Color.rgb(255, 120, 0, 0.85)),
        new Stop(0.6, Color.rgb(220, 40, 0, 0.95)),
        new Stop(1, Color.rgb(160, 10, 0, 0.98)));

    private static final Color LAVA_WAVE_COLOR = Color.rgb(255, 140, 0, 0.6);
    private static final Color LAVA_GLOW_COLOR = Color.rgb(255, 60, 0, 0.08);
    private static final Color LAVA_BORDER_COLOR = Color.rgb(255, 220, 0, 0.9);
    private static final Color LAVA_REFLECT_COLOR = Color.rgb(255, 200, 0, 0.25);
    private static final Color LAVA_BUBBLE_COLOR = Color.rgb(255, 180, 20, 0.60);
    private static final Color LAVA_STEAM_COLOR = Color.rgb(200, 100, 60, 0.20);

    private static final Color[] EMBER_COLORS = {
        Color.rgb(255, 200, 0, 0.85),
        Color.rgb(255, 150, 0, 0.65),
        Color.rgb(255, 100, 0, 0.45),
        Color.rgb(200, 50, 0, 0.25)
    };

    private static final String[] GUARDIAN_ANIM_DIRS;
    static {
        GUARDIAN_ANIM_DIRS = new String[GuardianEntity.SPRITE_PATHS.length];
        for (int i = 0; i < GuardianEntity.SPRITE_PATHS.length; i++) {
            String path = GuardianEntity.SPRITE_PATHS[i];
            GUARDIAN_ANIM_DIRS[i] = path.substring(0, path.lastIndexOf('.'));
        }
    }

    /** Qualidade dinâmica: o Canvas continua no thread JavaFX, mas os efeitos
     * mais caros cedem espaço para a jogabilidade quando há queda de FPS. */
    public void setReducedEffects(boolean reducedEffects) {
        this.reducedEffects = reducedEffects;
        lightingEngine.setReducedEffects(reducedEffects);
    }

    public boolean isReducedEffects() { return reducedEffects; }

    private static final String[] ATTACK_STYLE_NAMES = {"thrust", "slash", "spin", "charge"};

    public void render(GraphicsContext gc, GameContext ctx, double width, double height) {
        gc.clearRect(0, 0, width, height);
        double t = ctx.getNanoTime() / 1_000_000_000.0;

        // Shake é só uma transformação visual: coordenadas de mundo, colisões
        // e HUD continuam calculados normalmente pelo GameContext.
        gc.save();
        gc.translate(ctx.getCamera().getShakeX(ctx.getNanoTime()), ctx.getCamera().getShakeY(ctx.getNanoTime()));
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
            int gt = Math.min(ctx.getOverlayGuardianType(), GuardianEntity.SPRITE_PATHS.length - 1);
            Image portrait = spriteManager.getImage(GuardianEntity.SPRITE_PATHS[gt]);
            uiRenderer.drawDialogueOverlay(gc, width, height, t, ctx.getOverlayName(),
                portrait, ctx.getOverlayLines(), ctx.getOverlayIdx(), ctx.getOverlayCharsShown());
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
            gc.setFill(Color.rgb(80, 180, 255, 0.09 + Math.sin(timeSeconds + i) * 0.03));
            gc.fillOval(bx, by, 6 + (i % 5) * 10, 6 + (i % 5) * 10);
        }

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        gc.setFont(Font.font("Serif", FontWeight.BOLD, 64));
        gc.setFill(Color.rgb(0, 60, 160, 0.28)); gc.fillText("AS AGUAS DE APSU", width / 2.0 + 3, 105);
        gc.setFill(Color.web("#ffd700")); gc.fillText("AS AGUAS DE APSU", width / 2.0, 102);

        gc.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        gc.setFill(Color.web("#70b8d8"));
        gc.fillText("A Lenda dos Apkallu", width / 2.0, 150);

        // B1-FIX: aspecto calculado da imagem ESTÁTICA (não do frame animado)
        HeroType hero = ctx.getHeroType();
        Image imgStatic = spriteManager.getImage(hero.getStaticSpritePath());

        // Calcula aspecto UMA VEZ a partir da imagem estática — nunca muda entre frames
        final double ph = 210;
        double aspect = (imgStatic != null && imgStatic.getHeight() > 0)
            ? imgStatic.getWidth() / imgStatic.getHeight()
            : 0.5;  // fallback seguro
        final double pw = ph * aspect; // LARGURA ESTÁVEL — não depende do frame animado

        double px = width * 0.82 - pw / 2.0, py = 150;

        drawShadow(gc, px + pw / 2, py + ph - 5, pw * 0.8, 14);
        // Uma textura fixa no menu: o ciclo Blender tinha pivôs inconsistentes
        // e causava tremulação perceptível antes mesmo de iniciar a partida.
        if (imgStatic != null) {
            gc.drawImage(imgStatic, px, py, pw, ph);
        } else {
            gc.setFill(hero.getAura());
            gc.fillRoundRect(px, py, pw, ph, 16, 16);
        }
        lightingEngine.drawBioluminescentHalo(gc, px + pw/2, py + ph/2, pw, hero.getAura());

        // Card Glassmorphic de Atributos
        gc.setFill(Color.rgb(4, 16, 36, 0.88));
        gc.fillRoundRect(px - 35, py + ph + 15, pw + 70, 145, 12, 12);
        gc.setStroke(Color.web(hero.getAuraHex()).deriveColor(0, 1, 1, 0.6)); gc.setLineWidth(1.5);
        gc.strokeRoundRect(px - 35, py + ph + 15, pw + 70, 145, 12, 12);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        gc.setFill(Color.web("#ffd700"));
        gc.fillText(hero.getName(), px - 25, py + ph + 35);

        drawStatBar(gc, "Velocidade",  hero.getMaxSpeed() / 16.5, px - 25, py + ph + 48,  pw + 50, Color.web("#4db8ff"));
        drawStatBar(gc, "Agilidade",   hero.getAccel()    / 3.2,  px - 25, py + ph + 72,  pw + 50, Color.web("#44cc88"));
        drawStatBar(gc, "Resistência", (hero.getDrag() - 0.85) / 0.12, px - 25, py + ph + 96, pw + 50, Color.web("#ffaa44"));
        drawStatBar(gc, "Poder Bolha", hero.getPwr()      / 1.6,  px - 25, py + ph + 120, pw + 50, Color.web("#e060ff"));

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
                gc.setStroke(Color.web("#ffd700")); gc.setLineWidth(2);
                gc.strokeRoundRect(width / 2.0 - 340, ys[i] - 32, 680, 62, 16, 16);
            }
            gc.setFont(Font.font("Serif", sel ? FontWeight.BOLD : FontWeight.NORMAL, sel ? 28 : 22));
            gc.setFill(sel ? Color.web("#ffd700") : Color.web("#78b4cc"));
            gc.fillText(opts[i], width / 2.0, ys[i]);
        }

        gc.setFont(Font.font("Serif", 15));
        gc.setFill(Color.web("#ffd700"));
        gc.fillText(ctx.getDifficulty().getDescription(), width / 2.0, 625);
    }

    private void drawStatBar(GraphicsContext gc, String label, double ratio, double x, double y, double width, Color color) {
        gc.setFont(Font.font("Serif", 11));
        gc.setFill(Color.web("#a0c0e0"));
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

        Image enkiImg = switch (ctx.getDlgPhase()) {
            case 1 -> spriteManager.getImage("sprites/enki/03_enki_var_eremita.png");
            case 2 -> spriteManager.getImage("sprites/enki/03_enki_var_celestial.png");
            case 4 -> spriteManager.getImage("sprites/enki/03_enki_npc.png"); // Enki em pessoa na F5
            default -> spriteManager.getImage("sprites/enki/03_enki_npc.png");
        };

        if (enkiImg != null) {
            double eh2 = 320, ew2 = eh2 * 0.49;
            double bob = Math.sin(timeSeconds * 2.5) * 6;
            gc.drawImage(enkiImg, 35, height / 2.0 - eh2 / 2 - 30 + bob, ew2, eh2);
        }

        gc.setFill(Color.rgb(4, 10, 28, 0.94));
        gc.fillRoundRect(240, height / 2.0 - 150, width - 310, 295, 22, 22);
        gc.setStroke(Color.web("#0e3888")); gc.setLineWidth(2);
        gc.strokeRoundRect(240, height / 2.0 - 150, width - 310, 295, 22, 22);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        gc.setFill(Color.web("#60a0e0"));
        gc.fillText("Enki — Senhor das Águas Primordiais:", 265, height / 2.0 - 118);
        gc.setFill(Color.WHITE); gc.setFont(Font.font("Serif", 20));
        String[] currentDlg = GuardianEntity.DIALOGUES[Math.min(ctx.getDlgPhase(), GuardianEntity.DIALOGUES.length - 1)];
        if (ctx.getDlgIdx() < currentDlg.length) uiRenderer.wrapText(gc, currentDlg[ctx.getDlgIdx()], 265, height / 2.0 - 76, width - 440, 34);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#ffd700")); gc.setFont(Font.font("Serif", 15));
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
        Image bg1 = spriteManager.getImage("bg1.png");
        if (bg1 != null) {
            drawParallaxBackground(gc, bg1, ctx.getCamera().getX(), width, height, 0.40);
        } else {
            drawGradientBackground(gc, width, height, "#08192e", "#0e3050", "#144870");
        }

        lightingEngine.drawSunCaustics(gc, width, height, timeSeconds);

        // Faixa de areia/algas no rodapé
        gc.setFill(new LinearGradient(0, height - 38, 0, height, false, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(10, 45, 22, 0)),
            new Stop(1, Color.rgb(10, 45, 22, 0.95))));
        gc.fillRect(0, height - 38, width, 38);

        // Cenário
        for (SceneryElement elem : ctx.getSceneryElements()) {
            double cx = ctx.getCamera().toScreenX(elem.getWorldX());
            if (cx < -150 || cx > width + 150) continue;
            if (elem.getType() == SceneryElement.Type.GEYSER) {
                lightingEngine.drawGeyserHeatGlow(gc, cx, elem.getWorldY(), elem.getWidth(), elem.getHeight());
                drawGeyser(gc, cx + elem.getWidth()/2, elem.getWorldY(), elem.getWidth(), timeSeconds, Color.AQUA);
            }
        }

        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, gx, g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        drawPortal(gc, ctx.getCamera().toScreenX(4000 - 100), ctx.getTabletsCollected() >= 1, height, timeSeconds);

        for (EnemyEntity e : ctx.getEnemies()) {
            if (!e.isAlive()) continue;
            drawEnemy(gc, ctx.getCamera().toScreenX(e.getWorldX()), e.getCurrentY(), e.getType().getId(), timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawHero(gc, ctx.getCamera().toScreenX(ctx.getHero().getX()), ctx.getHero().getY(), ctx.getHero(), timeSeconds);
        ctx.getParticleSystem().render(gc);

        uiRenderer.drawHUD(gc, ctx.getHero(), ctx.getTabletsCollected(), "Fase 1 — Águas Claras", ctx.getDifficulty());
        uiRenderer.drawMinimap(gc, ctx, width);
        drawAlert(gc, ctx, width, height);
    }

    // =========================================================
    // FASE 2 — Cavernas de Coral
    // =========================================================
    private void drawP2(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        Image bg2 = spriteManager.getImage("bg2.png");
        if (bg2 != null) {
            drawParallaxBackground(gc, bg2, ctx.getCamera().getX(), width, height, 0.40);
        } else {
            drawGradientBackground(gc, width, height, "#040c16", "#081826", "#0a2234");
        }

        // === REDESIGN 2026-08-20: FUNDO RICO COM VIDA MARINHA DECORATIVA ===
        double camX = ctx.getCamera().getX();

        // Recifes decorativos no plano de fundo (sem colisão, paralaxe 0.2)
        Image recife = spriteManager.getImage("sprites/scenery/08_recifes_e_cardume_elemento-cenario.png");
        for (int ri = 0; ri < 5; ri++) {
            double rWorldX = 600 + ri * 680;
            double rxScreen = rWorldX - camX * 0.20;
            if (rxScreen < -200 || rxScreen > width + 200) continue;
            double ry = height - 180 - (ri % 3) * 50;
            if (recife != null) {
                gc.setGlobalAlpha(0.30 + (ri % 2) * 0.12);
                gc.drawImage(recife, rxScreen - 80, ry, 180, 160);
                gc.setGlobalAlpha(1.0);
            } else {
                // fallback: pequenos arbustos de coral no fundo
                gc.setFill(Color.rgb(0, 80, 120, 0.18));
                gc.fillOval(rxScreen - 40, ry + 40, 80, 80);
            }
        }

        // Polvos/medusas decorativas animadas no fundo (paralaxe 0.15)
        for (int pi = 0; pi < 4; pi++) {
            double pWorldX = 800 + pi * 750;
            double pxScreen = pWorldX - camX * 0.15;
            if (pxScreen < -100 || pxScreen > width + 100) continue;
            double py = 120 + pi * 130;
            double pulse = Math.sin(timeSeconds * 1.5 + pi * 1.8) * 0.08;
            gc.setFill(Color.rgb(100, 30, 160, 0.12));
            gc.fillOval(pxScreen - 22, py, 44 * (1 + pulse), 44);
            // tentáculos
            for (int ti = 0; ti < 5; ti++) {
                double tentX = pxScreen - 16 + ti * 8;
                double tentLen = 18 + Math.sin(timeSeconds * 2 + ti * 0.8 + pi) * 8;
                gc.setStroke(Color.rgb(120, 40, 180, 0.10));
                gc.setLineWidth(1.5);
                gc.strokeLine(tentX, py + 44, tentX + Math.sin(timeSeconds + ti) * 5, py + 44 + tentLen);
            }
        }

        // Galeão e Baú
        double csx = ctx.getCamera().toScreenX(ctx.getChestWX());
        if (csx > -300 && csx < width + 300) {
            Image galeao3D = spriteManager.getImage("sprites/scenery/07_navio_naufragado_elemento-cenario.png");
            if (galeao3D != null) {
                gc.drawImage(galeao3D, csx - 180, ctx.getChestWY() - 140, 450, 260);
            } else {
                // fallback: casco afundado estilizado
                gc.setFill(Color.rgb(8, 22, 44, 0.88));
                gc.fillPolygon(new double[]{csx - 140, csx - 40, csx + 180, csx + 130},
                               new double[]{ctx.getChestWY() + 80, ctx.getChestWY() - 60, ctx.getChestWY() - 60, ctx.getChestWY() + 80}, 4);
                gc.setStroke(Color.rgb(0, 100, 160, 0.5)); gc.setLineWidth(2);
                gc.strokeLine(csx - 40, ctx.getChestWY() - 60, csx - 40, ctx.getChestWY() - 120);
            }

            boolean open = ctx.isChestOpen();
            Image bau3D = spriteManager.getImage("sprites/scenery/06_bau_tesouro_elemento-cenario.png");
            if (bau3D != null) {
                gc.drawImage(bau3D, csx - 40, ctx.getChestWY() - 35, 100, 75);
            } else {
                gc.setFill(open ? Color.web("#ffd700") : Color.web("#8b5a2b"));
                gc.fillRoundRect(csx - 30, ctx.getChestWY() - 25, 60, 45, 10, 10);
            }

            if (open) {
                lightingEngine.drawBioluminescentHalo(gc, csx + 10, ctx.getChestWY(), 95, Color.GOLD);
            } else {
                gc.setFill(Color.rgb(0, 12, 28, 0.88));
                gc.fillRoundRect(csx - 70, ctx.getChestWY() - 62, 140, 24, 8, 8);
                gc.setFill(Color.web("#ffd700")); gc.setFont(Font.font("Serif", FontWeight.BOLD, 12));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText("[ E / ESPAÇO: BAÚ ]", csx, ctx.getChestWY() - 46);
            }
        }

        // === CORAIS ORGÂNICOS E BIOLUMINESCENTES ===
        // Cores de bioluminescência rotacionando por coluna
        Color[] coralGlows = {
            Color.web("#00ffee"), Color.web("#aa44ff"), Color.web("#00ff88"),
            Color.web("#44aaff"), Color.web("#ff44aa"), Color.web("#88ffff")
        };

        int coralIdx = 0;
        for (SceneryElement elem : ctx.getSceneryElements()) {
            double cx = ctx.getCamera().toScreenX(elem.getWorldX());
            if (cx < -140 || cx > width + 140) continue;

            if (elem.getType() == SceneryElement.Type.CORAL) {
                boolean isTop = (elem.getWorldY() == 0);
                Color glow = coralGlows[coralIdx % coralGlows.length];
                coralIdx++;

                drawOrganicCoral(gc, cx, elem.getWorldY(), elem.getWidth(), elem.getHeight(), isTop, glow, timeSeconds);

                // Halo bioluminescente na ponta do coral
                double glowY = isTop ? elem.getHeight() - 18 : elem.getWorldY() + 18;
                lightingEngine.drawBioluminescentHalo(gc, cx + elem.getWidth() / 2, glowY, 28, glow);

            } else if (elem.getType() == SceneryElement.Type.GEYSER) {
                lightingEngine.drawGeyserHeatGlow(gc, cx, elem.getWorldY(), elem.getWidth(), elem.getHeight());
                drawGeyser(gc, cx + elem.getWidth()/2, elem.getWorldY(), elem.getWidth(), timeSeconds, Color.AQUA);
            }
        }

        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, gx, g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        drawPortal(gc, ctx.getCamera().toScreenX(4000 - 100), ctx.getTabletsCollected() >= 2, height, timeSeconds);

        for (EnemyEntity e : ctx.getEnemies()) {
            if (!e.isAlive()) continue;
            drawEnemy(gc, ctx.getCamera().toScreenX(e.getWorldX()), e.getCurrentY(), e.getType().getId(), timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawHero(gc, ctx.getCamera().toScreenX(ctx.getHero().getX()), ctx.getHero().getY(), ctx.getHero(), timeSeconds);
        ctx.getParticleSystem().render(gc);

        uiRenderer.drawHUD(gc, ctx.getHero(), ctx.getTabletsCollected(), "Fase 2 — Cavernas de Coral", ctx.getDifficulty());
        uiRenderer.drawMinimap(gc, ctx, width);
        drawAlert(gc, ctx, width, height);
    }

    // =========================================================
    // FASE 3 — Correntes Abissais
    // =========================================================
    private void drawP3(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        Image bg3 = spriteManager.getImage("bg3.png");
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
            gc.setFill(Color.rgb(80, 160, 255, alpha));
            gc.fillOval(bx - br, by - br, br * 2, br * 2);
            // strokeOval removido — impacto visual negligível, custo alto
        }

        // Elementos ambientais
        for (SceneryElement elem : ctx.getSceneryElements()) {
            double cx = ctx.getCamera().toScreenX(elem.getWorldX());
            if (cx < -200 || cx > width + 200) continue;

            switch (elem.getType()) {
                case CURRENT -> drawCurrentZone(gc, cx, elem.getWorldY(), elem.getWidth(), elem.getHeight(),
                                                elem.getCurrentVx(), elem.getCurrentVy(), timeSeconds);
                case PRESSURE_ZONE -> drawPressureZone(gc, cx, elem.getWorldY(), elem.getWidth(), elem.getHeight(),
                                                        elem.getBuoyancyMult(), timeSeconds);
                case MOVING_OBSTACLE -> {
                    Image frame = spriteManager.getImage("sprites/scenery/13_obstaculo_abissal.png");
                    if (frame != null) {
                        gc.drawImage(frame, cx, elem.getWorldY(), elem.getWidth(), elem.getHeight());
                    } else {
                        gc.setFill(Color.rgb(25, 10, 55, 0.90));
                        gc.fillRoundRect(cx, elem.getWorldY(), elem.getWidth(), elem.getHeight(), 14, 14);
                    }
                    lightingEngine.drawBioluminescentHalo(gc, cx + elem.getWidth()/2, elem.getWorldY() + elem.getHeight()/2,
                        elem.getWidth() * 0.35, Color.web("#6020cc"));
                }
                case GEYSER -> {
                    lightingEngine.drawGeyserHeatGlow(gc, cx, elem.getWorldY(), elem.getWidth(), elem.getHeight());
                    drawGeyser(gc, cx + elem.getWidth()/2, elem.getWorldY(), elem.getWidth(), timeSeconds, Color.CYAN);
                }
                default -> {}
            }
        }

        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, gx, g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        drawPortal(gc, ctx.getCamera().toScreenX(4000 - 100), ctx.getTabletsCollected() >= 3, height, timeSeconds);

        for (EnemyEntity e : ctx.getEnemies()) {
            if (!e.isAlive()) continue;
            drawEnemy(gc, ctx.getCamera().toScreenX(e.getWorldX()), e.getCurrentY(), e.getType().getId(), timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawHero(gc, ctx.getCamera().toScreenX(ctx.getHero().getX()), ctx.getHero().getY(), ctx.getHero(), timeSeconds);
        ctx.getParticleSystem().render(gc);

        uiRenderer.drawHUD(gc, ctx.getHero(), ctx.getTabletsCollected(), "Fase 3 — Correntes Abissais", ctx.getDifficulty());
        uiRenderer.drawMinimap(gc, ctx, width);
        drawAlert(gc, ctx, width, height);
    }

    // =========================================================
    // FASE 4 — Abismo Vulcânico
    // =========================================================
    private void drawP4(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        Image bg4 = spriteManager.getImage("bg4.png");
        if (bg4 != null) {
            drawParallaxBackground(gc, bg4, ctx.getCamera().getX(), width, height, 0.30);
        } else {
            drawGradientBackground(gc, width, height, "#1a0500", "#380800", "#5a0a00");
        }

        // === REDESIGN 2026-08-20: FAIXA DE LAVA PULSANTE NO RODAPÉ (visual, não colisão) ===
        double lavaBaseY = height - 22;
        gc.setFill(new LinearGradient(0, lavaBaseY - 30, 0, height, false, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 80, 0, 0)),
            new Stop(0.5, Color.rgb(255, 80, 0, 0.35)),
            new Stop(1, Color.rgb(220, 30, 0, 0.92))));
        gc.fillRect(0, lavaBaseY - 30, width, height - lavaBaseY + 30);

        // Ondas de lava — 8 (era 12) é suficiente para a impressão de movimento
        gc.setFill(Color.rgb(255, 140, 0, 0.6));
        for (int wi = 0; wi < 8; wi++) {
            double waveX = (wi * 115 + timeSeconds * 25) % (width + 60) - 30;
            double waveH = 6 + Math.sin(timeSeconds * 2.5 + wi * 0.9) * 3;
            gc.fillOval(waveX - 30, lavaBaseY - waveH, 60, waveH * 2);
        }

        // Brilho de lava difuso — 4 halos (era 6)
        for (int i = 0; i < 4; i++) {
            double gx = (i * 220 + timeSeconds * 12 + 50) % (width + 200) - 100;
            double gy = height - 60 + Math.sin(timeSeconds * 1.5 + i) * 18;
            gc.setFill(Color.rgb(255, 60, 0, 0.07 + Math.sin(timeSeconds * 2.2 + i) * 0.03));
            gc.fillOval(gx - 80, gy - 20, 160, 40);
        }

        // Elementos do cenário
        for (SceneryElement elem : ctx.getSceneryElements()) {
            double cx = ctx.getCamera().toScreenX(elem.getWorldX());
            if (cx < -200 || cx > width + 200) continue;

            switch (elem.getType()) {
                case LAVA_POOL -> {
                    double pulse = 0.82 + Math.sin(timeSeconds * 2.8 + elem.getWorldX() * 0.01) * 0.18;
                    // Pool principal pulsante
                    gc.setFill(new LinearGradient(cx, elem.getWorldY(), cx, elem.getWorldY() + elem.getHeight(), false,
                        CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 120, 0, 0.85 * pulse)),
                        new Stop(0.6, Color.rgb(220, 40, 0, 0.95 * pulse)),
                        new Stop(1, Color.rgb(160, 10, 0, 0.98))));
                    gc.fillRoundRect(cx, elem.getWorldY(), elem.getWidth(), elem.getHeight(), 6, 6);
                    gc.setStroke(Color.rgb(255, 220, 0, 0.9)); gc.setLineWidth(2.5);
                    gc.strokeRoundRect(cx, elem.getWorldY(), elem.getWidth(), elem.getHeight(), 6, 6);
                    // Reflexo ondulante na superfície
                    for (int r = 0; r < 4; r++) {
                        double rx = cx + elem.getWidth() * (0.15 + r * 0.22) + Math.sin(timeSeconds * 3.5 + r) * 6;
                        gc.setFill(Color.rgb(255, 200, 0, 0.25 * pulse));
                        gc.fillOval(rx - 14, elem.getWorldY() + 3, 28, 7);
                    }
                    // Bolhas brotando — 3 (era 4)
                    for (int b = 0; b < 3; b++) {
                        double bprog = ((timeSeconds * 0.9 + b * 0.28) % 1.0);
                        double bx2 = cx + (b * 0.22 + 0.1) * elem.getWidth() + Math.sin(timeSeconds * 1.8 + b) * 8;
                        double by2 = elem.getWorldY() - bprog * 28;
                        double br2 = (1.0 - bprog) * 7 + 2;
                        gc.setFill(Color.rgb(255, 180, 20, (1.0 - bprog) * 0.65));
                        gc.fillOval(bx2 - br2, by2 - br2, br2 * 2, br2 * 2);
                    }
                    // Coluna de vapor ascendente sobre o pool
                    drawLavaSteam(gc, cx + elem.getWidth() / 2, elem.getWorldY(), elem.getWidth(), timeSeconds);
                }
                case VOLCANIC_ROCK -> {
                    Image frame = spriteManager.getImage("sprites/scenery/12_obstaculo_vulcanico.png");
                    if (frame != null) {
                        gc.drawImage(frame, cx, elem.getWorldY(), elem.getWidth(), elem.getHeight());
                    } else {
                        gc.setFill(Color.rgb(28, 12, 6, 0.96));
                        gc.fillRoundRect(cx, elem.getWorldY(), elem.getWidth(), elem.getHeight(), 10, 10);
                    }
                    drawEmbers(gc, cx + elem.getWidth() / 2,
                        (elem.getWorldY() == 0 ? elem.getHeight() : elem.getWorldY()),
                        elem.getWidth(), timeSeconds, elem.getWorldX());
                }
                case MOVING_OBSTACLE -> {
                    Image frame = spriteManager.getImage("sprites/scenery/12_obstaculo_vulcanico.png");
                    if (frame != null) {
                        gc.drawImage(frame, cx, elem.getWorldY(), elem.getWidth(), elem.getHeight());
                    } else {
                        gc.setFill(Color.rgb(35, 14, 4, 0.95));
                        gc.fillRoundRect(cx, elem.getWorldY(), elem.getWidth(), elem.getHeight(), 12, 12);
                    }
                    lightingEngine.drawBioluminescentHalo(gc,
                        cx + elem.getWidth()/2, elem.getWorldY() + elem.getHeight()/2,
                        elem.getWidth() * 0.30, Color.web("#ff5500"));
                    drawEmbers(gc, cx + elem.getWidth() / 2, elem.getWorldY() + elem.getHeight() / 2,
                        elem.getWidth(), timeSeconds, elem.getWorldX() * 1.3);
                }
                case GEYSER -> {
                    lightingEngine.drawGeyserHeatGlow(gc, cx, elem.getWorldY(), elem.getWidth(), elem.getHeight());
                    drawGeyser(gc, cx + elem.getWidth()/2, elem.getWorldY(), elem.getWidth(), timeSeconds, Color.ORANGERED);
                }
                case CURRENT -> {
                    drawCurrentZone(gc, cx, elem.getWorldY(), elem.getWidth(), elem.getHeight(),
                                    elem.getCurrentVx(), elem.getCurrentVy(), timeSeconds);
                }
                default -> {}
            }
        }

        for (GuardianEntity g : ctx.getGuardians()) {
            double gx = ctx.getCamera().toScreenX(g.getWorldX());
            drawGuardian(gc, gx, g.getWorldY(), g.getType(), !g.isContacted(), timeSeconds);
        }

        drawPortal(gc, ctx.getCamera().toScreenX(4000 - 100), ctx.getTabletsCollected() >= 4, height, timeSeconds);

        for (EnemyEntity e : ctx.getEnemies()) {
            if (!e.isAlive()) continue;
            drawEnemy(gc, ctx.getCamera().toScreenX(e.getWorldX()), e.getCurrentY(), e.getType().getId(), timeSeconds);
        }

        drawProjectiles(gc, ctx);
        drawHero(gc, ctx.getCamera().toScreenX(ctx.getHero().getX()), ctx.getHero().getY(), ctx.getHero(), timeSeconds);
        ctx.getParticleSystem().render(gc);

        uiRenderer.drawHUD(gc, ctx.getHero(), ctx.getTabletsCollected(), "Fase 4 — Abismo Vulcânico", ctx.getDifficulty());
        uiRenderer.drawMinimap(gc, ctx, width);
        drawAlert(gc, ctx, width, height);
    }

    // =========================================================
    // FASE 5 — Templo Final de Apsu (arena boss)
    // =========================================================
    private void drawP5(GraphicsContext gc, GameContext ctx, double width, double height, double timeSeconds) {
        Image bg5 = spriteManager.getImage("bg5.png");
        if (bg5 != null) {
            gc.drawImage(bg5, 0, 0, width, height);
        } else {
            drawGradientBackground(gc, width, height, "#080018", "#150030", "#200050");
        }

        // Colunas de Atlantis
        Image ruinas = spriteManager.getImage("sprites/scenery/09_ruinas_e_colunas_atlantis.png");
        if (ruinas != null) {
            gc.setGlobalAlpha(0.55);
            gc.drawImage(ruinas, 20, 50, 220, 500);
            gc.setGlobalAlpha(0.40);
            gc.drawImage(ruinas, width - 240, 50, 220, 500);
            gc.setGlobalAlpha(1.0);
        }

        // Correntes e zonas na arena
        for (SceneryElement elem : ctx.getSceneryElements()) {
            if (elem.getType() == SceneryElement.Type.CURRENT) {
                drawCurrentZone(gc, elem.getWorldX(), elem.getWorldY(), elem.getWidth(), elem.getHeight(),
                                elem.getCurrentVx(), elem.getCurrentVy(), timeSeconds);
            } else if (elem.getType() == SceneryElement.Type.PRESSURE_ZONE) {
                drawPressureZone(gc, elem.getWorldX(), elem.getWorldY(), elem.getWidth(), elem.getHeight(),
                                  elem.getBuoyancyMult(), timeSeconds);
            }
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

        uiRenderer.drawHUD(gc, ctx.getHero(), ctx.getTabletsCollected(), "Fase 5 — Templo de Apsu", ctx.getDifficulty());
        uiRenderer.drawMinimap(gc, ctx, width);
        drawAlert(gc, ctx, width, height);
    }

    // =========================================================
    // ELEMENTOS VISUAIS AMBIENTAIS
    // =========================================================

    /** Corrente 3D estática; a orientação comunica a força sem blocos translúcidos. */
    private void drawCurrentZone(GraphicsContext gc, double x, double y, double w, double h,
                                  double vx, double vy, double t) {
        Image current = spriteManager.getImage("sprites/scenery/16_corrente_abissal_3d.png");
        if (current == null) return;

        boolean horizontal = Math.abs(vx) > Math.abs(vy);
        double drawW = horizontal ? Math.max(w, h) : Math.min(w, h);
        double drawH = horizontal ? Math.min(w, h) : Math.max(w, h);
        double centerX = x + w / 2;
        double centerY = y + h / 2;
        double angle = horizontal ? (vx < 0 ? -90 : 90) : (vy > 0 ? 180 : 0);

        lightingEngine.drawBioluminescentHalo(gc, centerX, centerY, Math.min(w, h) * 0.72,
            Color.web("#28cfff"));
        gc.save();
        gc.translate(centerX, centerY);
        gc.rotate(angle);
        gc.drawImage(current, -drawW / 2, -drawH / 2, drawW, drawH);
        gc.restore();
    }

    /** Vórtice 3D para pressão/empuxo, sem placas, textos ou retângulos. */
    private void drawPressureZone(GraphicsContext gc, double x, double y, double w, double h,
                                   double buoyMult, double t) {
        Image vortex = spriteManager.getImage("sprites/scenery/16_corrente_abissal_3d.png");
        if (vortex == null) return;
        double cx = x + w / 2;
        double cy = y + h / 2;
        Color aura = buoyMult > 1.0 ? Color.web("#42f5c5") : Color.web("#7c65ff");
        lightingEngine.drawBioluminescentHalo(gc, cx, cy, Math.min(w, h) * 0.65, aura);
        gc.save();
        gc.setGlobalAlpha(0.72);
        gc.translate(cx, cy);
        gc.rotate(buoyMult > 1.0 ? 0 : 180);
        gc.drawImage(vortex, -w / 2, -h / 2, w, h);
        gc.restore();
    }

    /**
     * Gêiser com coluna sólida translúcida + partículas ascendendo.
     * Substitui o antigo drawGeyserParticles.
     */
    private void drawGeyser(GraphicsContext gc, double cx, double baseY, double colW, double t, Color color) {
        double columnH = 180;
        // Coluna sólida
        gc.setFill(new LinearGradient(cx - colW * 0.25, baseY - columnH, cx + colW * 0.25, baseY, false,
            CycleMethod.NO_CYCLE,
            new Stop(0, color.deriveColor(0, 1, 1, 0.0)),
            new Stop(0.6, color.deriveColor(0, 1, 1, 0.22)),
            new Stop(1.0, color.deriveColor(0, 1, 1, 0.45))));
        gc.fillRoundRect(cx - colW * 0.25, baseY - columnH, colW * 0.5, columnH, colW * 0.25, colW * 0.25);
        // Base (cápsula de origem)
        gc.setFill(color.deriveColor(0, 1, 0.8, 0.55));
        gc.fillOval(cx - colW * 0.38, baseY - 10, colW * 0.76, 20);
        // Partículas de topo — 5 (era 8) é indistinguível visualmente
        int gParticles = reducedEffects ? 3 : 5;
        for (int i = 0; i < gParticles; i++) {
            double progress = ((t * 1.4 + i * 0.135) % 1.0);
            double px = cx + Math.sin(t * 2.8 + i * 1.3) * 20;
            double py = baseY - columnH - progress * 80;
            double alpha = (1.0 - progress) * 0.65;
            double r = (1.0 - progress) * 7 + 2;
            gc.setFill(color.deriveColor(0, 1, 1, alpha));
            gc.fillOval(px - r, py - r, r * 2, r * 2);
        }
    }

    /** @deprecated Mantém assinatura antiga para compatibilidade interna. */
    @Deprecated
    private void drawGeyserParticles(GraphicsContext gc, double cx, double baseY, double t, Color color) {
        drawGeyser(gc, cx, baseY, 70, t, color);
    }

    /**
     * Coral orgânico procedural: corpo sólido + ramos laterais + bioluminescência.
     * Substitui os fillRoundRect simples da Fase 2.
     */
    private void drawOrganicCoral(GraphicsContext gc, double x, double y, double w, double h,
                                   boolean growsDown, Color glowColor, double t) {
        // Cenário é estático. Alternar frames renderizados separadamente faz as
        // bordas do coral mudarem de posição e produz o flicker observado.
        Image frame = spriteManager.getImage("sprites/scenery/10_perigos_e_obstaculos_cenario.png");

        if (frame != null) {
            gc.save();
            if (growsDown) {
                gc.translate(x + w / 2, y + h / 2);
                gc.scale(1, -1);
                gc.drawImage(frame, -w / 2, -h / 2, w, h);
            } else {
                gc.drawImage(frame, x, y, w, h);
            }
            gc.restore();
        }
    }

    /** Embers/faíscas subindo de rochas vulcânicas (zero alocação). */
    private void drawEmbers(GraphicsContext gc, double cx, double baseY, double w, double t, double seed) {
        int count = reducedEffects ? 2 : 4;
        for (int i = 0; i < count; i++) {
            double progress = ((t * 0.8 + i * 0.18 + seed * 0.001) % 1.0);
            double ex = cx + Math.sin(t * 2.5 + i * 1.6 + seed) * (w * 0.45);
            double ey = baseY - progress * 120;
            double r = (1.0 - progress) * 3.5 + 1.0;
            gc.setFill(EMBER_COLORS[i % EMBER_COLORS.length]);
            gc.fillOval(ex - r, ey - r, r * 2, r * 2);
        }
    }

    /** Vapor ascendente sobre piscinas de lava (zero alocação). */
    private void drawLavaSteam(GraphicsContext gc, double cx, double baseY, double poolW, double t) {
        int count = reducedEffects ? 2 : 3;
        gc.setFill(LAVA_STEAM_COLOR);
        for (int i = 0; i < count; i++) {
            double progress = ((t * 0.35 + i * 0.22) % 1.0);
            double sx = cx + Math.sin(t * 0.9 + i * 1.3) * poolW * 0.30;
            double sy = baseY - progress * 90 - 8;
            double sr = 8 + progress * 20;
            gc.fillOval(sx - sr, sy - sr * 0.5, sr * 2, sr);
        }
    }

    /** Seta de corrente sobre o herói quando está em corrente ativa. */
    private void drawCurrentArrowOnHero(GraphicsContext gc, GameContext ctx, double t) {
        double hx, hy;
        boolean isP5 = (ctx.getState() == GameContext.State.P5);
        hx = isP5 ? ctx.getHero().getX() : ctx.getCamera().toScreenX(ctx.getHero().getX());
        hy = ctx.getHero().getY();

        double fx = ctx.getCurrentFx(), fy = ctx.getCurrentFy();
        double angle = Math.atan2(fy, fx);
        double pulse = 0.8 + Math.sin(t * 5) * 0.2;
        double arrowX = hx + HeroEntity.HW/2 + Math.cos(angle) * 40;
        double arrowY = hy + HeroEntity.HH/2 + Math.sin(angle) * 40;

        gc.setFill(Color.rgb(0, 220, 255, pulse * 0.85));
        gc.fillOval(arrowX - 10, arrowY - 10, 20, 20);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        gc.setFill(Color.rgb(0, 255, 255, pulse));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("⟹", arrowX, arrowY + 4);
    }

    // =========================================================
    // HERÓI
    // =========================================================
    private void drawHero(GraphicsContext gc, double x, double y, HeroEntity hero, double timeSeconds) {
        if (hero.isInvulnerable()) {
            gc.setGlobalAlpha(0.45);
        }

        boolean shooting = hero.isShooting();
        Image fallback = spriteManager.getImage(hero.getType().getStaticSpritePath());
        Image frame = fallback;
        SpriteManager.FrameBlend swimBlend = null;
        boolean hasDedicatedAttackFrame = false;
        if (shooting) {
            String style = ATTACK_STYLE_NAMES[hero.getAttackStyleIndex() % ATTACK_STYLE_NAMES.length];
            // A troca de oito PNGs independentes a 8 FPS era a causa do
            // flicker: os renders não compartilham a mesma silhueta/pivô.
            // Uma pose 3D fixa por ataque mantém o impacto visual, mas cada
            // frame do Canvas desenha a mesma textura estável.
            Image attackFrame = spriteManager.getImage(hero.getType().getAttackDir(style) + ".png");
            if (attackFrame != null) {
                frame = attackFrame;
                hasDedicatedAttackFrame = true;
            }
        } else {
            // Mantém o charme da cauda/nado, mas mistura os dois frames
            // vizinhos para não haver "salto" de textura entre renders 3D.
            swimBlend = spriteManager.loadSequence(hero.getType().getSwimDir(), 8.0, fallback).blend(timeSeconds);
            frame = swimBlend.current() != null ? swimBlend.current() : fallback;
        }

        // O tamanho é sempre derivado da pose-base, nunca de um frame de animação.
        final double targetH = HeroEntity.HH;
        double aspect = (fallback != null && fallback.getHeight() > 0)
            ? fallback.getWidth() / fallback.getHeight() : 0.5;
        double targetW = Math.min(210.0, Math.max(60.0, targetH * aspect));

        double attackX = shooting ? hero.getAttackOffsetX() : 0;
        double attackY = shooting ? hero.getAttackOffsetY() : 0;
        x += attackX;
        y += attackY;
        drawShadow(gc, x + HeroEntity.HW / 2, y + HeroEntity.HH - 3, targetW * 0.55, 12);

        gc.save();
        gc.translate(x + HeroEntity.HW / 2, y + HeroEntity.HH / 2);
        gc.rotate(hero.getPitchAngle());
        if (!hero.isFacingRight()) gc.scale(-1, 1);
        // Escala fixa: o deslocamento/rotação já comunica nado e elimina
        // qualquer variação brusca de contorno sobre cenários detalhados.
        gc.scale(1.0, 1.0);

        if (frame != null) {
            if (swimBlend != null && swimBlend.next() != null && !hero.isInvulnerable()) {
                gc.setGlobalAlpha(1.0 - swimBlend.nextAlpha());
                gc.drawImage(swimBlend.current(), -targetW / 2, -targetH / 2, targetW, targetH);
                gc.setGlobalAlpha(swimBlend.nextAlpha());
                gc.drawImage(swimBlend.next(), -targetW / 2, -targetH / 2, targetW, targetH);
                gc.setGlobalAlpha(1.0);
            } else {
                gc.drawImage(frame, -targetW / 2, -targetH / 2, targetW, targetH);
            }
        } else {
            gc.setFill(hero.getType().getAura());
            gc.fillRoundRect(-HeroEntity.HW / 2, -HeroEntity.HH / 2, HeroEntity.HW, HeroEntity.HH, 8, 8);
        }
        gc.restore();

        lightingEngine.drawBioluminescentHalo(gc, x + HeroEntity.HW/2, y + HeroEntity.HH/2,
            targetW * 0.32, hero.getType().getAura());
        if (shooting && !hasDedicatedAttackFrame) {
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
    private void drawEnemy(GraphicsContext gc, double x, double y, int typeId, double timeSeconds) {
        br.apsu.model.enemy.EnemyType eType = br.apsu.model.enemy.EnemyType.fromId(typeId);
        Image frame = spriteManager.getImage(eType.getStaticSprite());

        double ew = eType.getWidth();
        double eh = eType.getHeight();

        drawShadow(gc, x + ew / 2, y + eh - 2, ew * 0.65, 9);
        if (frame != null) {
            gc.save();
            gc.translate(x + ew / 2, y + eh / 2);
            // Enguia (1) e Caranguejo (3) foram renderizados virados para a direita no Blender
            if (typeId == 1 || typeId == 3) {
                gc.scale(-1, 1);
            }
            gc.drawImage(frame, -ew / 2, -eh / 2, ew, eh);
            gc.restore();
        } else {
            // Fallback geométrico por tipo
            Color fallbackColor = switch (typeId) {
                case 1 -> Color.web("#0060ff");  // Enguia — azul elétrico
                case 2 -> Color.web("#aa00ff");  // Medusa — roxo
                case 3 -> Color.web("#cc3300");  // Caranguejo — vermelho
                case 4 -> Color.web("#004488");  // Arraia — azul escuro
                case 5 -> Color.web("#660099");  // Leviatã — roxo profundo
                default -> Color.web("#006600"); // Peixe — verde
            };
            gc.setFill(fallbackColor);
            if (typeId == 1) {
                // Enguia — forma horizontal
                gc.fillRoundRect(x, y + eh/4, ew, eh/2, 8, 8);
            } else if (typeId == 4) {
                // Arraia — forma diamante/losango
                gc.fillPolygon(new double[]{x + ew/2, x + ew, x + ew/2, x},
                               new double[]{y, y + eh/2, y + eh, y + eh/2}, 4);
            } else {
                gc.fillOval(x, y, ew, eh);
            }
        }
    }

    // =========================================================
    // BOSS
    // =========================================================
    private void drawBoss(GraphicsContext gc, BossEntity boss, double timeSeconds) {
        String bossStatic = switch (boss.getVariant()) {
            case 1 -> "sprites/kullullu/02_kullullu_var_glacial.png";
            case 2 -> "sprites/kullullu/02_kullullu_var_toxico.png";
            default -> "sprites/kullullu/02_kullullu_boss.png";
        };
        Image frame = spriteManager.getImage(bossStatic);

        drawShadow(gc, boss.getX() + BossEntity.BW / 2, boss.getY() + BossEntity.BH - 5, BossEntity.BW * 0.7, 22);
        if (frame != null) {
            gc.save();
            gc.translate(boss.getX() + BossEntity.BW / 2, boss.getY() + BossEntity.BH / 2);
            gc.drawImage(frame, -BossEntity.BW / 2, -BossEntity.BH / 2, BossEntity.BW, BossEntity.BH);
            gc.restore();
        }

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
        gc.setFill(hpRatio > 0.5 ? Color.web("#ff4444") : Color.web("#ff8800"));
        gc.fillRoundRect(barX, barY, barW * hpRatio, 10, 4, 4);
        gc.setStroke(Color.rgb(255, 60, 60, 0.7)); gc.setLineWidth(1.5);
        gc.strokeRoundRect(barX, barY, barW, 10, 4, 4);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 11));
        gc.setFill(Color.web("#ffd27a"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(boss.getCombatPhase().getLabel(), boss.getX() + BossEntity.BW / 2, barY - 5);
    }

    private void drawGuardian(GraphicsContext gc, double x, double y, int type, boolean prompt, double timeSeconds) {
        int safeType = Math.min(type, GuardianEntity.SPRITE_PATHS.length - 1);
        String gStatic = GuardianEntity.SPRITE_PATHS[safeType];
        Image frame = spriteManager.getImage(gStatic);

        lightingEngine.drawBioluminescentHalo(gc, x + GuardianEntity.GW / 2, y + GuardianEntity.GH / 2,
            GuardianEntity.GW * 0.9, Color.web("#40e0ff"));
        drawShadow(gc, x + GuardianEntity.GW / 2, y + GuardianEntity.GH - 4, GuardianEntity.GW * 0.7, 16);

        if (frame != null) {
            gc.drawImage(frame, x, y, GuardianEntity.GW, GuardianEntity.GH);
        }

        if (prompt) {
            gc.setFill(Color.rgb(0, 8, 24, 0.90));
            gc.fillRoundRect(x + GuardianEntity.GW / 2 - 85, y - 32, 170, 26, 8, 8);
            gc.setStroke(Color.web("#ffd700")); gc.setLineWidth(1.5);
            gc.strokeRoundRect(x + GuardianEntity.GW / 2 - 85, y - 32, 170, 26, 8, 8);
            gc.setFill(Color.web("#ffd700")); gc.setFont(Font.font("Serif", FontWeight.BOLD, 13));
            gc.setTextAlign(TextAlignment.CENTER); gc.fillText("[ E / ESPAÇO: CONVERSAR ]", x + GuardianEntity.GW / 2, y - 15);
        }
    }

    private void drawPortal(GraphicsContext gc, double px, boolean unlocked, double height, double timeSeconds) {
        double centerX = px + 8;
        double centerY = height / 2.0 - 16;
        Image portal = spriteManager.getImage("sprites/scenery/15_portal_atlantica_3d.png");

        if (portal != null) {
            double pulse = 0.88 + Math.sin(timeSeconds * 2.4) * 0.12;
            lightingEngine.drawBioluminescentHalo(gc, centerX, centerY, 104 * pulse,
                unlocked ? Color.web("#28d9ff") : Color.web("#d94a5e"));
            gc.save();
            if (!unlocked) gc.setGlobalAlpha(0.48);
            gc.drawImage(portal, centerX - 70, centerY - 70, 140, 140);
            gc.restore();
        }

        // Indicador de estado compacto; a geometria do portal vem do sprite 3D.
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(unlocked ? Color.web("#75ecff") : Color.web("#ff8290"));
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
                gc.setFill(Color.web("#ff2a4b"));
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
        gc.setStroke(Color.web("#ffd700")); gc.setLineWidth(1.5);
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
        gc.setFill(win ? Color.web("#ffd700") : Color.web("#ff2828"));
        gc.fillText(win ? "VITORIA" : "GAME OVER", width / 2.0, 210);
        gc.setFont(Font.font("Serif", 26));
        gc.setFill(win ? Color.web("#88ffaa") : Color.web("#ff9090"));
        gc.fillText(win ? "Adapa purificou o oceano Apsu!" : "Adapa caiu nas profundezas...", width / 2.0, 320);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        gc.setFill(Color.web("#ffd700"));
        gc.fillText("Tabuletas: " + ctx.getTabletsCollected() + " / " + ctx.getTotalTablets(), width / 2.0, 390);
        gc.setFill(Color.web("#80b8d0")); gc.setFont(Font.font("Serif", 16));
        gc.fillText("Herói: " + ctx.getHeroType().getName() + "  |  Dificuldade: " + ctx.getDifficulty().getLabel(), width / 2.0, 430);
        gc.setFill(Color.web("#ffd700")); gc.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        gc.fillText("[ ESPAÇO / R / ENTER — Menu Principal ]", width / 2.0, height - 80);
    }

    private void drawGradientBackground(GraphicsContext gc, double width, double height, String top, String mid, String bot) {
        gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(top)), new Stop(0.5, Color.web(mid)), new Stop(1, Color.web(bot))));
        gc.fillRect(0, 0, width, height);
    }

    private void drawShadow(GraphicsContext gc, double cx, double cy, double rw, double rh) {
        gc.setFill(Color.rgb(2, 8, 14, 0.40));
        gc.fillOval(cx - rw / 2, cy - rh / 2, rw, rh);
    }
}
