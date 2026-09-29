package br.apsu.graphics;

import br.apsu.core.GameContext;
import br.apsu.model.boss.BossEntity;
import br.apsu.model.enemy.EnemyEntity;
import br.apsu.model.enemy.EnemyType;
import br.apsu.model.guardian.GuardianEntity;
import br.apsu.model.hero.HeroEntity;
import br.apsu.model.hero.HeroType;
import br.apsu.model.environment.SceneryElement;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.AmbientLight;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Personagens 3D em tempo real sobre o cenário 2.5D renderizado no Canvas. */
public final class Runtime3DLayer {
    private static final double VIEW_W = 1366, VIEW_H = 768;
    private final Group world = new Group();
    private final Group scenery = new Group();
    private final Group actors = new Group();
    private final SubScene view;
    private final Map<String, Actor> actorsById = new HashMap<>();
    private final java.util.Set<String> failedModels = new java.util.HashSet<>();
    private double shakeX, shakeY;
    private static final String[] ENKI_MODELS = {
        "03_enki_npc", "03_enki_var_celestial", "03_enki_var_eremita"
    };

    private static final class Actor {
        final String modelName;
        final Group node;
        final double targetWidth, targetHeight;
        final Rotate yaw = new Rotate(180, Rotate.Y_AXIS);
        final Rotate pitch = new Rotate(0, Rotate.X_AXIS);
        Actor(String modelName, Group node, double targetWidth, double targetHeight) {
            this.modelName = modelName;
            this.node = node;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
            node.getTransforms().setAll(yaw, pitch);
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
        for (Actor cached : actorsById.values()) cached.node.setVisible(false);
        GameContext.State state = context.getState();
        if (state == GameContext.State.MENU) {
            HeroType type = context.getHeroType();
            Actor hero = actor("hero", modelName(type.getStaticSpritePath()), 130, 210);
            if (hero != null) place(hero, VIEW_W * 0.82, 260, 0.8, timeSeconds, 0);
            return;
        }
        if (state == GameContext.State.DIALOGUE) {
            int phase = Math.max(0, Math.min(4, context.getCurrentPhase() - 1));
            Actor npc = actor("phase-intro-enki", ENKI_MODELS[phase % ENKI_MODELS.length], 110, 200);
            if (npc != null) place(npc, 120, VIEW_H / 2.0, 0.2, timeSeconds, 0);
            return;
        }
        if (state.ordinal() < GameContext.State.P1.ordinal() || state.ordinal() > GameContext.State.P5.ordinal()) return;

        HeroEntity hero = context.getHero();
        String heroName;
        if (hero.isShooting()) {
            String[] styles = {"thrust", "slash", "spin", "charge"};
            heroName = modelName(hero.getType()
                .getAttackDir(styles[hero.getAttackStyleIndex() % styles.length]) + ".obj");
        } else {
            // A malha-base evita reler oito OBJs grandes durante o jogo. O nado
            // é sugerido por inclinação e movimento sutis, sem saltos de pose.
            heroName = modelName(hero.getType().getStaticSpritePath());
        }
        Actor heroActor = actor("hero", heroName, HeroEntity.RENDER_W, HeroEntity.RENDER_H);
        if (heroActor != null) {
            double x = context.getCamera().toScreenX(hero.getX()) + HeroEntity.HW / 2.0
                + (hero.isShooting() ? hero.getAttackOffsetX() : 0);
            double y = hero.getY() + HeroEntity.HH / 2.0
                + (hero.isShooting() ? hero.getAttackOffsetY() : 0);
            place(heroActor, x, y, context.getCamera().toScreenX(hero.getX()) / VIEW_W, timeSeconds,
                hero.isFacingRight() ? 18 : -18,
                Math.max(-20.0, Math.min(20.0,
                    hero.getPitchAngle() * 0.65
                    + Math.sin(timeSeconds * (2.1 + hero.getCurrentSpeedRatio() * 1.2)) * 2.5)));
            if (hero.isInvulnerable()) heroActor.node.setOpacity(0.45 + 0.3 * Math.sin(timeSeconds * 24.0));
            else heroActor.node.setOpacity(1.0);
            heroActor.node.setScaleX(1.0);
            heroActor.node.setScaleY(1.0);
        }

        int enemyIndex = 0;
        for (EnemyEntity enemy : context.getEnemies()) {
            int id = enemyIndex++;
            if (!enemy.isAlive()) continue;
            EnemyType type = enemy.getType();
            Actor enemyActor = actor("enemy-" + id, modelName(type.getStaticSprite()), type.getWidth(), type.getHeight());
            if (enemyActor != null) place(enemyActor,
                context.getCamera().toScreenX(enemy.getWorldX()) + type.getWidth() / 2.0,
                enemy.getCurrentY() + type.getHeight() / 2.0,
                enemy.getWorldX() / 4000.0, timeSeconds, 54);
        }

        int guardianIndex = 0;
        for (GuardianEntity guardian : context.getGuardians()) {
            String model = modelName(guardian.getSpritePath());
            Actor npc = actor("guardian-" + guardianIndex++, model, GuardianEntity.GW, GuardianEntity.GH);
            if (npc != null) place(npc,
                context.getCamera().toScreenX(guardian.getWorldX()) + GuardianEntity.GW / 2.0,
                guardian.getWorldY() + GuardianEntity.GH / 2.0,
                guardian.getWorldX() / 4000.0, timeSeconds, 0);
        }

        BossEntity boss = context.getBoss();
        if (state == GameContext.State.P5 && boss != null) {
            String name = switch (boss.getVariant()) {
                case 1 -> "02_kullullu_var_glacial";
                case 2 -> "02_kullullu_var_toxico";
                default -> "02_kullullu_boss";
            };
            Actor bossActor = actor("boss", name, BossEntity.BW, BossEntity.BH);
            if (bossActor != null) place(bossActor, boss.getX() + BossEntity.BW / 2.0,
                boss.getY() + BossEntity.BH / 2.0, 0.5, timeSeconds, -45);
        }

        updateScenery(context, timeSeconds);
    }

    private void updateScenery(GameContext context, double timeSeconds) {
        int index = 0;
        for (SceneryElement element : context.getSceneryElements()) {
            if (element.getType() == SceneryElement.Type.CORAL) { index++; continue; }
            String model = sceneryModel(element.getType());
            if (model == null) continue;
            double screenX = context.getCamera().toScreenX(element.getWorldX());
            if (screenX < -260 || screenX > VIEW_W + 260) { index++; continue; }
            int elementIndex = index++;
            double propWidth = element.getWidth();
            double propHeight = element.getHeight();
            if (element.getType() == SceneryElement.Type.CURRENT
                || element.getType() == SceneryElement.Type.PRESSURE_ZONE) {
                propWidth = 100;
                propHeight = 280;
            } else if (element.getType() == SceneryElement.Type.GEYSER) {
                propWidth = 72;
                propHeight = 190;
            }
            Actor prop = actor("scenery-" + elementIndex, "scenery/" + model,
                Math.max(48, propWidth), Math.max(48, propHeight));
            if (prop != null) place(prop, screenX + element.getWidth() / 2,
                element.getWorldY() + element.getHeight() / 2,
                element.getWorldX() / 4000.0, timeSeconds, 0);
        }
        if (context.getCurrentPhase() == 2) {
            Actor reef = actor("reef-school", "scenery/08_recifes_e_cardume_elemento-cenario", 760, 360);
            if (reef != null) place(reef, context.getCamera().toScreenX(1900), VIEW_H / 2,
                .12, timeSeconds, 0);
            Actor wreck = actor("shipwreck", "scenery/07_navio_naufragado_elemento-cenario", 230, 140);
            if (wreck != null) place(wreck, context.getCamera().toScreenX(context.getChestWX()) + 35,
                context.getChestWY() - 10, .25, timeSeconds, 0);
            Actor chest = actor("chest", "scenery/06_bau_tesouro_elemento-cenario", 54, 42);
            if (chest != null) place(chest, context.getCamera().toScreenX(context.getChestWX()) + 10,
                context.getChestWY(), .26, timeSeconds, 0);
        }
        // A arena final já tem ruínas e portal no panorama 2.5D; repetir aqui
        // uma ruína de 450x420 cobria a área de combate e escondia o boss.
        if (context.getCurrentPhase() >= 1 && context.getCurrentPhase() <= 4) {
            Actor portal = actor("portal", "scenery/15_portal_atlantica_3d", 140, 140);
            if (portal != null) place(portal, context.getCamera().toScreenX(3900), VIEW_H / 2 - 16,
                .8, timeSeconds, 0);
        }
    }

    private static String sceneryModel(SceneryElement.Type type) {
        return switch (type) {
            case CORAL -> "08_recifes_e_cardume_elemento-cenario";
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
        String cacheId = id + ":" + modelName + ":" + targetWidth + "x" + targetHeight;
        Actor current = actorsById.get(cacheId);
        if (current != null && current.modelName.equals(modelName)
            && current.targetWidth == targetWidth && current.targetHeight == targetHeight) return current;
        try {
            ObjModelLoader.Model model = ObjModelLoader.load(modelName, targetWidth, targetHeight);
            Actor loaded = new Actor(modelName, model.node(), targetWidth, targetHeight);
            actorsById.put(cacheId, loaded);
            (modelName.startsWith("scenery/") ? scenery : actors).getChildren().add(loaded.node);
            return loaded;
        } catch (IOException | RuntimeException error) {
            System.err.println("[3D] Não foi possível carregar " + modelName + ": " + error.getMessage());
            failedModels.add(modelName);
            actorsById.remove(cacheId);
            return null;
        }
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
        actor.node.setVisible(true);
    }

    private static String modelName(String spritePath) {
        String name = spritePath.substring(spritePath.lastIndexOf('/') + 1);
        int extension = name.lastIndexOf('.');
        return extension < 0 ? name : name.substring(0, extension);
    }
}
