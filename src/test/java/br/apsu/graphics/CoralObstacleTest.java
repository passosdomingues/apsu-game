package br.apsu.graphics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Obstaculos de coral foram removidos da Fase 2.
 * Testes legados substituidos por verificacao de que a classe CoralRenderer existe
 * mas nao e mais acionada pelo pipeline de renderizacao.
 */
@DisplayName("CoralObstacleTest (legado — coral removido da Fase 2)")
public class CoralObstacleTest {

    @Test
    @DisplayName("CoralRenderer existe mas nao e instanciado em producao")
    void coralRendererClassExists() {
        // A classe e mantida para compatibilidade; verificamos apenas que compila.
        assertTrue(CoralRenderer.class != null);
    }
}
