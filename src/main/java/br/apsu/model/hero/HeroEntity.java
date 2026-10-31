package br.apsu.model.hero;

/**
 * Entidade cinemática do herói, gerenciando posição, velocidade, inércia, vida,
 * movimento, vida, poderes e janela de disparo.
 */
public class HeroEntity {
    public static final double HW = 72, HH = 192;
    public static final double ATTACK_DURATION_SECONDS = 0.26;

    private HeroType type;
    private double x, y;
    private double vx, vy;
    private boolean facingRight = true;
    private double hp, maxHP;
    private boolean invulnerable;
    private long invulnerableTime;
    private boolean hasBubblePower = false;
    private boolean multipleShotMode;
    private long lastShotTime;
    private boolean shooting = false;
    private long shootingTime = 0;
    private double tailPhase = 0;
    private double currentSpeedRatio = 0;

    public HeroEntity(HeroType type, double initialHP) {
        this.type = type;
        this.maxHP = initialHP;
        this.hp = initialHP;
        resetPosition(100, 300);
    }

    public void resetPosition(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.vx = 0;
        this.vy = 0;
        this.facingRight = true;
        this.invulnerable = false;
        this.hasBubblePower = false;
        this.multipleShotMode = false;
        this.shooting = false;
        this.lastShotTime = 0;
        this.tailPhase = 0;
        this.currentSpeedRatio = 0;
    }

    /** Repositions at a phase transition while preserving HP and unlocked powers. */
    public void beginPhaseAt(double startX, double startY) {
        x = startX;
        y = startY;
        vx = 0;
        vy = 0;
        facingRight = true;
        shooting = false;
        currentSpeedRatio = 0;
    }

    /**
     * Atualiza física com suporte a correntes, pressão e cauda procedural.
     */
    public void updatePhysics(boolean left, boolean right, boolean up, boolean down,
                               double minY, double maxY, int phaseDone,
                               double buoyancyMult, double currentFx, double currentFy) {
        updatePhysics(left, right, up, down, minY, maxY, phaseDone,
            buoyancyMult, currentFx, currentFy, System.nanoTime());
    }

    /**
     * Atualiza a física usando o relógio monotônico do loop do jogo.
     *
     * <p>Receber o tempo aqui mantém a máquina de estados do ataque no mesmo
     * relógio das colisões, invulnerabilidade e projéteis. Também permite que
     * simulações e testes avancem a animação de forma determinística.</p>
     */
    public void updatePhysics(boolean left, boolean right, boolean up, boolean down,
                               double minY, double maxY, int phaseDone,
                               double buoyancyMult, double currentFx, double currentFy,
                               long currentNanoTime) {
        // O bônus por fase é deliberadamente pequeno: a progressão deve
        // recompensar sem transformar o controle em algo imprevisível.
        double speedMult = (1.0 + phaseDone * 0.025);
        double accelPower = type.getAccel() * speedMult;

        double ax = 0, ay = 0;
        if (left)  { ax -= accelPower; facingRight = false; }
        if (right) { ax += accelPower; facingRight = true;  }
        if (up)    { ay -= accelPower; }
        if (down)  { ay += accelPower; }

        // Zonas ambientais dão textura ao nado, mas não sequestram o input.
        // Limites estreitos evitam afundamento/subida súbita nas fases 3–5.
        double safeBuoyancy = Math.max(0.68, Math.min(1.18, buoyancyMult));
        ay -= type.getBuoy() * safeBuoyancy;
        ax += currentFx * 0.08;
        ay += currentFy * 0.08;

        vx += ax;
        vy += ay;

        // Controle subaquatico: ao inverter direcao, freia forte antes de ganhar impulso.
        // Valores maiores = heroi para/vira mais rapido = sensacao mais responsiva.
        boolean counterX = (left && vx > 0) || (right && vx < 0);
        boolean counterY = (up && vy > 0) || (down && vy < 0);
        if (counterX) {
            vx *= 0.58;   // era 0.70: virada bem mais afiada
        } else if (!left && !right) {
            vx *= 0.86;   // era 0.92: para mais rapido ao soltar
        }
        if (counterY) {
            vy *= 0.62;   // era 0.70
        } else if (!up && !down) {
            vy *= 0.88;   // era 0.94
        }

        // --- FÍSICA DE EMPUXO E ARRASTO REALISTAS (auditoria 2026-08-15) ---
        // Antes: empuxo era só a constante acima (linha "ay -= type.getBuoy()...")
        // e o arrasto decaía linearmente (vx *= drag) — funciona, mas não segue
        // nenhum dos dois efeitos físicos reais:
        //   1) Empuxo de verdade NÃO é uma força constante: o corpo oscila ao
        //      redor do ponto de flutuação neutra (pequenos ajustes passivos),
        //      e essa oscilação é dominada/some quando a natação ativa (nadadeiras)
        //      assume o controle — por isso amortecemos por (1 - currentSpeedRatio).
        //   2) Arrasto em natação acontece em regime turbulento (Reynolds alto),
        //      onde a força de arrasto escala com o QUADRADO da velocidade
        //      (F = -k·v·|v|), não linearmente. O coeficiente é derivado do
        //      `drag` linear já calibrado por HeroType, igualando o freio na
        //      velocidade máxima do tipo — o "feel" de topo de velocidade de
        //      cada herói continua parecido, só a curva muda: desliza mais solto
        //      perto do repouso (mais "flutuante") e freia mais forte perto do
        //      máximo (mais "peso" na água), em vez do freio uniforme de antes.
        // currentSpeedRatio usa o valor do frame anterior (calculado mais abaixo)
        // — defasagem de 1 frame, imperceptível para um termo decorativo.
        double idleBob = Math.sin(tailPhase * 0.4) * type.getBuoy() * safeBuoyancy * 0.45
                          * (1.0 - currentSpeedRatio);
        vy -= idleBob;

        double dragCoef = (1.0 - type.getDrag()) / Math.max(1.0, type.getMaxSpeed());
        vx -= dragCoef * vx * Math.abs(vx);
        vy -= dragCoef * vy * Math.abs(vy);

        double currentSpeed = Math.sqrt(vx * vx + vy * vy);
        double maxSpd = type.getMaxSpeed() * speedMult;
        if (currentSpeed > maxSpd) {
            vx = (vx / currentSpeed) * maxSpd;
            vy = (vy / currentSpeed) * maxSpd;
        }

        x += vx;
        y += vy;
        y = Math.max(minY, Math.min(maxY, y));

        currentSpeedRatio = Math.min(1.0, currentSpeed / Math.max(1.0, maxSpd));
        tailPhase += (4.0 + currentSpeedRatio * 8.0) * 0.016;

        if (shooting && currentNanoTime - shootingTime >= (long) (ATTACK_DURATION_SECONDS * 1_000_000_000L)) {
            shooting = false;
        }
    }

    public void updatePhysics(boolean left, boolean right, boolean up, boolean down,
                               double minY, double maxY, int phaseDone) {
        updatePhysics(left, right, up, down, minY, maxY, phaseDone, 1.0, 0, 0, System.nanoTime());
    }

    public void applyGeyserImpulse(double impulseVy) {
        vy = Math.min(vy, impulseVy);
    }

    /** Afasta o herói da fonte de dano com prioridade sobre a inércia atual. */
    public void applyKnockback(double impulseX, double impulseY) {
        if (Math.abs(impulseX) > 0.05) vx = impulseX;
        if (Math.abs(impulseY) > 0.05) vy = impulseY;
    }

    public void applyDamage(double damage, long nanoTime) {
        if (invulnerable) return;
        hp = Math.max(0, hp - damage);
        invulnerable = true;
        invulnerableTime = nanoTime;
    }

    public void heal(double amount) {
        hp = Math.min(maxHP, hp + amount);
    }

    public void checkInvulnerability(long nanoTime) {
        if (invulnerable && (nanoTime - invulnerableTime > 1_250_000_000L)) {
            invulnerable = false;
        }
    }

    public void triggerShooting(long nanoTime) {
        this.shooting = true;
        this.shootingTime = nanoTime;
    }

    // Getters & Setters
    public HeroType getType() { return type; }
    public void setType(HeroType type) { this.type = type; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public boolean isFacingRight() { return facingRight; }
    public double getHp() { return hp; }
    public double getMaxHP() { return maxHP; }
    public void setMaxHP(double maxHP) { this.maxHP = maxHP; this.hp = Math.min(hp, maxHP); }
    public boolean isInvulnerable() { return invulnerable; }
    public boolean hasBubblePower() { return hasBubblePower; }
    public void setHasBubblePower(boolean has) { this.hasBubblePower = has; }
    public boolean isShooting() { return shooting; }
    public double getAttackProgress(long currentNanoTime) {
        if (!shooting) return 0;
        long elapsed = Math.max(0, currentNanoTime - shootingTime);
        return Math.min(1.0, elapsed / (ATTACK_DURATION_SECONDS * 1_000_000_000.0));
    }
    public double getCurrentSpeedRatio() { return currentSpeedRatio; }

    public double getBubbleSpawnX() {
        return facingRight ? x + 92 : x - 20;
    }

    public double getBubbleSpawnY() {
        return y + 74;
    }

    public boolean isMultipleShotMode() { return multipleShotMode; }
    public void toggleMultipleShotMode() { multipleShotMode = !multipleShotMode; }
    public long getLastShotTime() { return lastShotTime; }
    public void setLastShotTime(long t) { this.lastShotTime = t; }
}
