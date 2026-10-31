package br.apsu.model;

import br.apsu.model.boss.BossEntity;
import br.apsu.model.hero.HeroEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — BossEntity")
public class BossEntityTest {

    @Test
    @DisplayName("Inicialização e danos nas 3 variantes do Boss Kullullû")
    void testBossVariantsAndDamage() {
        BossEntity bossBase = new BossEntity(0, 1000, 300);
        BossEntity bossGlacial = new BossEntity(1, 1000, 300);
        BossEntity bossToxico = new BossEntity(2, 1000, 300);

        assertEquals("Kullullû", bossBase.getName());
        assertEquals(5, bossBase.getHp());

        assertEquals("Kullullû Glacial", bossGlacial.getName());
        assertEquals(7, bossGlacial.getHp());

        assertEquals("Kullullû Tóxico", bossToxico.getName());
        assertEquals(6, bossToxico.getHp());

        bossBase.takeDamage(3);
        assertEquals(2, bossBase.getHp());
        assertFalse(bossBase.isDead());

        bossBase.takeDamage(2);
        assertEquals(0, bossBase.getHp());
        assertTrue(bossBase.isDead(), "Boss deve ser considerado morto com 0 HP");
    }

    @Test
    @DisplayName("Estágios da luta mudam em dois terços e um terço da vida com telegrafo")
    void changesCombatPhaseWithTelegraph() {
        BossEntity boss = new BossEntity(0, 1000, 300); // 5 HP
        assertEquals(BossEntity.CombatPhase.OBSIDIANA_FRIA, boss.getCombatPhase());

        boss.takeDamage(2); // 3/5: abaixo de 2/3
        assertTrue(boss.updateCombatPhase(1_000L));
        assertEquals(BossEntity.CombatPhase.FUSAO_VULCANICA, boss.getCombatPhase());
        assertTrue(boss.isTelegraphing(1_000L));
        assertFalse(boss.updateCombatPhase(2_000L), "A mesma transição não pode ser emitida duas vezes");

        boss.takeDamage(2); // 1/5: abaixo de 1/3
        assertTrue(boss.updateCombatPhase(2_000L));
        assertEquals(BossEntity.CombatPhase.FURIA_DE_APSU, boss.getCombatPhase());
        assertFalse(boss.isTelegraphing(2_000L + BossEntity.TELEGRAPH_NANOS));
    }

    @Test
    @DisplayName("Boss renderizado tem escala claramente superior à pose vertical do herói")
    void bossRenderBoundsAreLargeEnoughForFinalEncounter() {
        assertTrue(BossEntity.RENDER_H >= HeroEntity.HH * 3.5,
            "O modelo deve ocupar a hierarquia visual de boss final");
        assertTrue(BossEntity.RENDER_W >= HeroEntity.HW * 10,
            "A composição deve ampliar o boss sem ampliar sua hitbox de combate");
        assertTrue(BossEntity.BH >= HeroEntity.HH * 1.8,
            "A área de colisão precisa acompanhar o corpo maior do boss");
    }

    @Test
    @DisplayName("Boss flutua suavemente ao redor do centro, sem grandes saltos verticais")
    void bossMovementStaysNearArenaCenter() {
        BossEntity boss = new BossEntity(0, 900, 184);
        boss.updatePosition(0, 768);
        double centerAtRest = boss.getY() + BossEntity.BH / 2.0;
        boss.updatePosition(1.0, 768);
        assertTrue(Math.abs(boss.getY() + BossEntity.BH / 2.0 - centerAtRest) <= 28,
            "A animação de espera não deve empurrar o boss para fora do centro da arena");
    }
}
