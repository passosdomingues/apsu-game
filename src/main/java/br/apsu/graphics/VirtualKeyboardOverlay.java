package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.core.InputManager;
import br.apsu.tutorial.TutorialController;
import br.apsu.tutorial.TutorialStep;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * Teclado virtual translúcido glassmorphic interativo (APSU-U18B / APSU-U18C / APSU-U18D).
 *
 * Características:
 * 1. Renderização de layout com WASD, Setas de Direção (↑ ← ↓ →), Q (Sonar), E (Ação/Interagir),
 *    ESPAÇO (Disparar Bolhas), M (Alternar Tiro Único / Múltiplo), R (Reiniciar), P (Pausa),
 *    X (Pular Diálogos/Cutscenes/Tutorial), ENTER (Confirmar/Avançar) e ESC (Pausa/Voltar).
 * 2. Detecção em tempo real via InputManager: afundamento físico do keycap (2px), iluminação ciano/ouro
 *    e halo radiante quando a tecla é pressionada pelo jogador.
 * 3. Destaque pulsante (breathing highlight) em sintonia com o Tutorial da Fase 1 e 2:
 *    - P1 Passo 1 (Mergulhar): destaca [S] e [↓]
 *    - P1 Passo 2 (Desviar): destaca [A], [D], [←], [→]
 *    - P1 Passo 3 (Ecolocalização): destaca [Q]
 *    - P1 Passo 4 (Guardião): destaca [E] / [ESPAÇO]
 *    - P1 Tutorial / Diálogo ativo: destaca [X] para pular
 *    - P2 Baú e Poderes: destaca [ESPAÇO] (atacar) e [M] (alternar bolha tripla)
 * 4. Modo Showcase / Tutorial Animado no Menu Principal:
 *    Demonstração cíclica das ações do herói Adapa renderizado com sprite 3D autêntico
 *    (nadando, emitindo sonar, interagindo com relíquia, disparando bolhas purificadoras e
 *    alternando modo tático de disparo triplo via [M]) com teclas acendendo sincronizadas.
 * 5. Arquitetura Zero-Alloc: todas as teclas, geometrias e vetores são pré-alocados no construtor.
 */
public class VirtualKeyboardOverlay {

    // Paleta de cores imutáveis reutilizáveis (Zero-Alloc)
    private static final Color PANEL_BG = Palette.rgb(3, 12, 28, 0.82);
    private static final Color PANEL_BORDER = Palette.rgb(0, 180, 240, 0.45);
    private static final Color PANEL_BORDER_GLOW = Palette.rgb(0, 220, 255, 0.25);

    private static final Color KEY_BG_NORMAL = Palette.rgb(10, 26, 52, 0.88);
    private static final Color KEY_TOP_NORMAL = Palette.rgb(20, 50, 95, 0.95);
    private static final Color KEY_BORDER_NORMAL = Palette.rgb(40, 110, 190, 0.55);
    private static final Color KEY_TEXT_NORMAL = Palette.rgb(180, 225, 255, 0.95);
    private static final Color KEY_SUB_NORMAL = Palette.rgb(110, 175, 225, 0.80);

    private static final Color KEY_BG_PRESSED = Palette.rgb(0, 215, 255, 0.95);
    private static final Color KEY_TOP_PRESSED = Palette.rgb(160, 245, 255, 1.0);
    private static final Color KEY_BORDER_PRESSED = Palette.rgb(255, 255, 255, 1.0);
    private static final Color KEY_TEXT_PRESSED = Palette.rgb(2, 12, 28, 1.0);
    private static final Color KEY_SUB_PRESSED = Palette.rgb(5, 30, 60, 0.95);

    private static final Color KEY_HIGHLIGHT_HALO = Palette.rgb(255, 215, 0, 0.40);
    private static final Color KEY_HIGHLIGHT_BORDER = Palette.rgb(255, 220, 60, 0.95);

    private static final Color DEMO_BG = Palette.rgb(2, 8, 22, 0.92);
    private static final Color DEMO_WATER_TOP = Palette.rgb(0, 60, 130, 0.35);
    private static final Color DEMO_WATER_BOT = Palette.rgb(0, 20, 50, 0.85);

    /** Representação estática de um keycap no teclado virtual. */
    public static class KeyDef {
        public final String label;
        public final String sublabel;
        public final KeyCode[] codes;
        public final double relX, relY, width, height;
        public boolean pressed;
        public boolean highlighted;

        public KeyDef(String label, String sublabel, KeyCode[] codes, double relX, double relY, double width, double height) {
            this.label = label;
            this.sublabel = sublabel;
            this.codes = codes;
            this.relX = relX;
            this.relY = relY;
            this.width = width;
            this.height = height;
            this.pressed = false;
            this.highlighted = false;
        }
    }

    // Teclas do overlay de gameplay (layout compacto de 264x86 px)
    private final KeyDef[] hudKeys;

    // Teclas do showcase expandido do menu principal (layout detalhado de 340x135 px)
    private final KeyDef[] menuKeys;

    // Passos da demonstração cíclica do menu
    private static final int DEMO_STEPS = 5;
    private static final double DEMO_STEP_DURATION = 3.6; // segundos por demonstração

    public VirtualKeyboardOverlay() {
        // Layout do HUD de gameplay compacto (264 x 86)
        // Linha 0 (y=18): [ESC] [Q] [W] [E] [R] [P]   |  [↑]
        // Linha 1 (y=40): [ENT] [A] [S] [D] [X] [M]   | [←][↓][→]
        // Linha 2 (y=62): [    ESPAÇO: ATACAR    ]    | [M: TIRO 1/3]
        hudKeys = new KeyDef[] {
            new KeyDef("ESC", null, new KeyCode[]{KeyCode.ESCAPE}, 6, 18, 26, 19),
            new KeyDef("Q", null, new KeyCode[]{KeyCode.Q}, 35, 18, 22, 19),
            new KeyDef("W", null, new KeyCode[]{KeyCode.W}, 60, 18, 22, 19),
            new KeyDef("E", null, new KeyCode[]{KeyCode.E}, 85, 18, 22, 19),
            new KeyDef("R", null, new KeyCode[]{KeyCode.R}, 110, 18, 22, 19),
            new KeyDef("P", null, new KeyCode[]{KeyCode.P}, 135, 18, 22, 19),

            new KeyDef("ENT", null, new KeyCode[]{KeyCode.ENTER}, 6, 40, 26, 19),
            new KeyDef("A", null, new KeyCode[]{KeyCode.A}, 35, 40, 22, 19),
            new KeyDef("S", null, new KeyCode[]{KeyCode.S}, 60, 40, 22, 19),
            new KeyDef("D", null, new KeyCode[]{KeyCode.D}, 85, 40, 22, 19),
            new KeyDef("X", null, new KeyCode[]{KeyCode.X}, 110, 40, 22, 19),
            new KeyDef("M", null, new KeyCode[]{KeyCode.M}, 135, 40, 22, 19),

            new KeyDef("↑", null, new KeyCode[]{KeyCode.UP}, 184, 18, 22, 19),
            new KeyDef("←", null, new KeyCode[]{KeyCode.LEFT}, 159, 40, 22, 19),
            new KeyDef("↓", null, new KeyCode[]{KeyCode.DOWN}, 184, 40, 22, 19),
            new KeyDef("→", null, new KeyCode[]{KeyCode.RIGHT}, 209, 40, 22, 19),

            new KeyDef("ESPAÇO", "ATACAR", new KeyCode[]{KeyCode.SPACE}, 6, 62, 146, 19),
            new KeyDef("M: TIRO 1/3", null, new KeyCode[]{KeyCode.M}, 159, 62, 98, 19)
        };

        // Layout do showcase do Menu Principal (340 x 140)
        // Teclado mais espaçado e com sub-rótulos legíveis
        menuKeys = new KeyDef[] {
            new KeyDef("ESC", "PAUSA", new KeyCode[]{KeyCode.ESCAPE}, 8, 12, 34, 28),
            new KeyDef("Q", "SONAR", new KeyCode[]{KeyCode.Q}, 46, 12, 32, 28),
            new KeyDef("W", "CIMA", new KeyCode[]{KeyCode.W}, 82, 12, 32, 28),
            new KeyDef("E", "AÇÃO", new KeyCode[]{KeyCode.E}, 118, 12, 32, 28),
            new KeyDef("R", "RESET", new KeyCode[]{KeyCode.R}, 154, 12, 32, 28),
            new KeyDef("P", "PAUSA", new KeyCode[]{KeyCode.P}, 190, 12, 32, 28),

            new KeyDef("X", "PULAR", new KeyCode[]{KeyCode.X}, 8, 44, 34, 28),
            new KeyDef("A", "ESQ", new KeyCode[]{KeyCode.A}, 46, 44, 32, 28),
            new KeyDef("S", "BAIXO", new KeyCode[]{KeyCode.S}, 82, 44, 32, 28),
            new KeyDef("D", "DIR", new KeyCode[]{KeyCode.D}, 118, 44, 32, 28),
            new KeyDef("M", "MODO", new KeyCode[]{KeyCode.M}, 154, 44, 32, 28),
            new KeyDef("ENT", "OK", new KeyCode[]{KeyCode.ENTER}, 190, 44, 32, 28),

            // Cluster de Setas
            new KeyDef("↑", null, new KeyCode[]{KeyCode.UP}, 262, 12, 32, 28),
            new KeyDef("←", null, new KeyCode[]{KeyCode.LEFT}, 226, 44, 32, 28),
            new KeyDef("↓", null, new KeyCode[]{KeyCode.DOWN}, 262, 44, 32, 28),
            new KeyDef("→", null, new KeyCode[]{KeyCode.RIGHT}, 298, 44, 32, 28),

            // Barra de espaço e botões de atalho tático
            new KeyDef("ESPAÇO", "DISPARAR BOLHA DE PURIFICAÇÃO", new KeyCode[]{KeyCode.SPACE}, 8, 76, 214, 28),
            new KeyDef("M", "BOLHAS 1/3", new KeyCode[]{KeyCode.M}, 226, 76, 104, 28)
        };
    }

    /**
     * Renderiza o HUD do teclado translúcido durante a jogabilidade (canto inferior direito).
     */
    public void renderGameplayOverlay(GraphicsContext gc, InputManager input, TutorialController tutorial,
                                      GameContext context, double screenW, double screenH, double timeSeconds) {
        final double panelW = 264;
        final double panelH = 86;
        final double panelX = screenW - panelW - 18;
        final double panelY = screenH - panelH - 12;

        // Fundo glassmorphic translúcido
        gc.setFill(PANEL_BG);
        gc.fillRoundRect(panelX, panelY, panelW, panelH, 10, 10);
        gc.setStroke(PANEL_BORDER);
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(panelX, panelY, panelW, panelH, 10, 10);

        // Cabeçalho discreto
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(UiFonts.KEY_SUB_8);
        gc.setFill(Palette.rgb(80, 190, 240, 0.85));
        gc.fillText("CONTROLES EM TEMPO REAL", panelX + 8, panelY + 9);

        // Indicador de modo de bolha atual
        boolean hasBubble = (context.getHero() != null && context.getHero().hasBubblePower());
        boolean multiShot = hasBubble && context.getHero().isMultipleShotMode();
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(hasBubble ? (multiShot ? Palette.rgb(255, 180, 50, 0.95) : Palette.rgb(100, 240, 255, 0.95))
                             : Palette.rgb(120, 140, 160, 0.5));
        gc.fillText(hasBubble ? (multiShot ? "BOLHA: TRIPLA [M]" : "BOLHA: ÚNICA [M]") : "BOLHA: BLOQUEADA",
            panelX + panelW - 8, panelY + 9);

        // Determina destaques ativos de tutorial (P1 e P2)
        TutorialStep currentStep = (tutorial != null && tutorial.isActive()) ? tutorial.getCurrentStep() : null;
        boolean highlightS = (currentStep == TutorialStep.SWIM_DOWN);
        boolean highlightAD = (currentStep == TutorialStep.DODGE_ENEMY);
        boolean highlightQ = (currentStep == TutorialStep.READ_GPS);
        boolean highlightE = (currentStep == TutorialStep.TALK_GUARDIAN) || (context.getCurrentPhase() == 2 && context.canOpenChest());
        boolean highlightSpace = (currentStep == TutorialStep.TALK_GUARDIAN) || (context.getCurrentPhase() == 2 && context.canOpenChest());
        boolean highlightM = hasBubble && (context.getCurrentPhase() == 2);

        double pulse = 0.5 + 0.5 * Math.sin(timeSeconds * 6.0);

        // Renderiza cada tecla
        for (KeyDef key : hudKeys) {
            key.pressed = (input != null && input.isPressed(key.codes));
            key.highlighted = false;

            if (highlightS && (key.label.equals("S") || key.label.equals("↓"))) key.highlighted = true;
            if (highlightAD && (key.label.equals("A") || key.label.equals("D") || key.label.equals("←") || key.label.equals("→"))) key.highlighted = true;
            if (highlightQ && key.label.equals("Q")) key.highlighted = true;
            if (highlightE && key.label.equals("E")) key.highlighted = true;
            if (highlightSpace && key.label.startsWith("ESPAÇO")) key.highlighted = true;
            if (highlightM && key.label.contains("M")) key.highlighted = true;
            if (((tutorial != null && tutorial.isActive()) || (context != null && context.isOverlayActive())) && key.label.equals("X")) key.highlighted = true;

            drawKeycap(gc, panelX + key.relX, panelY + key.relY, key.width, key.height,
                key.label, key.sublabel, key.pressed, key.highlighted, pulse);
        }
    }

    /**
     * Renderiza o painel interativo de tutorial com animação no Menu Principal (lado esquerdo).
     */
    public void renderMenuShowcase(GraphicsContext gc, double x, double y, double width, double height, double timeSeconds) {
        // Painel Glassmorphic Card do Tutorial
        gc.setFill(PANEL_BG);
        gc.fillRoundRect(x, y, width, height, 16, 16);
        gc.setStroke(PANEL_BORDER);
        gc.setLineWidth(1.8);
        gc.strokeRoundRect(x, y, width, height, 16, 16);

        // Título e Emblema dos Apkallu
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(UiFonts.LABEL_BOLD_14);
        gc.setFill(Palette.GOLD);
        gc.fillText("✦  GUIA DOS APKALLU  ✦", x + width / 2.0, y + 22);

        gc.setFont(UiFonts.TEXT_11);
        gc.setFill(Palette.rgb(130, 200, 240, 0.95));
        gc.fillText("Domine os controles de navegação e purificação", x + width / 2.0, y + 40);

        // Determina o passo ativo da animação cíclica
        double totalDemoTime = DEMO_STEPS * DEMO_STEP_DURATION;
        double cycleTime = timeSeconds % totalDemoTime;
        int activeStep = (int) (cycleTime / DEMO_STEP_DURATION);
        double stepProgress = (cycleTime % DEMO_STEP_DURATION) / DEMO_STEP_DURATION;

        // Caixa de Visualização da Animação do Herói (Mini-Ocean Sandbox)
        double boxX = x + 12;
        double boxY = y + 56;
        double boxW = width - 24;
        double boxH = 145;

        gc.setFill(DEMO_BG);
        gc.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);
        gc.setStroke(Palette.rgb(0, 140, 220, 0.45));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(boxX, boxY, boxW, boxH, 12, 12);

        // Fundo aquático com raios de luz e bolhas
        drawDemoUnderwaterEnvironment(gc, boxX, boxY, boxW, boxH, timeSeconds);

        // Desenha a animação temática do herói de acordo com o passo atual
        drawHeroDemoAction(gc, boxX, boxY, boxW, boxH, activeStep, stepProgress, timeSeconds);

        // Barra de progresso do passo do tutorial
        double barY = boxY + boxH - 5;
        gc.setFill(Palette.rgb(15, 35, 65, 0.9));
        gc.fillRect(boxX + 6, barY, boxW - 12, 3);
        gc.setFill(Palette.GOLD);
        gc.fillRect(boxX + 6, barY, (boxW - 12) * stepProgress, 3);

        // Descrição do Passo Atual
        drawDemoStepDescription(gc, x + 14, boxY + boxH + 16, width - 28, activeStep);

        // Divisor decorativo
        double divY = y + height - 152;
        gc.setStroke(Palette.rgb(0, 180, 240, 0.25));
        gc.setLineWidth(1.0);
        gc.strokeLine(x + 20, divY, x + width - 20, divY);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(UiFonts.KEY_SUB_8);
        gc.setFill(Palette.rgb(100, 210, 255, 0.9));
        gc.fillText("TECLADO TRANSLÚCIDO EM TEMPO REAL", x + width / 2.0, divY + 10);

        // Teclado virtual expandido animado
        double kbdY = divY + 16;
        double pulse = 0.5 + 0.5 * Math.sin(timeSeconds * 7.0);

        for (KeyDef key : menuKeys) {
            // Teclas acendem de acordo com a ação demonstrada
            boolean demoActive = isKeyActiveInDemoStep(key.label, activeStep, stepProgress);
            key.pressed = demoActive;
            key.highlighted = demoActive;

            drawKeycap(gc, x + key.relX, kbdY + key.relY, key.width, key.height,
                key.label, key.sublabel, key.pressed, key.highlighted, pulse);
        }
    }

    /**
     * Renderiza o fundo marinho da caixa de demonstração com bolhas e luz.
     */
    private void drawDemoUnderwaterEnvironment(GraphicsContext gc, double bx, double by, double bw, double bh, double t) {
        // Cáusticas decorativas simples
        gc.setGlobalAlpha(0.20);
        gc.setFill(Palette.rgb(0, 180, 255, 0.4));
        for (int i = 0; i < 4; i++) {
            double cx = bx + (i * 75 + Math.sin(t * 1.5 + i) * 20) % bw;
            gc.fillOval(cx, by + 10, 45, bh - 20);
        }
        // Bolhas subindo
        gc.setFill(Palette.rgb(180, 235, 255, 0.45));
        for (int i = 0; i < 6; i++) {
            double bpx = bx + (i * 52 + Math.cos(t * 0.8 + i) * 15 + bw) % bw;
            double bpy = by + bh - (t * (25 + i * 5) + i * 30) % bh;
            double r = 2.0 + (i % 3);
            gc.fillOval(bpx - r, bpy - r, r * 2, r * 2);
        }
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Anima Adapa realizando cada uma das ações principais.
     */
    private void drawHeroDemoAction(GraphicsContext gc, double boxX, double boxY, double boxW, double boxH,
                                    int step, double progress, double timeSeconds) {
        double heroX = boxX + boxW / 2.0;
        double heroY = boxY + boxH / 2.0 + 8;

        switch (step) {
            case 0 -> {
                // Passo 0: NAVEGAÇÃO — Adapa nada ondulando suavemente e avança
                double swimOffset = Math.sin(progress * Math.PI * 4.0) * 45.0;
                double waveY = Math.cos(progress * Math.PI * 4.0) * 12.0;
                drawMiniAdapa(gc, heroX + swimOffset, heroY + waveY, true, false, timeSeconds);

                // Estelas de água
                gc.setStroke(Palette.rgb(0, 200, 255, 0.4));
                gc.setLineWidth(1.5);
                gc.strokeLine(heroX + swimOffset - 25, heroY + waveY, heroX + swimOffset - 45, heroY + waveY - 4);
                gc.strokeLine(heroX + swimOffset - 25, heroY + waveY + 6, heroX + swimOffset - 40, heroY + waveY + 8);
            }
            case 1 -> {
                // Passo 1: ECOLOCALIZAÇÃO — Adapa parado emitindo anéis dourados expansivos de sonar
                drawMiniAdapa(gc, heroX - 40, heroY, true, false, timeSeconds);

                // Ondas de eco expansivas
                double echoRadius = (progress * 130.0) % 110.0;
                double echoAlpha = Math.max(0.0, 1.0 - echoRadius / 110.0);
                gc.setStroke(Palette.rgb(255, 215, 0, echoAlpha * 0.9));
                gc.setLineWidth(2.2);
                gc.strokeOval(heroX - 40 - echoRadius, heroY - echoRadius, echoRadius * 2, echoRadius * 2);
                gc.setStroke(Palette.rgb(0, 220, 255, echoAlpha * 0.6));
                gc.strokeOval(heroX - 40 - echoRadius * 0.6, heroY - echoRadius * 0.6, echoRadius * 1.2, echoRadius * 1.2);

                // Relíquia ou inimigo revelado na escuridão
                double targetX = heroX + 65;
                gc.setFill(Palette.rgb(255, 215, 0, echoAlpha * 0.85));
                gc.fillRoundRect(targetX - 12, heroY - 14, 24, 28, 4, 4);
                gc.setStroke(Palette.rgb(255, 255, 255, echoAlpha));
                gc.strokeRoundRect(targetX - 12, heroY - 14, 24, 28, 4, 4);
            }
            case 2 -> {
                // Passo 2: INTERAÇÃO — Adapa se aproxima do Baú / Guardião Enki e ressoa com luz dourada
                drawMiniAdapa(gc, heroX - 50, heroY, true, false, timeSeconds);

                // Baú ou Guardião Enki
                double chestX = heroX + 45;
                gc.setFill(Palette.rgb(160, 100, 30, 0.95));
                gc.fillRoundRect(chestX - 18, heroY - 6, 36, 22, 6, 6);
                gc.setFill(Palette.rgb(255, 215, 0, 0.95));
                gc.fillOval(chestX - 4, heroY + 1, 8, 8);

                // Feixe sagrado de sabedoria
                double shimmer = 0.5 + 0.5 * Math.sin(timeSeconds * 8.0);
                gc.setStroke(Palette.rgb(255, 230, 80, shimmer * 0.8));
                gc.setLineWidth(2.0);
                gc.strokeLine(heroX - 30, heroY, chestX - 18, heroY);
            }
            case 3 -> {
                // Passo 3: COMBATE — Disparo de Bolhas de Purificação
                boolean attacking = (progress % 0.35) < 0.18;
                drawMiniAdapa(gc, heroX - 55, heroY, true, attacking, timeSeconds);

                // Bolhas voando
                for (int b = 0; b < 3; b++) {
                    double bx = heroX - 25 + ((progress * 180.0 + b * 55) % 150.0);
                    gc.setFill(Palette.rgb(160, 240, 255, 0.85));
                    gc.fillOval(bx - 6, heroY - 6, 12, 12);
                    gc.setStroke(Palette.rgb(0, 210, 255, 0.9));
                    gc.setLineWidth(1.5);
                    gc.strokeOval(bx - 6, heroY - 6, 12, 12);
                    gc.setFill(Palette.rgb(255, 255, 255, 0.9));
                    gc.fillOval(bx - 4, heroY - 4, 4, 4);
                }
            }
            case 4 -> {
                // Passo 4: TÁTICA MÚLTIPLA [M] — Alterna entre disparo único e leque triplo
                boolean isTriple = (progress >= 0.5);
                drawMiniAdapa(gc, heroX - 55, heroY, true, true, timeSeconds);

                if (isTriple) {
                    // 3 bolhas em leque
                    double bProgress = (progress - 0.5) * 2.0;
                    double bx = heroX - 20 + bProgress * 120.0;
                    drawBubble(gc, bx, heroY - bProgress * 25.0);
                    drawBubble(gc, bx, heroY);
                    drawBubble(gc, bx, heroY + bProgress * 25.0);
                } else {
                    // Bolha única concentrada frontal
                    double bProgress = progress * 2.0;
                    double bx = heroX - 20 + bProgress * 130.0;
                    drawBubble(gc, bx, heroY);
                }
            }
        }
    }

    private void drawBubble(GraphicsContext gc, double bx, double by) {
        gc.setFill(Palette.rgb(160, 240, 255, 0.85));
        gc.fillOval(bx - 7, by - 7, 14, 14);
        gc.setStroke(Palette.rgb(0, 220, 255, 0.95));
        gc.setLineWidth(1.4);
        gc.strokeOval(bx - 7, by - 7, 14, 14);
        gc.setFill(Palette.rgb(255, 255, 255, 0.95));
        gc.fillOval(bx - 4, by - 4, 4, 4);
    }

    /**
     * Renderiza o herói Adapa em miniatura com sprite real e animação fluida.
     */
    private void drawMiniAdapa(GraphicsContext gc, double cx, double cy, boolean right, boolean attack, double t) {
        // Halo bioluminescente sagrado dos Apkallu
        gc.setFill(Palette.rgb(0, 210, 255, 0.22));
        gc.fillOval(cx - 24, cy - 24, 48, 48);

        Image fallback = SpriteManager.getInstance().getImage("sprites/adapa/01_adapa_heroi.png");
        String seqDir = attack ? "sprites/adapa/01_adapa_nadando_disparo_bolhas" : "sprites/adapa/01_adapa_heroi";
        SpriteManager.FrameSeq seq = SpriteManager.getInstance().loadSequence(seqDir, 8.0, fallback);
        Image sprite = seq.get(t);
        if (sprite == null) sprite = fallback;

        double bob = Math.sin(t * 3.5) * 2.0;

        if (sprite != null) {
            double drawW = 54.0;
            double drawH = 54.0;
            gc.save();
            if (!right) {
                gc.translate(cx, cy + bob);
                gc.scale(-1, 1);
                gc.drawImage(sprite, -drawW / 2.0, -drawH / 2.0, drawW, drawH);
            } else {
                gc.drawImage(sprite, cx - drawW / 2.0, cy - drawH / 2.0 + bob, drawW, drawH);
            }
            gc.restore();
        } else {
            // Fallback estilizado do herói caso recursos gráficos não estejam disponíveis no ambiente
            gc.setFill(Palette.rgb(0, 160, 230, 0.95));
            gc.fillOval(cx - 8, cy - 18 + bob, 16, 16);
            gc.fillRoundRect(cx - 12, cy - 2 + bob, 24, 26, 6, 6);
        }

        // Se atacando, brilho adicional nos punhos/tridente
        if (attack) {
            gc.setFill(Palette.GOLD);
            gc.fillOval(cx + (right ? 22 : -26), cy + bob - 3, 6, 6);
        }
    }

    /**
     * Renderiza o texto explicativo do passo atual na demonstração do Menu.
     */
    private void drawDemoStepDescription(GraphicsContext gc, double x, double y, double w, int step) {
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);

        String title;
        String desc1;
        String desc2;

        switch (step) {
            case 0 -> {
                title = "1. NAVEGAÇÃO & MERGULHO [W, A, S, D / SETAS]";
                desc1 = "Mergulhe contra o empuxo natural e supere correntes.";
                desc2 = "Desvie de barreiras vulcânicas e peixes em patrulha.";
            }
            case 1 -> {
                title = "2. ECOLOCALIZAÇÃO MÍSTICA [Q]";
                desc1 = "Emite um pulso de sonar sagrado pelas águas de Apsu.";
                desc2 = "Revela posições de baús, guardiões e tabuletas ocultas.";
            }
            case 2 -> {
                title = "3. INTERAÇÃO SAGRADA [E]";
                desc1 = "Converse com os Guardiões Enki e acione mecanismos.";
                desc2 = "Abra baús de naufrágios e colete Tabuletas do Destino.";
            }
            case 3 -> {
                title = "4. COMBATE DE PURIFICAÇÃO [ESPAÇO]";
                desc1 = "Dispare bolhas místicas que dissipam a corrupção.";
                desc2 = "Purifique criaturas corrompidas e enfrente Kullullû.";
            }
            case 4 -> {
                title = "5. MODO TÁTICO DE DISPARO [M]";
                desc1 = "Alterne o ataque: Disparo Frontal Único ↔ Rajada Tripla!";
                desc2 = "Dispersão ampla ideal para cobrir áreas e encurralar chefes.";
            }
            default -> {
                title = "CONTROLES";
                desc1 = "";
                desc2 = "";
            }
        }

        gc.setFont(UiFonts.KEY_LABEL_11);
        gc.setFill(Palette.GOLD);
        gc.fillText(title, x, y);

        gc.setFont(UiFonts.TEXT_10);
        gc.setFill(Palette.rgb(200, 230, 255, 0.95));
        gc.fillText(desc1, x, y + 16);
        gc.fillText(desc2, x, y + 30);
    }

    /**
     * Determina se a tecla deve ser animada como pressionada na demonstração do menu.
     */
    private boolean isKeyActiveInDemoStep(String label, int step, double progress) {
        return switch (step) {
            case 0 -> {
                // Alterna WASD / Setas ritmicamente
                int phase = (int) (progress * 8.0) % 4;
                yield switch (phase) {
                    case 0 -> label.equals("W") || label.equals("↑");
                    case 1 -> label.equals("D") || label.equals("→");
                    case 2 -> label.equals("S") || label.equals("↓");
                    default -> label.equals("A") || label.equals("←");
                };
            }
            case 1 -> label.equals("Q");
            case 2 -> label.equals("E");
            case 3 -> label.startsWith("ESPAÇO");
            case 4 -> label.contains("M");
            default -> false;
        };
    }

    /**
     * Desenha um keycap individual realista com relevo 3D, sombra, brilho e texto.
     */
    private void drawKeycap(GraphicsContext gc, double kx, double ky, double kw, double kh,
                            String label, String sublabel, boolean pressed, boolean highlighted, double pulse) {
        double offsetY = pressed ? 2.0 : 0.0;
        double renderY = ky + offsetY;

        // Efeito de halo pulsante quando destacado
        if (highlighted && !pressed) {
            gc.setStroke(KEY_HIGHLIGHT_HALO);
            gc.setLineWidth(3.5 * pulse + 1.0);
            gc.strokeRoundRect(kx - 2, renderY - 2, kw + 4, kh + 4, 7, 7);
        }

        // Sombra inferior do keycap (quando não pressionado)
        if (!pressed) {
            gc.setFill(Palette.rgb(0, 8, 20, 0.75));
            gc.fillRoundRect(kx, renderY + 2.5, kw, kh, 6, 6);
        }

        // Fundo do keycap
        gc.setFill(pressed ? KEY_BG_PRESSED : KEY_BG_NORMAL);
        gc.fillRoundRect(kx, renderY, kw, kh, 5, 5);

        // Bisel superior (luz no topo do keycap)
        if (!pressed) {
            gc.setFill(KEY_TOP_NORMAL);
            gc.fillRoundRect(kx + 1, renderY + 1, kw - 2, kh * 0.45, 4, 4);
        } else {
            gc.setFill(KEY_TOP_PRESSED);
            gc.fillRoundRect(kx + 1, renderY + 1, kw - 2, kh * 0.45, 4, 4);
        }

        // Borda
        if (highlighted) {
            gc.setStroke(KEY_HIGHLIGHT_BORDER);
            gc.setLineWidth(1.6);
        } else {
            gc.setStroke(pressed ? KEY_BORDER_PRESSED : KEY_BORDER_NORMAL);
            gc.setLineWidth(1.0);
        }
        gc.strokeRoundRect(kx, renderY, kw, kh, 5, 5);

        // Texto do rótulo
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        if (sublabel != null && !sublabel.isEmpty()) {
            gc.setFont(UiFonts.KEY_LABEL_11);
            gc.setFill(pressed ? KEY_TEXT_PRESSED : KEY_TEXT_NORMAL);
            gc.fillText(label, kx + kw / 2.0, renderY + kh * 0.36);

            gc.setFont(UiFonts.KEY_SUB_8);
            gc.setFill(pressed ? KEY_SUB_PRESSED : KEY_SUB_NORMAL);
            gc.fillText(sublabel, kx + kw / 2.0, renderY + kh * 0.74);
        } else {
            gc.setFont(label.length() > 3 ? UiFonts.KEY_SPACE_10 : UiFonts.KEY_LABEL_11);
            gc.setFill(pressed ? KEY_TEXT_PRESSED : KEY_TEXT_NORMAL);
            gc.fillText(label, kx + kw / 2.0, renderY + kh / 2.0);
        }
    }
}
