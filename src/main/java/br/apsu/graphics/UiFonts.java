package br.apsu.graphics;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Constantes de fontes imutaveis para UI e RenderEngine.
 * Centraliza e pre-aloca fontes para evitar criacao de objetos por frame.
 */
public final class UiFonts {
    private UiFonts() {}

    public static final Font TITLE_80 = Font.font("Serif", FontWeight.BOLD, 80);
    public static final Font TITLE_64 = Font.font("Serif", FontWeight.BOLD, 64);
    public static final Font TEXT_26 = Font.font("Serif", 26);
    public static final Font OPT_SEL_28 = Font.font("Serif", FontWeight.BOLD, 28);
    public static final Font SUBTITLE_22 = Font.font("Serif", FontWeight.BOLD, 22);
    public static final Font OPT_UNSEL_22 = Font.font("Serif", FontWeight.NORMAL, 22);
    public static final Font TITLE_20 = Font.font("Serif", FontWeight.BOLD, 20);
    public static final Font TEXT_20 = Font.font("Serif", 20);
    public static final Font ROW_SEL_19 = Font.font("Serif", FontWeight.BOLD, 19);
    public static final Font ROW_UNSEL_19 = Font.font("Serif", FontWeight.NORMAL, 19);
    public static final Font TEXT_19 = Font.font("Serif", 19);
    public static final Font ALERT_18 = Font.font("Serif", FontWeight.BOLD, 18);
    public static final Font TEXT_16 = Font.font("Serif", 16);
    public static final Font HINT_15 = Font.font("Serif", 15);
    public static final Font LABEL_BOLD_14 = Font.font("Serif", FontWeight.BOLD, 14);
    public static final Font HINT_BOLD_13 = Font.font("Serif", FontWeight.BOLD, 13);
    public static final Font TEXT_13 = Font.font("Serif", 13);
    public static final Font TEXT_BOLD_12 = Font.font("Serif", FontWeight.BOLD, 12);
    public static final Font TEXT_12 = Font.font("Serif", 12);
    public static final Font TEXT_BOLD_11 = Font.font("Serif", FontWeight.BOLD, 11);
    public static final Font TEXT_11 = Font.font("Serif", 11);
    public static final Font RADAR_BOLD_10 = Font.font("Serif", FontWeight.BOLD, 10);
    public static final Font TEXT_10 = Font.font("Serif", 10);
    public static final Font DAMAGE_BOLD_9 = Font.font("Serif", FontWeight.BOLD, 9);
}
