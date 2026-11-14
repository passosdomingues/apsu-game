package br.apsu.core;

import br.apsu.model.environment.Difficulty;
import br.apsu.model.hero.HeroEntity;
import br.apsu.model.hero.HeroType;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Regressao - Progressao por Fase (US-004)")
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class PhaseProgressionRegressionTest {

    private GameContext context;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Ja inicializado
        }
    }

    @BeforeEach
    void setUp() {
        context = new GameContext();
        context.setDifficulty(Difficulty.FACIL);
        context.setHeroType(HeroType.GUARDIAO);
    }

    private void enterPhase(int phase) {
        switch (phase) {
            case 1 -> context.startP1();
            case 2 -> context.startP2();
            case 3 -> context.startP3();
            case 4 -> context.startP4();
            case 5 -> context.startP5();
            default -> throw new IllegalArgumentException("Fase desconhecida: " + phase);
        }
    }

    private void awardTabletForCurrentPhase(int expectedTabletsAfter) {
        if (!context.getGuardians().isEmpty()) {
            var guardian = context.getGuardians().get(0);
            context.getHero().setX(guardian.getWorldX());
            context.getHero().setY(guardian.getWorldY());
            context.getInputManager().registerKeyPress(KeyCode.E);
            context.update(expectedTabletsAfter * 1_000_000_000L);
            for (int i = 0; i < 64 && context.isOverlayActive(); i++) {
                context.advanceOverlay();
            }
        }
        assertEquals(expectedTabletsAfter, context.getTabletsCollected(),
            "Deve ter coletado a quantidade esperada de tabuletas");
    }

    @Test
    @DisplayName("Fases 1 a 4: saida sem tabuleta nao transiciona; com tabuleta transiciona uma unica vez (AC1)")
    void exitWithoutTabletDoesNotTransitionAndWithTabletTransitionsOnce() {
        for (int phase = 1; phase <= 4; phase++) {
            enterPhase(phase);
            assertEquals(phase, context.getCurrentPhase());
            assertEquals(phase - 1, context.getTabletsCollected());

            // Tentativa de saida sem a tabuleta da fase atual
            context.getHero().setVx(0);
            context.getHero().setVy(0);
            context.getHero().setX(3850);
            context.getHero().setY(300);
            context.update((phase * 10L) * 1_000_000_000L);

            // Nao transiciona
            assertEquals(phase, context.getCurrentPhase(), "Fase nao deve mudar sem a tabuleta necessaria");

            // Coleta a tabuleta correspondente
            awardTabletForCurrentPhase(phase);

            // Alcança a saida com a tabuleta
            context.getHero().setVx(0);
            context.getHero().setVy(0);
            context.getHero().setX(3850);
            context.getHero().setY(300);
            context.update((phase * 10L + 5L) * 1_000_000_000L);

            // Transicao ocorre exatamente uma vez (passa para o dialogo da proxima fase)
            assertEquals(GameContext.State.DIALOGUE, context.getState());
            assertEquals(phase, context.getDlgPhase(), "Transicao iniciada para a proxima fase (dlgPhase=" + phase + ")");
        }
    }

    @Test
    @DisplayName("Fase 2: abrir bau duas vezes libera o poder das bolhas uma unica vez (AC2)")
    void openingChestTwiceUnlocksBubblePowerOnlyOnce() {
        context.startP2();
        assertFalse(context.getHero().hasBubblePower(), "Heroi inicia Fase 2 sem poder de bolhas");
        assertFalse(context.isChestOpen(), "Bau deve comecar fechado");

        // Posiciona heroi sobre o bau
        context.getHero().setX(context.getChestWX());
        context.getHero().setY(context.getChestWY());
        assertTrue(context.canOpenChest(), "Heroi deve estar em alcance de abrir o bau");

        // Primeira abertura
        context.getInputManager().registerKeyPress(KeyCode.E);
        context.update(1_000_000_000L);

        assertTrue(context.isChestOpen(), "Bau deve estar aberto apos primeira interacao");
        assertTrue(context.getHero().hasBubblePower(), "Poder das bolhas deve ser liberado");
        assertFalse(context.canOpenChest(), "Bau aberto nao pode mais ser aberto");

        // Segunda tentativa de abertura
        long firstChestTime = context.getChestTime();
        context.getInputManager().registerKeyPress(KeyCode.E);
        context.update(2_000_000_000L);

        assertTrue(context.isChestOpen());
        assertTrue(context.getHero().hasBubblePower());
        assertEquals(firstChestTime, context.getChestTime(), "Timestamp de abertura do bau nao deve mudar");
    }

    @Test
    @DisplayName("Fases 1 a 4 nao possuem Kullullu; Fase 5 possui exatamente um Kullullu (AC3)")
    void phases1To4HaveNoKullulluAndPhase5HasExactlyOne() {
        for (int phase = 1; phase <= 4; phase++) {
            enterPhase(phase);
            assertNull(context.getBoss(), "Fase " + phase + " nao deve instanciar Kullullu");
        }

        enterPhase(5);
        assertNotNull(context.getBoss(), "Fase 5 deve instanciar Kullullu");
        assertEquals("Kullullû", context.getBoss().getName());
        assertEquals(0, context.getBoss().getVariant());
        assertFalse(context.getBoss().isDead());
    }

    @Test
    @DisplayName("Partida reiniciada apos Fase 5 limpa todo o estado inicial (AC4)")
    void resetGameAfterPhase5CleansAllStateToInitial() {
        // Avanca ate a fase 5 e modifica atributos
        enterPhase(5);
        context.getHero().setHasBubblePower(true);
        context.getHero().applyDamage(2.0, 1_000_000_000L);
        assertNotNull(context.getBoss());

        // Reinicia a partida
        context.reset();

        assertEquals(0, context.getCurrentPhase(), "Fase atual deve ser resetada para 0");
        assertEquals(Difficulty.FACIL.getInitialHP(), context.getHero().getHp(), 0.001, "Vida deve voltar ao maximo inicial");
        assertEquals(0, context.getTabletsCollected(), "Tabuletas devem ser zeradas");
        assertFalse(context.getHero().hasBubblePower(), "Poder das bolhas deve ser resetado");
        assertNull(context.getBoss(), "Boss Kullullu deve ser nulo apos reset");
        assertTrue(context.getEnemies().isEmpty(), "Lista de inimigos deve estar vazia");
        assertTrue(context.getGuardians().isEmpty(), "Lista de guardioes deve estar vazia");
        assertTrue(context.getSceneryElements().isEmpty(), "Lista de cenarios deve estar vazia");
        assertTrue(context.getProjectiles().isEmpty(), "Lista de projeteis deve estar vazia");
        assertEquals(100.0, context.getHero().getX(), 0.001, "Heroi deve voltar a posicao inicial X");
        assertEquals(768 / 2.0 - HeroEntity.HH / 2.0, context.getHero().getY(), 0.001, "Heroi deve voltar a posicao inicial Y");
        assertFalse(context.isChestOpen(), "Bau deve estar fechado");
    }
}
