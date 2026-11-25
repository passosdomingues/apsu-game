package br.apsu.cutscene;

import br.apsu.model.guardian.GuardianEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuardianIntroductionControllerTest {
    private final GuardianEntity guardian = new GuardianEntity(1, 1_000, 500);

    @Test
    void onlyIntroducesGuardiansInPhasesTwoThroughFour() {
        GuardianIntroductionController controller = new GuardianIntroductionController();
        double centerX = guardian.getWorldX() + GuardianEntity.GW / 2.0;
        double centerY = guardian.getWorldY() + GuardianEntity.GH / 2.0;

        assertNull(controller.check(1, centerX, centerY, guardian));
        assertNotNull(controller.check(2, centerX, centerY, guardian));
        assertNotNull(controller.check(3, centerX, centerY, guardian));
        assertNotNull(controller.check(4, centerX, centerY, guardian));
        assertNull(controller.check(5, centerX, centerY, guardian));
        assertNull(controller.check(2, centerX, centerY, null));
    }

    @Test
    void waitsUntilThePlayerIsWithin800UnitsAndTargetsGuardianCenter() {
        GuardianIntroductionController controller = new GuardianIntroductionController();
        double centerX = guardian.getWorldX() + GuardianEntity.GW / 2.0;
        double centerY = guardian.getWorldY() + GuardianEntity.GH / 2.0;

        assertNull(controller.check(2, centerX - 800.01, centerY, guardian));
        CutsceneRequest request = controller.check(2, centerX - 800.0, centerY, guardian);

        assertNotNull(request);
        assertEquals(centerX, request.targetWorldX(), 0.001);
        assertEquals(PhaseCutsceneConfig.guardianIntroDuration(2), request.holdDurationSeconds(), 0.001);
        assertEquals(guardian.getName(), request.label());
    }

    @Test
    void introductionIsOncePerPhaseAndResetStartsANewSession() {
        GuardianIntroductionController controller = new GuardianIntroductionController();
        double centerX = guardian.getWorldX() + GuardianEntity.GW / 2.0;
        double centerY = guardian.getWorldY() + GuardianEntity.GH / 2.0;

        assertNotNull(controller.check(2, centerX, centerY, guardian));
        assertNull(controller.check(2, centerX, centerY, guardian));
        assertNotNull(controller.check(3, centerX, centerY, guardian));

        controller.reset();
        assertNotNull(controller.check(2, centerX, centerY, guardian));
    }
}
