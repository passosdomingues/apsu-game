package br.apsu.cutscene;

import br.apsu.core.events.GameEvent;

import java.util.Arrays;

/** Schedules a single camera focus on the phase exit after a guardian grants its tablet. */
public final class PhaseExitIntroductionController {
    private static final double EXIT_WORLD_X = 3900.0;
    private static final double EXIT_WORLD_Y = 768.0 / 2.0;
    private final boolean[] introducedByPhase = new boolean[5];

    /** Creates a focus event once for phases 1 through 4. */
    public GameEvent tabletCollected(int phase) {
        if (phase < 1 || phase > 4 || introducedByPhase[phase]) return null;
        introducedByPhase[phase] = true;
        CutsceneRequest request = new CutsceneRequest(
            EXIT_WORLD_X,
            PhaseCutsceneConfig.phaseExitDuration(phase),
            "Saida da Fase " + phase
        );
        return new GameEvent(GameEvent.Type.CUTSCENE_REQUESTED, request, EXIT_WORLD_X, EXIT_WORLD_Y);
    }

    /** Starts a fresh gameplay session. */
    public void reset() {
        Arrays.fill(introducedByPhase, false);
    }
}
