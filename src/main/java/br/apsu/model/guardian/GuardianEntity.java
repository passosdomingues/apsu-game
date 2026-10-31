package br.apsu.model.guardian;

/**
 * Entidade de Guardião Atlante — agora com 5 variantes para as 5 fases.
 */
public class GuardianEntity {
    public static final double GW = 110, GH = 185;

    public static final String[] NAMES = {
        "Guardião Atlante",
        "Guardião de Coral Vivo",
        "Lamassu Aquático",
        "Oráculo das Chamas Abissais",
        "Enki — Senhor das Profundezas"
    };

    public static final String[][] DIALOGUES = {
        // Fase 1 — Águas Claras
        { "A tabuleta está adiante. Siga as ruínas." },

        // Fase 2 — Cavernas de Coral
        { "O naufrágio está adiante. O baú guarda a tabuleta." },

        // Fase 3 — Correntes Abissais
        { "As correntes apontam o caminho. Recupere a tabuleta." },

        // Fase 4 — Abismo Vulcânico
        { "Desvie da lava. A última tabuleta está adiante." },

        // Fase 5 — Templo Final de Apsu (Enki aparece em pessoa)
        { "Kullullû bloqueia o templo. Use suas bolhas e evite os ataques." }
    };

    /** Falas de Enki nas vinhetas entre menu/fases; guardiões falam ao encontrá-los na fase. */
    public static final String[][] ENKI_INTRO_DIALOGUES = {
        {"Encontre o Guardião Atlante. Recupere a tabuleta."},
        {"Procure o naufrágio. Abra o baú com E."},
        {"Siga as correntes até o guardião."},
        {"Desvie da lava. Busque a última tabuleta."},
        {"Kullullû está à frente. Mire bolhas e sobreviva."}
    };

    public static final String[] SPRITE_PATHS = {
        "sprites/guardioes/05_guardiao_atlante_npc.png",
        "sprites/guardioes/05_guardiao_coral_vivo.png",
        "sprites/guardioes/05_guardiao_lamassu_aquatico.png",
        "sprites/guardioes/05_guardiao_oraculo_correntes.png",
        "sprites/enki/03_enki_npc.png"   // Enki em pessoa no Templo Final
    };

    private int type; // 0=Atlante, 1=Coral, 2=Lamassu, 3=Oráculo, 4=Enki
    private double worldX, worldY;
    private boolean contacted = false;

    public GuardianEntity(int type, double worldX, double worldY) {
        this.type = type;
        this.worldX = worldX;
        this.worldY = worldY;
    }

    public int getType() { return type; }
    public String getName() { return NAMES[Math.min(type, NAMES.length - 1)]; }
    public String getSpritePath() { return SPRITE_PATHS[Math.min(type, SPRITE_PATHS.length - 1)]; }
    public String[] getDialogue() { return DIALOGUES[Math.min(type, DIALOGUES.length - 1)]; }
    public double getWorldX() { return worldX; }
    public double getWorldY() { return worldY; }
    public boolean isContacted() { return contacted; }
    public void setContacted(boolean c) { this.contacted = c; }
}
