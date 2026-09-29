package br.apsu.model.environment;

/** Progressão de profundidade, pressão ambiental e flutuabilidade por fase. */
public enum OceanDepthProfile {
    COASTAL(1, "Plataforma costeira", 20, 3, 1.12),
    DEEP_REEF(2, "Recifes profundos", 300, 31, 1.04),
    ABYSSAL_PLAIN(3, "Planície abissal", 1_500, 151, 0.94),
    HYDROTHERMAL_VENT(4, "Fontes hidrotermais", 3_000, 301, 0.84),
    HADAL_TRENCH(5, "Fossa hadal", 6_000, 601, 0.76);

    private static final OceanDepthProfile[] ORDERED = values();
    private final int phase;
    private final String label;
    private final int depthMeters;
    private final int pressureBar;
    private final double buoyancyFactor;

    OceanDepthProfile(int phase, String label, int depthMeters, int pressureBar, double buoyancyFactor) {
        this.phase = phase;
        this.label = label;
        this.depthMeters = depthMeters;
        this.pressureBar = pressureBar;
        this.buoyancyFactor = buoyancyFactor;
    }

    public static OceanDepthProfile forPhase(int phase) {
        int index = Math.max(1, Math.min(ORDERED.length, phase)) - 1;
        return ORDERED[index];
    }

    public int getPhase() { return phase; }
    public String getLabel() { return label; }
    public int getDepthMeters() { return depthMeters; }
    public int getPressureBar() { return pressureBar; }
    public double getBuoyancyFactor() { return buoyancyFactor; }
}
