package br.apsu.model.guardian;

/**
 * Entidade de Guardião Atlante — agora com 5 variantes para as 5 fases.
 */
public class GuardianEntity {
    public static final double GW = 110, GH = 185;

    public static final String[] NAMES = {
        "Enki — Sábio das Águas",
        "Enki — Guardião Celestial",
        "Enki — Eremita Abissal",
        "Enki — Guia do Abismo",
        "Enki — Senhor das Profundezas"
    };

    public static final String[][] DIALOGUES = {
        // Fase 1 — Águas Claras
        {
            "Adapa, meu filho. As profundezas de Apsu acordaram em desarmonia.",
            "Aproxime-se e pressione E para receber a primeira tabuleta sagrada.",
            "Recupere as tabuletas e siga pelas ruínas até o portal da próxima câmara."
        },

        // Fase 2 — Cavernas de Coral
        { "O galeão naufragado repousa adiante. Abra o antigo baú com E para libertar o poder das bolhas." },

        // Fase 3 — Correntes Abissais
        { "As correntes apontam o fluxo sagrado. Domine a força das águas e alcance a próxima tabuleta." },

        // Fase 4 — Abismo Vulcânico
        { "Cuidado com o magma submarino. A última tabuleta está diante de você. O confronto se aproxima." },

        // Fase 5 — Templo Final de Apsu (Enki orienta para a batalha final)
        { "Kullullû bloqueia o coração de Apsu. Use suas bolhas místicas e desvie de seus ataques colossais!" }
    };

    /** Falas de Enki nas vinhetas entre menu/fases; guardiões falam ao encontrá-los na fase. */
    public static final String[][] ENKI_INTRO_DIALOGUES = {
        {
            "Adapa, o abismo despertou. Encontre os guardiões e recupere as cinco tabuletas sagradas.",
            "Pressione E perto de cada guardião para interagir e receber a tabuleta."
        },
        {"Procure o naufrágio. Abra o baú com E."},
        {"Siga as correntes até o guardião."},
        {"Desvie da lava. Busque a última tabuleta."},
        {"Kullullû está à frente. Mire bolhas e sobreviva."}
    };

    public static final String[] SPRITE_PATHS = {
        "sprites/enki/03_enki_npc.png",
        "sprites/enki/03_enki_var_celestial.png",
        "sprites/enki/03_enki_var_eremita.png",
        "sprites/enki/03_enki_var_celestial.png",
        "sprites/enki/03_enki_npc.png"
    };

    private int type; // 0=Atlante, 1=Coral, 2=Lamassu, 3=Oráculo, 4=Enki
    private double worldX, worldY;
    private boolean contacted = false;
    private boolean promptVisible = false;

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
    public void setContacted(boolean c) {
        this.contacted = c;
        if (c) {
            this.promptVisible = false;
        }
    }

    public boolean isPromptVisible() {
        return promptVisible && !contacted;
    }

    public void setPromptVisible(boolean visible) {
        this.promptVisible = visible && !contacted;
    }

    /**
     * Atualiza o estado de exibição do aviso contextual com histerese anti-pisca (US-013).
     * Distância < 150 unidades ativa o aviso; distância > 180 desativa.
     */
    public void updatePromptDistance(double heroCenterX, double heroCenterY) {
        if (contacted) {
            promptVisible = false;
            return;
        }
        double targetCenterX = worldX + GW / 2.0;
        double targetCenterY = worldY + GH / 2.0;
        double dx = heroCenterX - targetCenterX;
        double dy = heroCenterY - targetCenterY;
        double distSq = dx * dx + dy * dy;

        if (promptVisible) {
            if (distSq > 180.0 * 180.0) {
                promptVisible = false;
            }
        } else {
            if (distSq <= 150.0 * 150.0) {
                promptVisible = true;
            }
        }
    }
}
