package br.apsu.tutorial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — TutorialController (US-020)")
public class TutorialControllerTest {

    private TutorialController controller;

    @BeforeEach
    void setUp() {
        controller = new TutorialController();
    }

    @Test
    @DisplayName("Início do tutorial no passo SWIM_DOWN e avanço sequencial")
    void testSequentialTutorialProgression() {
        assertTrue(controller.isActive());
        assertEquals(TutorialStep.SWIM_DOWN, controller.getCurrentStep());

        // 1. Descer contra empuxo
        controller.update(0.1, 100.0, 300.0, false, false, false);
        assertEquals(TutorialStep.SWIM_DOWN, controller.getCurrentStep(), "Sem comando para baixo nao avanca");

        controller.update(0.1, 100.0, 360.0, true, false, false);
        assertEquals(TutorialStep.DODGE_ENEMY, controller.getCurrentStep(), "Nadou para baixo: avanca para DODGE_ENEMY");

        // 2. Desviar do inimigo
        controller.update(1.0, 1000.0, 360.0, false, true, false);
        assertEquals(TutorialStep.DODGE_ENEMY, controller.getCurrentStep(), "Ainda nao passou o inimigo");

        controller.update(1.0, 1650.0, 360.0, false, false, false);
        assertEquals(TutorialStep.READ_GPS, controller.getCurrentStep(), "Ultrapassou o inimigo: avanca para READ_GPS");

        // 3. Ler o GPS / bússola
        controller.update(1.0, 2300.0, 360.0, false, false, false);
        assertEquals(TutorialStep.TALK_GUARDIAN, controller.getCurrentStep(), "Avancou navegando: avanca para TALK_GUARDIAN");

        // 4. Conversar com o guardião
        controller.update(1.0, 2500.0, 360.0, false, false, true);
        assertEquals(TutorialStep.COMPLETED, controller.getCurrentStep(), "Conversou com guardiao: concluido");
        assertFalse(controller.isActive());
    }

    @Test
    @DisplayName("Dica urgente reforçada após 15 segundos de inação")
    void testUrgentHintAfterTimeout() {
        assertFalse(controller.isHintUrgent());
        controller.update(14.9, 100.0, 300.0, false, false, false);
        assertFalse(controller.isHintUrgent());

        controller.update(0.2, 100.0, 300.0, false, false, false);
        assertTrue(controller.isHintUrgent(), "Apos 15s a dica deve ser reforcada");
        assertTrue(controller.getCurrentInstruction().startsWith("[ATENÇÃO]"));
    }

    @Test
    @DisplayName("Proteção de vida a 50% durante o tutorial e repetição do passo de desvio")
    void testDamageCapAndStepRepeat() {
        double maxHp = 10.0;
        double currentHp = 8.0;

        // Dano normal acima de 50%
        double hpAfter1 = controller.filterHeroDamage(currentHp, maxHp, 2.0);
        assertEquals(6.0, hpAfter1, 0.001);

        // Dano letal durante o tutorial nunca deixa cair abaixo de 50% (5.0)
        double hpAfterLethal = controller.filterHeroDamage(currentHp, maxHp, 7.0);
        assertEquals(5.0, hpAfterLethal, 0.001, "Vida nunca deve cair abaixo de 50% durante o tutorial");

        // Se sofre dano no passo DODGE_ENEMY, reinicia o cronometro do passo
        controller.update(0.1, 100.0, 450.0, true, false, false); // avanca para DODGE_ENEMY
        assertEquals(TutorialStep.DODGE_ENEMY, controller.getCurrentStep());
        controller.update(10.0, 800.0, 450.0, false, false, false);
        assertEquals(10.0, controller.getStepTimerSeconds(), 0.001);

        controller.filterHeroDamage(currentHp, maxHp, 1.0);
        assertEquals(0.0, controller.getStepTimerSeconds(), 0.001, "Timer reiniciado para repetir passo");
    }

    @Test
    @DisplayName("Pular tutorial com tecla Esc")
    void testSkipTutorial() {
        assertTrue(controller.isActive());
        controller.skip();
        assertFalse(controller.isActive());
        assertTrue(controller.isSkipped());
        assertEquals(TutorialStep.COMPLETED, controller.getCurrentStep());
        assertNull(controller.getCurrentInstruction());
    }

    @Test
    @DisplayName("Reinício da partida restaura o tutorial do primeiro passo")
    void testResetRestoresTutorial() {
        controller.skip();
        assertFalse(controller.isActive());

        controller.reset();
        assertTrue(controller.isActive());
        assertEquals(TutorialStep.SWIM_DOWN, controller.getCurrentStep());
        assertFalse(controller.isSkipped());
    }
}
