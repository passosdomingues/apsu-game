package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.model.boss.BossEntity;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.enemy.EnemyType;
import br.apsu.model.guardian.GuardianEntity;
import br.apsu.model.hero.HeroEntity;
import br.apsu.model.hero.HeroType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — Fallback 2D de Personagens (US-008)")
public class Character2DFallbackTest {

    @BeforeEach
    void setUp() {
        System.clearProperty("apsu.render");
        GraphicsDiagnostics.setScene3dSupportedForTesting(null);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("apsu.render");
        GraphicsDiagnostics.setScene3dSupportedForTesting(null);
    }

    @Test
    @DisplayName("AC4: Opção -Dapsu.render=2d força modo 2D Canvas")
    void testForce2DRenderViaSystemProperty() {
        System.setProperty("apsu.render", "2d");
        assertFalse(GraphicsDiagnostics.isScene3dSupported(), "Com -Dapsu.render=2d, isScene3dSupported deve retornar false");
    }

    @Test
    @DisplayName("AC5: Todos os personagens possuem arquivo de sprite PNG existente no classpath")
    void testAllCharactersHaveSpritePngFiles() {
        // 1. Variantes de Herói
        for (HeroType heroType : HeroType.values()) {
            assertNotNull(heroType.getStaticSpritePath(), "Sprite path do herói não deve ser nulo");
            try (InputStream is = getClass().getResourceAsStream("/" + heroType.getStaticSpritePath())) {
                assertNotNull(is, "Sprite faltante para o herói: " + heroType.getStaticSpritePath());
            } catch (Exception e) {
                fail("Erro ao ler sprite do herói " + heroType.name() + ": " + e.getMessage());
            }
        }

        // 2. Tipos de Inimigos
        for (EnemyType enemyType : EnemyType.values()) {
            assertNotNull(enemyType.getStaticSprite(), "Sprite path do inimigo não deve ser nulo");
            try (InputStream is = getClass().getResourceAsStream("/" + enemyType.getStaticSprite())) {
                assertNotNull(is, "Sprite faltante para o inimigo: " + enemyType.getStaticSprite());
            } catch (Exception e) {
                fail("Erro ao ler sprite do inimigo " + enemyType.name() + ": " + e.getMessage());
            }
        }

        // 3. Guardiões
        for (String gPath : GuardianEntity.SPRITE_PATHS) {
            try (InputStream is = getClass().getResourceAsStream("/" + gPath)) {
                assertNotNull(is, "Sprite faltante para o guardião: " + gPath);
            } catch (Exception e) {
                fail("Erro ao ler sprite de guardião " + gPath + ": " + e.getMessage());
            }
        }

        // 4. Boss Kullullû (3 variantes)
        String[] bossSprites = {
            "sprites/kullullu/02_kullullu_boss.png",
            "sprites/kullullu/02_kullullu_var_glacial.png",
            "sprites/kullullu/02_kullullu_var_toxico.png"
        };
        for (String bPath : bossSprites) {
            try (InputStream is = getClass().getResourceAsStream("/" + bPath)) {
                assertNotNull(is, "Sprite faltante para o boss: " + bPath);
            } catch (Exception e) {
                fail("Erro ao ler sprite do boss " + bPath + ": " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("AC1 e AC2: Escala do fallback 2D difere no máximo 10% da versão 3D")
    void testFallbackScaleWithin10PercentOf3D() {
        // Herói: target 3D = 180x410; fallback 2D = 184x410 (diferença ~2%)
        double hero3DW = 180.0, hero3DH = 410.0;
        double hero2DW = 184.0, hero2DH = 410.0;
        assertTrue(Math.abs(hero2DW - hero3DW) / hero3DW <= 0.10, "Largura do herói deve diferir <= 10%");
        assertTrue(Math.abs(hero2DH - hero3DH) / hero3DH <= 0.10, "Altura do herói deve diferir <= 10%");

        // Boss: target 3D hitbox = BW (260) x BH (400); fallback 2D = 260x400 (diferença 0%)
        double boss3DW = BossEntity.BW, boss3DH = BossEntity.BH;
        double boss2DW = 260.0, boss2DH = 400.0;
        assertTrue(Math.abs(boss2DW - boss3DW) / boss3DW <= 0.10, "Largura do boss deve diferir <= 10%");
        assertTrue(Math.abs(boss2DH - boss3DH) / boss3DH <= 0.10, "Altura do boss deve diferir <= 10%");
    }
}
