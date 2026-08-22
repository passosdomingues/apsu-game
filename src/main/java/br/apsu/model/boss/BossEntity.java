package br.apsu.model.boss;

/**
 * Entidade do Boss Kullullû (Base, Glacial e Tóxico).
 */
public class BossEntity {
    /** Três estágios legíveis da luta final, definidos pela vida restante. */
    public enum CombatPhase {
        OBSIDIANA_FRIA("Obsidiana Fria"),
        FUSAO_VULCANICA("Fusão Vulcânica"),
        FURIA_DE_APSU("Fúria de Apsu");

        private final String label;

        CombatPhase(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public static final long TELEGRAPH_NANOS = 500_000_000L;
    public static final double BW = 160, BH = 250;
    public static final int[] HP_BY_VAR = { 5, 7, 6 };
    public static final double[] SPD_MULT = { 1.0, 0.7, 1.4 };
    public static final double[] SPREAD = { Math.PI/5, Math.PI/6, Math.PI/3.5 };

    private int variant; // 0=Base, 1=Glacial, 2=Tóxico
    private double x, y;
    private int hp, maxHp;
    private long lastShotTime;
    private CombatPhase combatPhase = CombatPhase.OBSIDIANA_FRIA;
    private long telegraphUntil;

    public BossEntity(int variant, double startX, double startY) {
        this.variant = variant;
        this.x = startX;
        this.y = startY;
        this.maxHp = HP_BY_VAR[Math.min(variant, HP_BY_VAR.length - 1)];
        this.hp = maxHp;
        this.lastShotTime = 0;
    }

    public void updatePosition(double time, double screenHeight) {
        double spd = SPD_MULT[Math.min(variant, SPD_MULT.length - 1)];
        double targetY = screenHeight / 2.0 - BH / 2.0 + Math.sin(time * 1.6 * spd) * 80.0;
        this.y = Math.max(20.0, Math.min(screenHeight - BH - 20.0, targetY));
    }

    public void takeDamage(int dmg) {
        hp = Math.max(0, hp - dmg);
    }

    /**
     * Sincroniza o estágio com a vida. Retorna {@code true} uma única vez por
     * transição, para que a camada de jogo possa emitir feedback e pausar o
     * ataque por um curto telegrafo visual.
     */
    public boolean updateCombatPhase(long now) {
        CombatPhase next = phaseForHp();
        if (next == combatPhase) return false;
        combatPhase = next;
        telegraphUntil = now + TELEGRAPH_NANOS;
        return true;
    }

    private CombatPhase phaseForHp() {
        double hpRatio = maxHp == 0 ? 0 : (double) hp / maxHp;
        if (hpRatio > 2.0 / 3.0) return CombatPhase.OBSIDIANA_FRIA;
        if (hpRatio > 1.0 / 3.0) return CombatPhase.FUSAO_VULCANICA;
        return CombatPhase.FURIA_DE_APSU;
    }

    public boolean isDead() { return hp <= 0; }

    public String getName() {
        return switch(variant) {
            case 1 -> "Kullullû Glacial";
            case 2 -> "Kullullû Tóxico";
            default -> "Kullullû";
        };
    }

    public int getVariant() { return variant; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public long getLastShotTime() { return lastShotTime; }
    public void setLastShotTime(long t) { this.lastShotTime = t; }
    public CombatPhase getCombatPhase() { return combatPhase; }
    public boolean isTelegraphing(long now) { return now < telegraphUntil; }
    public long getTelegraphUntil() { return telegraphUntil; }
}
