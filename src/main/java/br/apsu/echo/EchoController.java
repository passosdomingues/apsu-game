package br.apsu.echo;

/**
 * Controlador desacoplado do radar de ecolocalizacao temporario (US-030).
 *
 * Parametros e regras:
 * - Duracao ativa: 4.0 segundos (ECHO_DURATION_SECONDS).
 * - Tempo de recarga: 8.0 segundos (COOLDOWN_DURATION_SECONDS).
 * - Tecla de acionamento padrao: Q (sem conflito com WASD, setas, Espaco, Enter, E, F).
 * - Disponivel imediatamente no inicio de uma nova partida.
 * - Suporta pausa e retomada de simulacao sem avanco indevido de timers.
 * - Zero alocacoes por quadro no hot path a 60 FPS (Regra 11).
 */
public final class EchoController {

    public static final double ECHO_DURATION_SECONDS = 4.0;
    public static final double COOLDOWN_DURATION_SECONDS = 8.0;

    public enum State {
        READY,
        ACTIVE,
        COOLDOWN
    }

    private State state = State.READY;
    private double activeTimer = 0.0;
    private double cooldownTimer = 0.0;
    private boolean pauseHighlightRequested = false;

    public EchoController() {
        reset();
    }

    /**
     * Reseta o controlador para o estado inicial pronto (READY).
     */
    public void reset() {
        this.state = State.READY;
        this.activeTimer = 0.0;
        this.cooldownTimer = 0.0;
        this.pauseHighlightRequested = false;
    }

    /**
     * Tenta ativar o eco.
     * Retorna true se a ativacao foi bem-sucedida (estava READY).
     * Retorna false se o eco estava em COOLDOWN ou ja ACTIVE.
     */
    public boolean trigger() {
        if (state == State.READY) {
            state = State.ACTIVE;
            activeTimer = ECHO_DURATION_SECONDS;
            cooldownTimer = 0.0;
            pauseHighlightRequested = false;
            return true;
        } else if (state == State.COOLDOWN) {
            // AC3: Quando em recarga e Q e pressionado, destaca o indicador de recarga
            pauseHighlightRequested = true;
            return false;
        }
        return false;
    }

    /**
     * Atualiza os contadores com base no delta de tempo dt em segundos.
     * Deve ser chamado apenas quando a simulacao do jogo esta rodando (nao pausado).
     */
    public void update(double dt) {
        if (dt <= 0.0) return;

        if (state == State.ACTIVE) {
            activeTimer -= dt;
            if (activeTimer <= 0.0) {
                activeTimer = 0.0;
                state = State.COOLDOWN;
                cooldownTimer = COOLDOWN_DURATION_SECONDS;
            }
        } else if (state == State.COOLDOWN) {
            cooldownTimer -= dt;
            if (cooldownTimer <= 0.0) {
                cooldownTimer = 0.0;
                state = State.READY;
                pauseHighlightRequested = false;
            }
        }
    }

    public State getState() {
        return state;
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    public boolean isReady() {
        return state == State.READY;
    }

    public boolean isOnCooldown() {
        return state == State.COOLDOWN;
    }

    public double getActiveTimer() {
        return activeTimer;
    }

    public double getCooldownTimer() {
        return cooldownTimer;
    }

    /**
     * Retorna o progresso da recarga de 0.0 (iniciando recarga) a 1.0 (recarga completa/pronto).
     */
    public double getCooldownProgress() {
        if (state == State.READY || state == State.ACTIVE) {
            return 1.0;
        }
        double elapsed = COOLDOWN_DURATION_SECONDS - cooldownTimer;
        return Math.max(0.0, Math.min(1.0, elapsed / COOLDOWN_DURATION_SECONDS));
    }

    public boolean isCooldownHighlightRequested() {
        return pauseHighlightRequested;
    }

    public void clearCooldownHighlight() {
        this.pauseHighlightRequested = false;
    }
}
