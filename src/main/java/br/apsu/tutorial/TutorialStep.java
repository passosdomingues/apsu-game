package br.apsu.tutorial;

/**
 * Passos do tutorial guiado da primeira fase (US-020).
 */
public enum TutorialStep {
    SWIM_DOWN("[↓ / S] Mergulhe — pressione para descer nas correntes"),
    DODGE_ENEMY("[←→] Desvie — evite os peixes que patrulham"),
    READ_GPS("[Q] Ecolocalização — emita o sonar para explorar o fundo"),
    TALK_GUARDIAN("[E] Fale com Enki — aproxime-se do guardião"),
    COMPLETED("");

    private final String instruction;

    TutorialStep(String instruction) {
        this.instruction = instruction;
    }

    public String getInstruction() {
        return instruction;
    }
}
