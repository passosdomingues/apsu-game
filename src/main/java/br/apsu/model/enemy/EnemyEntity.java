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
    private int hp = 1;
    private int maxHp = 1;

    public EnemyEntity(EnemyType type, double worldX, double baseY, double speed, double amplitude) {
        this.type = type;
        this.worldX = worldX;
        this.baseY = baseY;
        this.currentY = baseY;
        this.speed = speed;
        this.amplitude = amplitude;
        this.maxHp = switch (type) {
            case LEVIATA -> 4;
            case CARANGUEJO, POLVO_ABISSAL, ARRAIAO -> 2;
            default -> 1;
        };
        this.hp = this.maxHp;
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
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public void setHp(int hp) { this.hp = hp; if (this.hp <= 0) this.alive = false; }
    public void takeDamage(int dmg) {
        this.hp = Math.max(0, this.hp - dmg);
        if (this.hp <= 0) this.alive = false;
    }
}
