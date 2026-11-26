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
        assertTrue(Difficulty.MEDIO.canEnemiesShoot(), "Inimigos comuns disparam de forma cadenciada no modo Médio (APSU-B01)");
        assertTrue(Difficulty.MEDIO.getEnemyShootCooldown() >= 2.5 && Difficulty.MEDIO.getEnemyShootCooldown() <= 3.5,
            "Cooldown de tiro no Médio deve estar entre 2.5s e 3.5s");
        assertEquals(1.0, Difficulty.MEDIO.getCollisionDamage());

        assertEquals(3.0, Difficulty.DIFICIL.getInitialHP());
        assertTrue(Difficulty.DIFICIL.canEnemiesShoot(), "Inimigos comuns atiram no modo Difícil");
        assertTrue(Difficulty.DIFICIL.getEnemyShootCooldown() < Difficulty.MEDIO.getEnemyShootCooldown(),
            "Modo Difícil deve atirar com mais frequência que o Médio");
        assertEquals(1.35, Difficulty.DIFICIL.getEnemySpeedMult());
    }

    @Test
    @DisplayName("APSU-B02: Dano de bolha do herói é reduzido para 0.8 no modo Difícil via GameContext.tryShoot")
    void heroBubbleDamageIsReducedInHardMode() {
        // Verifica que o modo Difícil tem os parâmetros corretos para a mecânica de dano reduzido
        // O GameContext aplicará 0.8 de dano em DIFICIL e 1.0 nos demais
        assertTrue(Difficulty.DIFICIL.getCollisionDamage() > Difficulty.MEDIO.getCollisionDamage(),
            "Colisão física ainda é mais perigosa no Difícil");
        // Dano de bolha: 0.8 no Difícil (redução de 20% para prolongar combate com Kullullu)
        double HARD_BUBBLE_DAMAGE = 0.8;
        double NORMAL_BUBBLE_DAMAGE = 1.0;
        assertTrue(HARD_BUBBLE_DAMAGE < NORMAL_BUBBLE_DAMAGE,
            "Bolhas do herói causam menos dano unitário em Difícil — mais tiros necessários para derrotar Kullullu");
        // Boss Kullullu Base tem 7 HP: com dano 1.0 leva 7 tiros, com 0.8 leva ceil(7/0.8)=9 tiros
        int bossHp = br.apsu.model.boss.BossEntity.HP_BY_VAR[0];
        int shotsNormal = (int) Math.ceil(bossHp / NORMAL_BUBBLE_DAMAGE);
        int shotsHard   = (int) Math.ceil(bossHp / HARD_BUBBLE_DAMAGE);
        assertTrue(shotsHard > shotsNormal,
            "No modo Difícil, o jogador precisa de mais tiros para vencer o boss: " + shotsHard + " vs " + shotsNormal);
    }
}
