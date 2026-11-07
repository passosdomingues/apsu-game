package br.apsu.guidance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade - ObjectiveArrow (US-010)")
class ObjectiveArrowTest {

    @Test
    @DisplayName("Cálculo de ângulo aponta na direção correta com erro menor que 5 graus (AC1)")
    void angleCalculationAccuracy() {
        double heroX = 100, heroY = 384;
        double targetX = 900, targetY = 384; // exatamente à direita

        double angle = ObjectiveArrow.calculateAngle(heroX, heroY, targetX, targetY);
        double expectedAngle = 0.0;
        double error = ObjectiveArrow.angularErrorDegrees(angle, expectedAngle);
        assertTrue(error < 5.0, "Erro angular deve ser menor que 5 graus (foi " + error + ")");

        // Alvo na diagonal inferior direita (45 graus)
        targetX = 100 + 500;
        targetY = 384 + 500;
        angle = ObjectiveArrow.calculateAngle(heroX, heroY, targetX, targetY);
        expectedAngle = Math.PI / 4.0;
        error = ObjectiveArrow.angularErrorDegrees(angle, expectedAngle);
        assertTrue(error < 5.0, "Erro angular deve ser menor que 5 graus (foi " + error + ")");
    }

    @Test
    @DisplayName("Cálculo de erro angular trata descontinuidades de 360 graus")
    void angularErrorHandlesWrapAround() {
        double angleA = Math.PI * 0.99;
        double angleB = -Math.PI * 0.99;
        double error = ObjectiveArrow.angularErrorDegrees(angleA, angleB);
        assertTrue(error < 5.0, "Diferença deve ser ~3.6 graus e não 356 graus");
    }
}
