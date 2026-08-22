package br.apsu.core;

/**
 * Ajuste de desafio por fase, baseado no comportamento observado, sem mudar a
 * dificuldade escolhida no menu. Valores positivos aumentam pressão; danos
 * recebidos reduzem-na, enquanto domínio consistente a eleva gradualmente.
 */
public final class AdaptiveDifficulty {
    private int damageEvents;
    private int defeatedEnemies;

    public void beginPhase() {
        damageEvents = 0;
        defeatedEnemies = 0;
    }

    public void recordDamage() { damageEvents++; }
    public void recordEnemyDefeated() { defeatedEnemies++; }

    public double getAdjustment(double elapsedSeconds) {
        double recovery = Math.min(0.28, damageEvents * 0.07);
        double mastery = Math.min(0.16, defeatedEnemies * 0.02);
        double noDamageBonus = damageEvents == 0 && elapsedSeconds >= 25 ? 0.08 : 0.0;
        return Math.max(-0.28, Math.min(0.24, mastery + noDamageBonus - recovery));
    }

    public int getDamageEvents() { return damageEvents; }
    public int getDefeatedEnemies() { return defeatedEnemies; }
}
