package br.apsu.guidance;

import br.apsu.core.GameContext;
import br.apsu.model.guardian.GuardianEntity;

/**
 * Determina a posicao de mundo do objetivo corrente do heroi (US-010).
 * Nao altera o GameContext, operando como leitor puro.
 */
public final class ObjectiveTracker {

    public record Objective(double worldX, double worldY, String description) {}

    private ObjectiveTracker() {}

    /**
     * Resolve o objetivo atual com base na fase e no progresso de tabuletas.
     * Retorna null se a fase nao tiver objetivo de orientacao (ex: menu, dialogo, fim).
     */
    public static Objective resolveCurrentObjective(GameContext context) {
        if (context == null) return null;
        GameContext.State state = context.getState();
        if (state == null || state.ordinal() < GameContext.State.P1.ordinal() || state.ordinal() > GameContext.State.P5.ordinal()) {
            return null;
        }

        int phase = context.getCurrentPhase();
        if (phase == 5) {
            // Na arena do boss, não há seta de orientação
            return null;
        }

        // Fase 1: seta orienta o jogador até o primeiro guardião (Enki)
        if (phase == 1 && context.getTabletsCollected() < 1) {
            for (GuardianEntity g : context.getGuardians()) {
                if (!g.isContacted()) {
                    return new Objective(g.getWorldX() + GuardianEntity.GW / 2.0,
                        g.getWorldY() + GuardianEntity.GH / 2.0, "Fale com o guardião");
                }
            }
        }

        // Fase 2: antes de procurar a saída, orienta para o baú no galeão naufragado se fechado
        if (phase == 2 && !context.isChestOpen() && !context.getHero().hasBubblePower()) {
            return new Objective(context.getChestWX(), context.getChestWY(), "Encontre o naufrágio e abra o baú");
        }

        // Nas fases 1 a 4: uma vez obtida a tabuleta da fase, orienta para o portal de descida (x = 3900)
        if (context.getTabletsCollected() >= phase) {
            return new Objective(3900.0, 768 / 2.0, "Siga para o portal");
        }

        // Fases 2 a 4 antes da tabuleta: sem seta no guardião, permitindo exploração orgânica
        return null;
    }
}
