package br.apsu.graphics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — LightingEngine")
public class LightingEngineTest {

    @Test
    @DisplayName("Alternância e estado de efeitos reduzidos para performance")
    void testReducedEffectsState() {
        LightingEngine engine = new LightingEngine();

        // Por padrão reducedEffects é false
        engine.setReducedEffects(true);
        // Garante que o método não lança exceção ao alterar modo de performance
        assertDoesNotThrow(() -> engine.setReducedEffects(false));
    }
}
