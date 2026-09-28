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
        { "Saudações, jovem Apkallu! Sou o Guardião Atlante desta passagem.",
          "A sabedoria dos antigos está gravada nestas tabuletas cuneiformes.",
          "Tome esta Tabuleta — ela restaura sua vida e revela as correntes.",
          "Encontre o Baú Ancestral no Navio Naufragado da Fase 2!" },

        // Fase 2 — Cavernas de Coral
        { "Os recifes falam, jovem herói... eu sou sua voz.",
          "O veneno de Kullullû corrói nossos corais sagrados há eras.",
          "Esta Tabuleta registra o caminho seguro pelas cavernas de coral.",
          "Avance com cautela — as correntes abissais estão logo à frente!" },

        // Fase 3 — Correntes Abissais
        { "Eu sou Lamassu, sentinela alada das correntes profundas.",
          "Estas águas vivem — as correntes são a respiração de Apsu.",
          "Use o Poder das Bolhas contra as criaturas que patrulham as correntes.",
          "O Abismo Vulcânico aguarda. As rochas se movem — observe seus padrões!" },

        // Fase 4 — Abismo Vulcânico
        { "Oráculo das Chamas Abissais... há muito não via um Apkallu vivo aqui.",
          "O magma de Kullullû transformou estas criaturas em bestas impiedosas.",
          "As arraias absorvem o calor vulcânico — bolhas as resfriará e atordoará.",
          "Persista. O Templo de Apsu está próximo — e Kullullû espera pelo combate final." },

        // Fase 5 — Templo Final de Apsu (Enki aparece em pessoa)
        { "Adapa... você chegou ao coração de Apsu. Eu sou Enki, Senhor das Águas.",
          "Kullullû era meu discípulo antes de ser corrompido pelo fogo das profundezas.",
          "As tabuletas que você coletou são os selos de sua derrota.",
          "Use tudo que aprendeu. Bolhas, correntes, o próprio oceano é sua arma. Vá!" }
    };

    /** Falas de Enki nas vinhetas entre menu/fases; guardiões falam ao encontrá-los na fase. */
    public static final String[][] ENKI_INTRO_DIALOGUES = {
        {"Adapa, atravesse as Águas Claras e encontre o Guardião Atlante. Ele guarda a primeira tabuleta.",
         "Confie no seu instinto e siga para as ruínas."},
        {"Os corais de Apsu estão enfraquecendo. Procure o Guardião do Coral e recupere a tabuleta.",
         "As cavernas mudam de forma; observe os caminhos abertos."},
        {"As correntes abissais escondem o caminho. Lamassu conhece essas águas e pode orientá-lo.",
         "Use as correntes a seu favor."},
        {"O calor do abismo corrompeu seus habitantes. Encontre o Oráculo e atravesse as rochas vulcânicas.",
         "Suas bolhas podem abrir uma passagem segura."},
        {"Você chegou ao Templo de Apsu. Kullullû espera no coração das ruínas.",
         "Quando estiver pronto, enfrentaremos juntos o último desafio."}
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
