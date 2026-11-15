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
    /** Janela de aviso visual (telegrafo) antes de disparar o ataque (US-041: 0.6s a 1.0s). */
    public static final long ATTACK_TELEGRAPH_NANOS = 750_000_000L;
    /** Dano fixo por contato fisico direto com Kullullu (US-002). */
    public static final double CONTACT_DAMAGE = 1.0;
    /** Hitbox gameplay do boss; o render 3D mantém proporção maior que o herói. */
    public static final double BW = 260, BH = 400;
    public static final double RENDER_W = 3800, RENDER_H = 4600;
    public static final int[] HP_BY_VAR = { 5, 7, 6 };
    public static final double[] SPD_MULT = { 1.0, 0.7, 1.4 };
    public static final double[] SPREAD = { Math.PI/5, Math.PI/6, Math.PI/3.5 };

    private int variant; // 0=Base, 1=Glacial, 2=Tóxico
    private double x, y;
    private int hp, maxHp;
    private long lastShotTime;
    private CombatPhase combatPhase = CombatPhase.OBSIDIANA_FRIA;
    private long telegraphUntil;

    // Estado de telegrafo de ataque (US-041)
    private boolean attackTelegraphing = false;
    private long attackTelegraphStart = 0;
    private long attackTelegraphUntil = 0;
    private double attackTargetX = 0;
    private double attackTargetY = 0;
    private long attackSeed = 0;

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
        double targetY = screenHeight / 2.0 - BH / 2.0 - 130.0 + Math.sin(time * 1.2 * spd) * 28.0;
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
        clearAttackTelegraph();
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

    // Metodos de telegrafo de ataque (US-041)
    public void startAttackTelegraph(long now, double targetX, double targetY) {
        this.attackTelegraphing = true;
        this.attackTelegraphStart = now;
        this.attackTelegraphUntil = now + ATTACK_TELEGRAPH_NANOS;
        this.attackTargetX = targetX;
        this.attackTargetY = targetY;
        this.attackSeed = now;
    }

    public boolean isAttackTelegraphing(long now) {
        return attackTelegraphing && now < attackTelegraphUntil;
    }

    public boolean isAttackReadyToFire(long now) {
        return attackTelegraphing && now >= attackTelegraphUntil;
    }

    public void finishAttackTelegraph(long now) {
        this.attackTelegraphing = false;
        this.attackTelegraphStart = 0;
        this.attackTelegraphUntil = 0;
        this.lastShotTime = now;
    }

    public void clearAttackTelegraph() {
        this.attackTelegraphing = false;
        this.attackTelegraphStart = 0;
        this.attackTelegraphUntil = 0;
    }

    public double getAttackTelegraphProgress(long now) {
        if (!attackTelegraphing || attackTelegraphUntil <= attackTelegraphStart) return 0.0;
        if (now >= attackTelegraphUntil) return 1.0;
        return Math.max(0.0, Math.min(1.0, (double) (now - attackTelegraphStart) / (attackTelegraphUntil - attackTelegraphStart)));
    }

    public double getAttackTargetX() { return attackTargetX; }
    public double getAttackTargetY() { return attackTargetY; }
    public long getAttackSeed() { return attackSeed; }
    public long getAttackTelegraphStart() { return attackTelegraphStart; }
    public long getAttackTelegraphUntil() { return attackTelegraphUntil; }
}
