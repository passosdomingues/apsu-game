package br.apsu.cutscene;

import br.apsu.core.events.GameEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhaseExitIntroductionControllerTest {
    @Test
    void createsOneExitFocusForEachTabletInPhasesOneThroughFour() {
        PhaseExitIntroductionController controller = new PhaseExitIntroductionController();

        for (int phase = 1; phase <= 4; phase++) {
            GameEvent event = controller.tabletCollected(phase);
            assertNotNull(event);
            assertEquals(GameEvent.Type.CUTSCENE_REQUESTED, event.type());
            assertEquals(3900.0, event.worldX(), 0.001);
            assertEquals(384.0, event.worldY(), 0.001);
            assertEquals(3900.0, ((CutsceneRequest) event.payload()).targetWorldX(), 0.001);
            assertEquals(PhaseCutsceneConfig.phaseExitDuration(phase), ((CutsceneRequest) event.payload()).holdDurationSeconds(), 0.001);
            assertEquals("Saida da Fase " + phase, ((CutsceneRequest) event.payload()).label());
            assertNull(controller.tabletCollected(phase), "A apresentação não deve repetir na fase " + phase);
        }

        assertNull(controller.tabletCollected(0));
        assertNull(controller.tabletCollected(5));
    }

    @Test
    void resetAllowsTheIntroductionInANewGame() {
        PhaseExitIntroductionController controller = new PhaseExitIntroductionController();
        assertNotNull(controller.tabletCollected(1));
        assertNull(controller.tabletCollected(1));

        controller.reset();

        assertNotNull(controller.tabletCollected(1));
    }
}
