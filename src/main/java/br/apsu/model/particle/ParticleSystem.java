package br.apsu.model.particle;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sistema de partículas bioluminescentes, rastro e explosões.
 */
public class ParticleSystem {
    private static final int MAX_PARTICLES_HIGH = 320;
    private static final int MAX_PARTICLES_REDUCED = 140;

    public static class Particle {
        double x, y;
        double vx, vy;
        double alpha;
        double r, g, b;

        public Particle(double x, double y, double vx, double vy, double alpha, double r, double g, double b) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.alpha = alpha;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Map<Integer, Color[]> fillPalettes = new HashMap<>();
    private boolean reducedEffects = true;
    private static final int ALPHA_STEPS = 32;

    private int particleBudget() {
        return reducedEffects ? MAX_PARTICLES_REDUCED : MAX_PARTICLES_HIGH;
    }

    private boolean hasCapacity() {
        return particles.size() < particleBudget();
    }

    public void addBurst(double x, double y, Color c) {
        int count = reducedEffects ? 7 : 14;
        for (int i = 0; i < count && hasCapacity(); i++) {
            double a = Math.random() * Math.PI * 2;
            double s = 2.2 + Math.random() * 5.0;
            particles.add(new Particle(x, y, Math.cos(a)*s, Math.sin(a)*s, 1.0, c.getRed(), c.getGreen(), c.getBlue()));
        }
    }

    public void addBubble(double x, double y, double vx, double vy, Color c) {
        if (hasCapacity()) {
            particles.add(new Particle(x, y, vx, vy, 0.7, c.getRed(), c.getGreen(), c.getBlue()));
        }
    }

    public void update() {
        int alive = 0;
        for (int i = 0, size = particles.size(); i < size; i++) {
            Particle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.alpha -= 0.025;
            if (p.alpha > 0) particles.set(alive++, p);
        }
        while (particles.size() > alive) particles.remove(particles.size() - 1);
    }

    public void render(GraphicsContext gc) {
        for (Particle p : particles) {
            int alpha = Math.max(0, Math.min(ALPHA_STEPS - 1,
                (int) Math.round(p.alpha * (ALPHA_STEPS - 1))));
            if (alpha == 0) continue;
            int red = (int) Math.round(p.r * 255), green = (int) Math.round(p.g * 255), blue = (int) Math.round(p.b * 255);
            int key = (red << 16) | (green << 8) | blue;
            Color[] palette = fillPalettes.computeIfAbsent(key, ignored -> {
                Color[] colors = new Color[ALPHA_STEPS];
                for (int i = 0; i < colors.length; i++) {
                    colors[i] = Color.rgb(red, green, blue, i / (double) (ALPHA_STEPS - 1));
                }
                return colors;
            });
            gc.setFill(palette[alpha]);
            gc.fillOval(p.x - 4, p.y - 4, 8, 8);
        }
    }

    public void clear() {
        particles.clear();
        fillPalettes.clear();
    }

    /** Ajustado pelo loop conforme FPS; evita picos de alocação em combate. */
    public void setReducedEffects(boolean reducedEffects) {
        this.reducedEffects = reducedEffects;
    }

    public boolean isReducedEffects() { return reducedEffects; }

    public int size() { return particles.size(); }
}
