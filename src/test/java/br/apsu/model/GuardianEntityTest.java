package br.apsu.model;

import br.apsu.model.guardian.GuardianEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — GuardianEntity")
public class GuardianEntityTest {

    @Test
    @DisplayName("Inicialização e status de contato dos Guardiões")
    void testGuardianCreationAndContact() {
        GuardianEntity g0 = new GuardianEntity(0, 500, 600);
        GuardianEntity g1 = new GuardianEntity(1, 1200, 600);
        GuardianEntity g2 = new GuardianEntity(2, 200, 300);

        assertEquals("Enki — Sábio das Águas", g0.getName());
        assertEquals("Enki — Guardião Celestial", g1.getName());
        assertEquals("Enki — Eremita Abissal", g2.getName());

        assertFalse(g0.isContacted());
        g0.setContacted(true);
        assertTrue(g0.isContacted());

        assertEquals(3, g0.getDialogue().length,
            "Fase 1 possui 3 falas curtas instruindo a missao (US-025)");
        for (String line : g0.getDialogue()) {
            assertTrue(line.length() <= 120, "Cada caixa deve ter ate 120 caracteres");
        }
        String fullDialogue = String.join(" ", g0.getDialogue());
        assertTrue(fullDialogue.contains("tabuleta"), "Deve mencionar tabuleta sagrada");
        assertTrue(fullDialogue.contains("pressione E"), "Deve mencionar a tecla de interacao E");

        for (String[] intro : GuardianEntity.ENKI_INTRO_DIALOGUES) {
            assertTrue(intro.length <= 4, "A introducao deve ter no maximo 4 caixas");
            for (String line : intro) {
                assertTrue(line.length() <= 120, "Cada fala de introducao deve ter ate 120 caracteres");
            }
        }
    }
}
