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

        assertEquals("Guardião Atlante", g0.getName());
        assertEquals("Guardião de Coral Vivo", g1.getName());
        assertEquals("Lamassu Aquático", g2.getName());

        assertFalse(g0.isContacted());
        g0.setContacted(true);
        assertTrue(g0.isContacted());

        assertEquals(1, g0.getDialogue().length,
            "Cada interação deve orientar o jogador com uma única fala curta");
        assertTrue(g0.getDialogue()[0].length() <= 60);
        for (String[] intro : GuardianEntity.ENKI_INTRO_DIALOGUES) {
            assertEquals(1, intro.length, "A introdução da fase deve caber em uma fala");
            assertTrue(intro[0].length() <= 60, "A fala deve ser curta e acionável");
        }
    }
}
