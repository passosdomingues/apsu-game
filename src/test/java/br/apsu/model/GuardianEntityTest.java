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

        assertTrue(g0.getDialogue().length >= 4, "Cada guardião deve possuir pelo menos 4 frases de diálogo");
    }
}
