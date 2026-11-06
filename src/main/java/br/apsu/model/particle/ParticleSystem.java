package br.apsu.model.particle;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Sistema de particulas bioluminescentes, rastro e explosoes.
 * Otimizado com pool fixo de particulas e compactacao in-place sem alocacao.
 */
public class ParticleSystem {
    private static final int MAX_PARTICLES_HIGH = 320;
    private static final int MAX_PARTICLES_REDUCED = 140;
    private static final int ALPHA_STEPS = 32;

    public static class Particle {
        double x, y;
        double vx, vy;
        double alpha;
        double r, g, b;

        public Particle(double x, double y, double vx, double vy, double alpha, double r, double g, double b) {
            init(x, y, vx, vy, alpha, r, g, b);
        }

        public void init(double x, double y, double vx, double vy, double alpha, double r, double g, double b) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.alpha = alpha;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getAlpha() { return alpha; }
    }

    private final Particle[] pool = new Particle[MAX_PARTICLES_HIGH];
    private int aliveCount = 0;
    private final Map<Integer, Color[]> fillPalettes = new HashMap<>();
    private boolean reducedEffects = true;

    public ParticleSystem() {
        for (int i = 0; i < MAX_PARTICLES_HIGH; i++) {
            pool[i] = new Particle(0, 0, 0, 0, 0, 0, 0, 0);
        }
        // Pre-aquece as paletas de cores padrao mais usadas para evitar alocacao no frame
        cacheColorPalette(Color.CYAN);
        cacheColorPalette(Color.GOLD);
        cacheColorPalette(Color.BLUE);
        cacheColorPalette(Color.WHITE);
        cacheColorPalette(Color.RED);
    }

    private void cacheColorPalette(Color c) {
        int red = (int) Math.round(c.getRed() * 255);
        int green = (int) Math.round(c.getGreen() * 255);
        int blue = (int) Math.round(c.getBlue() * 255);
        int key = (red << 16) | (green << 8) | blue;
        getOrCreatePalette(key, red, green, blue);
    }

    private Color[] getOrCreatePalette(int key, int red, int green, int blue) {
        Color[] palette = fillPalettes.get(key);
        if (palette == null) {
            palette = new Color[ALPHA_STEPS]; // hot-alloc-ok: preenchimento de paleta raro na primeira ocorrencia de cor
            for (int i = 0; i < ALPHA_STEPS; i++) {
                palette[i] = Color.rgb(red, green, blue, i / (double) (ALPHA_STEPS - 1)); // hot-alloc-ok
            }
            fillPalettes.put(key, palette);
        }
        return palette;
    }

    private int particleBudget() {
        return reducedEffects ? MAX_PARTICLES_REDUCED : MAX_PARTICLES_HIGH;
    }

    private boolean hasCapacity() {
        return aliveCount < particleBudget();
    }

    public void addBurst(double x, double y, Color c) {
        int count = reducedEffects ? 7 : 14;
        double cr = c.getRed(), cg = c.getGreen(), cb = c.getBlue();
        for (int i = 0; i < count && hasCapacity(); i++) {
            double a = Math.random() * Math.PI * 2;
            double s = 2.2 + Math.random() * 5.0;
            pool[aliveCount++].init(x, y, Math.cos(a) * s, Math.sin(a) * s, 1.0, cr, cg, cb);
        }
    }

    public void addBubble(double x, double y, double vx, double vy, Color c) {
        if (hasCapacity()) {
            pool[aliveCount++].init(x, y, vx, vy, 0.7, c.getRed(), c.getGreen(), c.getBlue());
        }
    }

    public void update() {
        int alive = 0;
        for (int i = 0; i < aliveCount; i++) {
            Particle p = pool[i];
            p.x += p.vx;
            p.y += p.vy;
            p.alpha -= 0.025;
            if (p.alpha > 0) {
                if (i != alive) {
                    Particle temp = pool[alive];
                    pool[alive] = p;
                    pool[i] = temp;
                }
                alive++;
            }
        }
        aliveCount = alive;
    }

    public void render(GraphicsContext gc) {
        for (int i = 0; i < aliveCount; i++) {
            Particle p = pool[i];
            int alpha = (int) Math.round(p.alpha * (ALPHA_STEPS - 1));
            if (alpha <= 0) continue;
            if (alpha >= ALPHA_STEPS) alpha = ALPHA_STEPS - 1;
            int red = (int) (p.r * 255);
            int green = (int) (p.g * 255);
            int blue = (int) (p.b * 255);
            int key = (red << 16) | (green << 8) | blue;
            Color[] palette = getOrCreatePalette(key, red, green, blue);
            gc.setFill(palette[alpha]);
            gc.fillOval(p.x - 4, p.y - 4, 8, 8);
        }
    }

    public void clear() {
        aliveCount = 0;
    }

    /** Ajustado pelo loop conforme FPS; evita picos de alocacao em combate. */
    public void setReducedEffects(boolean reducedEffects) {
        this.reducedEffects = reducedEffects;
    }

    public boolean isReducedEffects() { return reducedEffects; }

    public int size() { return aliveCount; }

    public Particle getParticle(int index) {
        if (index < 0 || index >= aliveCount) throw new IndexOutOfBoundsException();
        return pool[index];
    }
}
