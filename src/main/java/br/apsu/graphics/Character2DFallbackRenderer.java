package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.model.boss.BossEntity;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.guardian.GuardianEntity;
import br.apsu.model.hero.HeroEntity;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Renderizador de Fallback 2D para personagens e entidades dinâmicas (US-008).
 * Ativado quando SCENE3D não está disponível ou forçado via -Dapsu.render=2d.
 *
 * Criterios de aceitação:
 * - Desenha Herói, Guardiões, Inimigos e Boss como sprites 2D no Canvas.
 * - Mantém proporção e escala com diferença de no máximo 10% em relação à versão 3D.
 * - Zero alocações no hot path de renderização a 60 FPS (Regra 11).
 */
public final class Character2DFallbackRenderer {

    private Character2DFallbackRenderer() {}

    /**
     * Renderiza o herói como sprite 2D com flip horizontal de direção.
     */
    public static void renderHero(GraphicsContext gc, GameContext ctx, HeroEntity hero,
                                 double x, double y, double timeSeconds) {
        if (hero == null) return;
        String spritePath = hero.getType().getStaticSpritePath();
        Image img = SpriteManager.getInstance().getImage(spritePath);
        if (img == null) return;

        // Dimensões do herói no Canvas 2D (dobrado para visibilidade arcade)
        double targetW = 184.0;
        double targetH = 410.0;

        boolean facingRight = hero.isFacingRight();
        double drawX = x + HeroEntity.HW / 2.0 - targetW / 2.0;
        double drawY = y + HeroEntity.HH / 2.0 - targetH / 2.0;

        gc.save();
        // US-042: variacao de opacidade no fallback 2D durante invulnerabilidade
        long now = ctx != null ? ctx.getNanoTime() : (long)(timeSeconds * 1_000_000_000L);
        gc.setGlobalAlpha(hero.getRenderOpacity(now));
        if (!facingRight) {
            // Inverte horizontalmente para olhar para a esquerda
            gc.translate(drawX + targetW, drawY);
            gc.scale(-1, 1);
            gc.drawImage(img, 0, 0, targetW, targetH);
        } else {
            gc.drawImage(img, drawX, drawY, targetW, targetH);
        }
        gc.restore();
    }

    /**
     * Renderiza um guardião NPC como sprite 2D.
     */
    public static void renderGuardian(GraphicsContext gc, GuardianEntity guardian,
                                     double screenX, double y) {
        if (guardian == null) return;
        String spritePath = guardian.getSpritePath();
        Image img = SpriteManager.getInstance().getImage(spritePath);
        if (img == null) return;

        double targetW = 125.0;
        double targetH = GuardianEntity.GH;
        double drawX = screenX + GuardianEntity.GW / 2.0 - targetW / 2.0;

        gc.drawImage(img, drawX, y, targetW, targetH);
    }

    /**
     * Renderiza um inimigo como sprite 2D.
     */
    public static void renderEnemy(GraphicsContext gc, EnemyEntity enemy,
                                  double screenX, double y) {
        if (enemy == null || (!enemy.isAlive() && !enemy.isDying())) return;
        String spritePath = enemy.getType().getStaticSprite();
        Image img = SpriteManager.getInstance().getImage(spritePath);
        if (img == null) return;

        double targetW = enemy.getType().getWidth() * 2.0;
        double targetH = enemy.getType().getHeight() * 2.0;

        double drawX = screenX + enemy.getType().getWidth() / 2.0 - targetW / 2.0;
        double drawY = y + enemy.getType().getHeight() / 2.0 - targetH / 2.0;

        gc.drawImage(img, drawX, drawY, targetW, targetH);
    }

    /**
     * Renderiza o chefe Kullullû como sprite 2D com escala monumental (dobro do herói).
     */
    public static void renderBoss(GraphicsContext gc, BossEntity boss, double timeSeconds) {
        if (boss == null || boss.isDead()) return;

        String spritePath = switch (boss.getVariant()) {
            case 1 -> "sprites/kullullu/02_kullullu_var_glacial.png";
            case 2 -> "sprites/kullullu/02_kullullu_var_toxico.png";
            default -> "sprites/kullullu/02_kullullu_boss.png";
        };

        Image img = SpriteManager.getInstance().getImage(spritePath);
        if (img == null) return;

        // Escala monumental: ~dobro da altura do herói e acompanhando a hitbox
        double targetW = BossEntity.BW;
        double targetH = BossEntity.BH;

        gc.drawImage(img, boss.getX(), boss.getY(), targetW, targetH);
    }
}
