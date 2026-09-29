package br.apsu.model.environment;

/**
 * Níveis de dificuldade e seus comportamentos.
 */
public enum Difficulty {
    FACIL("Fácil", "Inimigos lentos, sem projéteis, 5 de vida.", 5.0, 0.5, 0.7, false),
    MEDIO("Médio", "Inimigos mais rápidos, investidas, 4 de vida.", 4.0, 1.0, 1.0, false),
    DIFICIL("Difícil", "Inimigos agressivos que atiram bolhas malignas, exploração retroativa, 3 de vida.", 3.0, 1.25, 1.35, true);

    private final String label;
    private final String description;
    private final double initialHP;
    private final double collisionDamage;
    private final double enemySpeedMult;
    private final boolean enemiesCanShoot;

    Difficulty(String label, String description, double initialHP, double collisionDamage, double enemySpeedMult, boolean enemiesCanShoot) {
        this.label = label;
        this.description = description;
        this.initialHP = initialHP;
        this.collisionDamage = collisionDamage;
        this.enemySpeedMult = enemySpeedMult;
        this.enemiesCanShoot = enemiesCanShoot;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public double getInitialHP() { return initialHP; }
    public double getCollisionDamage() { return collisionDamage; }
    public double getEnemySpeedMult() { return enemySpeedMult; }
    public boolean canEnemiesShoot() { return enemiesCanShoot; }
}
