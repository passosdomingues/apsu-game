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
            // Na fase 5, o objetivo e o chefe Kullullu
            if (context.getBoss() != null && !context.getBoss().isDead()) {
                return new Objective(context.getBoss().getX(), context.getBoss().getY(), "Derrotar Kullullu");
            }
            return null;
        }

        // Fase 2: antes de procurar a tabuleta/portal, se o baú estiver fechado, orienta para o baú (US-021)
        if (phase == 2 && !context.isChestOpen() && !context.getHero().hasBubblePower()) {
            return new Objective(context.getChestWX(), context.getChestWY(), "Encontre o naufrágio e abra o baú");
        }

        // Fases 1 a 4: primeiro o guardiao correspondente da fase (se ainda nao contactado e tabuleta nao coletada)
        int requiredTablet = phase;
        if (context.getTabletsCollected() < requiredTablet) {
            for (GuardianEntity g : context.getGuardians()) {
                if (!g.isContacted()) {
                    return new Objective(g.getWorldX() + GuardianEntity.GW / 2.0,
                        g.getWorldY() + GuardianEntity.GH / 2.0, "Fale com o guardião");
                }
            }
        }

        // Tabuleta recebida ou guardiao contactado: objetivo passa a ser a saida do portal (x = 3900)
        return new Objective(3900.0, 768 / 2.0, "Siga para o portal");
    }
}
