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
        assertEquals("Fale com o guardião", obj.description());
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
        context.onKey(javafx.scene.input.KeyCode.ESCAPE); // fecha overlay
        context.revealAndCollectSacredTablet(); // resgata tabuleta revelada

        assertEquals(1, context.getTabletsCollected(), "Tabuleta deve ter sido coletada");
        ObjectiveTracker.Objective obj = ObjectiveTracker.resolveCurrentObjective(context);
        assertNotNull(obj);
        assertEquals("Siga para o portal", obj.description());
        assertEquals(3900.0, obj.worldX(), 1.0, "Deve apontar para o portal de saída a 3900 px");
    }

    @Test
    @DisplayName("Fase 2 com o poder das bolhas bloqueado orienta a abrir o baú (US-014 AC3)")
    void phase2LockedPowerPointsToChest() {
        context.startP2();
        assertFalse(context.getHero().hasBubblePower());
        assertFalse(context.isChestOpen());

        ObjectiveTracker.Objective obj = ObjectiveTracker.resolveCurrentObjective(context);
        assertNotNull(obj);
        assertEquals("Encontre o naufrágio e abra o baú", obj.description());
        assertEquals(context.getChestWX(), obj.worldX(), 1.0);
    }

    @Test
    @DisplayName("Fase 5 não possui seta de orientação no Boss Kullullû")
    void phase5HasNoObjectiveArrow() {
        context.startP5();
        ObjectiveTracker.Objective obj = ObjectiveTracker.resolveCurrentObjective(context);
        assertNull(obj, "Arena do boss não deve possuir seta de objetivo");
    }

    @Test
    @DisplayName("Fase 3 antes da tabuleta não possui seta de guardião (apenas na fase 1)")
    void phase3BeforeTabletHasNoGuardianArrow() {
        context.startP3();
        ObjectiveTracker.Objective obj = ObjectiveTracker.resolveCurrentObjective(context);
        assertNull(obj, "Fase 3 permite exploração livre sem seta no guardião");
    }
}
