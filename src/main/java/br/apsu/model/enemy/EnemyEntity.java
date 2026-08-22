package br.apsu.model.enemy;

/**
 * Entidade de inimigo comum.
 */
public class EnemyEntity {
    private EnemyType type;
    private double worldX, baseY, currentY;
    private double speed, amplitude;
    private boolean alive = true;
    private long lastShotTime = 0;

    public EnemyEntity(EnemyType type, double worldX, double baseY, double speed, double amplitude) {
        this.type = type;
        this.worldX = worldX;
        this.baseY = baseY;
        this.currentY = baseY;
        this.speed = speed;
        this.amplitude = amplitude;
    }

    public void update(double time, double threatMult) {
        if (!alive) return;
        currentY = baseY + Math.sin(time * speed * threatMult + worldX * 0.008) * amplitude;
    }

    public EnemyType getType() { return type; }
    public double getWorldX() { return worldX; }
    public double getBaseY() { return baseY; }
    public double getCurrentY() { return currentY; }
    public double getSpeed() { return speed; }
    public double getAmplitude() { return amplitude; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public long getLastShotTime() { return lastShotTime; }
    public void setLastShotTime(long t) { this.lastShotTime = t; }
}
