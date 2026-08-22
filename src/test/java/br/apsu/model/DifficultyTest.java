package br.apsu.model;

import br.apsu.model.environment.Difficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — Difficulty")
public class DifficultyTest {

    @Test
    @DisplayName("Validação dos 3 níveis de dificuldade (Fácil, Médio, Difícil)")
    void testDifficultyEnumValues() {
        assertEquals(5.0, Difficulty.FACIL.getInitialHP());
        assertFalse(Difficulty.FACIL.canEnemiesShoot());
        assertEquals(0.5, Difficulty.FACIL.getCollisionDamage());

        assertEquals(4.0, Difficulty.MEDIO.getInitialHP());
        assertFalse(Difficulty.MEDIO.canEnemiesShoot());
        assertEquals(1.0, Difficulty.MEDIO.getCollisionDamage());

        assertEquals(3.0, Difficulty.DIFICIL.getInitialHP());
        assertTrue(Difficulty.DIFICIL.canEnemiesShoot(), "Inimigos comuns atiram no modo Difícil");
        assertEquals(1.35, Difficulty.DIFICIL.getEnemySpeedMult());
    }
}
