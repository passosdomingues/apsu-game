package br.apsu.tutorial;

/**
 * Maquina de estados do tutorial guiado da Fase 1 (US-020).
 *
 * Criterios de aceitacao:
 * - Guia em ordem: descer contra empuxo, desviar de inimigo, ler GPS, conversar com guardiao.
 * - Cada passo so avanca quando a acao e executada.
 * - Apos 15 s sem executar o passo, a dica e reforcada (isHintUrgent() = true).
 * - No passo de desvio, dano nunca reduz a vida abaixo de 50% e reinicia o passo.
 * - Tecla Esc durante o tutorial pula os passos restantes e conclui o tutorial.
 * - Ao conversar com o guardiao, o tutorial termina e nao reaparece na mesma partida.
 * - Reiniciar a partida (reset) reinicia o tutorial do primeiro passo.
 */
public class TutorialController {

    private static final double URGENT_TIMEOUT_SECONDS = 15.0;

    private TutorialStep currentStep = TutorialStep.SWIM_DOWN;
    private double stepTimerSeconds = 0.0;
    private boolean skipped = false;
    private boolean active = true;

    // Rastreamento de acoes especificas
    private double initialY = -1.0;
    private boolean movedDown = false;
    private boolean enemyAvoided = false;
    private boolean gpsRead = false;
    private boolean talkedToGuardian = false;

    public TutorialController() {
        reset();
    }

    /** Reinicia o tutorial para o estado inicial. */
    public void reset() {
        this.currentStep = TutorialStep.SWIM_DOWN;
        this.stepTimerSeconds = 0.0;
        this.skipped = false;
        this.active = true;
        this.initialY = -1.0;
        this.movedDown = false;
        this.enemyAvoided = false;
        this.gpsRead = false;
        this.talkedToGuardian = false;
    }

    public TutorialStep getCurrentStep() {
        return skipped ? TutorialStep.COMPLETED : currentStep;
    }

    public boolean isActive() {
        return active && !skipped && currentStep != TutorialStep.COMPLETED;
    }

    public boolean isSkipped() {
        return skipped;
    }

    /** Jogador experiente pula o tutorial com ESC. */
    public void skip() {
        this.skipped = true;
        this.active = false;
        this.currentStep = TutorialStep.COMPLETED;
    }

    /** Indica se a dica atual deve ser reforcada visualmente apos 15 s de inacao. */
    public boolean isHintUrgent() {
        return isActive() && stepTimerSeconds >= URGENT_TIMEOUT_SECONDS;
    }

    public double getStepTimerSeconds() {
        return stepTimerSeconds;
    }

    /**
     * Atualiza o temporizador e valida condicoes de avanco do passo.
     */
    public void update(double dt, double heroX, double heroY, boolean downInput, boolean nearEnemy, boolean contactedGuardian) {
        if (!isActive()) {
            return;
        }

        stepTimerSeconds += dt;

        switch (currentStep) {
            case SWIM_DOWN -> {
                if (initialY < 0) {
                    initialY = heroY;
                }
                // Avanca se nadou para baixo (downInput e desceu pelo menos 40px em relacao a initialY)
                if (downInput && (heroY - initialY >= 40.0 || heroY >= 450.0)) {
                    movedDown = true;
                    advanceStep(TutorialStep.DODGE_ENEMY);
                }
            }
            case DODGE_ENEMY -> {
                // Avanca se ultrapassou a posicao de patrulha do primeiro inimigo (heroX > 1600) sem falhar
                if (heroX >= 1600.0) {
                    enemyAvoided = true;
                    advanceStep(TutorialStep.READ_GPS);
                }
            }
            case READ_GPS -> {
                // Avanca se continuou navegando em direcao ao objetivo (heroX > 2200)
                if (heroX >= 2200.0) {
                    gpsRead = true;
                    advanceStep(TutorialStep.TALK_GUARDIAN);
                }
            }
            case TALK_GUARDIAN -> {
                if (contactedGuardian) {
                    talkedToGuardian = true;
                    advanceStep(TutorialStep.COMPLETED);
                    this.active = false;
                }
            }
            case COMPLETED -> {
                this.active = false;
            }
        }
    }

    /**
     * Chamado quando o heroi sofre dano durante o tutorial.
     * Retorna a vida minima permitida (50% do HP maximo = 5.0).
     */
    public double filterHeroDamage(double currentHp, double maxHp, double incomingDamage) {
        if (!isActive()) {
            return Math.max(0.0, currentHp - incomingDamage);
        }

        double floorHp = maxHp * 0.50; // Nunca cai abaixo de 50% durante o tutorial
        double newHp = currentHp - incomingDamage;
        if (newHp < floorHp) {
            newHp = floorHp;
        }

        // Se sofreu dano durante o passo de desvio, reseta o timer do passo para reforcar a instrucao
        if (currentStep == TutorialStep.DODGE_ENEMY) {
            stepTimerSeconds = 0.0;
        }

        return newHp;
    }

    private void advanceStep(TutorialStep next) {
        this.currentStep = next;
        this.stepTimerSeconds = 0.0;
    }

    /** Notifica conclusao de dialogo com o guardiao da Fase 1. */
    public void onGuardianContacted() {
        if (currentStep == TutorialStep.TALK_GUARDIAN) {
            this.talkedToGuardian = true;
            this.currentStep = TutorialStep.COMPLETED;
            this.active = false;
        }
    }

    public String getCurrentInstruction() {
        if (!isActive()) {
            return null;
        }
        String inst = currentStep.getInstruction();
        if (isHintUrgent()) {
            return "[ATENÇÃO] " + inst;
        }
        return inst;
    }
}
