package br.apsu.model.enemy;

/**
 * Entidade de inimigo comum com ciclo de vida e animação de derrota.
 */
public class EnemyEntity {
    public static final double DEATH_DURATION = 0.65; // 650ms de animação de morte fluida

    private EnemyType type;
    private double worldX, baseY, currentY;
    private double speed, amplitude;
    private boolean alive = true;
    private boolean dying = false;
    private double deathTimer = 0.0;
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
            case LEVIATA -> 12; // subchefe épico da Fase 4
            case CARANGUEJO, POLVO_ABISSAL, ARRAIAO -> 5; // inimigos blindados/resistentes
            default -> 3; // Peixe, Enguia, Medusa: precisam de 3 tiros (nunca morrem com 1 tiro)
        };
        this.hp = this.maxHp;
    }

    public void update(double dt, double time, double threatMult) {
        if (dying) {
            deathTimer += dt;
            currentY += 2.0; // afunda suavemente nas profundezas oceânicas
            if (deathTimer >= DEATH_DURATION) {
                dying = false;
            }
            return;
        }
        if (!alive) return;
        currentY = baseY + Math.sin(time * speed * threatMult + worldX * 0.008) * amplitude;
    }

    public void update(double time, double threatMult) {
        update(1.0 / 60.0, time, threatMult);
    }

    public EnemyType getType() { return type; }
    public double getWorldX() { return worldX; }
    public double getBaseY() { return baseY; }
    public double getCurrentY() { return currentY; }
    public double getSpeed() { return speed; }
    public double getAmplitude() { return amplitude; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) {
        this.alive = alive;
        if (!alive) {
            this.dying = false;
        }
    }
    public boolean isDying() { return dying; }
    public double getDeathProgress() {
        return dying ? Math.min(1.0, deathTimer / DEATH_DURATION) : 0.0;
    }
    public long getLastShotTime() { return lastShotTime; }
    public void setLastShotTime(long t) { this.lastShotTime = t; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public void setHp(int hp) {
        this.hp = hp;
        if (this.hp <= 0 && alive) {
            this.alive = false;
            this.dying = true;
            this.deathTimer = 0.0;
        }
    }
    public void takeDamage(int dmg) {
        this.hp = Math.max(0, this.hp - dmg);
        if (this.hp <= 0 && alive) {
            this.alive = false;
            this.dying = true;
            this.deathTimer = 0.0;
        }
    }
}
