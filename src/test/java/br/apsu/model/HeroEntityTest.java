package br.apsu.model;

import br.apsu.model.hero.HeroEntity;
import br.apsu.model.hero.HeroType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade - HeroEntity")
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
    @DisplayName("Janela de ataque segue o relógio da simulação")
    void testAttackWindowUsesSimulationClock() {
        long shotAt = 1_000_000_000L;
        hero.triggerShooting(shotAt);

        hero.updatePhysics(false, false, false, false, 0, 768, 0,
            1.0, 0, 0, shotAt + 150_000_000L);
        assertTrue(hero.isShooting(), "O modelo 3D deve permanecer na pose de ataque durante o disparo");
        assertEquals(150.0 / 260.0, hero.getAttackProgress(shotAt + 150_000_000L), 0.001);

        hero.updatePhysics(false, false, false, false, 0, 768, 0,
            1.0, 0, 0, shotAt + 300_000_000L);
        assertFalse(hero.isShooting(), "A pose de ataque deve terminar após a duração configurada");
        assertEquals(0.0, hero.getAttackProgress(shotAt + 300_000_000L));
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

    @Test
    @DisplayName("Interpolação de coordenadas do herói entre ticks")
    void testHeroInterpolation() {
        double startX = hero.getX();
        hero.updatePhysics(false, true, false, false, 0, 768, 0);

        double nextX = hero.getX();
        assertTrue(nextX > startX);

        assertEquals(startX, hero.getInterpolatedX(0.0), 0.0001);
        assertEquals(nextX, hero.getInterpolatedX(1.0), 0.0001);
        assertEquals((startX + nextX) * 0.5, hero.getInterpolatedX(0.5), 0.0001);

        hero.snapPrevious();
        assertEquals(nextX, hero.getInterpolatedX(0.0), 0.0001);
    }

    @Test
    @DisplayName("Física com dt=1/60 reproduz trajetória do teste dourado (AC2)")
    void testGoldenTrajectoryAtDt60() {
        HeroEntity h1 = new HeroEntity(HeroType.GUARDIAO, 5.0);
        HeroEntity h2 = new HeroEntity(HeroType.GUARDIAO, 5.0);
        h1.setX(200.0);
        h1.setY(300.0);
        h2.setX(200.0);
        h2.setY(300.0);

        // Sequência determinística de 300 ticks (5 segundos)
        for (int i = 0; i < 300; i++) {
            boolean right = (i < 120) || (i >= 240);
            boolean down  = (i >= 60 && i < 120);
            boolean left  = (i >= 120 && i < 180);
            boolean up    = (i >= 120 && i < 180) || (i >= 240);
            long nano = i * 16_666_666L;

            // h1 usa chamada com dt explícito 1/60
            h1.updatePhysics(left, right, up, down, 30, 700, 0, 1.0, 0, 0, nano, 1.0 / 60.0);
            // h2 usa chamada legada que delega para dt=1/60
            h2.updatePhysics(left, right, up, down, 30, 700, 0, 1.0, 0, 0, nano);

            assertEquals(h1.getX(), h2.getX(), 1e-6, "Posição X deve coincidir com tolerância 1e-6 no tick " + i);
            assertEquals(h1.getY(), h2.getY(), 1e-6, "Posição Y deve coincidir com tolerância 1e-6 no tick " + i);
            assertEquals(h1.getVx(), h2.getVx(), 1e-6, "Velocidade Vx deve coincidir no tick " + i);
            assertEquals(h1.getVy(), h2.getVy(), 1e-6, "Velocidade Vy deve coincidir no tick " + i);
        }
    }

    @Test
    @DisplayName("Subpassos a dt=1/120 convergem com dt=1/60 em no máximo 2 px após 5s (AC3)")
    void testSubsteppingConvergenceAtDt120() {
        HeroEntity h60 = new HeroEntity(HeroType.GUARDIAO, 5.0);
        HeroEntity h120 = new HeroEntity(HeroType.GUARDIAO, 5.0);
        h60.setX(100.0);
        h60.setY(300.0);
        h120.setX(100.0);
        h120.setY(300.0);

        // 5 segundos = 300 frames a 60 Hz
        for (int frame = 0; frame < 300; frame++) {
            long nanoBase = frame * 16_666_666L;

            // 1 passo de 1/60
            h60.updatePhysics(false, true, false, false, 30, 700, 0, 1.0, 0, 0, nanoBase, 1.0 / 60.0);

            // 2 subpassos de 1/120
            h120.updatePhysics(false, true, false, false, 30, 700, 0, 1.0, 0, 0, nanoBase, 1.0 / 120.0);
            h120.updatePhysics(false, true, false, false, 30, 700, 0, 1.0, 0, 0, nanoBase + 8_333_333L, 1.0 / 120.0);
        }

        double diffX = Math.abs(h60.getX() - h120.getX());
        assertTrue(diffX <= 2.0, "Diferença final após 5 s de nado contínuo deve ser <= 2 px (foi " + diffX + " px)");
    }

    @Test
    @DisplayName("Métricas de calibração para todos os HeroTypes (AC1 e AC6)")
    void testHeroTypePhysicsMetrics() {
        for (HeroType type : HeroType.values()) {
            assertEquals(type.getMaxSpeed() * 60.0, type.getMaxSpeedPerSecond(), 1e-4);
            assertEquals(type.getAccel() * 3600.0, type.getAccelPerSecondSquared(), 1e-4);

            HeroEntity testHero = new HeroEntity(type, 5.0);
            testHero.setX(0.0);
            testHero.setY(350.0);

            // Medir aceleração até 90% da velocidade máxima
            int ticksTo90 = -1;
            double target90 = type.getMaxSpeed() * 0.90;
            for (int t = 0; t < 600; t++) {
                testHero.updatePhysics(false, true, false, false, 30, 700, 0, 1.0, 0, 0, t * 16_666_666L, 1.0 / 60.0);
                if (testHero.getVx() >= target90 && ticksTo90 == -1) {
                    ticksTo90 = t;
                }
            }

            assertTrue(ticksTo90 > 0 && ticksTo90 < 60,
                "HeroType " + type.name() + " deve atingir 90% da velocidade máxima em menos de 1 s (levou " + ticksTo90 + " ticks)");
            assertTrue(testHero.getVx() <= type.getMaxSpeed() + 1e-4,
                "Velocidade do herói não deve ultrapassar maxSpeed (" + testHero.getVx() + " vs " + type.getMaxSpeed() + ")");

            // Medir frenagem ao soltar os controles
            double speedAtRelease = testHero.getVx();
            double xAtRelease = testHero.getX();
            int stopTicks = -1;
            for (int t = 0; t < 300; t++) {
                testHero.updatePhysics(false, false, false, false, 30, 700, 0, 1.0, 0, 0, (600 + t) * 16_666_666L, 1.0 / 60.0);
                if (testHero.getVx() < 0.1 && stopTicks == -1) {
                    stopTicks = t;
                }
            }

            assertTrue(stopTicks > 0 && stopTicks < 60,
                "HeroType " + type.name() + " deve desacelerar e parar em menos de 1 s ao soltar (levou " + stopTicks + " ticks)");
            double stoppingDistance = testHero.getX() - xAtRelease;
            assertTrue(stoppingDistance > 0 && stoppingDistance < speedAtRelease * 25.0,
                "Distância de parada deve ser finita e condizente (foi " + stoppingDistance + " px)");
        }
    }
}
