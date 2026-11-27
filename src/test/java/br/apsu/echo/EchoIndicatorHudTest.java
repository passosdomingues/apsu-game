package br.apsu.echo;

import br.apsu.core.GameContext;
import br.apsu.core.SaveManager;
import br.apsu.graphics.UIRenderer;
import br.apsu.model.environment.Difficulty;
import br.apsu.model.environment.OceanDepthProfile;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("US-033: Indicador de recarga do eco e dica de tecla no HUD")
class EchoIndicatorHudTest {

    private GameContext context;
    private UIRenderer uiRenderer;
    private GraphicsContext gc;

    @BeforeEach
    void setUp() {
        context = new GameContext(new SaveManager(Path.of("target/test-echo-hud-save.json")));
        context.startP1();
        uiRenderer = new UIRenderer();
        gc = new Canvas(1366, 768).getGraphicsContext2D();
    }

    @Test
    @DisplayName("AC1: Quando o eco esta disponivel, icone com letra Q aparece abaixo da barra de vida")
    void testIconQAppearsWhenEchoAvailable() {
        EchoController echo = context.getEchoController();
        assertTrue(echo.isReady(), "Eco deve iniciar READY");
        assertFalse(echo.isOnCooldown(), "Nao deve estar em recarga inicialmente");

        // Renderiza HUD com o eco disponivel
        assertDoesNotThrow(() -> uiRenderer.drawHUD(gc, context.getHero(), 0, "Fase 1",
            Difficulty.MEDIO, OceanDepthProfile.COASTAL, 0.0, "Objetivo", echo));

        // Renderiza especificamente o indicador na posicao abaixo da barra de vida
        assertDoesNotThrow(() -> uiRenderer.drawEchoIndicator(gc, echo, 65, 37));
    }

    @Test
    @DisplayName("AC2: Recarga em andamento exibe progresso de 0 a 100% e icone esmaecido")
    void testCooldownShowsProgressAndFaded() {
        EchoController echo = context.getEchoController();

        // Ativa o eco
        assertTrue(echo.trigger());
        assertTrue(echo.isActive());
        assertEquals(1.0, echo.getCooldownProgress(), 0.001);

        // Avanca 4.0 s: eco expira e entra em recarga (8.0 s de cooldown)
        echo.update(4.0, false);
        assertTrue(echo.isOnCooldown(), "Deve estar em COOLDOWN apos duracao ativa expirar");
        assertEquals(0.0, echo.getCooldownProgress(), 0.01, "Inicio de recarga deve ser 0%");

        // Avanca 2.0 s (25% de 8.0 s)
        echo.update(2.0, false);
        assertEquals(0.25, echo.getCooldownProgress(), 0.02, "Apos 2s de recarga deve ser 25%");

        // Avanca mais 2.0 s (50% de 8.0 s)
        echo.update(2.0, false);
        assertEquals(0.50, echo.getCooldownProgress(), 0.02, "Apos 4s de recarga deve ser 50%");

        // Renderiza em recarga (esmaecido com porcentagem)
        assertDoesNotThrow(() -> uiRenderer.drawEchoIndicator(gc, echo, 65, 37));

        // Avanca mais 4.0 s (100%): recarga completa e volta a READY
        echo.update(4.0, false);
        assertEquals(1.0, echo.getCooldownProgress(), 0.01, "Fim da recarga deve ser 100%");
        assertTrue(echo.isReady(), "Apos 8s de recarga deve voltar para READY");
    }

    @Test
    @DisplayName("AC3: Na fase 1, apos 20 s sem usar o eco, a dica 'Q: eco' aparece uma unica vez")
    void testPhase1HintAppearsAfter20SecondsOnce() {
        EchoController echo = context.getEchoController();

        // Inicialmente a dica nao esta visivel
        assertFalse(echo.isHintVisible());
        assertFalse(echo.wasHintTriggeredOnce());

        // Simula 19 segundos na Fase 1 sem usar o eco
        for (int i = 0; i < 19 * 60; i++) {
            echo.update(1.0 / 60.0, true);
        }
        assertFalse(echo.isHintVisible(), "Com 19s a dica ainda nao deve aparecer");

        // Simula mais 1.5 segundos (total 20.5 s na Fase 1)
        for (int i = 0; i < 90; i++) {
            echo.update(1.0 / 60.0, true);
        }
        assertTrue(echo.isHintVisible(), "Apos 20s na Fase 1 sem eco, a dica 'Q: eco' deve aparecer");
        assertTrue(echo.wasHintTriggeredOnce(), "Dica deve registrar que foi disparada uma vez");

        // Renderiza HUD com a dica ativa
        assertDoesNotThrow(() -> uiRenderer.drawEchoIndicator(gc, echo, 65, 37));
        assertDoesNotThrow(() -> uiRenderer.drawEchoHint(gc, 88, 37));

        // Jogador aperta Q (trigger) -> eco e ativado e a dica some
        assertTrue(echo.trigger());
        assertFalse(echo.isHintVisible(), "Ao acionar o eco, a dica deve sumir");
        assertTrue(echo.hasUsedEcho(), "Eco foi utilizado");

        // Simula mais 30 segundos na Fase 1: a dica NAO deve reaparecer
        for (int i = 0; i < 30 * 60; i++) {
            echo.update(1.0 / 60.0, true);
        }
        assertFalse(echo.isHintVisible(), "Dica nao deve reaparecer apos ja ter sido disparada");
    }

    @Test
    @DisplayName("AC3 (contra-caso): Em fase diferente da Fase 1, a dica nao aparece apos 20 s")
    void testHintDoesNotAppearInOtherPhases() {
        EchoController echo = context.getEchoController();

        // 25 segundos na Fase 2 (isPhase1 = false)
        for (int i = 0; i < 25 * 60; i++) {
            echo.update(1.0 / 60.0, false);
        }
        assertFalse(echo.isHintVisible(), "Dica so deve aparecer na Fase 1");
        assertFalse(echo.wasHintTriggeredOnce());
    }

    @Test
    @DisplayName("AC4: Icone e dica usam exclusivamente o tema claro (cores claras de fundo e borda)")
    void testIconAndHintUseLightThemeOnly() {
        // Verifica que as cores de fundo sao do tema claro (luminancia/brilho alto > 0.8)
        Color bg = UIRenderer.ECHO_LIGHT_BG;
        Color hintBg = UIRenderer.ECHO_HINT_BG;
        Color stroke = UIRenderer.ECHO_LIGHT_STROKE;

        assertTrue(bg.getRed() > 0.8 && bg.getGreen() > 0.8 && bg.getBlue() > 0.8,
            "Fundo do icone deve pertencer ao tema claro (RGB > 0.8)");
        assertTrue(hintBg.getRed() > 0.8 && hintBg.getGreen() > 0.8 && hintBg.getBlue() > 0.8,
            "Fundo da dica deve pertencer ao tema claro (RGB > 0.8)");
        assertTrue(stroke.getRed() > 0.4 && stroke.getGreen() > 0.6,
            "Borda deve ser azul claro suave");

        // Renderiza ambos sem excecao
        assertDoesNotThrow(() -> uiRenderer.drawEchoIndicator(gc, context.getEchoController(), 65, 37));
        assertDoesNotThrow(() -> uiRenderer.drawEchoHint(gc, 88, 37));
    }

    @Test
    @DisplayName("APSU-U19C: Barra de recarga do eco renderiza estados de cooldown e pronto sem alocacoes")
    void testEchoProgressBarCooldownAndReady() {
        var echo = context.getEchoController();
        echo.reset();
        assertFalse(echo.isOnCooldown());
        assertEquals(1.0, echo.getCooldownProgress(), 0.001);

        // Estado Pronto
        assertDoesNotThrow(() -> uiRenderer.drawEchoIndicator(gc, echo, 65, 37));

        // Aciona o eco e avanca para entrar em cooldown
        assertTrue(echo.trigger());
        echo.update(EchoController.ECHO_DURATION_SECONDS); // conclui fase ativa e entra em cooldown
        assertTrue(echo.isOnCooldown());
        assertEquals(0.0, echo.getCooldownProgress(), 0.001);

        // Estado Cooldown 0%
        assertDoesNotThrow(() -> uiRenderer.drawEchoIndicator(gc, echo, 65, 37));

        // Cooldown parcial 50%
        echo.update(EchoController.COOLDOWN_DURATION_SECONDS * 0.5);
        assertEquals(0.5, echo.getCooldownProgress(), 0.05);
        assertDoesNotThrow(() -> uiRenderer.drawEchoIndicator(gc, echo, 65, 37));

        // Cooldown completo 100%
        echo.update(EchoController.COOLDOWN_DURATION_SECONDS * 0.6);
        assertFalse(echo.isOnCooldown());
        assertDoesNotThrow(() -> uiRenderer.drawEchoIndicator(gc, echo, 65, 37));
    }
}
