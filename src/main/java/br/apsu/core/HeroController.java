package br.apsu.core;

import br.apsu.model.hero.HeroEntity;

/**
 * Controlador de movimentacao e fisica do heroi desacoplado do GameContext (APSU-F03).
 * Zero-alocacao no loop de 60 Hz (regra 11).
 */
public class HeroController {

    public void step(HeroEntity hero,
                     InputManager inputManager,
                     double minY,
                     double maxY,
                     int phaseDone,
                     double buoyancyMult,
                     double currentFx,
                     double currentFy,
                     long currentNanoTime,
                     double dt) {
        if (hero == null || inputManager == null) return;

        double ax = inputManager.axisX();
        double ay = inputManager.axisY();
        boolean left = ax < 0;
        boolean right = ax > 0;
        boolean up = ay < 0;
        boolean down = ay > 0;

        hero.updatePhysics(left, right, up, down,
                           minY, maxY, phaseDone,
                           buoyancyMult, currentFx, currentFy,
                           currentNanoTime, dt);
    }

    public void step(HeroEntity hero,
                     boolean left, boolean right, boolean up, boolean down,
                     double minY,
                     double maxY,
                     int phaseDone,
                     double buoyancyMult,
                     double currentFx,
                     double currentFy,
                     long currentNanoTime,
                     double dt) {
        if (hero == null) return;

        hero.updatePhysics(left, right, up, down,
                           minY, maxY, phaseDone,
                           buoyancyMult, currentFx, currentFy,
                           currentNanoTime, dt);
    }
}
