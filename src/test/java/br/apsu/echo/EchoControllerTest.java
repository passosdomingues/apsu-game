package br.apsu.echo;

import br.apsu.core.GameContext;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — Habilidade de Ecolocalizacao Temporaria (US-030)")
public class EchoControllerTest {

    private EchoController controller;

    @BeforeEach
    void setUp() {
        controller = new EchoController();
    }

    @Test
    @DisplayName("AC1: Pressionar Q fora de recarga ativa o eco por 4.0 s")
    void testActivationDuration() {
        assertTrue(controller.isReady(), "Inicialmente deve estar em estado READY");
        assertEquals(EchoController.State.READY, controller.getState());

        boolean triggered = controller.trigger();
        assertTrue(triggered, "Trigger deve ser aceito quando READY");
        assertTrue(controller.isActive(), "Deve transicionar para ACTIVE");
        assertEquals(4.0, controller.getActiveTimer(), 0.001, "Duracao inicial do eco deve ser de 4.0 s");

        // Simula 2.0 s de execucao
        controller.update(2.0);
        assertTrue(controller.isActive());
        assertEquals(2.0, controller.getActiveTimer(), 0.001);
    }

    @Test
    @DisplayName("AC2: Ao expirar os 4.0 s, desativa e inicia recarga de 8.0 s")
    void testExpirationAndCooldownTransition() {
        controller.trigger();
        assertTrue(controller.isActive());

        // Atualiza por 4.0 s completos
        controller.update(4.0);
        assertFalse(controller.isActive(), "Nao deve mais estar ativo");
        assertTrue(controller.isOnCooldown(), "Deve estar em estado de COOLDOWN");
        assertEquals(8.0, controller.getCooldownTimer(), 0.001, "Tempo de recarga deve ser 8.0 s");

        // Atualiza mais 4.0 s (metade da recarga)
        controller.update(4.0);
        assertTrue(controller.isOnCooldown());
        assertEquals(4.0, controller.getCooldownTimer(), 0.001);
        assertEquals(0.5, controller.getCooldownProgress(), 0.01);

        // Conclui a recarga com mais 4.0 s
        controller.update(4.0);
        assertTrue(controller.isReady(), "Deve retornar ao estado READY apos 8.0 s");
        assertFalse(controller.isOnCooldown());
        assertEquals(1.0, controller.getCooldownProgress(), 0.01);
    }

    @Test
    @DisplayName("AC3: Pressionar Q durante recarga nao ativa e solicita destaque de recarga")
    void testTriggerDuringCooldownHighlightsIndicator() {
        controller.trigger();
        controller.update(4.0); // Entra em recarga
        assertTrue(controller.isOnCooldown());
        assertFalse(controller.isCooldownHighlightRequested());

        // Pressiona Q durante a recarga
        boolean triggered = controller.trigger();
        assertFalse(triggered, "Nao deve ativar durante recarga");
        assertTrue(controller.isOnCooldown());
        assertTrue(controller.isCooldownHighlightRequested(), "Deve solicitar destaque no indicador de recarga");

        controller.clearCooldownHighlight();
        assertFalse(controller.isCooldownHighlightRequested());
    }

    @Test
    @DisplayName("AC4: Nova partida inicia com eco disponivel imediatamente na Fase 1")
    void testAvailableAtGameStart() {
        GameContext context = new GameContext();
        context.startP1();

        assertNotNull(context.getEchoController(), "GameContext deve possuir EchoController");
        assertTrue(context.getEchoController().isReady(), "Na Fase 1 o eco ja deve estar disponivel");
        assertFalse(context.getEchoController().isActive());
        assertFalse(context.getEchoController().isOnCooldown());

        // Testa acionamento via onKey(KeyCode.Q)
        context.onKey(KeyCode.Q);
        assertTrue(context.getEchoController().isActive(), "onKey(Q) deve ativar o eco na Fase 1");
    }

    @Test
    @DisplayName("AC5: Jogo pausado ou em menu nao consome timers do eco")
    void testPauseAndStatePreservation() {
        GameContext context = new GameContext();
        context.startP1();
        context.onKey(KeyCode.Q);
        assertTrue(context.getEchoController().isActive());

        // Atualiza por 60 frames na fase 1 (1.0 s)
        for (int i = 0; i < 60; i++) {
            context.update(1_000_000_000L + i * 16_666_666L);
        }
        assertEquals(3.0, context.getEchoController().getActiveTimer(), 0.05);

        // Muda para MENU (pausa/menu)
        context.setState(GameContext.State.MENU);
        double timerAtMenu = context.getEchoController().getActiveTimer();

        // Roda atualizacoes enquanto no menu
        for (int i = 0; i < 60; i++) {
            context.update(2_000_000_000L + i * 16_666_666L);
        }
        assertEquals(timerAtMenu, context.getEchoController().getActiveTimer(), 0.001,
            "Timer nao deve avancar enquanto o jogo nao estiver em estado de fase ativa");

        // Retorna para a Fase 1
        context.setState(GameContext.State.P1);
        for (int i = 0; i < 60; i++) {
            context.update(3_000_000_000L + i * 16_666_666L);
        }
        assertEquals(timerAtMenu - 1.0, context.getEchoController().getActiveTimer(), 0.05,
            "Timer deve retomar o avanco normalmente ao sair da pausa");
    }
}
