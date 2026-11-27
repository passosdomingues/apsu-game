package br.apsu.core;

import br.apsu.core.events.EventBus;
import br.apsu.core.events.GameEvent;
import br.apsu.audio.AudioCue;
import br.apsu.cutscene.GuardianIntroductionController;
import br.apsu.cutscene.PhaseExitIntroductionController;
import br.apsu.tutorial.TutorialController;
import br.apsu.core.spatial.QuadTree;
import br.apsu.model.boss.BossEntity;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.enemy.EnemyType;
import br.apsu.model.environment.Difficulty;
import br.apsu.model.environment.OceanDepthProfile;
import br.apsu.model.environment.Projectile;
import br.apsu.model.environment.SceneryElement;
import br.apsu.model.guardian.GuardianEntity;
import br.apsu.model.hero.HeroEntity;
import br.apsu.model.hero.HeroType;
import br.apsu.model.particle.ParticleSystem;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import br.apsu.graphics.Palette;

import java.util.ArrayList;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Gerenciador mestre de estado, lógica e atualização das regras do jogo.
 * Sprint 5: 5 fases, Bug B3 corrigido (coordenadas de mundo consistentes),
 * mecânicas ambientais (correntes, pressão, obstáculos móveis, lava).
 */
public class GameContext {
    /** NOTA IMPORTANTE — SISTEMA DE COORDENADAS:
     * Herói, inimigos e projéteis operam em COORDENADAS DE MUNDO (worldX).
     * A câmera converte para tela APENAS na hora de renderizar.
     * Toda colisão lógica usa coordenadas de MUNDO. */
    public enum State { MENU, DIALOGUE, P1, P2, P3, P4, P5, PAUSE, WIN, OVER }
    private static final long MIN_BOSS_ATTACK_INTERVAL_NANOS = 650_000_000L;

    private State state = State.MENU;
    private State stateBeforePause = State.P1;
    private int pauseMenuSel = 0; // 0=Continuar, 1=Reiniciar Fase, 2=Menu Principal
    private Difficulty difficulty = Difficulty.MEDIO;
    private HeroType heroType = HeroType.GUARDIAO;
    private HeroEntity hero;
    private BossEntity boss;

    private int currentPhase = 0;
    private int phaseDone = 0;
    private int tabletsCollected = 0;
    private boolean backtrackingActive = false;
    private long lastBlockedExitAlertNanos = -10_000_000_000L;
    private static final int TOTAL_TABLETS = 5;

    private final List<EnemyEntity> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<GuardianEntity> guardians = new ArrayList<>();
    private final List<SceneryElement> sceneryElements = new ArrayList<>();
    private br.apsu.model.environment.SacredTabletEntity sacredTablet;
    private final ParticleSystem particleSystem = new ParticleSystem();

    private final QuadTree<EnemyEntity> enemyQuadTree = new QuadTree<>(0, new QuadTree.Bounds(0, 0, 4000, 768), new QuadTree.BoundingBoxProvider<>() {
        @Override public double getX(EnemyEntity e) { return e.getWorldX(); }
        @Override public double getY(EnemyEntity e) { return e.getCurrentY(); }
        @Override public double getWidth(EnemyEntity e) { return e.getType().getWidth(); }
        @Override public double getHeight(EnemyEntity e) { return e.getType().getHeight(); }
    });
    private final List<EnemyEntity> nearbyEnemiesBuffer = new ArrayList<>();

    private final InputManager inputManager = new InputManager();
    private final Camera camera = new Camera(1366, 4000);
    private final HeroController heroController = new HeroController();
    private final br.apsu.cutscene.CutsceneController cutsceneController = new br.apsu.cutscene.CutsceneController();
    private final GuardianIntroductionController guardianIntroductionController = new GuardianIntroductionController();
    private final PhaseExitIntroductionController phaseExitIntroductionController = new PhaseExitIntroductionController();
    private final TutorialController tutorialController = new TutorialController();
    private final br.apsu.echo.EchoController echoController = new br.apsu.echo.EchoController();
    private final br.apsu.echo.EchoWaveRenderer echoWaveRenderer = new br.apsu.echo.EchoWaveRenderer();
    private double renderAlpha = 1.0;
    private final SaveManager saveManager;
    private final EventBus eventBus;

    // Baú (Fase 2)
    private double chestWX = 1600, chestWY = 768 - 170;
    private boolean chestOpen = false;
    private long chestTime = 0;
    private boolean chestPromptVisible = false;

    // Menu & Diálogos
    private int menuSel = 0; // 0=Difficulty, 1=Start Game
    private boolean difficultyMenuOpen;
    private int difficultyMenuSel = Difficulty.MEDIO.ordinal();
    private int dlgPhase = 0;
    private int dlgIdx = 0;

    // Overlay de diálogo
    private boolean overlayActive = false;
    private int overlayIdx = 0;
    private String overlayName;
    private String[] overlayLines;
    private long overlayCharTime;
    private int overlayCharsShown;
    private Runnable overlayOnComplete;
    private int overlayGuardianType = 0;

    private String alertMessage = "";
    private long alertTime = 0;
    private long phaseStartTime = 0;
    private int phaseHits = 0;
    private long nanoTime = 0;
    private final AdaptiveDifficulty adaptiveDifficulty = new AdaptiveDifficulty();
    private boolean p1GuardianCutsceneTriggered = false;
    private boolean p2ChestCutsceneTriggered = false;

    // Mecânicas ambientais ativas
    private boolean inCurrent = false;
    private double currentFx = 0, currentFy = 0;
    private boolean inPressureZone = false;
    private double pressureBuoyMult = 1.0;

    public GameContext() {
        this(new SaveManager(), new EventBus());
    }

    /** Construtor injetável para manter persistência isolada em testes. */
    public GameContext(SaveManager saveManager) {
        this(saveManager, new EventBus());
    }

    public GameContext(SaveManager saveManager, EventBus eventBus) {
        this.saveManager = saveManager;
        this.eventBus = eventBus;
        SaveManager.SaveData saved = saveManager.load().orElseGet(saveManager::newGame);
        this.difficulty = Difficulty.valueOf(saved.difficulty());
        this.phaseDone = Math.max(0, saved.unlockedPhase() - 1);
        this.hero = new HeroEntity(heroType, difficulty.getInitialHP());
        this.eventBus.subscribe(br.apsu.core.events.GameEvent.Type.CUTSCENE_REQUESTED, event -> {
            if (event.payload() instanceof br.apsu.cutscene.CutsceneRequest req) {
                cutsceneController.enqueue(req, camera.getX() + 1366 * 0.3);
            }
        });
    }

    public void cycleDiff(int dir) {
        Difficulty[] vals = Difficulty.values();
        setDifficulty(vals[(difficulty.ordinal() + dir + vals.length) % vals.length]);
        hero.heal(difficulty.getInitialHP());
        persistProgress();
    }

    public void cycleHero(int dir) {
        // Adapa uses the principal model for every new game.
    }

    public void onKey(KeyCode k) {
        if (cutsceneController.isActive()) {
            if (k == KeyCode.SPACE || k == KeyCode.ENTER || k == KeyCode.ESCAPE) {
                cutsceneController.skip(hero.getX());
                return;
            }
        }
        if (overlayActive) {
            if (k == KeyCode.ESCAPE) {
                overlayActive = false;
                if (overlayOnComplete != null) overlayOnComplete.run();
                return;
            }
            if (k == KeyCode.SPACE || k == KeyCode.ENTER || k == KeyCode.E || k == KeyCode.F) {
                advanceOverlay();
            }
            return;
        }

        switch (state) {
            case MENU -> menuKey(k);
            case DIALOGUE -> {
                if (k == KeyCode.ESCAPE) {
                    advancePhaseFromDialogue();
                    return;
                }
                if (k == KeyCode.SPACE || k == KeyCode.ENTER || k == KeyCode.E || k == KeyCode.F) {
                    String[] currentDlg = GuardianEntity.ENKI_INTRO_DIALOGUES[
                        Math.min(dlgPhase, GuardianEntity.ENKI_INTRO_DIALOGUES.length - 1)];
                    if (++dlgIdx >= currentDlg.length) {
                        advancePhaseFromDialogue();
                    }
                    playSound(AudioCue.DIALOGUE_ADVANCE);
                }
            }
            case P1, P2, P3, P4, P5 -> {
                if (k == KeyCode.ESCAPE || k == KeyCode.P) {
                    if (state == State.P1 && tutorialController.isActive() && k == KeyCode.ESCAPE) {
                        tutorialController.skip();
                        showAlert("Tutorial pulado! Encontre Enki nas águas claras.");
                        return;
                    }
                    pauseGame();
                } else if (k == KeyCode.R) {
                    restartCurrentPhase();
                } else if (k == KeyCode.M && hero.hasBubblePower()) {
                    hero.toggleMultipleShotMode();
                    playSound(AudioCue.MENU_CONFIRM);
                    showAlert(hero.isMultipleShotMode() ? "Modo de Ataque: Rajada Tripla [M]" : "Modo de Ataque: Bolha Única [M]");
                } else if (k == KeyCode.Q) {
                    if (echoController.trigger()) {
                        eventBus.publish(new GameEvent(
                            GameEvent.Type.ECHO_PINGED,
                            new br.apsu.echo.EchoPayload(
                                br.apsu.echo.EchoController.ECHO_DURATION_SECONDS,
                                br.apsu.echo.EchoController.COOLDOWN_DURATION_SECONDS
                            ),
                            hero.getX(), hero.getY()
                        ));
                    }
                }
            }
            case PAUSE -> {
                if (k == KeyCode.ESCAPE || k == KeyCode.P) {
                    resumeGame();
                } else if (k == KeyCode.R) {
                    restartCurrentPhase();
                } else if (k == KeyCode.M) {
                    returnToMenu();
                } else if (k == KeyCode.UP || k == KeyCode.W) {
                    pauseMenuSel = (pauseMenuSel - 1 + 3) % 3;
                    playSound(AudioCue.MENU_NAVIGATE);
                } else if (k == KeyCode.DOWN || k == KeyCode.S) {
                    pauseMenuSel = (pauseMenuSel + 1) % 3;
                    playSound(AudioCue.MENU_NAVIGATE);
                } else if (k == KeyCode.ENTER || k == KeyCode.SPACE || k == KeyCode.E) {
                    switch (pauseMenuSel) {
                        case 0 -> resumeGame();
                        case 1 -> restartCurrentPhase();
                        case 2 -> returnToMenu();
                    }
                }
            }
            case WIN, OVER -> {
                if (k == KeyCode.R) {
                    restartCurrentPhase();
                } else if (k == KeyCode.SPACE || k == KeyCode.ENTER || k == KeyCode.ESCAPE) {
                    returnToMenu();
                }
            }
        }
    }

    /** Avança para a fase correta após o diálogo. */
    private void advancePhaseFromDialogue() {
        switch (dlgPhase) {
            case 0 -> {
                startP1();
                if (!p1GuardianCutsceneTriggered && !guardians.isEmpty()) {
                    p1GuardianCutsceneTriggered = true;
                    GuardianEntity g = guardians.get(0);
                    eventBus.publish(new GameEvent(
                        GameEvent.Type.CUTSCENE_REQUESTED,
                        new br.apsu.cutscene.CutsceneRequest(
                            g.getWorldX() + GuardianEntity.GW / 2.0,
                            br.apsu.cutscene.PhaseCutsceneConfig.P1_INTRO_DURATION,
                            "Primeiro Guardiao"
                        ),
                        0, 0
                    ));
                }
            }
            case 1 -> {
                startP2();
                if (!p2ChestCutsceneTriggered && !chestOpen && !hero.hasBubblePower()) {
                    p2ChestCutsceneTriggered = true;
                    eventBus.publish(new GameEvent(
                        GameEvent.Type.CUTSCENE_REQUESTED,
                        new br.apsu.cutscene.CutsceneRequest(
                            chestWX,
                            br.apsu.cutscene.PhaseCutsceneConfig.P2_CHEST_DURATION,
                            "Bau Naufragio"
                        ),
                        0, 0
                    ));
                }
            }
            case 2 -> startP3();
            case 3 -> startP4();
            case 4 -> startP5();
        }
    }

    private void menuKey(KeyCode k) {
        boolean navigated = false;
        if (difficultyMenuOpen) {
            if (k == KeyCode.UP || k == KeyCode.W) {
                difficultyMenuSel = (difficultyMenuSel - 1 + Difficulty.values().length) % Difficulty.values().length;
                navigated = true;
            } else if (k == KeyCode.DOWN || k == KeyCode.S) {
                difficultyMenuSel = (difficultyMenuSel + 1) % Difficulty.values().length;
                navigated = true;
            } else if (k == KeyCode.ESCAPE || k == KeyCode.LEFT) {
                difficultyMenuOpen = false;
                navigated = true;
            } else if (k == KeyCode.ENTER || k == KeyCode.SPACE || k == KeyCode.E) {
                setDifficulty(Difficulty.values()[difficultyMenuSel]);
                hero.heal(difficulty.getInitialHP());
                difficultyMenuOpen = false;
                playSound(AudioCue.MENU_CONFIRM);
                persistProgress();
                return;
            }
            if (navigated) playSound(AudioCue.MENU_NAVIGATE);
            return;
        }
        int totalOpts = canContinue() ? 3 : 2;
        if (k == KeyCode.UP || k == KeyCode.W) { menuSel = (menuSel - 1 + totalOpts) % totalOpts; navigated = true; }
        if (k == KeyCode.DOWN || k == KeyCode.S) { menuSel = (menuSel + 1) % totalOpts; navigated = true; }
        if (navigated) playSound(AudioCue.MENU_NAVIGATE);
        if (k == KeyCode.ENTER || k == KeyCode.SPACE || k == KeyCode.E) {
            if (canContinue()) {
                switch (menuSel) {
                    case 0 -> continueGame();
                    case 1 -> { phaseDone = 0; reset(); transitionToPhase(1); }
                    case 2 -> { difficultyMenuOpen = true; difficultyMenuSel = difficulty.ordinal(); }
                    default -> playSound(AudioCue.MENU_CONFIRM);
                }
            } else {
                switch (menuSel) {
                    case 0 -> { difficultyMenuOpen = true; difficultyMenuSel = difficulty.ordinal(); }
                    case 1 -> { phaseDone = 0; reset(); transitionToPhase(1); }
                    default -> playSound(AudioCue.MENU_CONFIRM);
                }
            }
            playSound(AudioCue.MENU_CONFIRM);
        }
    }

    public void reset() {
        hero.setMaxHP(difficulty.getInitialHP());
        hero.heal(difficulty.getInitialHP());
        hero.setHasBubblePower(false);
        hero.resetPosition(100, 768 / 2.0 - HeroEntity.HH / 2.0);
        tabletsCollected = 0;
        camera.setX(0);
        camera.clearShake();
        backtrackingActive = false;
        lastBlockedExitAlertNanos = -10_000_000_000L;
        chestOpen = false;
        chestPromptVisible = false;
        enemies.clear();
        projectiles.clear();
        guardians.clear();
        sceneryElements.clear();
        particleSystem.clear();
        alertMessage = "";
        phaseHits = 0;
        overlayActive = false;
        overlayLines = null;
        overlayOnComplete = null;
        currentPhase = 0;
        boss = null;
        sacredTablet = null;
        inCurrent = false;
        inPressureZone = false;
        p1GuardianCutsceneTriggered = false;
        p2ChestCutsceneTriggered = false;
        guardianIntroductionController.reset();
        phaseExitIntroductionController.reset();
        tutorialController.reset();
        cutsceneController.stop();
        echoController.reset();
        echoWaveRenderer.reset();
    }

    public void transitionToPhase(int targetPhase) {
        dlgPhase = targetPhase - 1;
        dlgIdx = 0;
        persistProgress();
        advancePhaseFromDialogue();
    }

    public boolean canContinue() {
        return phaseDone > 0 && phaseDone < 5;
    }

    public void continueGame() {
        int target = Math.min(5, Math.max(1, phaseDone + 1));
        hero.heal(difficulty.getInitialHP());
        camera.clearShake();
        if (target >= 2) {
            hero.setHasBubblePower(true);
        } else {
            hero.setHasBubblePower(false);
        }
        tabletsCollected = Math.max(0, target - 1);
        transitionToPhase(target);
        playSound(AudioCue.MENU_CONFIRM);
    }

    public void pauseGame() {
        if (state == State.P1 || state == State.P2 || state == State.P3 || state == State.P4 || state == State.P5) {
            stateBeforePause = state;
            state = State.PAUSE;
            pauseMenuSel = 0;
            playSound(AudioCue.MENU_CONFIRM);
        }
    }

    public void resumeGame() {
        if (state == State.PAUSE) {
            state = stateBeforePause;
            playSound(AudioCue.MENU_CONFIRM);
        }
    }

    public void returnToMenu() {
        if (state == State.P5) publishBossMusic(false);
        reset();
        if (phaseDone >= 5) {
            phaseDone = 0;
        }
        state = State.MENU;
        menuSel = canContinue() ? 0 : 1;
        playSound(AudioCue.MENU_CONFIRM);
    }

    public void restartCurrentPhase() {
        if (state == State.P5) publishBossMusic(false);
        int target = (currentPhase >= 1 && currentPhase <= 5) ? currentPhase : (phaseDone > 0 ? Math.min(5, phaseDone + 1) : 1);
        hero.heal(difficulty.getInitialHP());
        camera.clearShake();
        if (target >= 2) {
            hero.setHasBubblePower(true);
        } else {
            hero.setHasBubblePower(false);
        }
        tabletsCollected = Math.max(0, target - 1);
        switch (target) {
            case 1 -> startP1();
            case 2 -> startP2();
            case 3 -> startP3();
            case 4 -> startP4();
            case 5 -> startP5();
            default -> startP1();
        }
        playSound(AudioCue.MENU_CONFIRM);
    }

    public int getPauseMenuSel() { return pauseMenuSel; }
    public void setPauseMenuSel(int pauseMenuSel) { this.pauseMenuSel = pauseMenuSel; }
    public State getStateBeforePause() { return stateBeforePause; }

    // =========================================================
    // FASE 1 — Águas Claras (introdução, enguia corrigida)
    // =========================================================
    public void startP1() {
        boss = null;
        enemies.clear(); projectiles.clear(); guardians.clear(); sceneryElements.clear();
        particleSystem.clear(); echoWaveRenderer.reset();
        camera.setX(0); hero.beginPhaseAt(100, 768 / 2.0);
        double spd = 1.0;
        double gX = (difficulty == Difficulty.DIFICIL) ? 400 : 900;
        guardians.add(new GuardianEntity(0, gX, 768 - GuardianEntity.GH - 60));

        enemies.add(new EnemyEntity(EnemyType.PEIXE,     1200, 768 * .44, spd,       65));
        enemies.add(new EnemyEntity(EnemyType.ENGUIA,    1900, 768 * .30, spd * 1.3, 80));  // B2-FIX: escala corrigida no enum
        enemies.add(new EnemyEntity(EnemyType.MEDUSA,    2700, 768 * .55, spd,       70));
        enemies.add(new EnemyEntity(EnemyType.PEIXE,     3200, 768 * .35, spd * 1.1, 55));
        enemies.add(new EnemyEntity(EnemyType.CARANGUEJO,3500, 768 * .50, spd,       60));

        // Gêiser na Fase 1 como tutorial de mecânica ambiental
        sceneryElements.add(new SceneryElement(SceneryElement.Type.GEYSER, 2400, 768 - 200, 70, 200));
        // Recifes de coral decorativos no leito de Águas Claras com variação orgânica (APSU-A02)
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 1420, 768 - 240, 125, 240));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 1950, 768 - 210, 110, 210));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 2980, 768 - 270, 145, 270));

        currentPhase = 1;
        sacredTablet = new br.apsu.model.environment.SacredTabletEntity(1, 2350.0, 580.0);
        publishAudioDepth();
        state = State.P1; phaseStartTime = nanoTime; phaseHits = 0; adaptiveDifficulty.beginPhase();
        if (!br.apsu.graphics.GraphicsDiagnostics.isScene3dSupported()) {
            showAlert("Modo 2D: 3D indisponível neste equipamento");
        } else if (difficulty == Difficulty.DIFICIL) {
            showAlert("Fase 1 (DIFÍCIL) — O portal final estará selado até você resgatar a Tabuleta!");
        } else {
            showAlert("Fase 1 — Aguas Claras | Encontre Enki e colete a Tabuleta Sagrada!");
        }
    }

    // =========================================================
    // FASE 2 — Cavernas de Coral (bolhas desbloqueadas aqui)
    // =========================================================
    public void startP2() {
        tutorialController.skip();
        boss = null;
        enemies.clear(); projectiles.clear(); guardians.clear(); sceneryElements.clear();
        particleSystem.clear(); echoWaveRenderer.reset();
        camera.setX(0); hero.beginPhaseAt(100, 768 / 2.0);
        double spd = 1.0;

        // === REDESIGN 2026-08-20: inimigos nas SALAS ABERTAS, não nas passagens ===
        // Sala 1 (200-480px livre antes do 1º coral): caranguejo territorial
        enemies.add(new EnemyEntity(EnemyType.CARANGUEJO, 320,  768 * .55, spd,       35));
        // Sala 2 (entre corais 2 e 3, 1000-1400px): medusa pulsante
        enemies.add(new EnemyEntity(EnemyType.MEDUSA,     1200, 768 * .38, spd,       90));
        // Sala 3 (entre corais 4 e 5, ~2300px): enguia sinuosa
        enemies.add(new EnemyEntity(EnemyType.ENGUIA,     2350, 768 * .30, spd * 1.1, 75));
        // Sala final (3600-3800px): guarda antes do portal
        enemies.add(new EnemyEntity(EnemyType.CARANGUEJO, 3600, 768 * .48, spd,       45));

        double gX = (difficulty == Difficulty.DIFICIL) ? 600 : 1400;
        guardians.add(new GuardianEntity(1, gX, 768 - GuardianEntity.GH - 60));

        // 1 gêiser no final — tutorial de mecânica, não obstáculo punitivo
        sceneryElements.add(new SceneryElement(SceneryElement.Type.GEYSER, 3200, 768 - 200, 70, 200));

        // === 6 colunas de coral 3D em formação orgânica no leito das cavernas (APSU-A02) ===
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 620,  768 - 310, 130, 310));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 1080, 768 - 280, 145, 280));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 1530, 768 - 330, 120, 330));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 2120, 768 - 295, 135, 295));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 2740, 768 - 325, 150, 325));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 3420, 768 - 270, 125, 270));

        chestWX = 1800; chestWY = 768 - 170; // baú na sala aberta entre corais 2 e 3
        currentPhase = 2;
        sacredTablet = new br.apsu.model.environment.SacredTabletEntity(2, 2650.0, 520.0);
        publishAudioDepth();
        state = State.P2; phaseStartTime = nanoTime; phaseHits = 0; adaptiveDifficulty.beginPhase();
        showAlert("Fase 2 — Cavernas de Coral | Desperte o Poder das Bolhas no Galeão Naufragado!");
    }

    // =========================================================
    // FASE 3 — Correntes Abissais (nova mecânica principal)
    // =========================================================
    public void startP3() {
        tutorialController.skip();
        boss = null;
        enemies.clear(); projectiles.clear(); guardians.clear(); sceneryElements.clear();
        particleSystem.clear(); echoWaveRenderer.reset();
        camera.setX(0); hero.beginPhaseAt(100, 768 / 2.0);
        double spd = 1.0;

        // === REDESIGN 2026-08-20: 4 inimigos (não 6), bem separados, cada um em
        //     território próprio para o jogador processar antes do próximo ===
        enemies.add(new EnemyEntity(EnemyType.PEIXE,     700,  768 * .42, spd * 1.2, 55));
        enemies.add(new EnemyEntity(EnemyType.ENGUIA,    1600, 768 * .28, spd * 1.3, 80));
        enemies.add(new EnemyEntity(EnemyType.MEDUSA,    2500, 768 * .55, spd * 1.1, 90));
        enemies.add(new EnemyEntity(EnemyType.CARANGUEJO,3400, 768 * .35, spd,       70));

        double gX = (difficulty == Difficulty.DIFICIL) ? 600 : 1100;
        guardians.add(new GuardianEntity(2, gX, 768 - GuardianEntity.GH - 60));

        // === REDESIGN: correntes SEQUENCIAIS, espaçadas ~1000px cada ===
        // Cada corrente ocupa um terço da jornada — o jogador domina uma antes de enfrentar outra.
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CURRENT,  900, 80, 200, 620).withCurrent(-2.2, 0));     // zona 1: empurra à esquerda
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CURRENT, 2000, 180, 200, 520).withCurrent(0, 1.8));     // zona 2: afunda devagar
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CURRENT, 3100, 60, 200, 600).withCurrent(2.5, -0.8));  // zona 3: diagonal suave

        // 1 zona de pressão (rara = mais impactante quando aparece)
        sceneryElements.add(new SceneryElement(SceneryElement.Type.PRESSURE_ZONE, 1500, 0, 180, 768).withBuoyancy(.90));

        // 2 obstáculos móveis bem espaçados (não 4 sobrepostos)
        sceneryElements.add(new SceneryElement(SceneryElement.Type.MOVING_OBSTACLE, 1250, 280, 100, 80).withOscillation(0, 150, 1.1, 0.0));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.MOVING_OBSTACLE, 2700, 180, 100, 80).withOscillation(100, 0, 0.9, 1.2));

        // Gêiseres: 1 antes da corrente central, 1 no final como desafio
        sceneryElements.add(new SceneryElement(SceneryElement.Type.GEYSER, 1700, 768 - 200, 80, 200));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.GEYSER, 3600, 768 - 200, 80, 200));

        // Formações de coral no leito profundo com escala e espaçamento orgânico (APSU-A02)
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 650,  768 - 180, 115, 180));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 2350, 768 - 215, 130, 215));

        currentPhase = 3;
        sacredTablet = new br.apsu.model.environment.SacredTabletEntity(3, 2400.0, 600.0);
        publishAudioDepth();
        state = State.P3; phaseStartTime = nanoTime; phaseHits = 0; adaptiveDifficulty.beginPhase();
        showAlert("Fase 3 — Correntes Abissais | As águas vivem — lute contra a maré!");
    }

    // =========================================================
    // FASE 4 — Abismo Vulcânico (novos inimigos + lava)
    // =========================================================
    public void startP4() {
        tutorialController.skip();
        boss = null;
        enemies.clear(); projectiles.clear(); guardians.clear(); sceneryElements.clear();
        particleSystem.clear(); echoWaveRenderer.reset();
        camera.setX(0); hero.beginPhaseAt(100, 768 / 2.0);
        double spd = 1.0;

        // === REDESIGN: seis inimigos separados, Leviatã como momento especial ===
        // Abertura: arraião solitário para estabelecer o tema da fase
        enemies.add(new EnemyEntity(EnemyType.ARRAIAO,    600,  768 * .42, spd,       90));
        // Ato 1: peixe rápido + medusa
        enemies.add(new EnemyEntity(EnemyType.PEIXE,      1200, 768 * .28, spd * 1.3, 55));
        enemies.add(new EnemyEntity(EnemyType.MEDUSA,     1900, 768 * .55, spd * 1.2, 90));
        // === LEVIATÃ: 600px de espaço exclusivo (2400-3000px), isolado como sub-boss ===
        enemies.add(new EnemyEntity(EnemyType.LEVIATA,   2650, 768 * .38, spd * 1.3, 130));
        // Ato 3: polvo abissal no trecho final
        enemies.add(new EnemyEntity(EnemyType.POLVO_ABISSAL, 3700, 768 * .58, spd * .82, 75));

        double gX = (difficulty == Difficulty.DIFICIL) ? 500 : 1000;
        guardians.add(new GuardianEntity(3, gX, 768 - GuardianEntity.GH - 60));

        // 2 piscinas de lava MAIORES e mais impactantes (não 3 menores)
        sceneryElements.add(new SceneryElement(SceneryElement.Type.LAVA_POOL, 1550, 768 - 100, 380, 100));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.LAVA_POOL, 3100, 768 - 100, 420, 100));

        // 2 rochas vulcânicas como portais de passagem estreita (desafio PONTUAL)
        sceneryElements.add(new SceneryElement(SceneryElement.Type.VOLCANIC_ROCK, 2200, 768 - 280, 85, 280));

        // 2 obstáculos móveis com amplitude controlada (não 3 sobrepostos)
        sceneryElements.add(new SceneryElement(SceneryElement.Type.MOVING_OBSTACLE, 1350, 220, 90, 85).withOscillation(0, 180, 1.5, 0.0));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.MOVING_OBSTACLE, 2950, 260, 90, 85).withOscillation(140, 0, 1.2, 1.3));

        // Correntes de calor mais suaves — atmosfera, não punição
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CURRENT, 1600, 220, 110, 380).withCurrent(0, -2.5));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CURRENT, 3150, 220, 110, 380).withCurrent(0, -2.5));

        // A pressão aumenta nesta camada; a corrente mantém janelas de respiro.
        sceneryElements.add(new SceneryElement(SceneryElement.Type.PRESSURE_ZONE, 1950, 0, 130, 768).withBuoyancy(.84));

        // Gêiseres de lava — 1 antes do Leviatã, 1 no ato final
        sceneryElements.add(new SceneryElement(SceneryElement.Type.GEYSER, 2000, 768 - 200, 80, 200));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.GEYSER, 3500, 768 - 200, 80, 200));

        // Formações basálticas e corais vulcânicos enfileirados no leito da Fase 4 (APSU-A16)
        // 6 corais distribuídos ao longo do chão de lava para cobertura orgânica contínua
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL,  420, 768 - 150,  90, 150));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL,  720, 768 - 190, 105, 190));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 1150, 768 - 165,  95, 165));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 1850, 768 - 180, 100, 180));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 2480, 768 - 200, 120, 200));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, 3200, 768 - 170, 100, 170));

        currentPhase = 4;
        sacredTablet = new br.apsu.model.environment.SacredTabletEntity(4, 2750.0, 560.0);
        publishAudioDepth();
        state = State.P4; phaseStartTime = nanoTime; phaseHits = 0; adaptiveDifficulty.beginPhase();
        showAlert("Fase 4 — Abismo Vulcânico | A lava devora tudo — mantenha altitude!");
    }

    // =========================================================
    // FASE 5 — Templo Final de Apsu (boss + guardião Enki)
    // =========================================================
    public void startP5() {
        tutorialController.skip();
        cutsceneController.stop();
        overlayActive = false;
        overlayLines = null;
        boss = null;
        sacredTablet = null;
        tabletsCollected = Math.max(tabletsCollected, 4);
        enemies.clear(); projectiles.clear(); guardians.clear(); sceneryElements.clear();
        particleSystem.clear(); echoWaveRenderer.reset();
        // Fase 5 é a arena do boss — herói entra pela esquerda, boss à direita
        // BUGFIX 2026-08-15 (causa-raiz, complementar ao fix em updateP5()):
        // P5 nunca chama camera.update() — é arena fixa, hero/boss.getX() são
        // usados diretamente como coordenada de TELA. Sem este reset, a câmera
        // mantinha o offset acumulado da Fase 4 (até ~2634px), o que já causava
        // o burst de partícula fora da tela e poderia afetar qualquer outro
        // código futuro que assuma camera.getX()==0 durante a arena do boss.
        camera.setX(0);

        // The boss owns the right side of the arena with a visible margin.
        boss = new BossEntity(0, 1366 - BossEntity.BW - 110, 768 / 2.0 - BossEntity.BH / 2 - 130);
        boss.setLastShotTime(nanoTime);

        hero.beginPhaseAt(80, 768 / 2.0 - HeroEntity.HH / 2);

        // Arena limpa para o duelo final: sem NPC, correntes ou inimigos laterais.

        currentPhase = 5;
        publishAudioDepth();
        state = State.P5; phaseStartTime = nanoTime; phaseHits = 0; adaptiveDifficulty.beginPhase();
        showAlert("Fase 5 — Templo de Apsu | Derrote " + boss.getName() + "! Use tudo que aprendeu!");
    }

    // =========================================================
    // UPDATE PRINCIPAL
    // =========================================================
    public void update(long currentNanoTime) {
        this.nanoTime = currentNanoTime;
        if (state == State.PAUSE) return;
        camera.updateShake(nanoTime);
        double timeSeconds = nanoTime / 1_000_000_000.0;

        if (overlayActive && overlayLines != null && overlayIdx < overlayLines.length) {
            long elapsed = nanoTime - overlayCharTime;
            overlayCharsShown = Math.min(
                overlayLines[overlayIdx].length(),
                (int)(elapsed / 18_000_000L)
            );
        }

        hero.checkInvulnerability(nanoTime);
        particleSystem.update();

        // Atualiza obstáculos móveis
        for (SceneryElement elem : sceneryElements) {
            elem.updatePosition(timeSeconds);
        }

        double hpBeforeUpdate = hero.getHp();
        if (state == State.P1 || state == State.P2 || state == State.P3 || state == State.P4 || state == State.P5) {
            echoController.update(1.0 / 60.0, state == State.P1);
            echoWaveRenderer.update(this, 1.0 / 60.0);
        }
        switch (state) {
            case P1 -> updateP1();
            case P2 -> updateP2();
            case P3 -> updateP3();
            case P4 -> updateP4();
            case P5 -> updateP5();
        }
        if (hero.getHp() < hpBeforeUpdate) {
            phaseHits++;
            adaptiveDifficulty.recordDamage();
        }
    }

    public void stepHero(double dt) {
        applyEnvironmentalForces();
        double minY = (state == State.P1) ? 30.0 : (state == State.P2 ? 60.0 : 20.0);
        double maxY = 768.0 - HeroEntity.HH - minY;
        heroController.step(
            hero,
            inputManager,
            minY,
            maxY,
            phaseDone,
            getEffectiveBuoyancy(),
            inCurrent ? currentFx : 0,
            inCurrent ? currentFy : 0,
            nanoTime,
            dt
        );
    }

    // =========================================================
    // UPDATE P1
    // =========================================================
    private void updateP1() {
        stepHero(1.0 / 60.0);

        if (!overlayActive && !cutsceneController.isActive() && inputManager.isPressed(KeyCode.SPACE)) tryShoot();
        double focusX = cutsceneController.update(hero.getX(), 1.0 / 60.0);
        if (cutsceneController.isActive()) camera.updateFocus(focusX, 1.0 / 60.0);
        else camera.update(hero.getX());
        hero.setX(Math.max(30, Math.min(4000 - HeroEntity.HW - 30, hero.getX())));

        checkGuardians(false);
        checkSacredTablet();
        checkSceneryCollisions();
        updateEnemies();
        updateProjectiles();
        checkEnemyCollisions();

        if (state == State.P1 && tutorialController.isActive()) {
            boolean downInput = inputManager.isPressed(KeyCode.DOWN) || inputManager.isPressed(KeyCode.S);
            boolean nearEnemy = !enemies.isEmpty() && Math.abs(enemies.get(0).getWorldX() - hero.getX()) < 300;
            boolean guardianContacted = !guardians.isEmpty() && guardians.get(0).isContacted();
            tutorialController.update(1.0 / 60.0, hero.getX(), hero.getY(), downInput, nearEnemy, guardianContacted);
        }

        if (hero.getX() > 4000 - 200) {
            if (tabletsCollected >= 1) {
                phaseDone = Math.max(phaseDone, 1);
                transitionToPhase(2);
            } else {
                if (difficulty == Difficulty.DIFICIL && !backtrackingActive) {
                    backtrackingActive = true;
                    enemies.add(new EnemyEntity(EnemyType.PEIXE,  1500, 768 * .40, 1.8, 50));
                    enemies.add(new EnemyEntity(EnemyType.MEDUSA, 2200, 768 * .60, 2.0, 70));
                }
                notifyExitBlocked(1, "Portal selado! Use a Ecolocalização [Q] para revelar e resgatar a Tabuleta!");
            }
        }
    }

    // =========================================================
    // UPDATE P2 — B3-FIX: bolhas agora funcionam corretamente
    // =========================================================
    private void updateP2() {
        stepHero(1.0 / 60.0);

        if (!overlayActive && !cutsceneController.isActive() && inputManager.isPressed(KeyCode.SPACE)) tryShoot();
        checkGuardianIntroduction(2);
        double focusX = cutsceneController.update(hero.getX(), 1.0 / 60.0);
        if (cutsceneController.isActive()) camera.updateFocus(focusX, 1.0 / 60.0);
        else camera.update(hero.getX());
        hero.setX(Math.max(30, Math.min(4000 - HeroEntity.HW - 30, hero.getX())));

        // Gêiseres com impulso vertical real (B3-adjacente: mesmo sistema)
        for (SceneryElement elem : sceneryElements) {
            if (elem.getType() == SceneryElement.Type.GEYSER) {
                if (!hero.isInvulnerable() && heroHitsHazard(elem, 16, 24)) {
                    hero.applyGeyserImpulse(-6.5); // empurra sem arremessar para outro perigo
                    hurtHero(difficulty.getCollisionDamage() * 0.25,
                        0, -2.5, Color.GOLDENROD, 4);
                }
            }
        }

        checkGuardians(false);
        checkSacredTablet();

        // Baú (B3-FIX: coordenadas de mundo para colisão com baú)
        if (canOpenChest() && inputManager.isInteractPressed()) {
            chestOpen = true; chestTime = nanoTime;
            hero.setHasBubblePower(true);
            double csx = camera.toScreenX(chestWX);
            particleSystem.addBurst(csx, chestWY, Color.GOLD);
            particleSystem.addBurst(csx, chestWY, Color.CYAN);
            playSound(AudioCue.PICKUP);
            eventBus.publish(new GameEvent(GameEvent.Type.CHEST_OPENED, "Poder das Bolhas Desperto", chestWX, chestWY));
            showAlert("Espaço para atacar");
        }

        updateEnemies();
        updateProjectiles();
        checkEnemyCollisions();

        if (hero.getX() > 4000 - 200) {
            if (tabletsCollected >= 2) {
                phaseDone = Math.max(phaseDone, 2);
                transitionToPhase(3);
            } else {
                if (difficulty == Difficulty.DIFICIL && !backtrackingActive) {
                    backtrackingActive = true;
                    enemies.add(new EnemyEntity(EnemyType.ENGUIA,    1400, 768 * .50, 2.0, 60));
                    enemies.add(new EnemyEntity(EnemyType.CARANGUEJO, 2500, 768 * .35, 2.2, 80));
                }
                notifyExitBlocked(2, "Portal selado! Use a Ecolocalização [Q] para revelar e resgatar a Tabuleta!");
            }
        }
    }

    // =========================================================
    // UPDATE P3 — Correntes Abissais
    // =========================================================
    private void updateP3() {
        stepHero(1.0 / 60.0);

        if (!overlayActive && !cutsceneController.isActive() && inputManager.isPressed(KeyCode.SPACE)) tryShoot();
        checkGuardianIntroduction(3);
        double focusX = cutsceneController.update(hero.getX(), 1.0 / 60.0);
        if (cutsceneController.isActive()) camera.updateFocus(focusX, 1.0 / 60.0);
        else camera.update(hero.getX());
        hero.setX(Math.max(30, Math.min(4000 - HeroEntity.HW - 30, hero.getX())));

        checkGuardians(false);
        checkSacredTablet();
        checkSceneryCollisions();
        updateEnemies();
        updateProjectiles();
        checkEnemyCollisions();

        if (hero.getX() > 4000 - 200) {
            if (tabletsCollected >= 3) {
                phaseDone = Math.max(phaseDone, 3);
                transitionToPhase(4);
            } else {
                if (difficulty == Difficulty.DIFICIL && !backtrackingActive) {
                    backtrackingActive = true;
                    enemies.add(new EnemyEntity(EnemyType.ENGUIA, 2000, 768 * .40, 2.2, 80));
                }
                notifyExitBlocked(3, "Barreira de Correntes! Use a Ecolocalização [Q] para revelar e resgatar a Tabuleta!");
            }
        }
    }

    // =========================================================
    // UPDATE P4 — Abismo Vulcânico
    // =========================================================
    private void updateP4() {
        stepHero(1.0 / 60.0);

        if (!overlayActive && !cutsceneController.isActive() && inputManager.isPressed(KeyCode.SPACE)) tryShoot();
        checkGuardianIntroduction(4);
        double focusX = cutsceneController.update(hero.getX(), 1.0 / 60.0);
        if (cutsceneController.isActive()) camera.updateFocus(focusX, 1.0 / 60.0);
        else camera.update(hero.getX());
        hero.setX(Math.max(30, Math.min(4000 - HeroEntity.HW - 30, hero.getX())));

        checkGuardians(false);
        checkSacredTablet();
        checkSceneryCollisions();
        updateEnemies();
        updateProjectiles();
        checkEnemyCollisions();

        if (hero.getX() > 4000 - 200) {
            if (tabletsCollected >= 4) {
                phaseDone = Math.max(phaseDone, 4);
                transitionToPhase(5);
            } else {
                if (difficulty == Difficulty.DIFICIL && !backtrackingActive) {
                    backtrackingActive = true;
                    enemies.add(new EnemyEntity(EnemyType.ARRAIAO, 1800, 768 * .45, 2.5, 90));
                }
                notifyExitBlocked(4, "Selado pelas chamas! Use a Ecolocalização [Q] para revelar e resgatar a Tabuleta!");
            }
        }
    }

    // =========================================================
    // UPDATE P5 — Templo Final (boss fight)
    // =========================================================
    private void updateP5() {
        stepHero(1.0 / 60.0);
        hero.setX(Math.max(20, Math.min(1366 - HeroEntity.HW - 20, hero.getX())));
        cutsceneController.update(hero.getX(), 1.0 / 60.0);

        checkGuardians(true); // arena sem câmera

        if (!overlayActive && inputManager.isPressed(KeyCode.SPACE)) {
            tryShoot();
        }

        if (boss != null && !boss.isDead()) {
            double t = nanoTime / 1_000_000_000.0;
            boss.updatePosition(t, 768);
            if (boss.updateCombatPhase(nanoTime)) {
                camera.applyShake(12, 260_000_000L, nanoTime);
                particleSystem.addBurst(boss.getX() + BossEntity.BW / 2, boss.getY() + BossEntity.BH / 2, Color.ORANGERED);
                eventBus.publish(new GameEvent(GameEvent.Type.BOSS_PHASE_CHANGED, boss.getCombatPhase(), boss.getX(), boss.getY()));
                showAlert("ATENCAO: " + boss.getCombatPhase().getLabel() + " — Kullullû muda de padrão!");
            }

            // Colisão Direta Herói <-> Boss (US-002: dano de contato com Kullullû)
            if (!hero.isInvulnerable() && heroHits(boss.getX() + 18, boss.getY() + 18,
                                                    BossEntity.BW - 36, BossEntity.BH - 36)) {
                // BUGFIX 2026-08-15: P5 é arena fixa (sem scroll de câmera) — hero.getX()
                // já É a coordenada de tela nessa fase. camera.toScreenX() aqui subtraía o
                // offset residual deixado pela Fase 4 (camera.setX nunca é resetado ao
                // entrar em P5), jogando a partícula pra fora da tela.
                double direction = hero.getX() + HeroEntity.HW / 2 < boss.getX() + BossEntity.BW / 2 ? -1 : 1;
                // US-002: Dano por contato direto do chefe (maior ou igual ao inimigo comum e < 25% do HP max)
                double contactDmg = Math.max(difficulty.getCollisionDamage(), BossEntity.CONTACT_DAMAGE);
                hurtHero(contactDmg, direction * 5.5, -3.0, Color.PURPLE, 8);
            }

            // Cada estágio tem um padrão próprio. Durante os 0,75 s do
            // telegrafo de ataque (US-041), o jogador recebe uma janela clara para reposicionar.
            long baseInterval = (difficulty == Difficulty.FACIL ? 1_800_000_000L : (difficulty == Difficulty.MEDIO ? 1_200_000_000L : 850_000_000L));
            double phaseTempo = switch (boss.getCombatPhase()) {
                case OBSIDIANA_FRIA -> 1.0;
                case FUSAO_VULCANICA -> 0.82;
                case FURIA_DE_APSU -> 0.68;
            };
            long bullInterval = Math.max(MIN_BOSS_ATTACK_INTERVAL_NANOS,
                (long)(baseInterval * phaseTempo / (getThreatMultiplier() * BossEntity.SPD_MULT[boss.getVariant()])));

            // US-041: Telegraph dos ataques do chefe (0,6 a 1,0 s antes de disparar projeteis/dano)
            if (!boss.isTelegraphing(nanoTime)) {
                if (boss.isAttackReadyToFire(nanoTime)) {
                    fireBossAttack();
                    boss.finishAttackTelegraph(nanoTime);
                } else if (!boss.isAttackTelegraphing(nanoTime) && nanoTime - boss.getLastShotTime() > bullInterval) {
                    boss.startAttackTelegraph(nanoTime, hero.getX() + HeroEntity.HW / 2.0, hero.getY() + HeroEntity.HH / 2.0);
                }
            }
        }

        // Correntes e zonas de pressão na arena do boss — coordenadas de tela (sem scroll)
        checkSacredTablet();
        updateEnemies();
        checkEnemyCollisions();
        checkSceneryCollisionsArena();
        updateProjectiles();
    }

    private void fireBossAttack() {
        double bfx = boss.getX();
        double bfy = boss.getY() + BossEntity.BH / 2.0;
        double tx = boss.getAttackTargetX() - bfx;
        double ty = boss.getAttackTargetY() - bfy;
        double baseAngle = Math.atan2(ty, tx);
        double spread = BossEntity.SPREAD[boss.getVariant()];
        double spd = (difficulty == Difficulty.FACIL ? 4.5 : 7.0) * getThreatMultiplier() * BossEntity.SPD_MULT[boss.getVariant()];

        double bulletDmg = (difficulty == Difficulty.FACIL) ? 1.0 : ((difficulty == Difficulty.MEDIO) ? 1.2 : 1.5);

        switch (boss.getCombatPhase()) {
            case OBSIDIANA_FRIA -> fireBossFan(bfx, bfy, baseAngle, spread,
                difficulty == Difficulty.FACIL ? 3 : (difficulty == Difficulty.MEDIO ? 4 : 5), spd, bulletDmg);
            case FUSAO_VULCANICA -> {
                // Onda circular: forca leitura espacial, nao so reflexo.
                int count = difficulty == Difficulty.FACIL ? 6 : 8;
                for (int i = 0; i < count; i++) {
                    double angle = (Math.PI * 2 * i) / count;
                    projectiles.add(new Projectile(Projectile.Type.BOSS_BULLET, bfx, bfy,
                        Math.cos(angle) * spd * 0.78, Math.sin(angle) * spd * 0.78, bulletDmg, true));
                }
            }
            case FURIA_DE_APSU -> {
                // Meteoros de lava caem das colunas de aviso telegrafadas no teto.
                int count = difficulty == Difficulty.FACIL ? 3 : 5;
                for (int i = 0; i < count; i++) {
                    double meteorX = 260 + Math.floorMod(boss.getAttackSeed() / 100_000_000L + i * 193L, 820L);
                    projectiles.add(new Projectile(Projectile.Type.BOSS_BULLET, meteorX, -12,
                        Math.sin(i * 2.1) * 1.4, spd * 1.22, bulletDmg, true));
                }
            }
        }
    }

    private void fireBossFan(double x, double y, double baseAngle, double spread, int count, double speed, double bulletDmg) {
        for (int i = 0; i < count; i++) {
            double angle = baseAngle + (i - (count - 1) / 2.0) * (spread / Math.max(count - 1, 1));
            projectiles.add(new Projectile(Projectile.Type.BOSS_BULLET, x, y,
                Math.cos(angle) * speed, Math.sin(angle) * speed, bulletDmg, true));
        }
    }

    // =========================================================
    // MECÂNICAS AMBIENTAIS
    // =========================================================
    private double getEffectiveBuoyancy() {
        double phaseFactor = OceanDepthProfile.forPhase(currentPhase).getBuoyancyFactor();
        return phaseFactor * (inPressureZone ? pressureBuoyMult : 1.0);
    }

    /** Detecta correntes e zonas de pressão ao redor do herói. */
    private void applyEnvironmentalForces() {
        inCurrent = false;
        inPressureZone = false;
        currentFx = 0; currentFy = 0;
        pressureBuoyMult = 1.0;

        for (SceneryElement elem : sceneryElements) {
            if (elem.getType() == SceneryElement.Type.CURRENT) {
                double ex = elem.getWorldX();
                if (rectsHit(hero.getX(), hero.getY(), HeroEntity.HW, HeroEntity.HH,
                             ex, elem.getWorldY(), elem.getWidth(), elem.getHeight())) {
                    inCurrent = true;
                    currentFx = elem.getCurrentVx();
                    currentFy = elem.getCurrentVy();
                    break;
                }
            } else if (elem.getType() == SceneryElement.Type.PRESSURE_ZONE) {
                double ex = elem.getWorldX();
                if (rectsHit(hero.getX(), hero.getY(), HeroEntity.HW, HeroEntity.HH,
                             ex, elem.getWorldY(), elem.getWidth(), elem.getHeight())) {
                    inPressureZone = true;
                    pressureBuoyMult = elem.getBuoyancyMult();
                    break;
                }
            }
        }
    }

    /** Colisões com obstáculos do cenário (mundo com câmera). */
    private void checkSceneryCollisions() {
        double heroX = hero.getX();
        for (SceneryElement elem : sceneryElements) {
            // APSU-P17: culling espacial — ignora obstáculos fora da vizinhança imediata do herói
            if (elem.getWorldX() + elem.getWidth() < heroX - 200 || elem.getWorldX() > heroX + HeroEntity.HW + 200) {
                continue;
            }
            switch (elem.getType()) {
                case GEYSER -> {
                    if (!hero.isInvulnerable() && heroHitsHazard(elem, 16, 24)) {
                        hero.applyGeyserImpulse(-6.5);
                        hurtHero(difficulty.getCollisionDamage() * 0.25, 0, -2.5, Color.GOLDENROD, 4);
                    }
                }
                case MOVING_OBSTACLE, VOLCANIC_ROCK -> {
                    if (heroHitsHazard(elem, 12, 12)) {
                        double safeInsetX = Math.min(12, elem.getWidth() * 0.35);
                        double safeInsetY = Math.min(12, elem.getHeight() * 0.35);
                        pushHeroOutOfObstacle(elem.getWorldX() + safeInsetX, elem.getWorldY() + safeInsetY,
                            Math.max(1, elem.getWidth() - safeInsetX * 2), Math.max(1, elem.getHeight() - safeInsetY * 2));
                        if (!hero.isInvulnerable()) {
                            double direction = hero.getX() + HeroEntity.HW / 2 < elem.getWorldX() + elem.getWidth() / 2 ? -1 : 1;
                            hurtHero(difficulty.getCollisionDamage() * 0.65, direction * 5.5, -2.5, Color.ORANGERED, 6);
                            if (hero.getHp() <= 0) {
                                if (state == State.P5) publishBossMusic(false);
                                state = State.OVER;
                            }
                        }
                    }
                }
                case LAVA_POOL -> {
                    if (heroFeetHit(elem)) {
                        hero.applyGeyserImpulse(-5.0);
                        if (!hero.isInvulnerable()) {
                            hurtHero(difficulty.getCollisionDamage() * 0.45, 0, -5.0, Color.web("#ff4400"), 7);
                            if (hero.getHp() <= 0) {
                                if (state == State.P5) publishBossMusic(false);
                                state = State.OVER;
                            }
                        }
                    }
                }
                default -> {} // outros tipos sem colisão especial
            }
        }
    }

    /** Colisões na arena sem câmera (P5). */
    private void checkSceneryCollisionsArena() {
        for (SceneryElement elem : sceneryElements) {
            if (elem.getType() == SceneryElement.Type.GEYSER) {
                if (!hero.isInvulnerable() && heroHitsHazard(elem, 16, 24)) {
                    hero.applyGeyserImpulse(-6.5);
                    hurtHero(difficulty.getCollisionDamage() * 0.25, 0, -2.5, Color.GOLDENROD, 4);
                }
            }
        }
    }

    /** Agenda a apresentação única do guardião ao jogador que se aproxima. */
    private void checkGuardianIntroduction(int phase) {
        if (guardians.isEmpty()) return;
        GuardianEntity guardian = guardians.get(0);
        br.apsu.cutscene.CutsceneRequest request = guardianIntroductionController.check(
            phase,
            hero.getX() + HeroEntity.HW / 2.0,
            hero.getY() + HeroEntity.HH / 2.0,
            guardian
        );
        if (request == null) return;
        eventBus.publish(new GameEvent(
            GameEvent.Type.CUTSCENE_REQUESTED,
            request,
            guardian.getWorldX(),
            guardian.getWorldY()
        ));
    }

    /** Verifica interação com guardiões. */
    private void checkGuardians(boolean arenaMode) {
        double heroCenterX = hero.getX() + HeroEntity.HW / 2.0;
        double heroCenterY = hero.getY() + HeroEntity.HH / 2.0;
        for (GuardianEntity g : guardians) {
            g.updatePromptDistance(heroCenterX, heroCenterY, 1.0 / 60.0);
            double gx = arenaMode ? g.getWorldX() : camera.toScreenX(g.getWorldX());
            double heroScreenX = arenaMode ? hero.getX() : camera.toScreenX(hero.getX());
            if (!g.isContacted() && inputManager.isInteractPressed()
                    && rectsHit(heroScreenX, hero.getY(), HeroEntity.HW, HeroEntity.HH,
                                gx - 30, g.getWorldY() - 30, GuardianEntity.GW + 60, GuardianEntity.GH + 60)) {
                g.setContacted(true);
                // AC3: ao iniciar diálogo com Espaço, consome o disparo para nenhuma bolha sair naquele pressionamento
                hero.setLastShotTime(nanoTime);
                final double finalGx = gx;
                openOverlay(g.getType(), g.getName(), g.getDialogue(), () -> {
                    hero.heal(1.0);
                    if (currentPhase == 1) {
                        tutorialController.onGuardianContacted();
                    }
                    particleSystem.addBurst(finalGx + GuardianEntity.GW/2, g.getWorldY() + GuardianEntity.GH/2, Palette.GOLD);
                    particleSystem.addBurst(finalGx + GuardianEntity.GW/2, g.getWorldY() + GuardianEntity.GH/2, Palette.RADAR_STROKE);
                    playSound(AudioCue.MENU_CONFIRM);
                    showAlert("Pista obtida! Use a Ecolocalização [Q] na área indicada para revelar a Tabuleta!");
                    persistProgress();
                });
                break;
            }
        }
    }

    /** Verifica detecção e coleta física da Tabuleta Sagrada revelada pela Ecolocalização (APSU-G01). */
    private void checkSacredTablet() {
        if (sacredTablet == null) return;
        sacredTablet.update(1.0 / 60.0);
        if (sacredTablet.isRevealed()) {
            double tx = sacredTablet.getWorldX();
            double ty = sacredTablet.getWorldY();
            if (rectsHit(hero.getX(), hero.getY(), HeroEntity.HW, HeroEntity.HH,
                         tx - 26, ty - 26, br.apsu.model.environment.SacredTabletEntity.WIDTH, br.apsu.model.environment.SacredTabletEntity.HEIGHT)) {
                if (sacredTablet.collect()) {
                    tabletsCollected++;
                    hero.heal(2.0);
                    double screenTx = (state == State.P5) ? tx : camera.toScreenX(tx);
                    particleSystem.addBurst(screenTx, ty, Palette.GOLD);
                    particleSystem.addBurst(screenTx, ty, Palette.RADAR_STROKE);
                    particleSystem.addBurst(screenTx, ty, Palette.HUD_HP_HIGH);
                    if (state == State.P5) {
                        playSound(AudioCue.VICTORY);
                        showAlert("Todas as 5 Tabuletas Sagradas reunidas! Apsu foi purificado!");
                        state = State.WIN;
                        phaseDone = 5;
                        persistProgress();
                    } else {
                        GameEvent exitFocus = phaseExitIntroductionController.tabletCollected(currentPhase);
                        if (exitFocus != null) eventBus.publish(exitFocus);
                        playSound(AudioCue.PICKUP);
                        showAlert("Tabuleta Sagrada (" + tabletsCollected + "/" + TOTAL_TABLETS + ") resgatada! O portal de descida foi liberado!");
                        persistProgress();
                    }
                }
            }
        }
    }

    public void onTabletRevealed(br.apsu.model.environment.SacredTabletEntity tablet) {
        particleSystem.addBurst(camera.toScreenX(tablet.getWorldX()), tablet.getWorldY(), Palette.GOLD);
        particleSystem.addBurst(camera.toScreenX(tablet.getWorldX()), tablet.getWorldY(), Palette.RADAR_STROKE);
        playSound(AudioCue.PICKUP);
        showAlert("Ecolocalização revelou a Tabuleta Sagrada! Nade até a relíquia para resgatá-la!");
    }

    public void revealAndCollectSacredTablet() {
        if (sacredTablet != null) {
            sacredTablet.reveal();
            sacredTablet.collect();
        }
        tabletsCollected++;
        hero.heal(2.0);
        GameEvent exitFocus = phaseExitIntroductionController.tabletCollected(currentPhase);
        if (exitFocus != null) eventBus.publish(exitFocus);
        persistProgress();
    }

    public br.apsu.model.environment.SacredTabletEntity getSacredTablet() {
        return sacredTablet;
    }

    // =========================================================
    // INIMIGOS
    // =========================================================
    private void updateEnemies() {
        double t = nanoTime / 1_000_000_000.0;
        // Contact damage queries this tree every frame, whether or not a
        // hero bubble is active. Leaving it empty between shots disabled hits.
        enemyQuadTree.clear();

        // APSU-P17: culling de viewport — inimigos muito distantes só fazem
        // sinusóide Y (posição) mas não disparam. Mantém quadtree completa para colisão.
        final double CULL_MARGIN = 900.0; // px além da borda da tela
        final double camX = camera.getX();
        final double viewRight = camX + 1366.0;

        for (EnemyEntity e : enemies) {
            if (!e.isAlive()) continue;
            e.update(t, getThreatMultiplier());
            enemyQuadTree.insert(e);

            // Disparo: apenas inimigos visíveis (já existia ex > 0 && ex < 1366).
            // Física cara (IA de disparo) ignorada se muito longe — sem mudança de comportamento visível.
            double ex = e.getWorldX();
            if (ex < camX - CULL_MARGIN || ex > viewRight + CULL_MARGIN) continue;

            if (difficulty.canEnemiesShoot()) {
                if (e.getLastShotTime() == 0) {
                    e.setLastShotTime(nanoTime);
                    continue;
                }
                double screenEx = camera.toScreenX(ex);
                double heroScreenX = camera.toScreenX(hero.getX());
                long shootCooldown = difficulty.getEnemyShootIntervalNanos();
                if (screenEx > 0 && screenEx < 1366 && (nanoTime - e.getLastShotTime()) > shootCooldown) {
                    e.setLastShotTime(nanoTime);
                    double dx = heroScreenX + HeroEntity.HW/2 - screenEx;
                    double dy = hero.getY() + HeroEntity.HH/2 - e.getCurrentY();
                    double len = Math.sqrt(dx*dx + dy*dy);
                    if (len > 0) {
                        // B3-FIX: projétil inimigo criado em coordenadas de MUNDO
                        projectiles.add(new Projectile(Projectile.Type.ENEMY_MALIGN,
                            e.getWorldX(), e.getCurrentY() + e.getType().getHeight()/2,
                            dx/len * 4.5, dy/len * 4.5, 0.2, false));
                    }
                }
            }
        }
    }

    // =========================================================
    // B3-FIX PRINCIPAL — Projéteis em coordenadas de MUNDO
    // =========================================================
    private void updateProjectiles() {
        boolean isArena = (state == State.P5); // P5 sem câmera scrolling

        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            p.update();

            // Limites do mundo
            double maxW = isArena ? 1366 + 50 : 4050;
            if (p.getX() < -50 || p.getX() > maxW || p.getY() < -50 || p.getY() > 768 + 50) {
                it.remove(); continue;
            }

            if (p.getType() == Projectile.Type.HERO_BUBBLE) {
                // Otimização QuadTree 2D: busca candidatos próximos na região do projétil
                boolean hitSomething = false;
                nearbyEnemiesBuffer.clear();
                enemyQuadTree.retrieve(nearbyEnemiesBuffer, p.getX() - 10, p.getY() - 10, 48, 48);

                for (EnemyEntity e : nearbyEnemiesBuffer) {
                    if (!e.isAlive()) continue;
                    // Colisão MUNDO vs MUNDO (caixa generosa de acerto do herói)
                    if (rectsHit(p.getX() - 6, p.getY() - 6, 28, 28,
                                 e.getWorldX(), e.getCurrentY(), e.getType().getWidth(), e.getType().getHeight())) {
                        e.takeDamage((int) Math.max(1, Math.round(p.getDamage())));
                        if (!e.isAlive()) {
                            adaptiveDifficulty.recordEnemyDefeated();
                            playSound(AudioCue.ENEMY_DEFEATED);
                        }
                        double screenPx = isArena ? p.getX() : camera.toScreenX(p.getX());
                        particleSystem.addBurst(screenPx, p.getY(), Color.AQUAMARINE);
                        playSound(AudioCue.ENEMY_DAMAGED);
                        it.remove();
                        hitSomething = true;
                        break;
                    }
                }
                if (!hitSomething && boss != null) {
                    // Boss em coordenadas de tela (arena fixa P5)
                    if (rectsHit(p.getX() - 6, p.getY() - 6, 28, 28, boss.getX(), boss.getY(), BossEntity.BW, BossEntity.BH)) {
                        boss.takeDamage(1);
                        particleSystem.addBurst(boss.getX() + BossEntity.BW/2, boss.getY() + BossEntity.BH/2, Color.CYAN);
                        playSound(AudioCue.BOSS_DAMAGED);
                        it.remove();
                        if (boss.isDead()) {
                            camera.applyShake(18, 500_000_000L, nanoTime);
                            particleSystem.addBurst(boss.getX() + BossEntity.BW/2, boss.getY() + BossEntity.BH/2, Color.GOLD);
                            particleSystem.addBurst(boss.getX() + BossEntity.BW/2, boss.getY() + BossEntity.BH/2, Color.ORANGERED);
                            playSound(AudioCue.ENEMY_DEFEATED);
                            publishBossMusic(false);
                            projectiles.clear();
                            // APSU-G19F: A 5ª Tabuleta Sagrada emerge flutuando no centro do Templo de Apsu
                            sacredTablet = new br.apsu.model.environment.SacredTabletEntity(5, 683.0 - 24.0, 384.0 - 32.0);
                            sacredTablet.reveal();
                            showAlert("A 5ª Tabuleta Sagrada emergiu do altar! Aproxime-se para purificar Apsu!");
                        }
                    }
                }
            } else if (p.getType() == Projectile.Type.ENEMY_MALIGN) {
                // Inimigo acerta herói — caixa justa de projétil
                if (!hero.isInvulnerable() && heroHits(p.getX() - 3, p.getY() - 3, 6, 6)) {
                    hurtHero(p.getDamage(), p.getVx() * 0.35, p.getVy() * 0.35, Color.PURPLE, 6);
                    it.remove();
                }
            } else if (p.getType() == Projectile.Type.BOSS_BULLET) {
                // Boss bullet em tela (P5) - caixa generosa e justa compatível com a malha 3D (escala 54) e esfera 2D
                if (!hero.isInvulnerable() && rectsHit(hero.getX() + 8, hero.getY() + 16, HeroEntity.HW - 16, HeroEntity.HH - 32,
                                                       p.getX() - 25, p.getY() - 25, 50, 50)) {
                    hurtHero(p.getDamage(), p.getVx() * 0.40, p.getVy() * 0.40, Color.PURPLE, 7);
                    it.remove();
                }
            }
        }
    }

    // =========================================================
    // COLISÃO HERÓI <-> INIMIGOS (coordenadas de mundo)
    // =========================================================
    private void checkEnemyCollisions() {
        if (hero.isInvulnerable()) return;
        nearbyEnemiesBuffer.clear();
        enemyQuadTree.retrieve(nearbyEnemiesBuffer, hero.getX() - 200, hero.getY() - 160, HeroEntity.HW + 400, HeroEntity.HH + 320);

        for (EnemyEntity e : nearbyEnemiesBuffer) {
            if (!e.isAlive()) continue;
            // B3-FIX: coordenadas de mundo para colisão consistente
            if (heroHitsEnemy(e)) {
                if (e.getType() == EnemyType.LEVIATA) {
                    // US-040: Dano por contato direto com o Leviata Menor (sub-boss da Fase 4)
                    double direction = hero.getX() + HeroEntity.HW / 2 < e.getWorldX() + e.getType().getWidth() / 2 ? -1 : 1;
                    double contactDmg = Math.max(difficulty.getCollisionDamage() * 0.60, EnemyType.LEVIATHAN_CONTACT_DAMAGE);
                    hurtHero(contactDmg, direction * 5.5, -3.0, Color.web("#ff3b30"), 8);
                    if (hero.getHp() <= 0) {
                        state = State.OVER;
                    }
                    return;
                }
                e.setAlive(false);
                adaptiveDifficulty.recordEnemyDefeated();
                double direction = hero.getX() + HeroEntity.HW / 2 < e.getWorldX() + e.getType().getWidth() / 2 ? -1 : 1;
                hurtHero(difficulty.getCollisionDamage() * 0.60, direction * 4.0, -2.5, Color.RED, 6);
                if (hero.getHp() <= 0) {
                    if (state == State.P5) publishBossMusic(false);
                    state = State.OVER;
                }
                return;
            }
        }
    }

    // =========================================================
    // DISPARO — B3-FIX: posição de mundo
    // =========================================================
    private void tryShoot() {
        long cooldown = hero.isMultipleShotMode() ? 480_000_000L : 320_000_000L;
        if (nanoTime - hero.getLastShotTime() < cooldown) return;
        hero.setLastShotTime(nanoTime);
        if (hero.hasBubblePower()) {
            hero.triggerShooting(nanoTime);
            boolean right = hero.isFacingRight();
            // B3-FIX: getBubbleSpawnX() retorna coordenada de MUNDO
            double bx = hero.getBubbleSpawnX();
            double by = hero.getBubbleSpawnY();
            double vx = (right ? 1 : -1) * 16.0 * hero.getType().getPwr();
            // isArena=false para P1-P4 (mundo com scroll), true para P5
            boolean isArena = (state == State.P5);
            // APSU-B02: No modo Difícil, bolhas do herói causam 0.8 de dano (redução de 20%)
            // prolongando o combate, especialmente contra Kullullû (boss com 7-9 HP)
            double bubbleDamage = (difficulty == Difficulty.DIFICIL) ? 0.8 : 1.0;
            if (hero.isMultipleShotMode()) {
                projectiles.add(new Projectile(Projectile.Type.HERO_BUBBLE, bx, by, vx, -2.2, bubbleDamage, isArena));
                projectiles.add(new Projectile(Projectile.Type.HERO_BUBBLE, bx, by, vx, 0, bubbleDamage, isArena));
                projectiles.add(new Projectile(Projectile.Type.HERO_BUBBLE, bx, by, vx, 2.2, bubbleDamage, isArena));
            } else {
                projectiles.add(new Projectile(Projectile.Type.HERO_BUBBLE, bx, by, vx, 0, bubbleDamage, isArena));
            }
            playSound(AudioCue.ATTACK);
        } else {
            // BUGFIX 2026-08-15: mesmo padrão do fix em updateP5() — se o jogador
            // chegar em P5 sem ter achado o baú (poder de bolha), este alerta
            // podia nascer fora da tela por causa do offset de câmera da arena.
            boolean arenaMode = (state == State.P5);
            double screenX = arenaMode ? hero.getX() : camera.toScreenX(hero.getX());
            particleSystem.addBurst(screenX + HeroEntity.HW / 2, hero.getY() + HeroEntity.HH / 2, hero.getType().getAura());
            showAlert("Encontre o Baú no Navio Naufragado (Fase 2) para obter o Poder das Bolhas!");
        }
    }

    // =========================================================
    // OVERLAY
    // =========================================================
    public void openOverlay(int guardianType, String name, String[] lines, Runnable onComplete) {
        overlayActive = true;
        overlayGuardianType = guardianType;
        overlayIdx = 0;
        overlayName = name;
        overlayLines = lines;
        overlayCharTime = nanoTime;
        overlayCharsShown = 0;
        overlayOnComplete = onComplete;
        playSound(AudioCue.DIALOGUE_ADVANCE);
    }

    public void advanceOverlay() {
        if (overlayLines == null || overlayIdx >= overlayLines.length) {
            overlayActive = false;
            if (overlayOnComplete != null) overlayOnComplete.run();
            return;
        }

        if (overlayCharsShown < overlayLines[overlayIdx].length()) {
            overlayCharsShown = overlayLines[overlayIdx].length();
            return;
        }

        overlayIdx++;
        playSound(AudioCue.DIALOGUE_ADVANCE);
        if (overlayIdx < overlayLines.length) {
            overlayCharsShown = 0;
            overlayCharTime = nanoTime;
        } else {
            overlayActive = false;
            if (overlayOnComplete != null) overlayOnComplete.run();
        }
    }

    public boolean rectsHit(double x1, double y1, double w1, double h1,
                             double x2, double y2, double w2, double h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

    // Núcleo do herói: cobre o corpo principal (peito + barriga) do sprite 2.5D.
    // 28px de largura × 100px de altura — mais fiel ao visual do personagem.
    private boolean heroHits(double x, double y, double width, double height) {
        return rectsHit(hero.getX() + 20, hero.getY() + 44, 28, 100,
            x, y, width, height);
    }

    public void pushHeroOutOfObstacle(double ox, double oy, double ow, double oh) {
        double hx = hero.getX() + 20;
        double hy = hero.getY() + 44;
        double hw = 28;
        double hh = 100;

        if (!rectsHit(hx, hy, hw, hh, ox, oy, ow, oh)) return;

        double overlapLeft = (hx + hw) - ox;
        double overlapRight = (ox + ow) - hx;
        double overlapTop = (hy + hh) - oy;
        double overlapBottom = (oy + oh) - hy;

        double minOverlap = Math.min(Math.min(overlapLeft, overlapRight), Math.min(overlapTop, overlapBottom));

        if (minOverlap == overlapLeft) {
            hero.setX(ox - hw - 20);
            if (hero.getVx() > 0) hero.setVx(0);
        } else if (minOverlap == overlapRight) {
            hero.setX(ox + ow - 20);
            if (hero.getVx() < 0) hero.setVx(0);
        } else if (minOverlap == overlapTop) {
            hero.setY(oy - hh - 44);
            if (hero.getVy() > 0) hero.setVy(0);
        } else {
            hero.setY(oy + oh - 44);
            if (hero.getVy() < 0) hero.setVy(0);
        }
    }

    private boolean heroHitsHazard(SceneryElement elem, double insetX, double insetY) {
        double safeInsetX = Math.max(insetX + 12, elem.getWidth() * 0.28);
        double safeInsetY = Math.max(insetY + 12, elem.getHeight() * 0.25);
        return heroHits(elem.getWorldX() + safeInsetX, elem.getWorldY() + safeInsetY,
            Math.max(1, elem.getWidth() - safeInsetX * 2),
            Math.max(1, elem.getHeight() - safeInsetY * 2));
    }

    /** Lava só machuca quando a parte inferior do corpo toca a superfície. */
    private boolean heroFeetHit(SceneryElement elem) {
        return rectsHit(hero.getX() + 20, hero.getY() + HeroEntity.HH - 35,
            HeroEntity.HW - 40, 20,
            elem.getWorldX() + 15, elem.getWorldY() + 15,
            Math.max(1, elem.getWidth() - 30), Math.max(1, elem.getHeight() - 15));
    }

    private boolean heroHitsEnemy(EnemyEntity enemy) {
        double width = enemy.getType().getWidth();
        double height = enemy.getType().getHeight();
        if (enemy.getType() == EnemyType.LEVIATA) {
            // US-040: Intersecao de caixas delimitadoras para o Leviata Menor (sub-boss)
            return rectsHit(hero.getX(), hero.getY(), HeroEntity.HW, HeroEntity.HH,
                            enemy.getWorldX(), enemy.getCurrentY(), width, height);
        }
        // Inimigo comum precisa sobrepor 55% central do corpo — evita dano à distância
        return heroHits(enemy.getWorldX() + width * 0.225, enemy.getCurrentY() + height * 0.225,
            width * 0.55, height * 0.55);
    }

    /** Aplica dano uma única vez e cria separação suficiente para o jogador reagir. */
    private boolean hurtHero(double damage, double knockbackX, double knockbackY, Color color, int shake) {
        if (hero.isInvulnerable()) return false;
        if (state != State.P5 && cutsceneController.isActive()) return false;
        double effectiveDamage = damage;
        if (state == State.P1 && tutorialController.isActive()) {
            double cappedHp = tutorialController.filterHeroDamage(hero.getHp(), hero.getMaxHP(), damage);
            effectiveDamage = hero.getHp() - cappedHp;
        }
        hero.applyDamage(effectiveDamage, nanoTime);
        hero.applyKnockback(knockbackX, knockbackY);
        camera.applyShake(shake, 150_000_000L, nanoTime);
        double screenX = state == State.P5 ? hero.getX() : camera.toScreenX(hero.getX());
        particleSystem.addBurst(screenX + HeroEntity.HW / 2, hero.getY() + HeroEntity.HH / 2, color);
        playSound(AudioCue.HERO_DAMAGED);
        eventBus.publish(new GameEvent(GameEvent.Type.HERO_DAMAGED, effectiveDamage, screenX, hero.getY()));
        if (hero.getHp() <= 0) {
            if (state == State.P5) publishBossMusic(false);
            state = State.OVER;
        }
        return true;
    }

    public double getThreatMultiplier() {
        double el = (nanoTime - phaseStartTime) / 1_000_000_000.0;
        double b = difficulty.getEnemySpeedMult();
        return Math.max(0.5, Math.min(1.45, b + adaptiveDifficulty.getAdjustment(el)));
    }

    public void showAlert(String msg) {
        this.alertMessage = msg;
        this.alertTime = nanoTime;
    }

    public void notifyExitBlocked(int requiredTablets, String customObjective) {
        if (nanoTime - lastBlockedExitAlertNanos >= 5_000_000_000L) {
            lastBlockedExitAlertNanos = nanoTime;
            String msg = (customObjective != null && !customObjective.isBlank())
                ? customObjective
                : "Portal selado! Converse com o guardiao para obter a Tabuleta (" + tabletsCollected + "/" + requiredTablets + ")!";
            showAlert(msg);
        }
    }

    /** Salva progresso sem permitir que um erro de disco interrompa a partida. */
    private void persistProgress() {
        boolean[] collected = new boolean[TOTAL_TABLETS];
        for (int i = 0; i < tabletsCollected && i < collected.length; i++) collected[i] = true;
        try {
            saveManager.save(new SaveManager.SaveData(
                SaveManager.CURRENT_VERSION,
                difficulty.name(),
                heroType.name(),
                Math.min(5, phaseDone + 1),
                collected,
                0
            ));
        } catch (IOException ignored) {
            // O jogo continua funcional mesmo sem permissão de escrita no perfil.
        }
    }

    private void publishAudioDepth() {
        eventBus.publish(new GameEvent(GameEvent.Type.OCEAN_DEPTH_CHANGED,
            OceanDepthProfile.forPhase(currentPhase), 0, 0));
        publishBossMusic(currentPhase == 5);
    }

    private void publishBossMusic(boolean active) {
        eventBus.publish(new GameEvent(GameEvent.Type.BOSS_MUSIC_CHANGED, active, 0, 0));
    }

    private void playSound(AudioCue cue) {
        eventBus.publish(new GameEvent(GameEvent.Type.SOUND_REQUESTED, cue, 0, 0));
    }

    // Getters
    public int getPhaseDone() { return phaseDone; }
    public void setPhaseDone(int phaseDone) { this.phaseDone = phaseDone; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
        hero.setMaxHP(difficulty.getInitialHP());
    }
    public HeroType getHeroType() { return heroType; }
    public void setHeroType(HeroType heroType) { this.heroType = HeroType.GUARDIAO; hero.setType(HeroType.GUARDIAO); }
    public HeroEntity getHero() { return hero; }
    public BossEntity getBoss() { return boss; }
    public AdaptiveDifficulty getAdaptiveDifficulty() { return adaptiveDifficulty; }
    public List<EnemyEntity> getEnemies() { return enemies; }
    public List<Projectile> getProjectiles() { return projectiles; }
    public List<GuardianEntity> getGuardians() { return guardians; }
    public List<SceneryElement> getSceneryElements() { return sceneryElements; }
    public ParticleSystem getParticleSystem() { return particleSystem; }
    public InputManager getInputManager() { return inputManager; }
    public SaveManager getSaveManager() { return saveManager; }
    public EventBus getEventBus() { return eventBus; }
    public Camera getCamera() { return camera; }
    public int getTabletsCollected() { return tabletsCollected; }
    public int getTotalTablets() { return TOTAL_TABLETS; }
    public boolean isOverlayActive() { return overlayActive; }
    public int getOverlayGuardianType() { return overlayGuardianType; }
    public int getOverlayIdx() { return overlayIdx; }
    public String getOverlayName() { return overlayName; }
    public String[] getOverlayLines() { return overlayLines; }
    public int getOverlayCharsShown() { return overlayCharsShown; }
    public String getAlertMessage() { return alertMessage; }
    public long getAlertTime() { return alertTime; }
    public long getNanoTime() { return nanoTime; }

    public int getDlgPhase() { return dlgPhase; }
    public int getDlgIdx() { return dlgIdx; }
    public void setDlgIdx(int idx) { this.dlgIdx = idx; }
    public int getMenuSel() { return menuSel; }
    public void setMenuSel(int menuSel) { this.menuSel = menuSel; }
    public boolean isDifficultyMenuOpen() { return difficultyMenuOpen; }
    public int getDifficultyMenuSel() { return difficultyMenuSel; }
    public boolean isChestOpen() { return chestOpen; }
    public boolean canOpenChest() {
        return state == State.P2 && !chestOpen
            && rectsHit(hero.getX() - 40, hero.getY() - 40, HeroEntity.HW + 80, HeroEntity.HH + 80,
                chestWX - 80, chestWY - 70, 160, 140);
    }
    public boolean isChestPromptVisible() {
        if (state != State.P2 || chestOpen) return false;
        double heroCenterX = hero.getX() + HeroEntity.HW / 2.0;
        double heroCenterY = hero.getY() + HeroEntity.HH / 2.0;
        double dx = heroCenterX - chestWX;
        double dy = heroCenterY - chestWY;
        double distSq = dx * dx + dy * dy;
        if (chestPromptVisible) {
            if (distSq > 180.0 * 180.0) {
                chestPromptVisible = false;
            }
        } else {
            if (distSq <= 150.0 * 150.0) {
                chestPromptVisible = true;
            }
        }
        return chestPromptVisible;
    }
    public void setChestPromptVisible(boolean visible) {
        this.chestPromptVisible = visible && state == State.P2 && !chestOpen;
    }
    public double getChestWX() { return chestWX; }
    public double getChestWY() { return chestWY; }
    public long getChestTime() { return chestTime; }
    public boolean isInCurrent() { return inCurrent; }
    public double getCurrentFx() { return currentFx; }
    public double getCurrentFy() { return currentFy; }
    public boolean isInPressureZone() { return inPressureZone; }
    public double getPressureBuoyMult() { return pressureBuoyMult; }
    public int getCurrentPhase() { return currentPhase; }
    public double getRenderAlpha() { return renderAlpha; }
    public void setRenderAlpha(double renderAlpha) { this.renderAlpha = renderAlpha; }
    public HeroController getHeroController() { return heroController; }
    public br.apsu.cutscene.CutsceneController getCutsceneController() { return cutsceneController; }
    public TutorialController getTutorialController() { return tutorialController; }
    public br.apsu.echo.EchoController getEchoController() { return echoController; }
    public br.apsu.echo.EchoWaveRenderer getEchoWaveRenderer() { return echoWaveRenderer; }
}
