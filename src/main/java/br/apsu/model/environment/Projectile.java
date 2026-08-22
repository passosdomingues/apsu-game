package br.apsu.model.environment;

/**
 * Projéteis no jogo (Bolhas do herói, projéteis inimigos, projéteis do boss).
 * Sprint 5: coordenadas de mundo para HERO_BUBBLE e ENEMY_MALIGN;
 * isArena=true para projéteis da P5 (arena sem câmera).
 */
public class Projectile {
    public enum Type { HERO_BUBBLE, ENEMY_MALIGN, BOSS_BULLET }

    private Type type;
    private double x, y;
    private double vx, vy;
    private double damage;
    private boolean alive = true;
    /**
     * true = projétil opera em coordenadas de TELA (P5: arena sem scroll).
     * false = projétil opera em coordenadas de MUNDO (P1-P4: com scroll da câmera).
     */
    private boolean isArena;

    public Projectile(Type type, double x, double y, double vx, double vy, double damage, boolean isArena) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.damage = damage;
        this.isArena = isArena;
    }

    /** Construtor legado — assume coordenadas de mundo (não arena). */
    public Projectile(Type type, double x, double y, double vx, double vy, double damage) {
        this(type, x, y, vx, vy, damage, false);
    }

    public void update() {
        x += vx;
        y += vy;
    }

    public Type getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public double getDamage() { return damage; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public boolean isArena() { return isArena; }
}
