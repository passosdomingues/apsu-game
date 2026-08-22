package br.apsu.model;

import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.enemy.EnemyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — EnemyEntity")
public class EnemyEntityTest {

    @Test
    @DisplayName("Inicialização e movimentação senoidal de inimigos")
    void testEnemyCreationAndSineWobble() {
        EnemyEntity enemy = new EnemyEntity(EnemyType.PEIXE, 1000, 400, 1.5, 50);

        assertEquals(EnemyType.PEIXE, enemy.getType());
        assertEquals(1000, enemy.getWorldX());
        assertEquals(400, enemy.getBaseY());
        assertTrue(enemy.isAlive());

        double initialY = enemy.getCurrentY();
        enemy.update(1.0, 1.0);
        assertNotEquals(initialY, enemy.getCurrentY(), "A posição Y deve oscilar segundo a função seno");
    }

    @Test
    @DisplayName("Dimensões dos tipos de inimigos")
    void testEnemyTypeDimensions() {
        assertEquals(100, EnemyType.PEIXE.getWidth());
        assertEquals(142, EnemyType.PEIXE.getHeight());

        // B2-FIX: enguia ajustada para escala proporcional coerente
        assertEquals(160, EnemyType.ENGUIA.getWidth());
        assertEquals(85, EnemyType.ENGUIA.getHeight());

        assertEquals(80, EnemyType.MEDUSA.getWidth());
        assertEquals(135, EnemyType.MEDUSA.getHeight());

        assertEquals(130, EnemyType.CARANGUEJO.getWidth());
        assertEquals(90, EnemyType.CARANGUEJO.getHeight());

        // Novos inimigos Sprint 5
        assertEquals(200, EnemyType.ARRAIAO.getWidth());
        assertEquals(110, EnemyType.ARRAIAO.getHeight());

        assertEquals(180, EnemyType.LEVIATA.getWidth());
        assertEquals(160, EnemyType.LEVIATA.getHeight());
    }
}
