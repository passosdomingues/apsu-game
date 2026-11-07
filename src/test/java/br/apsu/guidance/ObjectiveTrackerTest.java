package br.apsu.guidance;

import br.apsu.core.GameContext;
import br.apsu.core.SaveManager;
import br.apsu.core.events.EventBus;
import br.apsu.model.guardian.GuardianEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade - ObjectiveTracker (US-010)")
class ObjectiveTrackerTest {

    private GameContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        context = new GameContext(new SaveManager(tempDir.resolve("save.json")), new EventBus());
        context.reset();
    }

    @Test
    @DisplayName("Menu e diálogo não possuem objetivo ativo")
    void menuHasNoObjective() {
        assertNull(ObjectiveTracker.resolveCurrentObjective(context));
    }

    @Test
    @DisplayName("Fase 1 antes da conversa aponta para o Guardião (AC1)")
    void phase1BeforeConversationPointsToGuardian() {
        context.startP1();
        ObjectiveTracker.Objective obj = ObjectiveTracker.resolveCurrentObjective(context);

        assertNotNull(obj);
        GuardianEntity g = context.getGuardians().get(0);
        assertEquals(g.getWorldX() + GuardianEntity.GW / 2.0, obj.worldX(), 1.0);
        assertEquals(g.getWorldY() + GuardianEntity.GH / 2.0, obj.worldY(), 1.0);
    }

    @Test
    @DisplayName("Após conversa e tabuleta coletada, aponta para a saída da fase (AC3)")
    void phase1AfterTabletPointsToExit() {
        context.startP1();
        GuardianEntity g = context.getGuardians().get(0);
        context.getHero().setX(g.getWorldX());
        context.getHero().setY(g.getWorldY());
        context.getInputManager().registerKeyPress(javafx.scene.input.KeyCode.E);
        context.update(1_000_000_000L);
        context.onKey(javafx.scene.input.KeyCode.ESCAPE); // fecha overlay e coleta tabuleta

        assertEquals(1, context.getTabletsCollected(), "Tabuleta deve ter sido coletada na conversa");
        ObjectiveTracker.Objective obj = ObjectiveTracker.resolveCurrentObjective(context);
        assertNotNull(obj);
        assertEquals(3900.0, obj.worldX(), 1.0, "Deve apontar para o portal de saída a 3900 px");
    }

    @Test
    @DisplayName("Fase 5 aponta para o Kullullû")
    void phase5PointsToBoss() {
        context.startP5();
        ObjectiveTracker.Objective obj = ObjectiveTracker.resolveCurrentObjective(context);

        assertNotNull(obj);
        assertEquals("Derrotar Kullullu", obj.description());
        assertEquals(context.getBoss().getX(), obj.worldX(), 1.0);
    }
}
