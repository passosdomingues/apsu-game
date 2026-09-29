package br.apsu.model.particle;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    private boolean reducedEffects = true;

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
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.alpha -= 0.025;
            if (p.alpha <= 0) it.remove();
        }
    }

    public void render(GraphicsContext gc) {
        for (Particle p : particles) {
            gc.setFill(Color.color(p.r, p.g, p.b, Math.max(0, p.alpha)));
            gc.fillOval(p.x - 4, p.y - 4, 8, 8);
        }
    }

    public void clear() {
        particles.clear();
    }

    /** Ajustado pelo loop conforme FPS; evita picos de alocação em combate. */
    public void setReducedEffects(boolean reducedEffects) {
        this.reducedEffects = reducedEffects;
    }

    public boolean isReducedEffects() { return reducedEffects; }

    public int size() { return particles.size(); }
}
