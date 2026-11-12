package br.apsu.cutscene;

/**
 * Pedido imutavel de cutscene de foco temporario de camera (US-011).
 */
public record CutsceneRequest(
    double targetWorldX,
    double holdDurationSeconds,
    String label
) {
    public CutsceneRequest(double targetWorldX, double holdDurationSeconds) {
        this(targetWorldX, holdDurationSeconds, "");
    }
}
