package br.apsu.core;

import br.apsu.core.events.EventBus;
import br.apsu.core.events.GameEvent;
import br.apsu.audio.AudioCue;
import br.apsu.core.map.MPIMapLoader;
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

import java.util.ArrayList;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    public enum State { MENU, DIALOGUE, P1, P2, P3, P4, P5, WIN, OVER }
    private static final long MIN_BOSS_ATTACK_INTERVAL_NANOS = 650_000_000L;

    private State state = State.MENU;
    private Difficulty difficulty = Difficulty.MEDIO;
    private HeroType heroType = HeroType.GUARDIAO;
    private HeroEntity hero;
    private BossEntity boss;

    private int currentPhase = 0;
    private int phaseDone = 0;
    private int tabletsCollected = 0;
    private boolean backtrackingActive = false;
    private static final int TOTAL_TABLETS = 5;

    private final List<EnemyEntity> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<GuardianEntity> guardians = new ArrayList<>();
    private final List<SceneryElement> sceneryElements = new ArrayList<>();
    private final ParticleSystem particleSystem = new ParticleSystem();

    private final QuadTree<EnemyEntity> enemyQuadTree = new QuadTree<>(0, new QuadTree.Bounds(0, 0, 4000, 768), new QuadTree.BoundingBoxProvider<>() {
        @Override public double getX(EnemyEntity e) { return e.getWorldX(); }
        @Override public double getY(EnemyEntity e) { return e.getCurrentY(); }
        @Override public double getWidth(EnemyEntity e) { return e.getType().getWidth(); }
        @Override public double getHeight(EnemyEntity e) { return e.getType().getHeight(); }
    });
    private final List<EnemyEntity> nearbyEnemiesBuffer = new ArrayList<>();

    private final InputManager inputManager = new InputManager();
    private final MPIMapLoader mpiMapLoader = new MPIMapLoader();
    private final Camera camera = new Camera(1366, 4000);
    private final SaveManager saveManager;
    private final EventBus eventBus;

    // Baú (Fase 2)
    private double chestWX = 1600, chestWY = 768 - 170;
    private boolean chestOpen = false;
    private long chestTime = 0;

    // Menu & Diálogos
    private int menuSel = 2; // 0=Difficulty, 1=Hero, 2=Start Game
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
        this.heroType = HeroType.valueOf(saved.selectedHero());
        this.phaseDone = Math.max(0, saved.unlockedPhase() - 1);
        this.hero = new HeroEntity(heroType, difficulty.getInitialHP());
    }

    public void cycleDiff(int dir) {
        Difficulty[] vals = Difficulty.values();
        setDifficulty(vals[(difficulty.ordinal() + dir + vals.length) % vals.length]);
        hero.heal(difficulty.getInitialHP());
        persistProgress();
    }

    public void cycleHero(int dir) {
        HeroType[] vals = HeroType.values();
        heroType = vals[(heroType.ordinal() + dir + vals.length) % vals.length];
        hero.setType(heroType);
        persistProgress();
    }

    public void onKey(KeyCode k) {
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
                if (k == KeyCode.ESCAPE) {
                    if (state == State.P5) publishBossMusic(false);
                    state = State.MENU;
                }
            }
            case WIN, OVER -> {
                if (k == KeyCode.SPACE || k == KeyCode.ENTER || k == KeyCode.R) {
                    state = State.MENU;
                }
            }
        }
    }

    /** Avança para a fase correta após o diálogo. */
    private void advancePhaseFromDialogue() {
        switch (dlgPhase) {
            case 0 -> startP1();
            case 1 -> startP2();
            case 2 -> startP3();
            case 3 -> startP4();
            case 4 -> startP5();
        }
    }

    private void menuKey(KeyCode k) {
        boolean navigated = false;
        if (k == KeyCode.UP   || k == KeyCode.W) { menuSel = (menuSel - 1 + 3) % 3; navigated = true; }
        if (k == KeyCode.DOWN || k == KeyCode.S) { menuSel = (menuSel + 1) % 3; navigated = true; }
        if (k == KeyCode.LEFT || k == KeyCode.A) {
            if (menuSel == 0) { cycleDiff(-1); navigated = true; }
            else if (menuSel == 1) { cycleHero(-1); navigated = true; }
        }
        if (k == KeyCode.RIGHT|| k == KeyCode.D) {
            if (menuSel == 0) { cycleDiff(+1); navigated = true; }
            else if (menuSel == 1) { cycleHero(+1); navigated = true; }
        }
        if (navigated) playSound(AudioCue.MENU_NAVIGATE);
        if (k == KeyCode.ENTER || k == KeyCode.SPACE || k == KeyCode.E) {
            switch (menuSel) {
                case 0 -> cycleDiff(+1);
                case 1 -> cycleHero(+1);
                case 2 -> { reset(); transitionToPhase(1); }
            }
            playSound(AudioCue.MENU_CONFIRM);
        }
    }

    public void reset() {
        hero.setMaxHP(difficulty.getInitialHP());
        hero.heal(difficulty.getInitialHP());
        hero.resetPosition(100, 768 / 2.0 - HeroEntity.HH / 2.0);
        tabletsCollected = 0;
        camera.setX(0);
        camera.clearShake();
        backtrackingActive = false;
        chestOpen = false;
        enemies.clear();
        projectiles.clear();
        guardians.clear();
        sceneryElements.clear();
        particleSystem.clear();
        alertMessage = "";
        phaseHits = 0;
        overlayActive = false;
        currentPhase = 0;
        boss = null;
        inCurrent = false;
        inPressureZone = false;
    }

    public void transitionToPhase(int targetPhase) {
        dlgPhase = targetPhase - 1;
        dlgIdx = 0;
        state = State.DIALOGUE;
        persistProgress();
    }

    // =========================================================
    // FASE 1 — Águas Claras (introdução, enguia corrigida)
    // =========================================================
    public void startP1() {
        enemies.clear(); projectiles.clear(); guardians.clear(); sceneryElements.clear();
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

        currentPhase = 1;
        publishAudioDepth();
        state = State.P1; phaseStartTime = nanoTime; phaseHits = 0; adaptiveDifficulty.beginPhase();
        if (difficulty == Difficulty.DIFICIL) {
            showAlert("Fase 1 (DIFÍCIL) — O portal final estará selado até você resgatar a Tabuleta!");
        } else {
            showAlert("Fase 1 — Aguas Claras | Encontre o Guardião Atlante e colete a Tabuleta!");
        }
    }

    // =========================================================
    // FASE 2 — Cavernas de Coral (bolhas desbloqueadas aqui)
    // =========================================================
    public void startP2() {
        enemies.clear(); projectiles.clear(); guardians.clear(); sceneryElements.clear();
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
        enemies.add(new EnemyEntity(EnemyType.DELFIN_ABISSAL, 2850, 768 * .42, spd * 1.05, 58));

        double gX = (difficulty == Difficulty.DIFICIL) ? 600 : 1400;
        guardians.add(new GuardianEntity(1, gX, 768 - GuardianEntity.GH - 60));

        // 1 gêiser no final — tutorial de mecânica, não obstáculo punitivo
        sceneryElements.add(new SceneryElement(SceneryElement.Type.GEYSER, 3200, 768 - 200, 70, 200));

        // === REDESIGN: 6 colunas de coral com padrão em S suave e legível ===
        // O gap percorre um arco gentil: centro → topo → centro → base → centro → topo → base
        // O jogador lê o padrão e antecipa. Salas abertas (~500px) entre colunas pares.
        // Coluna:      0     1     2     3     4     5
        double[] cxArr = {500, 1000, 1520, 2100, 2650, 3200};
        // gapY = Y onde começa o espaço livre (centro do canal)
        double[] gapY  = {284, 180,  320,  140,  360,  200};
        final double gapH = 420; // espaço livre generoso: 420px para o herói (corpo de 80px)
        for (int i = 0; i < cxArr.length; i++) {
            double topH = gapY[i];
            double botY = topH + gapH;
            double botH = 768 - botY;
            // Coral superior (só se houver espaço mínimo de 30px)
            if (topH > 30) sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, cxArr[i], 0, 95, topH));
            // Coral inferior
            if (botH > 30) sceneryElements.add(new SceneryElement(SceneryElement.Type.CORAL, cxArr[i], botY, 95, botH));
        }

        chestWX = 1800; chestWY = 768 - 170; // baú na sala aberta entre corais 2 e 3
        currentPhase = 2;
        publishAudioDepth();
        state = State.P2; phaseStartTime = nanoTime; phaseHits = 0; adaptiveDifficulty.beginPhase();
        showAlert("Fase 2 — Cavernas de Coral | Desperte o Poder das Bolhas no Galeão Naufragado!");
    }

    // =========================================================
    // FASE 3 — Correntes Abissais (nova mecânica principal)
    // =========================================================
    public void startP3() {
        enemies.clear(); projectiles.clear(); guardians.clear(); sceneryElements.clear();
        camera.setX(0); hero.beginPhaseAt(100, 768 / 2.0);
        double spd = 1.0;

        // === REDESIGN 2026-08-20: 4 inimigos (não 6), bem separados, cada um em
        //     território próprio para o jogador processar antes do próximo ===
        enemies.add(new EnemyEntity(EnemyType.PEIXE,     700,  768 * .42, spd * 1.2, 55));
        enemies.add(new EnemyEntity(EnemyType.ENGUIA,    1600, 768 * .28, spd * 1.3, 80));
        enemies.add(new EnemyEntity(EnemyType.MEDUSA,    2500, 768 * .55, spd * 1.1, 90));
        enemies.add(new EnemyEntity(EnemyType.CARANGUEJO,3400, 768 * .35, spd,       70));
        enemies.add(new EnemyEntity(EnemyType.DELFIN_ABISSAL, 2250, 768 * .44, spd * 1.1, 62));

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

        currentPhase = 3;
        publishAudioDepth();
        state = State.P3; phaseStartTime = nanoTime; phaseHits = 0; adaptiveDifficulty.beginPhase();
        showAlert("Fase 3 — Correntes Abissais | As águas vivem — lute contra a maré!");
    }

    // =========================================================
    // FASE 4 — Abismo Vulcânico (novos inimigos + lava)
    // =========================================================
    public void startP4() {
        enemies.clear(); projectiles.clear(); guardians.clear(); sceneryElements.clear();
        camera.setX(0); hero.beginPhaseAt(100, 768 / 2.0);
        double spd = 1.0;

        // === REDESIGN 2026-08-20: 5 inimigos (não 7), Leviatã como MOMENTO ESPECIAL ===
        // Abertura: arraião solitário para estabelecer o tema da fase
        enemies.add(new EnemyEntity(EnemyType.ARRAIAO,    600,  768 * .42, spd,       90));
        // Ato 1: peixe rápido + medusa
        enemies.add(new EnemyEntity(EnemyType.PEIXE,      1200, 768 * .28, spd * 1.3, 55));
        enemies.add(new EnemyEntity(EnemyType.MEDUSA,     1900, 768 * .55, spd * 1.2, 90));
        // === LEVIATÃ: 600px de espaço exclusivo (2400-3000px), isolado como sub-boss ===
        enemies.add(new EnemyEntity(EnemyType.LEVIATA,   2650, 768 * .38, spd * 1.3, 130));
        // Ato 3: arraião veloz após o Leviatã
        enemies.add(new EnemyEntity(EnemyType.ARRAIAO,   3400, 768 * .48, spd * 1.4, 90));
        enemies.add(new EnemyEntity(EnemyType.POLVO_ABISSAL, 3700, 768 * .58, spd * .82, 75));

        double gX = (difficulty == Difficulty.DIFICIL) ? 500 : 1000;
        guardians.add(new GuardianEntity(3, gX, 768 - GuardianEntity.GH - 60));

        // 2 piscinas de lava MAIORES e mais impactantes (não 3 menores)
        sceneryElements.add(new SceneryElement(SceneryElement.Type.LAVA_POOL, 1550, 768 - 100, 380, 100));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.LAVA_POOL, 3100, 768 - 100, 420, 100));

        // 2 rochas vulcânicas como portais de passagem estreita (desafio PONTUAL)
        sceneryElements.add(new SceneryElement(SceneryElement.Type.VOLCANIC_ROCK, 900,  0,      85, 260));
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

        currentPhase = 4;
        publishAudioDepth();
        state = State.P4; phaseStartTime = nanoTime; phaseHits = 0; adaptiveDifficulty.beginPhase();
        showAlert("Fase 4 — Abismo Vulcânico | A lava devora tudo — mantenha altitude!");
    }

    // =========================================================
    // FASE 5 — Templo Final de Apsu (boss + guardião Enki)
    // =========================================================
    public void startP5() {
        enemies.clear(); projectiles.clear(); guardians.clear(); sceneryElements.clear();
        // Fase 5 é a arena do boss — herói entra pela esquerda, boss à direita
        // BUGFIX 2026-08-15 (causa-raiz, complementar ao fix em updateP5()):
        // P5 nunca chama camera.update() — é arena fixa, hero/boss.getX() são
        // usados diretamente como coordenada de TELA. Sem este reset, a câmera
        // mantinha o offset acumulado da Fase 4 (até ~2634px), o que já causava
        // o burst de partícula fora da tela e poderia afetar qualquer outro
        // código futuro que assuma camera.getX()==0 durante a arena do boss.
        camera.setX(0);
        guardians.add(new GuardianEntity(4, 160, 768 / 2.0 - GuardianEntity.GH)); // Enki em pessoa!

        int bossVar = (difficulty == Difficulty.DIFICIL) ? (1 + (int)(Math.random() * 2)) : 0;
        boss = new BossEntity(bossVar, 1366 - BossEntity.BW - 80, 768 / 2.0 - BossEntity.BH / 2);
        boss.setLastShotTime(nanoTime);

        hero.beginPhaseAt(80, 768 / 2.0 - HeroEntity.HH / 2);

        // Correntes na arena do boss — obrigam o jogador a dominar a mecânica
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CURRENT, 400, 200, 150, 400).withCurrent(0, -3.5));
        sceneryElements.add(new SceneryElement(SceneryElement.Type.CURRENT, 800, 100, 150, 500).withCurrent(0, 4.0));

        // Zonas de pressão que mudam a dinâmica da arena
        sceneryElements.add(new SceneryElement(SceneryElement.Type.PRESSURE_ZONE, 550, 0, 120, 768).withBuoyancy(.76));
        enemies.add(new EnemyEntity(EnemyType.LULA_VAMPIRA, 710, 768 * .30, .78, 48));

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

    // =========================================================
    // UPDATE P1
    // =========================================================
    private void updateP1() {
        applyEnvironmentalForces();
        hero.updatePhysics(
            inputManager.isPressed(KeyCode.LEFT, KeyCode.A),
            inputManager.isPressed(KeyCode.RIGHT, KeyCode.D),
            inputManager.isPressed(KeyCode.UP, KeyCode.W),
            inputManager.isPressed(KeyCode.DOWN, KeyCode.S),
            30, 768 - HeroEntity.HH - 30, phaseDone,
            getEffectiveBuoyancy(),
            inCurrent ? currentFx : 0, inCurrent ? currentFy : 0, nanoTime
        );

        if (!overlayActive && inputManager.isPressed(KeyCode.SPACE)) tryShoot();
        camera.update(hero.getX());
        hero.setX(Math.max(30, Math.min(4000 - HeroEntity.HW - 30, hero.getX())));

        checkGuardians(false);
        checkSceneryCollisions();
        updateEnemies();
        updateProjectiles();
        checkEnemyCollisions();

        if (hero.getX() > 4000 - 200) {
            if (tabletsCollected >= 1) {
                phaseDone = Math.max(phaseDone, 1);
                transitionToPhase(2);
            } else if (difficulty == Difficulty.DIFICIL && !backtrackingActive) {
                backtrackingActive = true;
                enemies.add(new EnemyEntity(EnemyType.PEIXE,  1500, 768 * .40, 1.8, 50));
                enemies.add(new EnemyEntity(EnemyType.MEDUSA, 2200, 768 * .60, 2.0, 70));
                showAlert("Barreira Selada! Retroceda para encontrar o Guardiao escondido!");
            }
        }
    }

    // =========================================================
    // UPDATE P2 — B3-FIX: bolhas agora funcionam corretamente
    // =========================================================
    private void updateP2() {
        applyEnvironmentalForces();
        hero.updatePhysics(
            inputManager.isPressed(KeyCode.LEFT, KeyCode.A),
            inputManager.isPressed(KeyCode.RIGHT, KeyCode.D),
            inputManager.isPressed(KeyCode.UP, KeyCode.W),
            inputManager.isPressed(KeyCode.DOWN, KeyCode.S),
            60, 768 - HeroEntity.HH - 60, phaseDone,
            getEffectiveBuoyancy(),
            inCurrent ? currentFx : 0, inCurrent ? currentFy : 0, nanoTime
        );

        if (!overlayActive && inputManager.isPressed(KeyCode.SPACE)) tryShoot();
        camera.update(hero.getX());
        hero.setX(Math.max(30, Math.min(4000 - HeroEntity.HW - 30, hero.getX())));

        // Colisões com coral (coordenadas de mundo)
        for (SceneryElement elem : sceneryElements) {
            if (elem.getType() == SceneryElement.Type.CORAL) {
                if (heroHitsHazard(elem, 10, 0)) {
                    pushHeroOutOfObstacle(elem.getWorldX() + 10, elem.getWorldY(), elem.getWidth() - 20, elem.getHeight());
                    if (!hero.isInvulnerable()) {
                        boolean topCoral = elem.getWorldY() == 0;
                        hurtHero(difficulty.getCollisionDamage() * 0.65,
                            0, topCoral ? 5.5 : -5.5, Color.ORANGERED, 7);
                    }
                }
            }
        }

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

        // Baú (B3-FIX: coordenadas de mundo para colisão com baú)
        if (!chestOpen && inputManager.isInteractPressed()
                && rectsHit(hero.getX(), hero.getY(), HeroEntity.HW + 80, HeroEntity.HH + 80,
                            chestWX - 40, chestWY - 40, 160, 140)) {
            chestOpen = true; chestTime = nanoTime;
            hero.setHasBubblePower(true);
            double csx = camera.toScreenX(chestWX);
            particleSystem.addBurst(csx + 60, chestWY + 40, Color.GOLD);
            particleSystem.addBurst(csx + 60, chestWY + 40, Color.CYAN);
            playSound(AudioCue.PICKUP);
            playSound(AudioCue.VICTORY);
            showAlert("PODER DAS BOLHAS DESPERTADO! Pressione ESPAÇO para disparar! ");
        }

        updateEnemies();
        updateProjectiles();
        checkEnemyCollisions();

        if (hero.getX() > 4000 - 200) {
            if (tabletsCollected >= 2) {
                phaseDone = Math.max(phaseDone, 2);
                transitionToPhase(3);
            } else if (difficulty == Difficulty.DIFICIL && !backtrackingActive) {
                backtrackingActive = true;
                enemies.add(new EnemyEntity(EnemyType.ENGUIA,    1400, 768 * .50, 2.0, 60));
                enemies.add(new EnemyEntity(EnemyType.CARANGUEJO, 2500, 768 * .35, 2.2, 80));
                showAlert("Barreira Selada! Retroceda para encontrar o Guardiao nas cavernas!");
            }
        }
    }

    // =========================================================
    // UPDATE P3 — Correntes Abissais
    // =========================================================
    private void updateP3() {
        applyEnvironmentalForces();
        hero.updatePhysics(
            inputManager.isPressed(KeyCode.LEFT, KeyCode.A),
            inputManager.isPressed(KeyCode.RIGHT, KeyCode.D),
            inputManager.isPressed(KeyCode.UP, KeyCode.W),
            inputManager.isPressed(KeyCode.DOWN, KeyCode.S),
            20, 768 - HeroEntity.HH - 20, phaseDone,
            getEffectiveBuoyancy(),
            inCurrent ? currentFx : 0, inCurrent ? currentFy : 0, nanoTime
        );

        if (!overlayActive && inputManager.isPressed(KeyCode.SPACE)) tryShoot();
        camera.update(hero.getX());
        hero.setX(Math.max(30, Math.min(4000 - HeroEntity.HW - 30, hero.getX())));

        checkGuardians(false);
        checkSceneryCollisions();
        updateEnemies();
        updateProjectiles();
        checkEnemyCollisions();

        if (hero.getX() > 4000 - 200) {
            if (tabletsCollected >= 3) {
                phaseDone = Math.max(phaseDone, 3);
                transitionToPhase(4);
            } else if (difficulty == Difficulty.DIFICIL && !backtrackingActive) {
                backtrackingActive = true;
                enemies.add(new EnemyEntity(EnemyType.ENGUIA, 2000, 768 * .40, 2.2, 80));
                showAlert("Barreira de Correntes! Encontre o Lamassu Aquatico antes de avancar!");
            }
        }
    }

    // =========================================================
    // UPDATE P4 — Abismo Vulcânico
    // =========================================================
    private void updateP4() {
        applyEnvironmentalForces();
        hero.updatePhysics(
            inputManager.isPressed(KeyCode.LEFT, KeyCode.A),
            inputManager.isPressed(KeyCode.RIGHT, KeyCode.D),
            inputManager.isPressed(KeyCode.UP, KeyCode.W),
            inputManager.isPressed(KeyCode.DOWN, KeyCode.S),
            20, 768 - HeroEntity.HH - 20, phaseDone,
            getEffectiveBuoyancy(),
            inCurrent ? currentFx : 0, inCurrent ? currentFy : 0, nanoTime
        );

        if (!overlayActive && inputManager.isPressed(KeyCode.SPACE)) tryShoot();
        camera.update(hero.getX());
        hero.setX(Math.max(30, Math.min(4000 - HeroEntity.HW - 30, hero.getX())));

        // Dano contínuo em zonas de lava (leve por segundo)
        long currentSec = nanoTime / 1_000_000_000L;
        for (SceneryElement elem : sceneryElements) {
            if (elem.getType() == SceneryElement.Type.LAVA_POOL && !hero.isInvulnerable()) {
                if (heroFeetHit(elem)) {
                    hurtHero(difficulty.getCollisionDamage() * 0.45,
                        0, -5.0, Color.ORANGERED, 7);
                    break;
                }
            }
        }

        checkGuardians(false);
        checkSceneryCollisions();
        updateEnemies();
        updateProjectiles();
        checkEnemyCollisions();

        if (hero.getX() > 4000 - 200) {
            if (tabletsCollected >= 4) {
                phaseDone = Math.max(phaseDone, 4);
                transitionToPhase(5);
            } else if (difficulty == Difficulty.DIFICIL && !backtrackingActive) {
                backtrackingActive = true;
                enemies.add(new EnemyEntity(EnemyType.ARRAIAO, 1800, 768 * .45, 2.5, 90));
                showAlert(" Selado pelas chamas! Encontre o Oraculo das Correntes primeiro!");
            }
        }
    }

    // =========================================================
    // UPDATE P5 — Templo Final (boss fight)
    // =========================================================
    private void updateP5() {
        applyEnvironmentalForces();
        hero.updatePhysics(
            inputManager.isPressed(KeyCode.LEFT, KeyCode.A),
            inputManager.isPressed(KeyCode.RIGHT, KeyCode.D),
            inputManager.isPressed(KeyCode.UP, KeyCode.W),
            inputManager.isPressed(KeyCode.DOWN, KeyCode.S),
            20, 768 - HeroEntity.HH - 20, phaseDone,
            getEffectiveBuoyancy(),
            inCurrent ? currentFx : 0, inCurrent ? currentFy : 0, nanoTime
        );
        hero.setX(Math.max(20, Math.min(1366 - HeroEntity.HW - 20, hero.getX())));

        checkGuardians(true); // arena sem câmera

        if (!overlayActive && inputManager.isPressed(KeyCode.SPACE) && nanoTime - hero.getLastShotTime() > 260_000_000L) {
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

            // Colisão Direta Herói <-> Boss
            if (!hero.isInvulnerable() && heroHits(boss.getX() + 18, boss.getY() + 18,
                                                    BossEntity.BW - 36, BossEntity.BH - 36)) {
                // BUGFIX 2026-08-15: P5 é arena fixa (sem scroll de câmera) — hero.getX()
                // já É a coordenada de tela nessa fase. camera.toScreenX() aqui subtraía o
                // offset residual deixado pela Fase 4 (camera.setX nunca é resetado ao
                // entrar em P5), jogando a partícula pra fora da tela.
                double direction = hero.getX() + HeroEntity.HW / 2 < boss.getX() + BossEntity.BW / 2 ? -1 : 1;
                hurtHero(difficulty.getCollisionDamage() * 0.65, direction * 5.5, -3.0, Color.PURPLE, 8);
            }

            // Cada estágio tem um padrão próprio. Durante os 0,5 s do
            // telegrafo o jogador recebe uma janela clara para reposicionar.
            long baseInterval = (difficulty == Difficulty.FACIL ? 1_800_000_000L : (difficulty == Difficulty.MEDIO ? 1_200_000_000L : 850_000_000L));
            double phaseTempo = switch (boss.getCombatPhase()) {
                case OBSIDIANA_FRIA -> 1.0;
                case FUSAO_VULCANICA -> 0.82;
                case FURIA_DE_APSU -> 0.68;
            };
            long bullInterval = Math.max(MIN_BOSS_ATTACK_INTERVAL_NANOS,
                (long)(baseInterval * phaseTempo / (getThreatMultiplier() * BossEntity.SPD_MULT[boss.getVariant()])));
            if (!boss.isTelegraphing(nanoTime) && nanoTime - boss.getLastShotTime() > bullInterval) {
                boss.setLastShotTime(nanoTime);
                double bfx = boss.getX();
                double bfy = boss.getY() + BossEntity.BH / 2;
                double tx = hero.getX() + HeroEntity.HW/2 - bfx, ty = hero.getY() + HeroEntity.HH/2 - bfy;
                double baseAngle = Math.atan2(ty, tx);
                double spread = BossEntity.SPREAD[boss.getVariant()];
                double spd = (difficulty == Difficulty.FACIL ? 4.5 : 7.0) * getThreatMultiplier() * BossEntity.SPD_MULT[boss.getVariant()];
                switch (boss.getCombatPhase()) {
                    case OBSIDIANA_FRIA -> fireBossFan(bfx, bfy, baseAngle, spread,
                        difficulty == Difficulty.FACIL ? 3 : (difficulty == Difficulty.MEDIO ? 4 : 5), spd);
                    case FUSAO_VULCANICA -> {
                        // Onda circular: força leitura espacial, não só reflexo.
                        int count = difficulty == Difficulty.FACIL ? 6 : 8;
                        for (int i = 0; i < count; i++) {
                            double angle = (Math.PI * 2 * i) / count;
                            projectiles.add(new Projectile(Projectile.Type.BOSS_BULLET, bfx, bfy,
                                Math.cos(angle) * spd * 0.78, Math.sin(angle) * spd * 0.78, 0.6, true));
                        }
                    }
                    case FURIA_DE_APSU -> {
                        // Meteoros de lava caem de pontos alternados do teto.
                        int count = difficulty == Difficulty.FACIL ? 3 : 5;
                        for (int i = 0; i < count; i++) {
                            double meteorX = 260 + Math.floorMod(nanoTime / 100_000_000L + i * 193L, 820L);
                            projectiles.add(new Projectile(Projectile.Type.BOSS_BULLET, meteorX, -12,
                                Math.sin(i * 2.1) * 1.4, spd * 1.22, 0.7, true));
                        }
                    }
                }
            }
        }

        // Correntes e zonas de pressão na arena do boss — coordenadas de tela (sem scroll)
        updateEnemies();
        checkEnemyCollisions();
        checkSceneryCollisionsArena();
        updateProjectiles();
    }

    private void fireBossFan(double x, double y, double baseAngle, double spread, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = baseAngle + (i - (count - 1) / 2.0) * (spread / Math.max(count - 1, 1));
            projectiles.add(new Projectile(Projectile.Type.BOSS_BULLET, x, y,
                Math.cos(angle) * speed, Math.sin(angle) * speed, 0.5, true));
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
                double ex = (state == State.P5) ? elem.getWorldX() : elem.getWorldX();
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
        for (SceneryElement elem : sceneryElements) {
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
                default -> {} // CORAL já tratado no updateP2
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

    /** Verifica interação com guardiões. */
    private void checkGuardians(boolean arenaMode) {
        for (GuardianEntity g : guardians) {
            double gx = arenaMode ? g.getWorldX() : camera.toScreenX(g.getWorldX());
            double heroScreenX = arenaMode ? hero.getX() : camera.toScreenX(hero.getX());
            if (!g.isContacted() && inputManager.isInteractPressed()
                    && rectsHit(heroScreenX, hero.getY(), HeroEntity.HW, HeroEntity.HH,
                                gx - 30, g.getWorldY() - 30, GuardianEntity.GW + 60, GuardianEntity.GH + 60)) {
                g.setContacted(true);
                final double finalGx = gx;
                openOverlay(g.getType(), g.getName(), g.getDialogue(), () -> {
                    tabletsCollected++;
                    hero.heal(2.0);
                    particleSystem.addBurst(finalGx + GuardianEntity.GW/2, g.getWorldY() + GuardianEntity.GH/2, Color.GOLD);
                    particleSystem.addBurst(finalGx + GuardianEntity.GW/2, g.getWorldY() + GuardianEntity.GH/2, Color.LIMEGREEN);
                    playSound(AudioCue.PICKUP);
                    showAlert("Tabuleta " + tabletsCollected + "/" + TOTAL_TABLETS + " recebida! +2 de vida restaurada!");
                    persistProgress();
                });
                break;
            }
        }
    }

    // =========================================================
    // INIMIGOS
    // =========================================================
    private void updateEnemies() {
        double t = nanoTime / 1_000_000_000.0;
        boolean indexNeeded = false;
        for (Projectile projectile : projectiles) {
            if (projectile.getType() == Projectile.Type.HERO_BUBBLE) {
                indexNeeded = true;
                break;
            }
        }
        if (indexNeeded) enemyQuadTree.clear();

        for (EnemyEntity e : enemies) {
            if (!e.isAlive()) continue;
            e.update(t, getThreatMultiplier());
            if (indexNeeded) enemyQuadTree.insert(e);

            if (difficulty.canEnemiesShoot()) {
                if (e.getLastShotTime() == 0) {
                    e.setLastShotTime(nanoTime);
                    continue;
                }
                double ex = camera.toScreenX(e.getWorldX());
                double heroScreenX = camera.toScreenX(hero.getX());
                if (ex > 0 && ex < 1366 && (nanoTime - e.getLastShotTime()) > 2_400_000_000L) {
                    e.setLastShotTime(nanoTime);
                    double dx = heroScreenX + HeroEntity.HW/2 - ex;
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
                        e.setAlive(false);
                        adaptiveDifficulty.recordEnemyDefeated();
                        double screenPx = isArena ? p.getX() : camera.toScreenX(p.getX());
                        particleSystem.addBurst(screenPx, p.getY(), Color.AQUAMARINE);
                        playSound(AudioCue.ENEMY_DAMAGED);
                        playSound(AudioCue.ENEMY_DEFEATED);
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
                            camera.applyShake(16, 420_000_000L, nanoTime);
                            particleSystem.addBurst(boss.getX() + BossEntity.BW/2, boss.getY() + BossEntity.BH/2, Color.GOLD);
                            playSound(AudioCue.VICTORY);
                            publishBossMusic(false);
                            state = State.WIN;
                            phaseDone = 5;
                            persistProgress();
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
                // Boss bullet em tela (P5)
                if (!hero.isInvulnerable() && heroHits(p.getX() - 6, p.getY() - 6, 12, 12)) {
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
        enemyQuadTree.retrieve(nearbyEnemiesBuffer, hero.getX() - 20, hero.getY() - 20, HeroEntity.HW + 40, HeroEntity.HH + 40);

        for (EnemyEntity e : nearbyEnemiesBuffer) {
            if (!e.isAlive()) continue;
            // B3-FIX: coordenadas de mundo para colisão consistente
            if (heroHitsEnemy(e)) {
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
        if (nanoTime - hero.getLastShotTime() < 260_000_000L) return;
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
            projectiles.add(new Projectile(Projectile.Type.HERO_BUBBLE, bx, by, vx, 0, 1.0, isArena));
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
            hero.setX(ox - hw - 20 - 4);
        } else if (minOverlap == overlapRight) {
            hero.setX(ox + ow - 20 + 4);
        } else if (minOverlap == overlapTop) {
            hero.setY(oy - hh - 44 - 4);
        } else {
            hero.setY(oy + oh - 44 + 4);
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
        // Inimigo precisa sobrepor 55% central do corpo — evita dano à distância
        return heroHits(enemy.getWorldX() + width * 0.225, enemy.getCurrentY() + height * 0.225,
            width * 0.55, height * 0.55);
    }

    /** Aplica dano uma única vez e cria separação suficiente para o jogador reagir. */
    private boolean hurtHero(double damage, double knockbackX, double knockbackY, Color color, int shake) {
        if (hero.isInvulnerable()) return false;
        hero.applyDamage(damage, nanoTime);
        hero.applyKnockback(knockbackX, knockbackY);
        camera.applyShake(shake, 150_000_000L, nanoTime);
        double screenX = state == State.P5 ? hero.getX() : camera.toScreenX(hero.getX());
        particleSystem.addBurst(screenX + HeroEntity.HW / 2, hero.getY() + HeroEntity.HH / 2, color);
        playSound(AudioCue.HERO_DAMAGED);
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
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
        hero.setMaxHP(difficulty.getInitialHP());
    }
    public HeroType getHeroType() { return heroType; }
    public void setHeroType(HeroType heroType) { this.heroType = heroType; hero.setType(heroType); }
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

    /**
     * Disponibiliza o layout paralelo da fase ativa quando a execução foi
     * iniciada com {@code -Dapsu.map.layoutDir=...}. A ausência do arquivo é
     * intencionalmente segura: o layout autoral da fase continua sendo usado.
     */
    public Optional<MPIMapLoader.Layout> getGeneratedMapLayout() {
        return currentPhase == 0 ? Optional.empty() : mpiMapLoader.loadConfiguredPhase(currentPhase);
    }
    public int getDlgPhase() { return dlgPhase; }
    public int getDlgIdx() { return dlgIdx; }
    public void setDlgIdx(int idx) { this.dlgIdx = idx; }
    public int getMenuSel() { return menuSel; }
    public void setMenuSel(int menuSel) { this.menuSel = menuSel; }
    public boolean isChestOpen() { return chestOpen; }
    public double getChestWX() { return chestWX; }
    public double getChestWY() { return chestWY; }
    public long getChestTime() { return chestTime; }
    public boolean isInCurrent() { return inCurrent; }
    public double getCurrentFx() { return currentFx; }
    public double getCurrentFy() { return currentFy; }
    public boolean isInPressureZone() { return inPressureZone; }
    public double getPressureBuoyMult() { return pressureBuoyMult; }
    public int getCurrentPhase() { return currentPhase; }
}
