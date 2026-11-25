package br.apsu.model.environment;

/**
 * Entidade da Tabuleta Sagrada dos Me (APSU-G01).
 * <p>
 * Regras:
 * - Permanece oculta (HIDDEN) no cenário no início de cada fase.
 * - Revela-se (REVEALED) exclusivamente quando atingida pela frente de onda da Ecolocalização (Q).
 * - Torna-se coletável após a revelação; ao ser coletada pelo herói, desbloqueia o portal de descida.
 * - Zero alocações por quadro no hotpath.
 */
public final class SacredTabletEntity {

    public enum State {
        HIDDEN,
        REVEALED,
        COLLECTED
    }

    public static final double WIDTH = 52.0;
    public static final double HEIGHT = 52.0;

    private final int phase;
    private final double worldX;
    private final double worldY;
    private State state;
    private double revealProgress; // 0.0 a 1.0 para transição suave de glow pós-revelação

    public SacredTabletEntity(int phase, double worldX, double worldY) {
        this.phase = phase;
        this.worldX = worldX;
        this.worldY = worldY;
        this.state = State.HIDDEN;
        this.revealProgress = 0.0;
    }

    public void update(double dt) {
        if (state == State.REVEALED && revealProgress < 1.0) {
            revealProgress = Math.min(1.0, revealProgress + dt * 2.0); // 500 ms fade-in
        }
    }

    public boolean reveal() {
        if (state == State.HIDDEN) {
            state = State.REVEALED;
            revealProgress = 0.0;
            return true;
        }
        return false;
    }

    public boolean collect() {
        if (state == State.REVEALED) {
            state = State.COLLECTED;
            return true;
        }
        return false;
    }

    public int getPhase() { return phase; }
    public double getWorldX() { return worldX; }
    public double getWorldY() { return worldY; }
    public State getState() { return state; }
    public boolean isHidden() { return state == State.HIDDEN; }
    public boolean isRevealed() { return state == State.REVEALED; }
    public boolean isCollected() { return state == State.COLLECTED; }
    public double getRevealProgress() { return revealProgress; }

    /** Para testes e resets */
    public void setState(State state) {
        this.state = state;
        if (state == State.REVEALED) {
            this.revealProgress = 1.0;
        }
    }
}
