package br.apsu.graphics;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import java.util.HashMap;
import java.util.Map;

/**
 * Paleta de cores e gradientes estaticos compartilhados.
 * Elimina chamadas a Color.web, Color.rgb e deriveColor durante o frame.
 */
public final class Palette {
    private Palette() {}

    private static final Map<String, Color> COLOR_CACHE = new HashMap<>(64);
    private static final Map<Long, Color> RGB_CACHE = new HashMap<>(64);

    public static Color color(String web) {
        return COLOR_CACHE.computeIfAbsent(web, Color::web);
    }

    public static Color rgb(int red, int green, int blue, double alpha) {
        long key = ((long) red << 24) | ((long) green << 16) | ((long) blue << 8)
            | Math.round(alpha * 255);
        return RGB_CACHE.computeIfAbsent(key, ignored -> Color.rgb(red, green, blue, alpha));
    }

    // Cores comuns pre-computadas
    public static final Color GOLD = color("#ffd700");
    public static final Color GOLD_LIGHT = color("#ffe066");
    public static final Color GOLD_BORDER = color("#ffd27a");
    public static final Color BLUE_SUBTITLE = color("#70b8d8");
    public static final Color BLUE_TITLE_SHADOW = Color.rgb(0, 60, 160, 0.28);
    public static final Color CARD_BG = Color.rgb(4, 16, 36, 0.88);
    public static final Color CARD_BG_DIALOGUE = Color.rgb(4, 10, 28, 0.94);
    public static final Color CARD_STROKE_DIALOGUE = color("#0e3888");
    public static final Color ENKI_NAME = color("#60a0e0");
    public static final Color MENU_HINT = color("#a8c9d8");
    public static final Color MENU_UNSELECTED = color("#78b4cc");
    public static final Color MENU_SELECTED_BG = Color.rgb(0, 100, 220, 0.32);
    public static final Color DIFF_PANEL_BG = Color.rgb(3, 12, 30, 0.97);
    public static final Color DIFF_PANEL_STROKE = color("#4db8ff");
    public static final Color DIFF_ROW_SELECTED_BG = Color.rgb(0, 100, 220, 0.38);
    public static final Color DIFF_HINT = color("#9ac6da");
    public static final Color STAT_LABEL = color("#a0c0e0");
    public static final Color STAT_BG = Color.rgb(8, 24, 48, 0.9);
    public static final Color STAT_SPEED = color("#4db8ff");
    public static final Color STAT_AGILITY = color("#44cc88");
    public static final Color STAT_RESIST = color("#ffaa44");
    public static final Color STAT_BUBBLE = color("#e060ff");
    public static final Color STAT_SPEED_STROKE = color("#4db8ff").deriveColor(0, 1, 1, 0.5);
    public static final Color STAT_AGILITY_STROKE = color("#44cc88").deriveColor(0, 1, 1, 0.5);
    public static final Color STAT_RESIST_STROKE = color("#ffaa44").deriveColor(0, 1, 1, 0.5);
    public static final Color STAT_BUBBLE_STROKE = color("#e060ff").deriveColor(0, 1, 1, 0.5);

    // Cores de HUD
    public static final Color HUD_BG = Color.rgb(0, 4, 14, 0.88);
    public static final Color HUD_STROKE = Color.rgb(0, 100, 200, 0.45);
    public static final Color HUD_LABEL = color("#80c0f0");
    public static final Color HUD_HP_BG = Color.rgb(30, 10, 14, 0.85);
    public static final Color HUD_HP_HIGH = color("#22dd66");
    public static final Color HUD_HP_MED = color("#ffaa00");
    public static final Color HUD_HP_LOW = color("#ff2244");
    public static final Color HUD_HP_HIGH_STROKE = color("#22dd66").deriveColor(0, 1, 1, 0.5);
    public static final Color HUD_HP_MED_STROKE = color("#ffaa00").deriveColor(0, 1, 1, 0.5);
    public static final Color HUD_HP_LOW_STROKE = color("#ff2244").deriveColor(0, 1, 1, 0.5);
    public static final Color HUD_HP_DIVIDER = Color.rgb(0, 20, 40, 0.7);
    public static final Color HUD_HP_INVULN_STROKE = color("#ff6b6b");
    public static final Color HUD_HP_TEXT = color("#c0e0ff");
    public static final Color HUD_DAMAGE_TEXT = color("#ff7777");
    public static final Color HUD_TABLET_SLOT = Color.rgb(40, 30, 10, 0.7);
    public static final Color HUD_POWER_ACTIVE = color("#40ff88");
    public static final Color HUD_POWER_LOCKED = color("#ff5555");
    public static final Color HUD_PHASE_INFO = color("#4a7090");
    public static final Color HUD_DEPTH_INFO = color("#72a8c8");
    public static final Color HUD_BUBBLE_MODE = color("#80b8d0");
    public static final Color HUD_KEYS_HINT = color("#9cc8df");

    // Radar
    public static final Color RADAR_BG = Color.rgb(1, 10, 27, 0.84);
    public static final Color RADAR_STROKE = Color.rgb(70, 185, 255, 0.65);
    public static final Color RADAR_TITLE = color("#80c0e0");
    public static final Color RADAR_ENEMY = color("#ff5c72");
    public static final Color RADAR_GUARDIAN = color("#ffd700");
    public static final Color RADAR_BOSS = color("#b050ff");

    // Dialogo Overlay
    public static final Color DIALOGUE_OVERLAY_BG = Color.rgb(4, 12, 28, 0.92);
    public static final Color DIALOGUE_OVERLAY_HINT = color("#80c0e0");

    // Fases e Boss
    public static final Color PHASE2_DIM = Color.rgb(1, 12, 27, 0.34);
    public static final Color CHEST_PROMPT_BG = Color.rgb(0, 12, 28, 0.88);
    public static final Color BOSS_TELEGRAPH_BG = Color.rgb(8, 16, 28, 0.92);
    public static final Color BOSS_TELEGRAPH_STROKE = Color.rgb(255, 190, 40, 0.95);
    public static final Color BOSS_TELEGRAPH_TEXT = Color.rgb(255, 210, 90);
    public static final Color BOSS_HP_BG = Color.rgb(20, 0, 0, 0.8);
    public static final Color BOSS_HP_STROKE = Color.rgb(255, 60, 60, 0.7);
    public static final Color BOSS_HP_HIGH = color("#ff4444");
    public static final Color BOSS_HP_LOW = color("#ff8800");
    public static final Color GUARDIAN_HALO = color("#40e0ff");
    public static final Color GUARDIAN_PROMPT_BG = Color.rgb(0, 8, 24, 0.90);
    public static final Color EXIT_UNLOCKED = color("#9cecff");
    public static final Color EXIT_LOCKED = color("#d6b77a");

    // Fim de jogo
    public static final Color WIN_GOLD = color("#ffd700");
    public static final Color OVER_RED = color("#ff2828");
    public static final Color WIN_TEXT = color("#88ffaa");
    public static final Color OVER_TEXT = color("#ff9090");
    public static final Color END_HERO = color("#80b8d0");

    // Gradientes pre-alocados
    public static final LinearGradient GRADIENT_MENU = new LinearGradient(
        0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0, color("#040c1e")),
        new Stop(0.5, color("#082040")),
        new Stop(1, color("#0a3058"))
    );

    public static final LinearGradient GRADIENT_DIALOGUE = new LinearGradient(
        0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0, color("#030a14")),
        new Stop(0.5, color("#081226")),
        new Stop(1, color("#0a1e3c"))
    );

    public static final LinearGradient GRADIENT_WIN = new LinearGradient(
        0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0, color("#041408")),
        new Stop(0.5, color("#0a2d10")),
        new Stop(1, color("#103a1a"))
    );

    public static final LinearGradient GRADIENT_OVER = new LinearGradient(
        0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0, color("#140408")),
        new Stop(0.5, color("#2a0810")),
        new Stop(1, color("#400a16"))
    );
}
