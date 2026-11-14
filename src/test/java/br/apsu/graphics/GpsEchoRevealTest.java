package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.core.SaveManager;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.enemy.EnemyType;
import br.apsu.model.guardian.GuardianEntity;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("US-032: GPS passivo e revelacao de inimigos pelo eco")
class GpsEchoRevealTest {

    private GameContext context;
    private UIRenderer uiRenderer;
    private GraphicsContext gc;

    @BeforeEach
    void setUp() {
        context = new GameContext(new SaveManager(Path.of("target/test-gps-echo-save.json")));
        context.startP1();
        uiRenderer = new UIRenderer();
        gc = new Canvas(1366, 768).getGraphicsContext2D();
    }

    @Test
    @DisplayName("AC1: Quando eco inativo, GPS mostra apenas o heroi e os guardioes (inimigos ocultos)")
    void testGpsShowsOnlyHeroAndGuardiansWhenEchoInactive() {
        context.getHero().setX(1000);
        context.getGuardians().clear();
        context.getGuardians().add(new GuardianEntity(0, 1200, 400));

        context.getEnemies().clear();
        context.getEnemies().add(new EnemyEntity(EnemyType.PEIXE, 1300, 400, 1.0, 0));

        // Eco inativo
        assertFalse(context.getEchoController().isActive());

        // Guardiao visivel
        assertTrue(uiRenderer.isGuardianVisibleOnMinimap(0, context),
            "Guardiao deve estar visivel no GPS mesmo com eco inativo");

        // Inimigo oculto
        assertFalse(uiRenderer.isEnemyVisibleOnMinimap(0, context),
            "Inimigo nao deve estar visivel no GPS quando o eco esta inativo");

        // Renderizacao do minimap sem excecao
        assertDoesNotThrow(() -> uiRenderer.drawMinimap(gc, context, 1366));
    }

    @Test
    @DisplayName("AC2: Quando eco ativo, inimigos dentro do raio do eco aparecem em vermelho no GPS")
    void testGpsShowsEnemiesWithinEchoRadiusWhenActive() {
        context.getHero().setX(1000);
        context.getHero().setY(400);

        context.getEnemies().clear();
        // Inimigo dentro do raio de 600 unidades (~300 de distancia)
        context.getEnemies().add(new EnemyEntity(EnemyType.PEIXE, 1300, 400, 1.0, 0));
        // Inimigo fora do raio de 600 unidades (~900 de distancia)
        context.getEnemies().add(new EnemyEntity(EnemyType.PEIXE, 1900, 400, 1.0, 0));

        // Ativa eco e avanca simulacao (onda atinge 360 unidades em 0.6s)
        assertTrue(context.getEchoController().trigger());
        context.getEchoWaveRenderer().update(context, 0.6);

        // Inimigo a 300 unidades foi atingido pela onda e fica visivel
        assertTrue(uiRenderer.isEnemyVisibleOnMinimap(0, context),
            "Inimigo dentro do raio deve aparecer no GPS quando revelado pelo eco");

        // Inimigo a 900 unidades continua oculto
        assertFalse(uiRenderer.isEnemyVisibleOnMinimap(1, context),
            "Inimigo fora do raio do eco nao deve aparecer no GPS");

        // Renderiza com inimigo visivel
        assertDoesNotThrow(() -> uiRenderer.drawMinimap(gc, context, 1366));
    }

    @Test
    @DisplayName("AC3: Apos o eco encerrar e passarem 3,0 s, os inimigos somem do GPS")
    void testEnemiesDisappearFromGpsAfter3Seconds() {
        context.getHero().setX(1000);
        context.getHero().setY(400);

        context.getEnemies().clear();
        context.getEnemies().add(new EnemyEntity(EnemyType.PEIXE, 1100, 400, 1.0, 0));

        // Ativa eco e atinge o inimigo
        context.getEchoController().trigger();
        context.getEchoWaveRenderer().update(context, 0.2);

        assertTrue(uiRenderer.isEnemyVisibleOnMinimap(0, context),
            "Inimigo atingido pelo eco deve estar visivel no GPS");

        // Avanca 2.0 s (destaque ainda ativo)
        context.getEchoWaveRenderer().update(context, 2.0);
        assertTrue(uiRenderer.isEnemyVisibleOnMinimap(0, context),
            "Inimigo ainda deve estar visivel antes dos 3.0 s");

        // Avanca mais 1.1 s (total 3.1 s apos ser atingido): destaque expira
        context.getEchoWaveRenderer().update(context, 1.1);
        assertFalse(uiRenderer.isEnemyVisibleOnMinimap(0, context),
            "Inimigo deve sumir do GPS apos expirarem os 3.0 s");
    }

    @Test
    @DisplayName("AC4: Na fase 5, mesmo com eco inativo, o chefe continua visivel no GPS")
    void testBossAlwaysVisibleInPhase5Gps() {
        context.startP5();

        // Eco inativo
        assertFalse(context.getEchoController().isActive());
        assertNotNull(context.getBoss(), "Chefe deve existir na Fase 5");

        // Chefe continua visivel no GPS
        assertTrue(uiRenderer.isBossVisibleOnMinimap(context),
            "Chefe deve continuar visivel no GPS da Fase 5 mesmo com eco inativo");

        // Renderizacao do minimap na Fase 5
        assertDoesNotThrow(() -> uiRenderer.drawMinimap(gc, context, 1366));
    }
}
