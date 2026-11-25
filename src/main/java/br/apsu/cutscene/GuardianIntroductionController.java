package br.apsu.cutscene;

import br.apsu.model.guardian.GuardianEntity;

import java.util.Arrays;

/** Creates one short camera introduction when the player first approaches a phase guardian. */
public final class GuardianIntroductionController {
    private static final double INTRO_DISTANCE = 800.0;
    private final boolean[] introducedByPhase = new boolean[5];

    /** Returns a cutscene request once the player is within 800 world units of a phase 2-4 guardian. */
    public CutsceneRequest check(int phase, double heroCenterX, double heroCenterY, GuardianEntity guardian) {
        if (phase < 2 || phase > 4 || guardian == null || introducedByPhase[phase]) return null;

        double dx = heroCenterX - (guardian.getWorldX() + GuardianEntity.GW / 2.0);
        double dy = heroCenterY - (guardian.getWorldY() + GuardianEntity.GH / 2.0);
        if (dx * dx + dy * dy > INTRO_DISTANCE * INTRO_DISTANCE) return null;

        introducedByPhase[phase] = true;
        return new CutsceneRequest(
            guardian.getWorldX() + GuardianEntity.GW / 2.0,
            PhaseCutsceneConfig.guardianIntroDuration(phase),
            guardian.getName()
        );
    }

    /** Starts a fresh gameplay session. */
    public void reset() {
        Arrays.fill(introducedByPhase, false);
    }
}
