package br.apsu.core;

import br.apsu.model.hero.HeroEntity;
import br.apsu.model.hero.HeroType;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade - HeroController")
public class HeroControllerTest {

    private HeroController controller;
    private InputManager inputManager;
    private HeroEntity hero;

    @BeforeEach
    void setUp() {
        controller = new HeroController();
        inputManager = new InputManager();
        hero = new HeroEntity(HeroType.GUARDIAO, 5.0);
    }

    @Test
    @DisplayName("HeroController move heroi para a direita quando seta direita esta pressionada")
    void testStepMovesRight() {
        double startX = hero.getX();
        inputManager.registerKeyPress(KeyCode.RIGHT);

        controller.step(hero, inputManager, 30, 700, 0, 1.0, 0, 0, 1_000_000L, 1.0 / 60.0);

        assertTrue(hero.getX() > startX, "Heroi deve ter se movido para a direita");
        assertTrue(hero.isFacingRight());
    }

    @Test
    @DisplayName("HeroController neutraliza movimento quando teclas opostas estao pressionadas")
    void testStepOppositeKeysNeutral() {
        inputManager.registerKeyPress(KeyCode.LEFT);
        inputManager.registerKeyPress(KeyCode.RIGHT);

        controller.step(hero, inputManager, 30, 700, 0, 1.0, 0, 0, 1_000_000L, 1.0 / 60.0);

        assertEquals(0.0, hero.getVx(), 0.0001, "Velocidade horizontal deve ser zero com teclas opostas");
    }

    @Test
    @DisplayName("HeroController lida de forma segura com valores nulos")
    void testStepNullSafe() {
        assertDoesNotThrow(() -> controller.step(null, inputManager, 30, 700, 0, 1.0, 0, 0, 0L, 1.0 / 60.0));
        assertDoesNotThrow(() -> controller.step(hero, (InputManager) null, 30, 700, 0, 1.0, 0, 0, 0L, 1.0 / 60.0));
    }
}
