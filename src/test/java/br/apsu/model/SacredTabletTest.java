package br.apsu.model;

import br.apsu.core.GameContext;
import br.apsu.core.SaveManager;
import br.apsu.core.events.EventBus;
import br.apsu.model.environment.SacredTabletEntity;
import br.apsu.model.guardian.GuardianEntity;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("APSU-G01: Tabuleta Sagrada Oculta e Revelação por Ecolocalização")
class SacredTabletTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Ja inicializado
        }
    }

    @Test
    @DisplayName("AC1: Tabuleta inicia oculta e conversa com Enki não entrega a tabuleta de bandeja")
    void tabletStartsHiddenAndGuardianDoesNotAwardDirectly() {
        EventBus bus = new EventBus();
        GameContext context = new GameContext(new SaveManager(Path.of("target/test-tablet-save.json")), bus);
        context.startP1();

        SacredTabletEntity tablet = context.getSacredTablet();
        assertNotNull(tablet, "Tabuleta da Fase 1 deve existir");
        assertTrue(tablet.isHidden(), "Tabuleta deve iniciar oculta (HIDDEN)");
        assertFalse(tablet.isRevealed(), "Tabuleta não deve estar revelada inicialmente");
        assertEquals(0, context.getTabletsCollected());

        // Conduz conversa com o Guardião
        GuardianEntity g = context.getGuardians().get(0);
        context.getHero().setX(g.getWorldX());
        context.getHero().setY(g.getWorldY());
        context.getInputManager().registerKeyPress(KeyCode.E);
        context.update(1_000_000_000L);

        assertTrue(context.isOverlayActive(), "Diálogo deve abrir");
        for (int i = 0; i < 64 && context.isOverlayActive(); i++) {
            context.advanceOverlay();
        }
        assertFalse(context.isOverlayActive());

        // Pós-diálogo: Enki curou e deu a pista, mas NÃO entregou a tabuleta de bandeja!
        assertEquals(0, context.getTabletsCollected(),
            "A tabuleta NÃO deve ser entregue na conversa; deve ser buscada no cenário");
        assertTrue(tablet.isHidden(), "Tabuleta continua oculta até ser atingida pela ecolocalização");
    }

    @Test
    @DisplayName("AC2 & AC3: Ecolocalização (Q) expande onda que revela a tabuleta oculta ao alcançá-la")
    void echoWaveRevealsHiddenTabletOnHit() {
        EventBus bus = new EventBus();
        GameContext context = new GameContext(new SaveManager(Path.of("target/test-tablet-save.json")), bus);
        context.startP1();

        SacredTabletEntity tablet = context.getSacredTablet();
        assertNotNull(tablet);
        assertTrue(tablet.isHidden());

        // Move herói para perto da região da tabuleta (ex: a 250 unidades de distância)
        context.getHero().setX(tablet.getWorldX() - 250);
        context.getHero().setY(tablet.getWorldY());

        // Aciona ecolocalização (Q)
        context.onKey(KeyCode.Q);
        assertTrue(context.getEchoController().isActive(), "Ecolocalização deve estar ativa");

        // Simula avanço da onda sonora (0.5 s = 300 unidades de expansão, ultrapassando a distância de 250)
        context.getEchoWaveRenderer().update(context, 0.5);

        // A onda alcançou a tabuleta: estado transiciona para REVEALED
        assertTrue(tablet.isRevealed(), "Tabuleta deve ter sido revelada pelo impacto da onda");
        assertFalse(tablet.isHidden());
        assertFalse(tablet.isCollected());
    }

    @Test
    @DisplayName("AC4: Adapa nada até a tabuleta revelada, coleta o artefato e libera a saída")
    void heroCollectsRevealedTabletAndOpensPortal() {
        EventBus bus = new EventBus();
        GameContext context = new GameContext(new SaveManager(Path.of("target/test-tablet-save.json")), bus);
        context.startP1();

        SacredTabletEntity tablet = context.getSacredTablet();
        assertNotNull(tablet);
        assertTrue(tablet.reveal(), "Força revelação da tabuleta para teste de coleta");

        assertEquals(0, context.getTabletsCollected());

        // Posiciona herói sobre a tabuleta revelada
        context.getHero().setX(tablet.getWorldX());
        context.getHero().setY(tablet.getWorldY());

        // Atualiza ciclo de física e interação
        context.update(1_000_000_000L);

        // Tabuleta coletada
        assertTrue(tablet.isCollected(), "Tabuleta deve estar no estado COLLECTED");
        assertEquals(1, context.getTabletsCollected(), "Contador de tabuletas deve ter incrementado");

        // Move até o portal (x = 3850)
        context.getHero().setX(3850);
        context.update(2_000_000_000L);

        // Transiciona com sucesso para a Fase 2 porque a tabuleta foi coletada
        assertEquals(2, context.getCurrentPhase(), "Deve ter avançado para a Fase 2");
    }

    @Test
    @DisplayName("AC5: Tentar sair sem coletar a tabuleta bloqueia o portal com aviso contextual")
    void exitWithoutCollectingTabletIsBlocked() {
        EventBus bus = new EventBus();
        GameContext context = new GameContext(new SaveManager(Path.of("target/test-tablet-save.json")), bus);
        context.startP1();

        assertEquals(0, context.getTabletsCollected());

        // Move até o portal sem coletar a tabuleta
        context.getHero().setX(3850);
        context.update(1_000_000_000L);

        assertEquals(1, context.getCurrentPhase(), "Não deve avançar para a fase 2");
        assertTrue(context.getAlertMessage().contains("Portal selado"),
            "Deve exibir alerta de portal selado orientando o uso da Ecolocalização");
    }
}
