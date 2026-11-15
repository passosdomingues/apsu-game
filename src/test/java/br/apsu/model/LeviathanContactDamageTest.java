package br.apsu.model;

import br.apsu.core.GameContext;
import br.apsu.core.SaveManager;
import br.apsu.core.events.EventBus;
import br.apsu.core.events.GameEvent;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.enemy.EnemyType;
import br.apsu.model.environment.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("US-040: Dano por contato do Leviata Menor")
public class LeviathanContactDamageTest {

    private GameContext context;
    private EventBus eventBus;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        context = new GameContext(new SaveManager(tempDir.resolve("savegame.json")), eventBus);
        context.reset();
    }

    @Test
    @DisplayName("AC1: Na fase 4, contato do heroi com Leviata causa perda de vida, emite HERO_DAMAGED uma vez e Leviata permanece vivo")
    void testLeviathanContactDamageAndPersistence() {
        context.startP4();
        assertEquals(GameContext.State.P4, context.getState());

        EnemyEntity leviathan = context.getEnemies().stream()
                .filter(e -> e.getType() == EnemyType.LEVIATA)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Leviata Menor nao encontrado na Fase 4"));

        assertTrue(leviathan.isAlive(), "Leviata deve iniciar vivo");

        AtomicInteger damagedEvents = new AtomicInteger(0);
        eventBus.subscribe(GameEvent.Type.HERO_DAMAGED, e -> damagedEvents.incrementAndGet());

        double initialHp = context.getHero().getHp();

        // Posiciona heroi intersectando a caixa do Leviata Menor
        context.getHero().setX(leviathan.getWorldX() + 10);
        context.getHero().setY(leviathan.getCurrentY() + 10);

        context.update(1_000_000_000L); // 1.0s

        assertTrue(context.getHero().getHp() < initialHp, "Heroi deve perder HP ao colidir com Leviata");
        assertEquals(1, damagedEvents.get(), "HERO_DAMAGED deve ser publicado exatamente uma vez no contato inicial");
        assertTrue(context.getHero().isInvulnerable(), "Heroi deve ficar invulneravel apos sofrer dano");
        assertTrue(leviathan.isAlive(), "Ao contrario dos inimigos comuns, o Leviata Menor NAO deve morrer por contato com o heroi");
    }

    @Test
    @DisplayName("AC2: Heroi invulneravel em contato continuo nao sofre dano adicional ate 1.0s; novo dano ocorre apos expiracao")
    void testInvulnerabilityDuringContinuousContact() {
        context.startP4();
        EnemyEntity leviathan = context.getEnemies().stream()
                .filter(e -> e.getType() == EnemyType.LEVIATA)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Leviata Menor nao encontrado na Fase 4"));

        AtomicInteger damagedEvents = new AtomicInteger(0);
        eventBus.subscribe(GameEvent.Type.HERO_DAMAGED, e -> damagedEvents.incrementAndGet());

        context.getHero().setX(leviathan.getWorldX() + 10);
        context.getHero().setY(leviathan.getCurrentY() + 10);

        // 1. Primeiro contato
        context.update(1_000_000_000L);
        assertEquals(1, damagedEvents.get());
        double hpAfterFirstHit = context.getHero().getHp();
        assertTrue(context.getHero().isInvulnerable());

        // 2. Contato continuo durante invulnerabilidade (em 0.5s e 1.0s apos o hit)
        context.getHero().setX(leviathan.getWorldX() + 10);
        context.getHero().setY(leviathan.getCurrentY() + 10);
        context.update(1_500_000_000L); // +0.5s
        assertEquals(hpAfterFirstHit, context.getHero().getHp(), "Nenhum dano em +0.5s");
        assertEquals(1, damagedEvents.get(), "Nenhum evento extra em +0.5s");

        context.getHero().setX(leviathan.getWorldX() + 10);
        context.getHero().setY(leviathan.getCurrentY() + 10);
        context.update(2_000_000_000L); // +1.0s
        assertEquals(hpAfterFirstHit, context.getHero().getHp(), "Nenhum dano ate o fim de 1.0s");
        assertEquals(1, damagedEvents.get(), "Nenhum evento extra ate o fim de 1.0s");

        // 3. Apos fim da janela de invulnerabilidade (1.25s), novo dano e aplicado
        context.getHero().setX(leviathan.getWorldX() + 10);
        context.getHero().setY(leviathan.getCurrentY() + 10);
        context.update(2_300_000_000L); // +1.3s
        assertTrue(context.getHero().getHp() < hpAfterFirstHit, "Novo dano apos fim da invulnerabilidade");
        assertEquals(2, damagedEvents.get(), "Segundo evento HERO_DAMAGED emitido apos expiracao");
        assertTrue(leviathan.isAlive(), "Leviata permanece vivo apos multiplos contatos");
    }

    @Test
    @DisplayName("AC3: Constante nomeada de dano do Leviata e maior ou igual ao de inimigo comum em todas as dificuldades")
    void testLeviathanContactDamageConstant() {
        assertTrue(EnemyType.LEVIATHAN_CONTACT_DAMAGE > 0, "Dano do Leviata deve ser positivo");
        assertEquals(1.0, EnemyType.LEVIATHAN_CONTACT_DAMAGE, 0.001);

        for (Difficulty diff : Difficulty.values()) {
            double commonEnemyDmg = diff.getCollisionDamage() * 0.60;
            assertTrue(EnemyType.LEVIATHAN_CONTACT_DAMAGE >= commonEnemyDmg,
                    "Dano de contato do Leviata (" + EnemyType.LEVIATHAN_CONTACT_DAMAGE +
                    ") deve ser >= dano de inimigo comum na dificuldade " + diff + " (" + commonEnemyDmg + ")");
        }
    }
}
