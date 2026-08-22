package br.apsu.model;

import br.apsu.model.hero.HeroEntity;
import br.apsu.model.hero.HeroType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — HeroEntity")
public class HeroEntityTest {

    private HeroEntity hero;

    @BeforeEach
    void setUp() {
        hero = new HeroEntity(HeroType.GUARDIAO, 5.0);
    }

    @Test
    @DisplayName("Inicialização correta dos atributos do herói")
    void testHeroInitialization() {
        assertEquals(HeroType.GUARDIAO, hero.getType());
        assertEquals(5.0, hero.getHp());
        assertEquals(5.0, hero.getMaxHP());
        assertEquals(100.0, hero.getX());
        assertTrue(hero.isFacingRight());
        assertFalse(hero.isInvulnerable());
        assertFalse(hero.hasBubblePower());
    }

    @Test
    @DisplayName("Atualização cinemática de nado para a direita")
    void testSwimmingRightMovement() {
        double startX = hero.getX();
        hero.updatePhysics(false, true, false, false, 0, 768, 0);
        assertTrue(hero.getX() > startX, "O herói deve ter se movido para a direita");
        assertTrue(hero.getVx() > 0, "A velocidade horizontal deve ser positiva");
        assertTrue(hero.isFacingRight(), "O herói deve estar virado para a direita");
    }

    @Test
    @DisplayName("Diferenciação de física entre o Velocista (Deus) e o Tanque (Abissal)")
    void testHeroPhysicsDifferentiation() {
        HeroEntity deus = new HeroEntity(HeroType.DEUS, 5.0);
        HeroEntity abissal = new HeroEntity(HeroType.ABISSAL, 5.0);

        deus.updatePhysics(false, true, false, false, 0, 768, 0);
        abissal.updatePhysics(false, true, false, false, 0, 768, 0);

        assertTrue(deus.getVx() > abissal.getVx(), "O Deus Dourado deve acelerar mais rápido que o Apkallu Abissal");
    }

    @Test
    @DisplayName("Soltar ou inverter direção reduz a deriva do herói")
    void testHeroBrakesWhenInputStopsOrReverses() {
        for (int i = 0; i < 12; i++) {
            hero.updatePhysics(false, true, false, false, 0, 768, 0);
        }
        double cruisingSpeed = hero.getVx();
        hero.updatePhysics(false, false, false, false, 0, 768, 0);
        assertTrue(hero.getVx() < cruisingSpeed, "Soltar a direção deve iniciar a frenagem");

        double beforeReverse = hero.getVx();
        hero.updatePhysics(true, false, false, false, 0, 768, 0);
        assertTrue(hero.getVx() < beforeReverse, "Inverter direção deve frear com mais força");
    }

    @Test
    @DisplayName("Animação de ataque segue o relógio fornecido pela simulação")
    void testAttackAnimationUsesSimulationClock() {
        long shotAt = 1_000_000_000L;
        hero.triggerShooting(shotAt);

        hero.updatePhysics(false, false, false, false, 0, 768, 0,
            1.0, 0, 0, shotAt + 150_000_000L);
        assertTrue(hero.isShooting(), "O ataque deve continuar durante a fase de impulso");
        assertTrue(hero.getAttackOffsetX() > 0, "O impulso deve deslocar a pose para frente");

        hero.updatePhysics(false, false, false, false, 0, 768, 0,
            1.0, 0, 0, shotAt + 500_000_000L);
        assertFalse(hero.isShooting(), "O ataque deve terminar após a recuperação, sem depender do relógio real");
        assertEquals(0.0, hero.getAttackOffsetX(), 0.001);
    }

    @Test
    @DisplayName("Aplicação de dano e janela de invulnerabilidade")
    void testDamageAndInvulnerability() {
        hero.applyDamage(1.5, 1000L);
        assertEquals(3.5, hero.getHp(), 0.001);
        assertTrue(hero.isInvulnerable());

        // Dano subsequente durante a invulnerabilidade deve ser ignorado
        hero.applyDamage(1.0, 2000L);
        assertEquals(3.5, hero.getHp(), 0.001);

        // Após expirar a janela (2.2s), invulnerabilidade deve desativar
        hero.checkInvulnerability(3_500_000_000L);
        assertFalse(hero.isInvulnerable());
    }

    @Test
    @DisplayName("Cura de vida respeitando o limite maxHP")
    void testHealing() {
        hero.applyDamage(2.0, 1000L);
        assertEquals(3.0, hero.getHp(), 0.001);

        hero.heal(1.5);
        assertEquals(4.5, hero.getHp(), 0.001);

        hero.heal(2.0);
        assertEquals(5.0, hero.getHp(), 0.001, "A vida não deve ultrapassar maxHP");
    }
}
