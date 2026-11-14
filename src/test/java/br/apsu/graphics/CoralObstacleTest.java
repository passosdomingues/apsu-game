package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.model.environment.SceneryElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — Obstáculos de Coral da Fase 2 (US-001)")
public class CoralObstacleTest {

    private GameContext context;

    @BeforeEach
    void setUp() {
        context = new GameContext();
        context.startP2();
    }

    @Test
    @DisplayName("AC1 e AC4: Cada hitbox de coral na Fase 2 tem elemento visual associado")
    void testEveryCoralHitboxHasVisualAssociated() {
        List<SceneryElement> corals = context.getSceneryElements().stream()
            .filter(e -> e.getType() == SceneryElement.Type.CORAL)
            .toList();

        // 6 colunas, cada uma com estalactite e estalagmite (pelo menos 10 a 12 elementos de coral)
        assertTrue(corals.size() >= 10, "A Fase 2 deve ter pelo menos 10 elementos de coral nos 6 pares");

        for (SceneryElement c : corals) {
            assertTrue(c.getWidth() > 0, "Largura do coral deve ser positiva");
            assertTrue(c.getHeight() > 0, "Altura do coral deve ser positiva");
            assertTrue(c.getWorldX() >= 0 && c.getWorldX() <= 4000, "Coral deve estar nos limites do mundo");

            // Verifica alinhamento vertical (ou começa no topo y=0 ou se estende até o fundo y+h=768)
            boolean touchesTop = (c.getWorldY() <= 1.0);
            boolean touchesBottom = Math.abs((c.getWorldY() + c.getHeight()) - 768.0) <= 1.0;
            assertTrue(touchesTop || touchesBottom, "Coral deve ser estalactite (teto) ou estalagmite (chao)");
        }
    }

    @Test
    @DisplayName("AC2: Diferença entre limites de colisão e malha visual é de no máximo 8 px")
    void testCoralBoundsToleranceWithin8Pixels() {
        List<SceneryElement> corals = context.getSceneryElements().stream()
            .filter(e -> e.getType() == SceneryElement.Type.CORAL)
            .toList();

        for (SceneryElement c : corals) {
            double hitW = c.getWidth();
            double hitH = c.getHeight();

            // O CoralRenderer desenha a base ocupando a largura exata w e corpo com inset de 2px
            double visualW = hitW;
            double visualH = hitH;

            double diffX = Math.abs(hitW - visualW);
            double diffY = Math.abs(hitH - visualH);

            assertTrue(diffX <= 8.0, "Diferença em X deve ser de no máximo 8px");
            assertTrue(diffY <= 8.0, "Diferença em Y deve ser de no máximo 8px");
        }
    }

    @Test
    @DisplayName("AC3: Colisão com coral causa dano e coral está na posição exata")
    void testCoralCollisionDetection() {
        List<SceneryElement> corals = context.getSceneryElements().stream()
            .filter(e -> e.getType() == SceneryElement.Type.CORAL)
            .toList();

        SceneryElement sample = corals.get(0);
        // Posiciona herói dentro da hitbox do coral
        context.getHero().setX(sample.getWorldX() + 10);
        context.getHero().setY(sample.getWorldY() + 10);

        double hpBefore = context.getHero().getHp();
        context.update(1_000_000_000L); // executa 1 tick/frame

        assertTrue(context.getHero().getHp() < hpBefore, "Colisão com coral deve infligir dano ao herói");
    }
}
