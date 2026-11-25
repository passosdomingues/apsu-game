package br.apsu.tutorial;

/**
 * Passos do tutorial guiado da primeira fase (US-020).
 */
public enum TutorialStep {
    SWIM_DOWN("Mergulhe contra o empuxo (Seta Baixo ou S)"),
    DODGE_ENEMY("Desvie dos peixes que patrulham a corrente"),
    READ_GPS("Consulte a bússola dourada na ecolocalização do HUD"),
    TALK_GUARDIAN("Fale com o Guardião Atlante (aproxime-se e tecle E)"),
    COMPLETED("");

    private final String instruction;

    TutorialStep(String instruction) {
        this.instruction = instruction;
    }

    public String getInstruction() {
        return instruction;
    }
}
