package br.apsu.model.environment;

/**
 * Níveis de dificuldade e seus comportamentos.
 */
public enum Difficulty {
    FACIL("Fácil", "Inimigos lentos, sem projéteis, 5 de vida.", 5.0, 0.5, 0.7, false, 0.0),
    MEDIO("Médio", "Inimigos cadenciados com disparos e investidas, 4 de vida.", 4.0, 1.0, 1.0, true, 3.0),
    DIFICIL("Difícil", "Inimigos agressivos com disparos rápidos, 3 de vida.", 3.0, 1.25, 1.35, true, 2.2);

    private final String label;
    private final String description;
    private final double initialHP;
    private final double collisionDamage;
    private final double enemySpeedMult;
    private final boolean enemiesCanShoot;
    private final double enemyShootCooldownSeconds;

    Difficulty(String label, String description, double initialHP, double collisionDamage, double enemySpeedMult,
               boolean enemiesCanShoot, double enemyShootCooldownSeconds) {
        this.label = label;
        this.description = description;
        this.initialHP = initialHP;
        this.collisionDamage = collisionDamage;
        this.enemySpeedMult = enemySpeedMult;
        this.enemiesCanShoot = enemiesCanShoot;
        this.enemyShootCooldownSeconds = enemyShootCooldownSeconds;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public double getInitialHP() { return initialHP; }
    public double getCollisionDamage() { return collisionDamage; }
    public double getEnemySpeedMult() { return enemySpeedMult; }
    public boolean canEnemiesShoot() { return enemiesCanShoot; }
    public double getEnemyShootCooldown() { return enemyShootCooldownSeconds; }
    public long getEnemyShootIntervalNanos() { return (long) (enemyShootCooldownSeconds * 1_000_000_000L); }
}
