package br.apsu.cutscene;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade - CutsceneController (US-011)")
class CutsceneControllerTest {

    @Test
    @DisplayName("Cutscene transiciona PAN_TO em ate 0.8s, mantém HOLD e retorna PAN_BACK ao herói (AC1)")
    void cutsceneFullCycleTiming() {
        CutsceneController controller = new CutsceneController();
        double heroX = 100.0;
        double targetX = 900.0;
        double holdDuration = 1.0;

        controller.enqueue(new CutsceneRequest(targetX, holdDuration, "Foco Guardião"), heroX);
        assertTrue(controller.isActive());
        assertEquals(CutsceneController.Phase.PAN_TO, controller.getPhase());

        // Simula 0.4s de PAN_TO
        double xMidPan = controller.update(heroX, 0.4);
        assertTrue(xMidPan > heroX && xMidPan < targetX);
        assertEquals(CutsceneController.Phase.PAN_TO, controller.getPhase());

        // Completa PAN_TO (mais 0.4s = 0.8s total)
        double xAtHold = controller.update(heroX, 0.4);
        assertEquals(CutsceneController.Phase.HOLD, controller.getPhase());
        assertEquals(targetX, xAtHold, 0.01);

        // Mantém HOLD por 1.0s
        double xInHold = controller.update(heroX, 1.0);
        assertEquals(CutsceneController.Phase.PAN_BACK, controller.getPhase());
        assertEquals(targetX, xInHold, 0.01);

        // Retorna em PAN_BACK (0.8s)
        double xReturned = controller.update(heroX, 0.8);
        assertFalse(controller.isActive());
        assertEquals(CutsceneController.Phase.IDLE, controller.getPhase());
        assertEquals(heroX, xReturned, 0.01);
    }

    @Test
    @DisplayName("Pulo de cutscene: tecla Espaço/Enter/Esc termina em até 0.3s (AC2)")
    void cutsceneSkipTiming() {
        CutsceneController controller = new CutsceneController();
        double heroX = 100.0;
        double targetX = 1500.0;

        controller.enqueue(new CutsceneRequest(targetX, 2.0), heroX);
        controller.update(heroX, 0.2); // durante PAN_TO

        // Jogador aperta pular
        controller.skip(heroX);
        assertEquals(CutsceneController.Phase.PAN_BACK, controller.getPhase());

        // Deve retornar ao herói em até 0.3s
        double pos = controller.update(heroX, 0.3);
        assertFalse(controller.isActive());
        assertEquals(CutsceneController.Phase.IDLE, controller.getPhase());
        assertEquals(heroX, pos, 0.01);
    }

    @Test
    @DisplayName("Fila: cutscenes disparadas no mesmo quadro são enfileiradas e executadas em ordem (AC4)")
    void cutsceneQueueExecutionOrder() {
        CutsceneController controller = new CutsceneController();
        double heroX = 100.0;

        CutsceneRequest req1 = new CutsceneRequest(800.0, 0.5, "Primeira");
        CutsceneRequest req2 = new CutsceneRequest(1800.0, 0.5, "Segunda");

        controller.enqueue(req1, heroX);
        controller.enqueue(req2, heroX);

        assertEquals(1, controller.getQueueSize(), "Segunda cutscene deve estar na fila");
        assertEquals("Primeira", controller.getActiveRequest().label());

        // Conclui a primeira via skip
        controller.skip(heroX);
        controller.update(heroX, 0.3);

        // A segunda inicia imediatamente
        assertTrue(controller.isActive());
        assertEquals("Segunda", controller.getActiveRequest().label());
        assertEquals(0, controller.getQueueSize());
    }

    @Test
    @DisplayName("Sem cutscene ativa, update devolve a posição do herói sem alterações")
    void idleUpdateReturnsHeroPosition() {
        CutsceneController controller = new CutsceneController();
        assertEquals(250.0, controller.update(250.0, 0.016));
        assertFalse(controller.isActive());
    }
}
