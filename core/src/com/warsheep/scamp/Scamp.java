package com.warsheep.scamp;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.warsheep.scamp.components.*;
import com.warsheep.scamp.processors.*;

import java.util.ArrayList;

public class Scamp extends Game {

    public static final String TITLE = "SCAMP";

    public static final int V_WIDTH = 320, V_HEIGHT = 240; // Internal dimensions in pixels

    public static Engine ecs; // Ashley Entity-Component System
    VisibilityProcessor visibilityProcessor;
    MovementProcessor movementProcessor;
    CollisionProcessor collisionProcessor;
    ControlProcessor controlProcessor;
    DeathProcessor deathProcessor;
    StateProcessor stateProcessor;
    CombatProcessor combatProcessor;
    CameraProcessor cameraProcessor;
    TileProcessor tileProcessor;
    AIProcessor aiProcessor;


    Entity wizard;

    private long startTime;
    private long delta;

    @Override
    public void create() {
        ecs = new Engine();

        ArrayList<CollisionProcessor.CollisionListener> collisionListeners = new ArrayList();
        // Initialize processors and associate them with ecs engine
        visibilityProcessor = new VisibilityProcessor();
        movementProcessor = new MovementProcessor();
        combatProcessor = new CombatProcessor();
        collisionListeners.add(movementProcessor);
        collisionProcessor = new CollisionProcessor(collisionListeners);
        ArrayList<StateProcessor.StateListener> stateListeners = new ArrayList();
        stateListeners.add(collisionProcessor);
        stateListeners.add(combatProcessor);
        stateProcessor = new StateProcessor(stateListeners);
        controlProcessor = new ControlProcessor();
        cameraProcessor = new CameraProcessor();
        deathProcessor = new DeathProcessor();
        tileProcessor = new TileProcessor();
        aiProcessor = new AIProcessor();
        ecs.addSystem(visibilityProcessor);
        ecs.addSystem(collisionProcessor);
        ecs.addSystem(tileProcessor);
        ecs.addSystem(movementProcessor);
        ecs.addSystem(cameraProcessor);
        ecs.addSystem(deathProcessor);
        ecs.addSystem(combatProcessor);
        ecs.addSystem(stateProcessor);
        ecs.addSystem(aiProcessor);
        ecs.addSystem(controlProcessor);
        Gdx.input.setInputProcessor(controlProcessor);

        AssetDepot assets = AssetDepot.getInstance();

        // Skeleton blocker of doom
        for (int i = 1; i < 2; i++) {
            Entity skeleton = new Entity();
            skeleton.add(new VisibleComponent());
            skeleton.add(new TransformComponent());
            skeleton.add(new CollidableComponent());
            skeleton.add(new DamageableComponent());
            skeleton.add(new TilePositionComponent());
            skeleton.add(new AIControllableComponent());
            skeleton.add(new AttackerComponent());
            skeleton.add(new StateComponent());
            ecs.addEntity(skeleton);
            VisibleComponent skeletonVisComp = ECSMapper.visible.get(skeleton);
            skeletonVisComp.image = assets.fetch("creatures_24x24", "oryx_n_skeleton");
            skeletonVisComp.originY = skeletonVisComp.image.getRegionHeight() / 2;
            skeletonVisComp.originX = skeletonVisComp.image.getRegionWidth() / 2;
            ECSMapper.transform.get(skeleton).position.y = i * 24;
            ECSMapper.transform.get(skeleton).position.x = i * 24;
            ECSMapper.tilePosition.get(skeleton).y = i;
            ECSMapper.tilePosition.get(skeleton).x = i;
        }

        // Crappy Debug Wizard mans
        wizard = new Entity();
        wizard.add(new VisibleComponent());
        wizard.add(new TransformComponent());
        wizard.add(new CollidableComponent());
        wizard.add(new ControllableComponent());
        wizard.add(new AttackerComponent());
        wizard.add(new DamageableComponent());
        wizard.add(new TilePositionComponent());
        wizard.add(new StateComponent());
        ecs.addEntity(wizard);

        DamageableComponent dmgComp = ECSMapper.damage.get(wizard);
        dmgComp.essential = true;

        VisibleComponent wizardVisComp = ECSMapper.visible.get(wizard);
        wizardVisComp.image = assets.fetch("creatures_24x24", "oryx_m_wizard");
        wizardVisComp.originX = wizardVisComp.image.getRegionWidth() / 2;
        wizardVisComp.originY = wizardVisComp.image.getRegionHeight() / 2;

        createCamera(wizard);

        MapImporter mapImporter = new MapImporter();
        mapImporter.loadTiledMapJson(AssetDepot.MAP_PATH);

        for (Entity e : mapImporter.getEntities()) {
            ecs.addEntity(e);
        }

        //Start calculating game time
        startTime = System.currentTimeMillis();

    }

    @Override
    public void render() {
        visibilityProcessor.startBatch();
        delta = (System.currentTimeMillis() - startTime);
        ecs.update(delta);
        visibilityProcessor.endBatch();
    }

    private void createCamera(Entity target) {
        Entity entity = new Entity();

        CameraComponent camera = new CameraComponent();
        camera.camera = ecs.getSystem(VisibilityProcessor.class).getCamera();
        camera.target = target;

        entity.add(camera);

        ecs.addEntity(entity);
    }
}
