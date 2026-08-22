package br.apsu.core;

/**
 * Câmera 2.5D desacoplada para gerenciamento de rolagem de tela.
 */
public class Camera {
    private double x;
    private final double viewportWidth;
    private final double worldWidth;
    private double shakeIntensity;
    private long shakeStartedAt;
    private long shakeDurationNanos;

    public Camera(double viewportWidth, double worldWidth) {
        this.viewportWidth = viewportWidth;
        this.worldWidth = worldWidth;
        this.x = 0;
    }

    public void update(double targetWorldX) {
        double targetCamX = targetWorldX - viewportWidth * 0.3;
        this.x = Math.max(0, Math.min(worldWidth - viewportWidth, targetCamX));
    }

    /**
     * Inicia uma vibração curta de tela. A oscilação é calculada a partir do
     * relógio do jogo, portanto não cria estado aleatório nem afeta a física.
     */
    public void applyShake(double intensity, long durationNanos, long now) {
        if (intensity <= 0 || durationNanos <= 0) return;
        shakeIntensity = Math.max(shakeIntensity, intensity);
        shakeStartedAt = now;
        shakeDurationNanos = durationNanos;
    }

    /** Atualiza o ciclo de vida do shake; deve ser chamado uma vez por frame. */
    public void updateShake(long now) {
        if (shakeDurationNanos > 0 && now - shakeStartedAt >= shakeDurationNanos) {
            shakeIntensity = 0;
            shakeDurationNanos = 0;
        }
    }

    /** Cancela feedback pendente ao reiniciar ou trocar de sessão. */
    public void clearShake() {
        shakeIntensity = 0;
        shakeStartedAt = 0;
        shakeDurationNanos = 0;
    }

    public double getShakeX(long now) {
        return shakeOffset(now, 37.0);
    }

    public double getShakeY(long now) {
        return shakeOffset(now, 53.0);
    }

    private double shakeOffset(long now, double frequency) {
        if (shakeDurationNanos <= 0 || now < shakeStartedAt) return 0;
        double progress = (double) (now - shakeStartedAt) / shakeDurationNanos;
        if (progress >= 1) return 0;
        double envelope = 1.0 - progress;
        double seconds = (now - shakeStartedAt) / 1_000_000_000.0;
        return Math.sin(seconds * frequency) * shakeIntensity * envelope;
    }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double toScreenX(double worldX) { return worldX - x; }
}
