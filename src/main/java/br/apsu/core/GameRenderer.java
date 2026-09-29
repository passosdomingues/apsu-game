package br.apsu.core;

import javafx.scene.canvas.GraphicsContext;

/** Porta de desenho do loop, substituível por uma implementação de teste headless. */
public interface GameRenderer {
    boolean isReducedEffects();
    void setReducedEffects(boolean reducedEffects);
    void render(GraphicsContext graphics, GameContext context, double width, double height);
}
