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
            "Aproxime-se e pressione E para ouvir a sabedoria: a primeira tabuleta repousa velada nos recifes.",
            "Ela está oculta ao olhar comum: emita sua Ecolocalização [Q] para fazer a relíquia ressoar e se revelar."
        },

        // Fase 2 — Cavernas de Coral (Enki — Guardião Celestial)
        {
            "O antigo galeão guarda o legado de Eridu. Abra o baú com E para despertar o poder das bolhas.",
            "A segunda tabuleta está oculta nas sombras além dos corais. Use a Ecolocalização [Q] para revelar o artefato sagrado."
        },

        // Fase 3 — Correntes Abissais (Enki — Eremita Abissal)
        {
            "Nas trevas, a pressão testa você. As marés sepultaram a terceira tabuleta em uma fenda intermediária da câmara.",
            "Avance contra as correntes violentas e ative a Ecolocalização [Q] no leito rochoso para revelar o artefato."
        },

        // Fase 4 — Abismo Vulcânico (Enki — Guia do Abismo)
        {
            "O magma implacável oculta a quarta tabuleta sagrada próximo às chaminés hidrotermais de basalto.",
            "Fique em altitude segura, ative a Ecolocalização [Q] nos respiradouros e liberte a relíquia antes do portal."
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
            "Adapa, o abismo despertou. Ouça os guardiões e use a Ecolocalização [Q] para revelar as tabuletas ocultas.",
            "Recupere os Me sagrados para abrir os portais e restaurar o equilíbrio do oceano primordial."
        },
        {"Explore o galeão naufragado nas cavernas de coral, liberte o poder ancestral e use o eco para achar a tabuleta."},
        {"Domine as correntes submarinas das profundezas e use a ecolocalização nas fendas para encontrar a terceira tabuleta."},
        {"Desvie do magma vulcânico e sonde as chaminés hidrotermais com ecolocalização para resgatar a quarta tabuleta sagrada."},
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

    // APSU-V01: Aparição suave dos Enkis (fade-in e presença divina, zero-alloc)
    private double alphaEnki = 0.0;
    private double scaleEnki = 0.85;

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
    public double getAlphaEnki() { return alphaEnki; }
    public double getScaleEnki() { return scaleEnki; }
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
     * Atualiza o estado de exibição do aviso contextual com histerese anti-pisca (US-013)
     * e interpolação de fade-in/scale suave dos Enkis ao se aproximar (APSU-V01).
     * Distância < 800 unidades inicia a manifestação divina; < 150 ativa o prompt de diálogo.
     */
    public void updatePromptDistance(double heroCenterX, double heroCenterY) {
        updatePromptDistance(heroCenterX, heroCenterY, 1.0 / 60.0);
    }

    public void updatePromptDistance(double heroCenterX, double heroCenterY, double dt) {
        double targetCenterX = worldX + GW / 2.0;
        double targetCenterY = worldY + GH / 2.0;
        double dx = heroCenterX - targetCenterX;
        double dy = heroCenterY - targetCenterY;
        double distSq = dx * dx + dy * dy;

        // APSU-V01: Transição de 400ms (taxa de 2.5 por segundo)
        boolean nearEnki = distSq <= 800.0 * 800.0;
        if (nearEnki) {
            alphaEnki = Math.min(1.0, alphaEnki + 2.5 * dt);
            scaleEnki = Math.min(1.0, scaleEnki + (0.15 / 0.4) * dt);
        } else {
            alphaEnki = Math.max(0.0, alphaEnki - 2.5 * dt);
            scaleEnki = Math.max(0.85, scaleEnki - (0.15 / 0.4) * dt);
        }

        if (contacted) {
            promptVisible = false;
            return;
        }

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
