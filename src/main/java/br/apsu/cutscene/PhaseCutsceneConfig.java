package br.apsu.cutscene;

/**
 * Configuracao imutavel de timing e duracao de cutscenes por fase (APSU-N02).
 * Elimina numeros magicos dispersos e padroniza a experiencia narrativa.
 */
public final class PhaseCutsceneConfig {

    public static final double P1_INTRO_DURATION = 4.0;
    public static final double P2_CHEST_DURATION = 3.0;
    public static final double GUARDIAN_APPROACH_DURATION = 3.0;
    public static final double PHASE_EXIT_DURATION = 2.0;
    public static final double P5_BOSS_INTRO_DURATION = 5.0;

    private PhaseCutsceneConfig() {}

    /** Retorna a duracao de apresentacao do guardiao para a fase solicitada. */
    public static double guardianIntroDuration(int phase) {
        return phase == 1 ? P1_INTRO_DURATION : GUARDIAN_APPROACH_DURATION;
    }

    /** Retorna a duracao do foco na saida da fase (portal). */
    public static double phaseExitDuration(int phase) {
        return PHASE_EXIT_DURATION;
    }
}
