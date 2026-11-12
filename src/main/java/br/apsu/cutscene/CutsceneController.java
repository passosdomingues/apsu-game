package br.apsu.cutscene;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Gerencia a maquina de estados de cutscenes de foco de camera (US-011).
 *
 * Transicoes:
 * - PAN_TO: deslocamento suave da camera ate o alvo (ate 0.8 s)
 * - HOLD: mantem o foco no alvo pela duracao solicitada
 * - PAN_BACK: retorno suave da camera ao heroi (ate 0.8 s)
 *
 * Regras:
 * - Pular (Espaco, Enter, Esc): transiciona para retorno rapido (conclui em <= 0.3 s).
 * - Fila: pedidos simultaneos sao enfileirados em ordem sem sobreposicao.
 * - Zero alocacao no hot path (update).
 */
public final class CutsceneController {

    public enum Phase {
        IDLE,
        PAN_TO,
        HOLD,
        PAN_BACK
    }

    private static final double MAX_PAN_DURATION = 0.8;
    private static final double SKIP_PAN_DURATION = 0.3;

    private final Queue<CutsceneRequest> queue = new ArrayDeque<>();
    private CutsceneRequest activeRequest = null;
    private Phase phase = Phase.IDLE;

    private double startCameraWorldX;
    private double targetCameraWorldX;
    private double currentCameraWorldX;

    private double phaseElapsedTime = 0.0;
    private double panToDuration = MAX_PAN_DURATION;
    private double panBackDuration = MAX_PAN_DURATION;

    public CutsceneController() {}

    /**
     * Enfileira uma cutscene. Se estiver ocioso, inicia imediatamente.
     */
    public void enqueue(CutsceneRequest request, double currentCameraX) {
        if (request == null) return;
        queue.offer(request);
        if (phase == Phase.IDLE) {
            startNext(currentCameraX);
        }
    }

    private void startNext(double currentCameraX) {
        activeRequest = queue.poll();
        if (activeRequest == null) {
            phase = Phase.IDLE;
            return;
        }
        startCameraWorldX = currentCameraX;
        targetCameraWorldX = activeRequest.targetWorldX();
        currentCameraWorldX = currentCameraX;
        phaseElapsedTime = 0.0;
        panToDuration = MAX_PAN_DURATION;
        panBackDuration = MAX_PAN_DURATION;
        phase = Phase.PAN_TO;
    }

    /**
     * Atualiza a cutscene a cada frame.
     * Retorna a posicao de foco da camera no mundo.
     */
    public double update(double heroWorldX, double dt) {
        if (phase == Phase.IDLE) {
            return heroWorldX;
        }

        phaseElapsedTime += dt;

        switch (phase) {
            case PAN_TO -> {
                double t = Math.min(1.0, phaseElapsedTime / panToDuration);
                double ease = smoothStep(t);
                currentCameraWorldX = startCameraWorldX + (targetCameraWorldX - startCameraWorldX) * ease;
                if (t >= 1.0) {
                    phase = Phase.HOLD;
                    phaseElapsedTime = 0.0;
                }
            }
            case HOLD -> {
                currentCameraWorldX = targetCameraWorldX;
                if (phaseElapsedTime >= activeRequest.holdDurationSeconds()) {
                    phase = Phase.PAN_BACK;
                    phaseElapsedTime = 0.0;
                    startCameraWorldX = targetCameraWorldX;
                }
            }
            case PAN_BACK -> {
                double t = Math.min(1.0, phaseElapsedTime / panBackDuration);
                double ease = smoothStep(t);
                currentCameraWorldX = startCameraWorldX + (heroWorldX - startCameraWorldX) * ease;
                if (t >= 1.0) {
                    currentCameraWorldX = heroWorldX;
                    if (!queue.isEmpty()) {
                        startNext(heroWorldX);
                    } else {
                        phase = Phase.IDLE;
                        activeRequest = null;
                    }
                }
            }
            case IDLE -> {
                return heroWorldX;
            }
        }

        return currentCameraWorldX;
    }

    /**
     * Pula a cutscene atual (Espaco, Enter ou Esc).
     * Transiciona imediatamente para o retorno acelerado (conclusao em ate 0.3 s).
     */
    public void skip(double heroWorldX) {
        if (phase == Phase.IDLE) return;
        if (phase == Phase.PAN_BACK) {
            // Acelera o retorno restante
            panBackDuration = SKIP_PAN_DURATION;
            phaseElapsedTime = Math.min(phaseElapsedTime, SKIP_PAN_DURATION);
            return;
        }
        // Se estiver em PAN_TO ou HOLD, inicia o retorno imediato
        startCameraWorldX = currentCameraWorldX;
        phase = Phase.PAN_BACK;
        phaseElapsedTime = 0.0;
        panBackDuration = SKIP_PAN_DURATION;
    }

    /**
     * Interrompe totalmente e limpa a fila.
     */
    public void stop() {
        queue.clear();
        activeRequest = null;
        phase = Phase.IDLE;
        phaseElapsedTime = 0.0;
    }

    public boolean isActive() {
        return phase != Phase.IDLE;
    }

    public Phase getPhase() {
        return phase;
    }

    public CutsceneRequest getActiveRequest() {
        return activeRequest;
    }

    public int getQueueSize() {
        return queue.size();
    }

    public double getCurrentCameraWorldX() {
        return currentCameraWorldX;
    }

    private static double smoothStep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }
}
