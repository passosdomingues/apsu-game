package br.apsu.model;

import br.apsu.model.environment.Projectile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — Projectile")
public class ProjectileTest {

    @Test
    @DisplayName("Movimentação e danos de projéteis de heróis e inimigos")
    void testProjectileMovementAndDamage() {
        Projectile bubble = new Projectile(Projectile.Type.HERO_BUBBLE, 100, 200, 15.0, 0, 1.0);
        Projectile malign = new Projectile(Projectile.Type.ENEMY_MALIGN, 500, 300, -4.5, 0, 0.2);

        assertEquals(100, bubble.getX());
        assertEquals(1.0, bubble.getDamage());

        bubble.update();
        assertEquals(115, bubble.getX());

        assertEquals(0.2, malign.getDamage(), "Bolhas de inimigos comuns devem causar 0.2 HP de dano (5 bolhas = 1 ❤)");
    }
}
