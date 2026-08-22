package br.apsu.model.environment;

/**
 * Elementos do cenário (gêiseres, colunas de coral, baú, correntes, zonas de pressão,
 * obstáculos móveis e rochas vulcânicas).
 */
public class SceneryElement {
    public enum Type {
        CORAL,
        GEYSER,
        CHEST,
        SHIPWRECK,
        RUINS,
        /** Corrente de água: aplica força lateral/vertical ao herói na zona de colisão. */
        CURRENT,
        /** Zona de empuxo variável: altera a flutuabilidade local. */
        PRESSURE_ZONE,
        /** Obstáculo móvel: oscila entre dois pontos, colisão AABB dinâmica. */
        MOVING_OBSTACLE,
        /** Rocha/coluna vulcânica: causa dano por calor ao contato. */
        VOLCANIC_ROCK,
        /** Zona de lava: dano contínuo enquanto dentro. */
        LAVA_POOL
    }

    private Type type;
    private double worldX, worldY, width, height;
    private boolean active = true;

    // --- Campos para mecânicas dinâmicas ---
    /** Velocidade da corrente (vx, vy) aplicada ao herói quando na zona. */
    private double currentVx = 0, currentVy = 0;

    /** Multiplicador de empuxo local (1.0 = normal, <1 = afunda, >1 = sobe). */
    private double buoyancyMult = 1.0;

    /** Para MOVING_OBSTACLE: ponto base e amplitude de oscilação. */
    private double baseX, baseY;
    private double oscAmplX = 0, oscAmplY = 0;
    /** Frequência de oscilação em rad/s. */
    private double oscFreq = 1.0;
    /** Offset de fase da oscilação (para desincronizar obstáculos). */
    private double oscPhase = 0;

    public SceneryElement(Type type, double worldX, double worldY, double width, double height) {
        this.type = type;
        this.worldX = worldX;
        this.worldY = worldY;
        this.baseX = worldX;
        this.baseY = worldY;
        this.width = width;
        this.height = height;
    }

    /** Configura corrente de água. */
    public SceneryElement withCurrent(double vx, double vy) {
        this.currentVx = vx;
        this.currentVy = vy;
        return this;
    }

    /** Configura zona de pressão/empuxo. */
    public SceneryElement withBuoyancy(double mult) {
        this.buoyancyMult = mult;
        return this;
    }

    /** Configura oscilação do obstáculo móvel. */
    public SceneryElement withOscillation(double amplX, double amplY, double freq, double phase) {
        this.oscAmplX = amplX;
        this.oscAmplY = amplY;
        this.oscFreq = freq;
        this.oscPhase = phase;
        return this;
    }

    /** Atualiza posição de obstáculos móveis a cada frame. */
    public void updatePosition(double timeSeconds) {
        if (type == Type.MOVING_OBSTACLE) {
            worldX = baseX + oscAmplX * Math.sin(oscFreq * timeSeconds + oscPhase);
            worldY = baseY + oscAmplY * Math.cos(oscFreq * timeSeconds + oscPhase * 1.3);
        }
    }

    // Getters
    public Type getType() { return type; }
    public double getWorldX() { return worldX; }
    public double getWorldY() { return worldY; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public double getCurrentVx() { return currentVx; }
    public double getCurrentVy() { return currentVy; }
    public double getBuoyancyMult() { return buoyancyMult; }
    public double getBaseX() { return baseX; }
    public double getBaseY() { return baseY; }
    public double getOscAmplX() { return oscAmplX; }
    public double getOscAmplY() { return oscAmplY; }
    public double getOscFreq() { return oscFreq; }
    public double getOscPhase() { return oscPhase; }
}
