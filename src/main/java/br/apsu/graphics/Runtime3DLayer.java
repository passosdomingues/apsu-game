package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.model.boss.BossEntity;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.enemy.EnemyType;
import br.apsu.model.guardian.GuardianEntity;
import br.apsu.model.environment.SceneryElement;
import br.apsu.model.hero.HeroEntity;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.AmbientLight;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;

/** Malhas 3D em tempo real sincronizadas ao Canvas e à câmera lateral. */
public final class Runtime3DLayer {
    private static final double VIEW_W = 1366, VIEW_H = 768;
    private static final boolean PERF_LOG = Boolean.getBoolean("apsu.perf");
    private static final String[] ENEMY_IDS = createIds("enemy-", 32);
    private static final String[] GUARDIAN_IDS = createIds("guardian-", 16);
    private static final String[] SCENERY_IDS = createIds("scenery-", 64);
    private static final double WRECK_RENDER_WIDTH = 620;
    private record PhasePalette(Color character, Color enemy, Color prop, Color accent, Color boss) {}
    // 60/30/10 color hierarchy: phase panorama remains the dominant 60%;
    // textured actors and props receive a restrained 30% supporting grade;
    // warm treasure/guardian/boss highlights reserve the 10% focal accent.
    private static final PhasePalette[] PHASE_PALETTES = {
        new PhasePalette(Color.rgb(255, 247, 225), Color.rgb(232, 248, 255), Color.rgb(239, 249, 246), Color.rgb(255, 223, 154), Color.rgb(255, 224, 212)),
        new PhasePalette(Color.rgb(255, 242, 222), Color.rgb(221, 242, 250), Color.rgb(247, 236, 214), Color.rgb(255, 212, 138), Color.rgb(255, 220, 204)),
        new PhasePalette(Color.rgb(235, 244, 255), Color.rgb(220, 229, 255), Color.rgb(218, 236, 244), Color.rgb(255, 210, 126), Color.rgb(255, 211, 205)),
        new PhasePalette(Color.rgb(224, 244, 250), Color.rgb(249, 223, 221), Color.rgb(249, 231, 210), Color.rgb(255, 197, 112), Color.rgb(255, 205, 194)),
        new PhasePalette(Color.rgb(225, 243, 249), Color.rgb(222, 232, 255), Color.rgb(219, 234, 239), Color.rgb(255, 207, 130), Color.rgb(255, 208, 194))
    };
    private final Group world = new Group();
    private final Group scenery = new Group();
    private final Group actors = new Group();
    private final SubScene view;
    private final Map<String, Actor> actorsById = new HashMap<>();
    private final Map<String, ObjModelLoader.Model> modelTemplates = new HashMap<>();
    private final Map<String, CompletableFuture<ObjModelLoader.Model>> pendingModels = new HashMap<>();
    private final Map<String, String> modelNames = new HashMap<>();
    private final ExecutorService modelLoader = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "apsu-3d-model-loader");
        thread.setDaemon(true);
        return thread;
    });
    private java.util.List<Actor> visibleActors = new java.util.ArrayList<>();
    private java.util.List<Actor> nextVisibleActors = new java.util.ArrayList<>();
    private final java.util.Set<String> failedModels = new java.util.HashSet<>();
    private double shakeX, shakeY;
    private static final String[] ENKI_MODELS = {
        "03_enki_npc", "03_enki_var_celestial", "03_enki_var_eremita"
    };

    private static final class Actor {
        final String modelName;
        final Group node;
        final double targetWidth, targetHeight;
        final Color tint;
        final Rotate yaw = new Rotate(180, Rotate.Y_AXIS);
        final Rotate pitch = new Rotate(0, Rotate.X_AXIS);
        final Rotate bodyMotion = new Rotate(0, Rotate.Z_AXIS);
        boolean usedThisFrame;
        Actor(String modelName, Group node, double targetWidth, double targetHeight, Color tint) {
            this.modelName = modelName;
            this.node = node;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
            this.tint = tint;
            node.getTransforms().setAll(yaw, pitch, bodyMotion);
        }
    }

    public Runtime3DLayer() {
        AmbientLight ambient = new AmbientLight(Color.color(0.72, 0.78, 0.88));
        world.getChildren().add(ambient);
        world.getChildren().addAll(scenery, actors);
        view = new SubScene(world, VIEW_W, VIEW_H, true, javafx.scene.SceneAntialiasing.DISABLED);
        view.setFill(Color.TRANSPARENT);
        view.setMouseTransparent(true);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(30);
        camera.setNearClip(0.1);
        camera.setFarClip(5000);
        camera.setTranslateX(VIEW_W / 2);
        camera.setTranslateY(VIEW_H / 2);
        camera.setTranslateZ(-1433);
        view.setCamera(camera);
    }

    public Node view() { return view; }

    public void setViewportScale(double scale) {
        view.setScaleX(scale);
        view.setScaleY(scale);
    }

    public void update(GameContext context, double timeSeconds, double shakeX, double shakeY) {
        this.shakeX = shakeX;
        this.shakeY = shakeY;
        if (!view.isVisible()) view.setVisible(true);
        for (Actor cached : visibleActors) cached.usedThisFrame = false;
        nextVisibleActors.clear();
        activePhase = context.getCurrentPhase();
        GameContext.State state = context.getState();
        if (state == GameContext.State.MENU) {
            if (!actors.isVisible()) actors.setVisible(true);
            requestModel("characters/01_adapa_ataque_thrust");
            Actor hero = actor("menu-hero", "characters/01_adapa_heroi", 112, 210);
            if (hero != null) place(hero, VIEW_W * 0.82, 255, 0.0, timeSeconds, 0);
            finishVisibleActors();
            return;
        }
        if (state == GameContext.State.DIALOGUE) {
            if (!actors.isVisible()) actors.setVisible(true);
            int phase = Math.max(0, Math.min(4, context.getCurrentPhase() - 1));
            Actor npc = actor("phase-intro-enki", ENKI_MODELS[phase % ENKI_MODELS.length], 120, 210);
            if (npc != null) place(npc, 120, VIEW_H / 2.0, 0.2, timeSeconds, 0);
            Actor hero = actor("dialogue-hero", "characters/01_adapa_heroi", 90, 190);
            if (hero != null) place(hero, VIEW_W * 0.76, VIEW_H * 0.67, 0.25, timeSeconds, 0);
            finishVisibleActors();
            return;
        }
        if (state.ordinal() < GameContext.State.P1.ordinal() || state.ordinal() > GameContext.State.P5.ordinal()) {
            finishVisibleActors();
            return;
        }
        if (context.getCurrentPhase() == 2) {
            requestModel("scenery/07_navio_naufragado_elemento-cenario");
            requestModel("scenery/06_bau_tesouro_elemento-cenario");
        }

        // Personagens, inimigos e props usam malhas Blender na camada 3D.
        int enemyIndex = 0;
        for (EnemyEntity enemy : context.getEnemies()) {
            int id = enemyIndex++;
            if (!enemy.isAlive()) continue;
            EnemyType type = enemy.getType();
            double screenX = context.getCamera().toScreenX(enemy.getWorldX()) + type.getWidth() / 2.0;
            if (!insideViewport(screenX, type.getWidth())) continue;
            Actor enemyActor = actor(id < ENEMY_IDS.length ? ENEMY_IDS[id] : "enemy-" + id,
                modelName(type.getStaticSprite()), type.getWidth(), type.getHeight());
            if (enemyActor != null) place(enemyActor,
                screenX,
                enemy.getCurrentY() + type.getHeight() / 2.0,
                enemy.getWorldX() / 4000.0, timeSeconds, 54);
        }

        HeroEntity hero = context.getHero();
        boolean attacking = hero.isShooting();
        String heroModel = attacking ? "characters/01_adapa_ataque_thrust" : "characters/01_adapa_heroi";
        Actor heroActor = actor(attacking ? "hero-attack" : "hero-swim", heroModel, 90, 205);
        // Se o modelo de ataque ainda estiver sendo carregado, mantém o modelo
        // de nado visível até o asset ficar pronto; nunca pisca para vazio.
        if (heroActor == null && attacking) {
            heroActor = actor("hero-swim", "characters/01_adapa_heroi", 90, 205);
        }
        if (heroActor != null) {
            double heroX = context.getCamera().toScreenX(hero.getX()) + HeroEntity.HW / 2.0;
            double heroY = hero.getY() + HeroEntity.HH / 2.0;
            double facingSign = hero.isFacingRight() ? 1 : -1;
            double facingYaw = hero.isFacingRight() ? 0 : 180;
            double attackProgress = hero.getAttackProgress(context.getNanoTime());
            double attackLunge = Math.sin(Math.PI * attackProgress) * 8 * facingSign;
            heroActor.bodyMotion.setAngle(attacking
                ? -Math.sin(Math.PI * attackProgress) * 3.5 * facingSign
                : Math.sin(timeSeconds * (3.0 + hero.getCurrentSpeedRatio() * 3.5)) * 0.7);
            place(heroActor, heroX + attackLunge, heroY,
                hero.getX() / 4000.0, timeSeconds, facingYaw);
        }

        int guardianIndex = 0;
        for (GuardianEntity guardian : context.getGuardians()) {
            double screenX = context.getCamera().toScreenX(guardian.getWorldX()) + GuardianEntity.GW / 2.0;
            if (!insideViewport(screenX, GuardianEntity.GW)) { guardianIndex++; continue; }
            String model = modelName(guardian.getSpritePath());
            int id = guardianIndex++;
            int type = guardian.getType();
            double targetWidth = type == 0 ? 145 : type == 4 ? 120 : 125;
            double targetHeight = type == 0 ? 230 : type == 4 ? 210 : 200;
            Actor npc = actor(id < GUARDIAN_IDS.length ? GUARDIAN_IDS[id] : "guardian-" + id,
                model, targetWidth, targetHeight);
            if (npc != null) place(npc,
                screenX,
                guardian.getWorldY() + GuardianEntity.GH / 2.0,
                guardian.getWorldX() / 4000.0, timeSeconds, 0);
        }

        BossEntity boss = context.getBoss();
        if (state == GameContext.State.P5 && boss != null) {
            Actor bossActor = actor("boss", "characters/02_kullullu_boss", BossEntity.RENDER_W, BossEntity.RENDER_H);
            if (bossActor != null) {
                // The Blender export includes a long spear and asymmetric
                // depth. After turning the boss left, its body centroid is
                // -15.5% of model width in X and +50.5% in screen Y from bounds
                // center; keep the body, hitbox and HUD on one screen anchor.
                double bodyX = boss.getX() + BossEntity.BW / 2.0;
                double bodyY = boss.getY() + BossEntity.BH / 2.0;
                place(bossActor, bodyX - BossEntity.RENDER_W * -0.155,
                    bodyY - BossEntity.RENDER_W * 0.505, 0.5, timeSeconds, 90);
            }
        }

        updateScenery(context, timeSeconds);
        finishVisibleActors();
    }

    private void finishVisibleActors() {
        for (Actor previous : visibleActors) {
            if (!previous.usedThisFrame) previous.node.setVisible(false);
        }
        java.util.List<Actor> previous = visibleActors;
        visibleActors = nextVisibleActors;
        nextVisibleActors = previous;
        nextVisibleActors.clear();
    }

    private void updateScenery(GameContext context, double timeSeconds) {
        int index = 0;
        for (SceneryElement element : context.getSceneryElements()) {
            String model = sceneryModel(element.getType());
            if (model == null) continue;
            double screenX = context.getCamera().toScreenX(element.getWorldX());
            if (screenX < -260 || screenX > VIEW_W + 260) { index++; continue; }
            int elementIndex = index++;
            double propWidth = element.getWidth();
            double propHeight = element.getHeight();
            if (element.getType() == SceneryElement.Type.CURRENT
                || element.getType() == SceneryElement.Type.PRESSURE_ZONE) {
                propWidth = Math.max(150, element.getWidth() * 0.9);
                propHeight = Math.max(320, element.getHeight() * 0.85);
            } else if (element.getType() == SceneryElement.Type.GEYSER) {
                propWidth = 72;
                propHeight = 190;
            }
            String id = elementIndex < SCENERY_IDS.length ? SCENERY_IDS[elementIndex] : "scenery-" + elementIndex;
            Actor prop = actor(id, "scenery/" + model,
                Math.max(48, propWidth), Math.max(48, propHeight));
            if (prop != null) place(prop, screenX + element.getWidth() / 2,
                element.getWorldY() + element.getHeight() / 2,
                element.getWorldX() / 4000.0, timeSeconds, 0);
        }
        if (context.getCurrentPhase() == 2) {
            // The mesh's original built-in chest sits about 3% right and 5%
            // below its hull center. Keep the interactive reward at that exact
            // broken-midship opening after its replacement mesh is removed.
            double wreckWorldX = context.getChestWX() - WRECK_RENDER_WIDTH * 0.03;
            double wreckWorldY = context.getChestWY() - WRECK_RENDER_WIDTH * 0.05;
            double wreckX = context.getCamera().toScreenX(wreckWorldX);
            if (insideViewport(wreckX, WRECK_RENDER_WIDTH)) {
                Actor wreck = actor("shipwreck", "scenery/07_navio_naufragado_elemento-cenario", WRECK_RENDER_WIDTH, 360);
                if (wreck != null) place(wreck, wreckX, wreckWorldY, .20, timeSeconds, 0);
            }
            double chestX = context.getCamera().toScreenX(context.getChestWX());
            if (insideViewport(chestX, 84)) {
                Actor chest = actor("chest", "scenery/06_bau_tesouro_elemento-cenario", 84, 66);
                if (chest != null) place(chest, chestX, context.getChestWY(), .26, timeSeconds, 0);
            }
        }
        // Marco de descida: ruína atlante renderizada como objeto 3D real.
        if (context.getCurrentPhase() >= 1 && context.getCurrentPhase() <= 4) {
            double exitX = context.getCamera().toScreenX(3900);
            if (insideViewport(exitX, 300)) {
                Actor exitMarker = actor("phase-descent", "scenery/09_ruinas_e_colunas_atlantis", 280, 360);
                if (exitMarker != null) place(exitMarker, exitX, VIEW_H / 2, .8, timeSeconds, 0);
            }
        }
    }

    static String sceneryModel(SceneryElement.Type type) {
        return switch (type) {
            // The only available reef OBJ is a complete reef-and-school set,
            // not an individual coral obstacle. Repeating it at each collision
            // column scattered full set pieces across the whole phase.
            case CORAL -> null;
            case CURRENT, PRESSURE_ZONE -> "16_corrente_abissal_3d";
            case MOVING_OBSTACLE -> "13_obstaculo_abissal";
            case GEYSER -> "14_geiser_hidrotermal_3d";
            case LAVA_POOL -> "17_piscina_lava_3d";
            case VOLCANIC_ROCK -> "12_obstaculo_vulcanico";
            default -> null;
        };
    }

    private Actor actor(String id, String modelName, double targetWidth, double targetHeight) {
        if (failedModels.contains(modelName)) return null;
        Color tint = paletteTint(contextPhase(), id);
        Actor current = actorsById.get(id);
        if (current != null && current.modelName.equals(modelName)
            && current.targetWidth == targetWidth && current.targetHeight == targetHeight
            && current.tint.equals(tint)) return current;
        if (current != null) {
            current.node.setVisible(false);
            (current.modelName.startsWith("scenery/") ? scenery : actors).getChildren().remove(current.node);
        }
        try {
            ObjModelLoader.Model template = modelTemplates.get(modelName);
            if (template == null) {
                CompletableFuture<ObjModelLoader.Model> pending = requestModel(modelName);
                if (!pending.isDone()) return null;
                pendingModels.remove(modelName);
                template = pending.join();
                modelTemplates.put(modelName, template);
            }
            ObjModelLoader.Model model = ObjModelLoader.instance(template, targetWidth, targetHeight, tint);
            Actor loaded = new Actor(modelName, model.node(), targetWidth, targetHeight, tint);
            loaded.node.setVisible(false);
            actorsById.put(id, loaded);
            (modelName.startsWith("scenery/") ? scenery : actors).getChildren().add(loaded.node);
            return loaded;
        } catch (RuntimeException error) {
            Throwable cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause() : error;
            System.err.println("[3D] Não foi possível carregar " + modelName + ": " + cause.getMessage());
            failedModels.add(modelName);
            actorsById.remove(id);
            return null;
        }
    }

    private int contextPhase() {
        return Math.max(1, Math.min(5, activePhase));
    }

    private int activePhase = 1;

    static Color paletteTint(int phase, String actorId) {
        PhasePalette palette = PHASE_PALETTES[Math.max(0, Math.min(4, phase - 1))];
        if (actorId.equals("boss")) return palette.boss();
        if (actorId.equals("chest") || actorId.equals("phase-descent")
            || actorId.startsWith("guardian-") || actorId.equals("phase-intro-enki")) return palette.accent();
        if (actorId.startsWith("enemy-")) return palette.enemy();
        if (actorId.contains("hero")) return palette.character();
        return palette.prop();
    }

    private CompletableFuture<ObjModelLoader.Model> requestModel(String modelName) {
        if (modelTemplates.containsKey(modelName)) return CompletableFuture.completedFuture(modelTemplates.get(modelName));
        return pendingModels.computeIfAbsent(modelName, name -> CompletableFuture.supplyAsync(() -> {
            long loadStart = PERF_LOG ? System.nanoTime() : 0;
            try {
                ObjModelLoader.Model loaded = ObjModelLoader.load(name, 1, 1);
                if (PERF_LOG) System.out.printf(java.util.Locale.ROOT,
                    "[PERF] OBJ async %s: %.1f ms%n", name,
                    (System.nanoTime() - loadStart) / 1_000_000.0);
                return loaded;
            } catch (IOException error) {
                throw new CompletionException(error);
            }
        }, modelLoader));
    }

    private void place(Actor actor, double centerX, double centerY, double phase, double time, double yaw) {
        place(actor, centerX, centerY, phase, time, yaw, 0);
    }

    private void place(Actor actor, double centerX, double centerY, double phase, double time, double yaw, double pitch) {
        // A câmera já está posicionada no centro da viewport (683, 384), e a
        // projeção perspectiva foi calibrada para coordenadas de tela. Subtrair
        // esse centro aqui deslocava todos os modelos para fora da SubScene.
        actor.node.setTranslateX(centerX + shakeX);
        actor.node.setTranslateY(centerY + shakeY);
        actor.node.setTranslateZ(phase * 2.0);
        actor.yaw.setAngle(180 + yaw);
        actor.pitch.setAngle(Math.max(-20, Math.min(20, pitch)));
        if (!actor.usedThisFrame) {
            actor.usedThisFrame = true;
            nextVisibleActors.add(actor);
        }
        if (!actor.node.isVisible()) actor.node.setVisible(true);
    }

    private String modelName(String spritePath) {
        return modelNames.computeIfAbsent(spritePath, path -> {
            String name = toModelName(path);
            if (path.startsWith("sprites/scenery/")) return "scenery/" + name;
            if (path.startsWith("sprites/adapa/") || path.startsWith("sprites/kullullu/")) {
                return "characters/" + name;
            }
            return name;
        });
    }

    private static String toModelName(String spritePath) {
        String name = spritePath.substring(spritePath.lastIndexOf('/') + 1);
        int extension = name.lastIndexOf('.');
        return extension < 0 ? name : name.substring(0, extension);
    }

    private static String[] createIds(String prefix, int count) {
        String[] ids = new String[count];
        for (int i = 0; i < count; i++) ids[i] = prefix + i;
        return ids;
    }

    private static boolean insideViewport(double centerX, double width) {
        return centerX + width / 2 >= -180 && centerX - width / 2 <= VIEW_W + 180;
    }
}
