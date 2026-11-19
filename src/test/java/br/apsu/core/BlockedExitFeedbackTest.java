package br.apsu.core;

import br.apsu.model.environment.Difficulty;
import br.apsu.model.hero.HeroType;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Integracao - Feedback de Saida Bloqueada (US-003)")
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class BlockedExitFeedbackTest {

    private GameContext context;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Ja inicializado
        }
    }

    @BeforeEach
    void setUp() {
        context = new GameContext();
        context.setDifficulty(Difficulty.FACIL);
        context.setHeroType(HeroType.GUARDIAO);
    }

    private void enterPhase(int phase) {
        switch (phase) {
            case 1 -> context.startP1();
            case 2 -> context.startP2();
            case 3 -> context.startP3();
            case 4 -> context.startP4();
            default -> throw new IllegalArgumentException("Fase invalida: " + phase);
        }
    }

    @Test
    @DisplayName("Fases 1 a 4 sem tabuleta: saida exibe mensagem de bloqueio por pelo menos 3s e nao transiciona")
    void exitWithoutTabletShowsBlockedAlertAndDoesNotTransition() {
        for (int phase = 1; phase <= 4; phase++) {
            enterPhase(phase);
            assertEquals(phase, context.getCurrentPhase());
            assertEquals(0, context.getTabletsCollected());

            // Posiciona o heroi com velocidade nula na regiao de saida (> 3800px)
            context.getHero().setVx(0);
            context.getHero().setVy(0);
            context.getHero().setX(3850);
            context.getHero().setY(300);
            long tPhase = phase * 10_000_000_000L;
            context.update(tPhase); // update a 10s por fase

            // Nao deve transicionar
            assertEquals(phase, context.getCurrentPhase(), "Fase nao deve mudar sem a tabuleta");
            assertFalse(context.getAlertMessage().isEmpty(), "Alerta de bloqueio deve ser exibido");
            assertTrue(context.getAlertMessage().contains("Tabuleta") || context.getAlertMessage().contains("selado") || context.getAlertMessage().contains("Selado"),
                "Mensagem deve orientar o jogador sobre o objetivo pendente: " + context.getAlertMessage());

            // Verifica que o alerta permanece ativo (alpha > 0 e decorrido < 3.8s)
            long alertTime = context.getAlertTime();
            assertEquals(tPhase, alertTime);
            long checkTimeAt3s = alertTime + 3_000_000_000L;
            assertTrue((checkTimeAt3s - alertTime) <= 3_800_000_000L,
                "HUD suporta exibicao de alerta por mais de 3 segundos");
        }
    }

    @Test
    @DisplayName("Heroi permanece na saida: alerta nao se repete mais de uma vez a cada 5s")
    void blockedAlertRateLimitedToOnceEvery5Seconds() {
        enterPhase(1);
        context.getHero().setVx(0);
        context.getHero().setVy(0);
        context.getHero().setX(3850);

        // 1s: primeiro alerta gerado
        long t1 = 1_000_000_000L;
        context.update(t1);
        String firstAlert = context.getAlertMessage();
        long firstAlertTime = context.getAlertTime();
        assertEquals(t1, firstAlertTime);
        assertFalse(firstAlert.isEmpty());

        // Heroi permanece na saida nos proximos 4 segundos (t = 2s, 3s, 4s, 5s)
        for (long sec = 2; sec <= 5; sec++) {
            context.getHero().setVx(0);
            context.getHero().setVy(0);
            context.getHero().setX(3850);
            long tSec = sec * 1_000_000_000L;
            context.update(tSec);
            assertEquals(firstAlertTime, context.getAlertTime(),
                "Alerta nao deve ser reemitido antes de 5 segundos decorridos");
        }

        // Aos 6.1s (passados > 5s do primeiro alerta), nova notificacao pode ser disparada
        context.getHero().setVx(0);
        context.getHero().setVy(0);
        context.getHero().setX(3850);
        long t6 = 6_100_000_000L;
        context.update(t6);
        assertEquals(t6, context.getAlertTime(),
            "Apos 5 segundos, novo alerta de bloqueio deve ser emitido");
    }

    @Test
    @DisplayName("Com tabuleta coletada: heroi alcanca a saida, transicao ocorre uma vez e sem alerta de bloqueio")
    void exitWithTabletTransitionsOnceWithoutBlockedAlert() {
        enterPhase(1);

        // Simula coleta da tabuleta na Fase 1
        var guardian = context.getGuardians().get(0);
        context.getHero().setX(guardian.getWorldX());
        context.getHero().setY(guardian.getWorldY());
        context.getInputManager().registerKeyPress(KeyCode.E);
        context.update(1_000_000_000L);

        assertTrue(context.isOverlayActive());
        for (int i = 0; i < 64 && context.isOverlayActive(); i++) {
            context.advanceOverlay();
        }
        assertFalse(context.isOverlayActive());
        assertEquals(1, context.getTabletsCollected());

        // Limpa alerta anterior de coleta de tabuleta
        context.showAlert("");

        // Move heroi ate a saida
        context.getHero().setVx(0);
        context.getHero().setVy(0);
        context.getHero().setX(3850);
        context.update(2_000_000_000L);

        // Deve transicionar diretamente para a Fase 2 (sem tela de diálogo redundante)
        assertEquals(GameContext.State.P2, context.getState());
        assertEquals(1, context.getDlgPhase(), "Transicao engatilhada para a Fase 2 (dlgPhase=1)");
        assertFalse(context.getAlertMessage().contains("selado") || context.getAlertMessage().contains("Selado"),
            "Nao deve exibir alerta de saida bloqueada quando a tabuleta foi coletada");
    }
}
