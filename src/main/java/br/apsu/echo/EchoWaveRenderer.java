package br.apsu.echo;

import br.apsu.core.GameContext;
import br.apsu.graphics.Palette;
import br.apsu.model.boss.BossEntity;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.environment.SceneryElement;
import br.apsu.model.guardian.GuardianEntity;
import br.apsu.model.hero.HeroEntity;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Renderizador da onda visual de ecolocalizacao e destaques temporarios (US-031).
 * <p>
 * Regras e criterios:
 * <ul>
 *   <li>Quando o eco e ativado, um anel se expande do heroi ate o raio de 600 unidades em 1,0 s.</li>
 *   <li>Guardioes, inimigos e obstaculos fixos atingidos pela onda sao destacados por 3,0 s
 *       em cores distintas (amarelo, vermelho e branco), coerentes com a paleta do GPS/Radar.</li>
 *   <li>O anel e os destaques sao renderizados abaixo do heroi e abaixo do HUD, garantindo
 *       que nunca cubram nenhum deles.</li>
 *   <li>Zero alocacoes por quadro no hot path a 60 FPS (Regra 11): arrays primitivos
 *       pre-alocados, constantes estaticas de cores e sem criacao de objetos na renderizacao.</li>
 * </ul>
 */
public final class EchoWaveRenderer {

    // ── Constantes da onda e destaque ─────────────────────────────────────────
    public static final double WAVE_RADIUS_MAX = 600.0;
    public static final double WAVE_DURATION = 1.0;
    public static final double HIGHLIGHT_DURATION = 3.0;

    // ── Cores estaticas (zero-alloc por quadro) ───────────────────────────────
    public static final Color COLOR_GUARDIAN = Palette.RADAR_GUARDIAN; // Amarelo (#ffd700)
    public static final Color COLOR_ENEMY = Palette.RADAR_ENEMY;       // Vermelho (#ff5c72)
    public static final Color COLOR_SCENERY = Color.WHITE;             // Branco
    public static final Color COLOR_WAVE = Palette.RADAR_STROKE;       // Ciano (#46b9ff)

    // ── Capacidades maximas pre-alocadas ─────────────────────────────────────
    public static final int MAX_GUARDIANS = 16;
    public static final int MAX_ENEMIES = 64;
    public static final int MAX_SCENERY = 128;

    // ── Estado da onda ────────────────────────────────────────────────────────
    private boolean wasEchoActive = false;
    private boolean waveActive = false;
    private double waveTimer = 0.0;
    private double waveRadius = 0.0;
    private double prevWaveRadius = 0.0;
    private double originX = 0.0;
    private double originY = 0.0;

    // ── Timers de destaque (segundos restantes) ──────────────────────────────
    private final double[] guardianHighlight = new double[MAX_GUARDIANS];
    private final double[] enemyHighlight = new double[MAX_ENEMIES];
    private final double[] sceneryHighlight = new double[MAX_SCENERY];
    private double bossHighlight = 0.0;

    // ── Flags de impacto na onda corrente (evita re-acionamentos) ─────────────
    private final boolean[] guardianHit = new boolean[MAX_GUARDIANS];
    private final boolean[] enemyHit = new boolean[MAX_ENEMIES];
    private final boolean[] sceneryHit = new boolean[MAX_SCENERY];
    private boolean bossHit = false;

    public EchoWaveRenderer() {
        reset();
    }

    /**
     * Reseta todo o estado da onda e zera todos os destaques.
     */
    public void reset() {
        wasEchoActive = false;
        waveActive = false;
        waveTimer = 0.0;
        waveRadius = 0.0;
        prevWaveRadius = 0.0;
        originX = 0.0;
        originY = 0.0;
        bossHighlight = 0.0;
        bossHit = false;

        for (int i = 0; i < MAX_GUARDIANS; i++) {
            guardianHighlight[i] = 0.0;
            guardianHit[i] = false;
        }
        for (int i = 0; i < MAX_ENEMIES; i++) {
            enemyHighlight[i] = 0.0;
            enemyHit[i] = false;
        }
        for (int i = 0; i < MAX_SCENERY; i++) {
            sceneryHighlight[i] = 0.0;
            sceneryHit[i] = false;
        }
    }

    /**
     * Atualiza a expansao da onda e a duracao dos destaques.
     * Zero alocacoes.
     *
     * @param ctx contexto do jogo
     * @param dt  delta-time em segundos
     */
    public void update(GameContext ctx, double dt) {
        if (dt <= 0.0) return;

        EchoController echo = ctx.getEchoController();
        boolean echoActive = echo != null && echo.isActive();

        // Detecta inicio de novo eco
        if (echoActive && !wasEchoActive) {
            waveActive = true;
            waveTimer = 0.0;
            waveRadius = 0.0;
            prevWaveRadius = 0.0;
            originX = ctx.getHero().getX() + HeroEntity.HW / 2.0;
            originY = ctx.getHero().getY() + HeroEntity.HH / 2.0;

            for (int i = 0; i < MAX_GUARDIANS; i++) guardianHit[i] = false;
            for (int i = 0; i < MAX_ENEMIES; i++) enemyHit[i] = false;
            for (int i = 0; i < MAX_SCENERY; i++) sceneryHit[i] = false;
            bossHit = false;
        }
        wasEchoActive = echoActive;

        // Reduz timers de destaque existentes antes de novos impactos
        for (int i = 0; i < MAX_GUARDIANS; i++) {
            if (guardianHighlight[i] > 0.0) {
                guardianHighlight[i] = Math.max(0.0, guardianHighlight[i] - dt);
            }
        }
        for (int i = 0; i < MAX_ENEMIES; i++) {
            if (enemyHighlight[i] > 0.0) {
                enemyHighlight[i] = Math.max(0.0, enemyHighlight[i] - dt);
            }
        }
        if (bossHighlight > 0.0) {
            bossHighlight = Math.max(0.0, bossHighlight - dt);
        }
        for (int i = 0; i < MAX_SCENERY; i++) {
            if (sceneryHighlight[i] > 0.0) {
                sceneryHighlight[i] = Math.max(0.0, sceneryHighlight[i] - dt);
            }
        }

        // Expansao da onda e deteccao de novos impactos
        if (waveActive) {
            waveTimer += dt;
            prevWaveRadius = waveRadius;
            waveRadius = Math.min(WAVE_RADIUS_MAX, waveTimer * (WAVE_RADIUS_MAX / WAVE_DURATION));

            detectHits(ctx);

            if (waveTimer >= WAVE_DURATION) {
                waveRadius = WAVE_RADIUS_MAX;
                waveActive = false;
            }
        }
    }

    private void detectHits(GameContext ctx) {
        // Guardioes
        List<GuardianEntity> guardians = ctx.getGuardians();
        int gCount = Math.min(guardians.size(), MAX_GUARDIANS);
        for (int i = 0; i < gCount; i++) {
            if (guardianHit[i]) continue;
            GuardianEntity g = guardians.get(i);
            double gx = g.getWorldX() + GuardianEntity.GW / 2.0;
            double gy = g.getWorldY() + GuardianEntity.GH / 2.0;
            double dist = distance(originX, originY, gx, gy);
            if (dist <= waveRadius && dist <= WAVE_RADIUS_MAX) {
                guardianHit[i] = true;
                guardianHighlight[i] = HIGHLIGHT_DURATION;
            }
        }

        // Inimigos
        List<EnemyEntity> enemies = ctx.getEnemies();
        int eCount = Math.min(enemies.size(), MAX_ENEMIES);
        for (int i = 0; i < eCount; i++) {
            if (enemyHit[i]) continue;
            EnemyEntity e = enemies.get(i);
            if (!e.isAlive()) continue;
            double ew = e.getType() != null ? e.getType().getWidth() : 60.0;
            double eh = e.getType() != null ? e.getType().getHeight() : 40.0;
            double ex = e.getWorldX() + ew / 2.0;
            double ey = e.getCurrentY() + eh / 2.0;
            double dist = distance(originX, originY, ex, ey);
            if (dist <= waveRadius && dist <= WAVE_RADIUS_MAX) {
                enemyHit[i] = true;
                enemyHighlight[i] = HIGHLIGHT_DURATION;
            }
        }

        // Chefe (P5)
        BossEntity boss = ctx.getBoss();
        if (boss != null && !boss.isDead() && !bossHit) {
            double bx = boss.getX() + BossEntity.BW / 2.0;
            double by = boss.getY() + BossEntity.BH / 2.0;
            double dist = distance(originX, originY, bx, by);
            if (dist <= waveRadius && dist <= WAVE_RADIUS_MAX) {
                bossHit = true;
                bossHighlight = HIGHLIGHT_DURATION;
            }
        }

        // Obstaculos fixos de cenario
        List<SceneryElement> scenery = ctx.getSceneryElements();
        int sCount = Math.min(scenery.size(), MAX_SCENERY);
        for (int i = 0; i < sCount; i++) {
            if (sceneryHit[i]) continue;
            SceneryElement s = scenery.get(i);
            double cx = Math.max(s.getWorldX(), Math.min(originX, s.getWorldX() + s.getWidth()));
            double cy = Math.max(s.getWorldY(), Math.min(originY, s.getWorldY() + s.getHeight()));
            double dist = distance(originX, originY, cx, cy);
            if (dist <= waveRadius && dist <= WAVE_RADIUS_MAX) {
                sceneryHit[i] = true;
                sceneryHighlight[i] = HIGHLIGHT_DURATION;
            }
        }

        // Tabuleta Sagrada Oculta revelada por Ecolocalizacao (APSU-G01)
        br.apsu.model.environment.SacredTabletEntity tablet = ctx.getSacredTablet();
        if (tablet != null && tablet.isHidden()) {
            double tx = tablet.getWorldX();
            double ty = tablet.getWorldY();
            double dist = distance(originX, originY, tx, ty);
            if (dist <= waveRadius && dist <= WAVE_RADIUS_MAX) {
                if (tablet.reveal()) {
                    ctx.onTabletRevealed(tablet);
                }
            }
        }
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Renderiza o anel da onda e os destaques dos elementos atingidos.
     * Deve ser chamado antes de desenhar o heroi e antes do HUD.
     * Zero alocacoes por quadro.
     *
     * @param gc     GraphicsContext do Canvas
     * @param ctx    contexto do jogo
     * @param width  largura da tela
     * @param height altura da tela
     */
    public void render(GraphicsContext gc, GameContext ctx, double width, double height) {
        renderHighlights(gc, ctx, width, height);

        if (waveActive && waveRadius > 0.0) {
            renderWaveRing(gc, ctx, width, height);
        }
    }

    private void renderHighlights(GraphicsContext gc, GameContext ctx, double width, double height) {
        // Guardioes (amarelo)
        List<GuardianEntity> guardians = ctx.getGuardians();
        int gCount = Math.min(guardians.size(), MAX_GUARDIANS);
        for (int i = 0; i < gCount; i++) {
            if (guardianHighlight[i] <= 0.0) continue;
            GuardianEntity g = guardians.get(i);
            double sx = ctx.getCamera().toScreenX(g.getWorldX());
            // APSU-P17: culling — ignora elemento fora da tela
            if (sx + GuardianEntity.GW < -20 || sx > width + 20) continue;
            double sy = g.getWorldY();
            double alpha = Math.min(1.0, guardianHighlight[i] / HIGHLIGHT_DURATION);

            gc.setGlobalAlpha(alpha * 0.85);
            gc.setStroke(COLOR_GUARDIAN);
            gc.setLineWidth(3.0);
            gc.strokeRoundRect(sx - 4, sy - 4, GuardianEntity.GW + 8, GuardianEntity.GH + 8, 12, 12);
        }

        // Inimigos (vermelho)
        List<EnemyEntity> enemies = ctx.getEnemies();
        int eCount = Math.min(enemies.size(), MAX_ENEMIES);
        for (int i = 0; i < eCount; i++) {
            if (enemyHighlight[i] <= 0.0) continue;
            EnemyEntity e = enemies.get(i);
            if (!e.isAlive()) continue;
            double sx = ctx.getCamera().toScreenX(e.getWorldX());
            double ew = e.getType() != null ? e.getType().getWidth() : 60.0;
            // APSU-P17: culling — ignora inimigo fora da tela
            if (sx + ew < -20 || sx > width + 20) continue;
            double sy = e.getCurrentY();
            double eh = e.getType() != null ? e.getType().getHeight() : 40.0;
            double alpha = Math.min(1.0, enemyHighlight[i] / HIGHLIGHT_DURATION);

            gc.setGlobalAlpha(alpha * 0.85);
            gc.setStroke(COLOR_ENEMY);
            gc.setLineWidth(3.0);
            gc.strokeOval(sx - 4, sy - 4, ew + 8, eh + 8);
        }

        // Chefe (vermelho)
        BossEntity boss = ctx.getBoss();
        if (boss != null && !boss.isDead() && bossHighlight > 0.0) {
            double sx = ctx.getCamera().toScreenX(boss.getX());
            if (sx + BossEntity.BW >= -20 && sx <= width + 20) {
                double sy = boss.getY();
                double alpha = Math.min(1.0, bossHighlight / HIGHLIGHT_DURATION);

                gc.setGlobalAlpha(alpha * 0.85);
                gc.setStroke(COLOR_ENEMY);
                gc.setLineWidth(4.0);
                gc.strokeOval(sx - 6, sy - 6, BossEntity.BW + 12, BossEntity.BH + 12);
            }
        }

        // Obstaculos fixos (branco - ondas acusticas esfericas organicas APSU-G19B)
        List<SceneryElement> scenery = ctx.getSceneryElements();
        int sCount = Math.min(scenery.size(), MAX_SCENERY);
        for (int i = 0; i < sCount; i++) {
            if (sceneryHighlight[i] <= 0.0) continue;
            SceneryElement s = scenery.get(i);
            double sx = ctx.getCamera().toScreenX(s.getWorldX());
            // APSU-P17: culling — ignora elemento de cenário fora da tela com margem segura
            if (sx + s.getWidth() < -40 || sx > width + 40) continue;
            double sy = s.getWorldY();
            double alpha = Math.min(1.0, sceneryHighlight[i] / HIGHLIGHT_DURATION);

            double cx = sx + s.getWidth() / 2.0;
            double cy = sy + s.getHeight() / 2.0;
            double baseRadius = Math.max(s.getWidth(), s.getHeight()) * 0.55;
            double pulse = (1.0 - alpha) * 26.0;

            gc.setStroke(COLOR_SCENERY);

            // Anel acustico principal circundando o obstaculo organicamente
            gc.setGlobalAlpha(alpha * 0.75);
            gc.setLineWidth(2.5);
            double r1 = baseRadius + pulse;
            gc.strokeOval(cx - r1, cy - r1, r1 * 2.0, r1 * 2.0);

            // Segundo anel acustico concentrico harmonico
            if (r1 > 16.0) {
                gc.setGlobalAlpha(alpha * 0.35);
                gc.setLineWidth(1.5);
                double r2 = Math.max(6.0, r1 * 0.62);
                gc.strokeOval(cx - r2, cy - r2, r2 * 2.0, r2 * 2.0);
            }
        }

        gc.setGlobalAlpha(1.0);
    }

    private void renderWaveRing(GraphicsContext gc, GameContext ctx, double width, double height) {
        double screenX = ctx.getCamera().toScreenX(originX);
        double screenY = originY;
        double r = waveRadius;

        // APSU-P17: culling — se o anel está completamente fora da tela, pula o traçado
        if (screenX + r < -20 || screenX - r > width + 20 || screenY + r < -20 || screenY - r > height + 20) return;

        double progress = Math.min(1.0, waveTimer / WAVE_DURATION);
        double alpha = (1.0 - progress * 0.5) * 0.7;

        gc.setGlobalAlpha(alpha);
        gc.setStroke(COLOR_WAVE);
        gc.setLineWidth(3.0);
        gc.strokeOval(screenX - r, screenY - r, r * 2.0, r * 2.0);

        if (r > 30.0) {
            gc.setGlobalAlpha(alpha * 0.4);
            gc.setLineWidth(1.5);
            gc.strokeOval(screenX - (r - 20.0), screenY - (r - 20.0), (r - 20.0) * 2.0, (r - 20.0) * 2.0);
        }

        gc.setGlobalAlpha(1.0);
    }

    // ── Getters para consultas e testes de criterios ─────────────────────────

    public boolean isWaveActive() {
        return waveActive;
    }

    public double getWaveRadius() {
        return waveRadius;
    }

    public double getWaveTimer() {
        return waveTimer;
    }

    public double getOriginX() {
        return originX;
    }

    public double getOriginY() {
        return originY;
    }

    public boolean isGuardianHighlighted(int index) {
        return index >= 0 && index < MAX_GUARDIANS && guardianHighlight[index] > 0.0;
    }

    public double getGuardianHighlightTimer(int index) {
        return (index >= 0 && index < MAX_GUARDIANS) ? guardianHighlight[index] : 0.0;
    }

    public boolean isEnemyHighlighted(int index) {
        return index >= 0 && index < MAX_ENEMIES && enemyHighlight[index] > 0.0;
    }

    public double getEnemyHighlightTimer(int index) {
        return (index >= 0 && index < MAX_ENEMIES) ? enemyHighlight[index] : 0.0;
    }

    public boolean isEnemyRevealed(EnemyEntity enemy, GameContext ctx) {
        if (enemy == null || !enemy.isAlive()) return false;
        List<EnemyEntity> enemies = ctx.getEnemies();
        int idx = enemies.indexOf(enemy);
        return idx >= 0 && idx < MAX_ENEMIES && enemyHighlight[idx] > 0.0;
    }

    public boolean isBossHighlighted() {
        return bossHighlight > 0.0;
    }

    public double getBossHighlightTimer() {
        return bossHighlight;
    }

    public boolean isSceneryHighlighted(int index) {
        return index >= 0 && index < MAX_SCENERY && sceneryHighlight[index] > 0.0;
    }

    public double getSceneryHighlightTimer(int index) {
        return (index >= 0 && index < MAX_SCENERY) ? sceneryHighlight[index] : 0.0;
    }

    public Color getGuardianColor() {
        return COLOR_GUARDIAN;
    }

    public Color getEnemyColor() {
        return COLOR_ENEMY;
    }

    public Color getSceneryColor() {
        return COLOR_SCENERY;
    }

    public Color getWaveColor() {
        return COLOR_WAVE;
    }
}
