package br.apsu.model.hero;

/**
 * Entidade cinemática do herói, gerenciando posição, velocidade, inércia, vida,
 * sequência natural de ataque em 4 estágios e física procedural da cauda.
 */
public class HeroEntity {
    public static final double HW = 72, HH = 192;
    public static final double RENDER_W = 84.0, RENDER_H = 192.0;

    private HeroType type;
    private double x, y;
    private double vx, vy;
    private boolean facingRight = true;
    private double hp, maxHP;
    private boolean invulnerable;
    private long invulnerableTime;
    private boolean hasBubblePower = false;
    private boolean attackPose2;
    // Estilo visual do ataque (2026-08-16): cicla thrust/slash/spin/charge
    // a cada tiro — independente de attackPose2 (que só controla offset de
    // spawn da bolha, não tocado, pra não arriscar mudar física/colisão).
    private int attackStyleIndex = 0;
    private long lastShotTime;
    private double pitchAngle = 0;

    // --- Ataque em 4 Estágios (Charge -> Impulse -> Firing -> Recovery) ---
    private boolean shooting = false;
    private long shootingTime = 0;
    private double attackOffsetX = 0; // Deslocamento suave X do ataque
    private double attackOffsetY = 0; // Deslocamento suave Y do ataque

    // --- Física Procedural da Cauda ---
    private double tailPhase = 0;
    private double tailAngle = 0;
    private double tailAmplitude = 2.0;
    private double currentSpeedRatio = 0;
    private double lastSpeed = 0;

    // --- Squash & Stretch (leitura de peso/impulso, estilo Donkey Kong Country) ---
    private double squashX = 1.0;
    private double squashY = 1.0;

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
        this.pitchAngle = 0;
        this.facingRight = true;
        this.invulnerable = false;
        this.hasBubblePower = false;
        this.attackPose2 = false;
        this.attackStyleIndex = 0;
        this.shooting = false;
        this.lastShotTime = 0;
        this.attackOffsetX = 0;
        this.attackOffsetY = 0;
        this.tailPhase = 0;
        this.tailAngle = 0;
        this.tailAmplitude = 2.0;
        this.lastSpeed = 0;
        this.squashX = 1.0;
        this.squashY = 1.0;
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
        double safeBuoyancy = Math.max(0.82, Math.min(1.18, buoyancyMult));
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

        // --- SQUASH & STRETCH (leitura de peso/impulso, estilo Donkey Kong Country) ---
        // Estica levemente na direção do movimento quando acelera, "esmaga" quando
        // freia/colide — reforça a sensação de peso na água sem precisar de frames
        // de sprite extras (funciona em cima de qualquer pose renderizada).
        double speedDelta = currentSpeed - lastSpeed;
        double stretch = Math.max(-0.10, Math.min(0.14, speedDelta * 0.9));
        double targetSquashX = 1.0 + stretch;
        double targetSquashY = 1.0 - stretch * 0.6;
        squashX += (targetSquashX - squashX) * 0.25;
        squashY += (targetSquashY - squashY) * 0.25;
        lastSpeed = currentSpeed;

        // --- ATUALIZAÇÃO DA ANIMAÇÃO PROCEDURAL DA CAUDA ---
        // Nado rápido -> maior frequência e amplitude; parado -> ondulação leve idle
        double waveFreq = 4.0 + currentSpeedRatio * 8.0;
        tailPhase += waveFreq * 0.016; // incrementa fase por frame (~60 FPS)

        double targetAmp = 1.8 + currentSpeedRatio * 6.2;
        this.tailAmplitude += (targetAmp - this.tailAmplitude) * 0.15;

        // Atraso de inércia da cauda em curvas (tailLag)
        double targetTailAngle = -vy * 2.2 * (facingRight ? 1 : -1);
        this.tailAngle += (targetTailAngle - this.tailAngle) * 0.12;

        // Pitch Angle (inclinação de nado)
        double targetPitch = Math.max(-22.0, Math.min(22.0, vy * 3.2 * (facingRight ? 1 : -1)));
        this.pitchAngle += (targetPitch - this.pitchAngle) * 0.18;

        // --- MÁQUINA DE ESTADOS DO ATAQUE EM 4 ESTÁGIOS ---
        if (shooting) {
            long elapsedNano = Math.max(0, currentNanoTime - shootingTime);
            long elapsedMs = elapsedNano / 1_000_000L;

            if (elapsedMs < 80) {
                // Estágio 1: Recuo / Carga (-4px)
                double progress = elapsedMs / 80.0;
                attackOffsetX = -4.0 * progress * (facingRight ? 1 : -1);
                attackOffsetY = -1.0 * progress;
            } else if (elapsedMs < 180) {
                // Estágio 2: Impulso / Projeção (+6px)
                double progress = (elapsedMs - 80) / 100.0;
                attackOffsetX = (-4.0 + 10.0 * progress) * (facingRight ? 1 : -1);
                attackOffsetY = (-1.0 + 2.0 * progress);
                // Pop de squash no pico do impulso (leitura de impacto imediata)
                double impactPulse = Math.sin(progress * Math.PI) * 0.12;
                squashX = 1.0 + impactPulse;
                squashY = 1.0 - impactPulse * 0.7;
            } else if (elapsedMs < 320) {
                // Estágio 3: Disparo e Sustentação (+4px -> 0px)
                double progress = (elapsedMs - 180) / 140.0;
                attackOffsetX = (6.0 - 6.0 * progress) * (facingRight ? 1 : -1);
                attackOffsetY = (1.0 - 1.0 * progress);
            } else if (elapsedMs < 450) {
                // Estágio 4: Recuperação Suave (0px)
                double progress = (elapsedMs - 320) / 130.0;
                attackOffsetX = (0.0);
                attackOffsetY = (0.0);
            } else {
                shooting = false;
                attackOffsetX = 0;
                attackOffsetY = 0;
            }
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
        if (invulnerable && (nanoTime - invulnerableTime > 2_200_000_000L)) {
            invulnerable = false;
        }
    }

    public void triggerShooting(long nanoTime) {
        this.shooting = true;
        this.shootingTime = nanoTime;
        this.attackPose2 = !this.attackPose2;
        this.attackStyleIndex = (this.attackStyleIndex + 1) % 4;
        this.attackOffsetX = 0;
        this.attackOffsetY = 0;
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
    public double getPitchAngle() { return pitchAngle; }
    public double getSquashX() { return squashX; }
    public double getSquashY() { return squashY; }
    public boolean isFacingRight() { return facingRight; }
    public double getHp() { return hp; }
    public double getMaxHP() { return maxHP; }
    public void setMaxHP(double maxHP) { this.maxHP = maxHP; this.hp = Math.min(hp, maxHP); }
    public boolean isInvulnerable() { return invulnerable; }
    public boolean hasBubblePower() { return hasBubblePower; }
    public void setHasBubblePower(boolean has) { this.hasBubblePower = has; }
    public boolean isShooting() { return shooting; }
    public double getAttackElapsedSeconds(long currentNanoTime) {
        return shooting ? Math.max(0, currentNanoTime - shootingTime) / 1_000_000_000.0 : 0;
    }
    public double getAttackOffsetX() { return attackOffsetX; }
    public double getAttackOffsetY() { return attackOffsetY; }
    public double getTailPhase() { return tailPhase; }
    public double getTailAngle() { return tailAngle; }
    public double getTailAmplitude() { return tailAmplitude; }
    public double getCurrentSpeedRatio() { return currentSpeedRatio; }

    public double getBubbleSpawnX() {
        double baseOffset = attackPose2 ? 125 : 180;
        return facingRight ? x + baseOffset : x - 15;
    }

    public double getBubbleSpawnY() {
        return attackPose2 ? y + 65 : y + 33;
    }

    public boolean isAttackPose2() { return attackPose2; }
    public int getAttackStyleIndex() { return attackStyleIndex; }
    public long getLastShotTime() { return lastShotTime; }
    public void setLastShotTime(long t) { this.lastShotTime = t; }
}
