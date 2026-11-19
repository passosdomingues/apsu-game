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
        // Fase 1 — Águas Claras (Enki — Sábio das Águas)
        {
            "Adapa, meu filho. As profundezas de Apsu acordaram em desarmonia.",
            "Aproxime-se e pressione E para receber a primeira tabuleta sagrada dos Me.",
            "Recupere as tabuletas e siga pelas ruínas até o portal da próxima câmara."
        },

        // Fase 2 — Cavernas de Coral (Enki — Guardião Celestial)
        {
            "O antigo galeão guarda o legado de Eridu. Abra o baú com E para despertar as bolhas místicas.",
            "Use as correntes favoráveis e resgate a segunda tabuleta sagrada."
        },

        // Fase 3 — Correntes Abissais (Enki — Eremita Abissal)
        {
            "Nas trevas abissais, a pressão testa sua determinação. As águas fluem com fúria ancestral.",
            "Receba a terceira tabuleta sagrada e atravesse as marés traiçoeiras."
        },

        // Fase 4 — Abismo Vulcânico (Enki — Guia do Abismo)
        {
            "As fendas ardentes anunciam a proximidade do Templo Primordial. Desvie do magma implacável.",
            "Esta é a quarta tabuleta sagrada. Apenas o confronto decisivo resta adiante."
        },

        // Fase 5 — Templo Final de Apsu (Enki — Senhor das Profundezas)
        {
            "Kullullû ergueu-se para impedir a restauração dos Me. Mire suas bolhas e liberte Apsu!",
            "Mostre a bravura de Eridu e sele o destino das águas primordiais!"
        }
    };

    /** Falas de Enki nas vinhetas entre menu/fases; guardiões falam ao encontrá-los na fase. */
    public static final String[][] ENKI_INTRO_DIALOGUES = {
        {
            "Adapa, o abismo despertou. Encontre os guardiões e recupere as cinco tabuletas sagradas.",
            "Pressione E perto de cada guardião para interagir e receber a tabuleta."
        },
        {"Explore o galeão naufragado nas cavernas de coral e liberte o poder ancestral."},
        {"Domine as correntes submarinas das profundezas e alcance o eremita abissal."},
        {"Desvie do magma vulcânico e busque a última tabuleta sagrada antes do templo."},
        {"Kullullû aguarda no coração do templo. Sobreviva e restaure a harmonia de Apsu."}
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
