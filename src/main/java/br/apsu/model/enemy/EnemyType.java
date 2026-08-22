package br.apsu.model.enemy;

/**
 * Tipos de inimigos e suas dimensões reais de sprite.
 */
public enum EnemyType {
    PEIXE(0, 100, 142,
        "sprites/inimigos/04_peixe_sombrio_inimigo.png",
        "sprites/inimigos/04_peixe_sombrio_inimigo"),

    // B2-FIX: corrigido de 200×27 (desproporcional) para 160×85 (coerente)
    ENGUIA(1, 160, 85,
        "sprites/inimigos/04_inimigo_enguia_abissal.png",
        "sprites/inimigos/04_inimigo_enguia_abissal"),

    MEDUSA(2, 80, 135,
        "sprites/inimigos/04_inimigo_medusa_eletrica.png",
        "sprites/inimigos/04_inimigo_medusa_eletrica"),

    CARANGUEJO(3, 130, 90,
        "sprites/inimigos/04_inimigo_caranguejo_blindado.png",
        "sprites/inimigos/04_inimigo_caranguejo_blindado"),

    /** Arraia Abissal — Fase 4: grande, deslize lateral lento mas difícil de desviar */
    ARRAIAO(4, 200, 110,
        "sprites/inimigos/11_arraiao_abissal.png",
        "sprites/inimigos/11_arraiao_abissal"),

    /** Leviatã Menor — sub-boss da Fase 4, move rápido em diagonal */
    LEVIATA(5, 180, 160,
        "sprites/inimigos/12_leviata_menor.png",
        "sprites/inimigos/12_leviata_menor");

    private final int id;
    private final double width, height;
    private final String staticSprite, animDir;

    EnemyType(int id, double width, double height, String staticSprite, String animDir) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.staticSprite = staticSprite;
        this.animDir = animDir;
    }

    public int getId() { return id; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public String getStaticSprite() { return staticSprite; }
    public String getAnimDir() { return animDir; }

    public static EnemyType fromId(int id) {
        for (EnemyType t : values()) if (t.id == id) return t;
        return PEIXE;
    }
}
