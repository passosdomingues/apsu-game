package br.apsu.model.hero;

import javafx.scene.paint.Color;

/**
 * Atributos e características de cada variante de Herói.
 * Overhaul: velocidades e acelerações calibradas para nado fluido e responsivo.
 */
public enum HeroType {
    GUARDIAO("Guardião dos Oceanos",
        "sprites/adapa/01_adapa_heroi.png",
        "sprites/adapa/01_adapa_nadando_disparo_bolhas.png",
        "sprites/adapa/01_adapa_nadando_atirando_bolhas.png",
        "sprites/adapa/01_adapa_heroi",
        11.0, 1.35, 0.945, 0.06, 1.0, "#4db8ff",
        "Equilibrado — velocidade legivel e controle preciso."),

    ABISSAL("Apkallu Abissal",
        "sprites/adapa/01_adapa_var_abissal.png",
        "sprites/adapa/01_adapa_nadando_disparo_bolhas.png",
        "sprites/adapa/01_adapa_nadando_atirando_bolhas.png",
        "sprites/adapa/01_adapa_var_abissal",
        9.0, 1.00, 0.960, 0.04, 1.6, "#1b6fa8",
        "Tanque Pesado — Muita inErcia e poder de ataque devastador."),

    DEUS("Deus Dourado",
        "sprites/adapa/01_adapa_var_deus_dourado.png",
        "sprites/adapa/01_adapa_nadando_disparo_bolhas.png",
        "sprites/adapa/01_adapa_nadando_atirando_bolhas.png",
        "sprites/adapa/01_adapa_var_deus_dourado",
        13.5, 1.60, 0.935, 0.09, 1.2, "#ffd700",
        "Velocista — rapido, mas ainda previsivel nas curvas."),

    RECIFE("Guardião dos Recifes",
        "sprites/adapa/01_adapa_var_recife.png",
        "sprites/adapa/01_adapa_nadando_disparo_bolhas.png",
        "sprites/adapa/01_adapa_nadando_atirando_bolhas.png",
        "sprites/adapa/01_adapa_var_recife",
        12.0, 1.70, 0.900, 0.07, 1.0, "#44cc88",
        "Tatico Agil — Desaceleracao rapida e curvas ultra precisas.");

    private final String name, staticSpritePath, attack1Path, attack2Path, swimDir;
    private final double maxSpeed, accel, drag, buoy, pwr;
    private final String auraHex, description;
    private final Color auraColor;

    HeroType(String name, String staticSpritePath, String attack1Path, String attack2Path, String swimDir,
             double maxSpeed, double accel, double drag, double buoy, double pwr, String auraHex, String description) {
        this.name = name;
        this.staticSpritePath = staticSpritePath;
        this.attack1Path = attack1Path;
        this.attack2Path = attack2Path;
        this.swimDir = swimDir;
        this.maxSpeed = maxSpeed;
        this.accel = accel;
        this.drag = drag;
        this.buoy = buoy;
        this.pwr = pwr;
        this.auraHex = auraHex;
        this.auraColor = Color.web(auraHex);
        this.description = description;
    }

    public String getName() { return name; }
    public String getStaticSpritePath() { return staticSpritePath; }
    public String getAttack1Path() { return attack1Path; }
    public String getAttack2Path() { return attack2Path; }
    public String getSwimDir() { return swimDir; }
    public double getMaxSpeed() { return maxSpeed; }
    public double getAccel() { return accel; }
    public double getDrag() { return drag; }
    public double getBuoy() { return buoy; }
    public double getPwr() { return pwr; }
    public String getAuraHex() { return auraHex; }
    public Color getAura() { return auraColor; }
    public String getDescription() { return description; }
    public String getAttackDir(String style) {
        return this == GUARDIAO
            ? "sprites/adapa/01_adapa_ataque_" + style
            : swimDir + "_ataque_" + style;
    }
}
