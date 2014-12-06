package com.warsheep.scamp.processors;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.warsheep.scamp.Pair;
import com.warsheep.scamp.components.*;
import com.warsheep.scamp.components.StateComponent;
import com.warsheep.scamp.components.StateComponent.Directionality;
import com.warsheep.scamp.components.StateComponent.State;
import com.warsheep.scamp.processors.TileProcessor.TileBound;

import java.util.ArrayDeque;
import java.util.Queue;

public class AIProcessor extends EntitySystem implements StateProcessor.StateListener {

    private ImmutableArray<Entity> aiControllableEntities;
    private ImmutableArray<Entity> damageableEntities;
    private Queue<Pair<Entity, Pair<State, Directionality>>> actions;
    private CollisionProcessor collisions;

    public AIProcessor() {
        this.actions = new ArrayDeque<>();
    }

    public void addedToEngine(Engine engine) {
        aiControllableEntities = engine.getEntitiesFor(Family.all(AIControllableComponent.class, TilePositionComponent.class, AttackerComponent.class, StateComponent.class).get());
        damageableEntities = engine.getEntitiesFor(Family.all(DamageableComponent.class, TilePositionComponent.class, ControllableComponent.class, StateComponent.class).get());
        collisions = engine.getSystem(CollisionProcessor.class);
    }

    @Override
    public Queue<Pair<Entity, Pair<State, Directionality>>> turnEnd() {
        this.actions.removeAll(this.actions); // clear
        System.out.println("TURN===============================");
        for (Entity aiEntity : aiControllableEntities) {
            System.out.println("AI Controllable: " + aiEntity.getId());

            if (ECSMapper.state.get(aiEntity).state != State.DEAD) {
                TilePositionComponent aiTilePos = ECSMapper.tilePosition.get(aiEntity);

                int sightRange = ECSMapper.aiControllable.get(aiEntity).sightRange;
                Entity closestDamageableEntity = scanForEnemy(aiTilePos, sightRange, this.damageableEntities); // Entity to move towards
                if(closestDamageableEntity == null) {
                    System.out.println("I can't see you");
                } else {
                    System.out.println(ECSMapper.control.get(closestDamageableEntity).movementBonus);
                }


                if (closestDamageableEntity != null) { // If null, no damageable-ctrl-entities nearby

                    TileBound simulatedAiPos = aiTilePos;
                    TilePositionComponent closestDmgTilePos = ECSMapper.tilePosition.get(closestDamageableEntity);
                    AttackerComponent attackerComponent = ECSMapper.attack.get(aiEntity);

                    int moveCount = 0;

                    while (moveCount <= ECSMapper.aiControllable.get(aiEntity).movementBonus &&
                            !isInAttackRange(simulatedAiPos, closestDmgTilePos, attackerComponent.attackRange)) {

                        Directionality direction = approachEnemy(simulatedAiPos, closestDmgTilePos, aiEntity, collisions);

                        if (direction != Directionality.NONE) {
                            Pair<State, Directionality> action =
                                    new Pair<>(State.MOVING, direction);
                            this.actions.add(new Pair(aiEntity, action));
                            simulatedAiPos = simulateAIMovement(simulatedAiPos, direction);
                        }
                        moveCount++;
                    }

                    // Attack if possible
                    if (isInAttackRange(simulatedAiPos, closestDmgTilePos, attackerComponent.attackRange)) {
                        Pair<State, Directionality> action =
                                new Pair<>(State.ATTACKING, approachEnemy(simulatedAiPos, closestDmgTilePos, aiEntity, collisions));
                        this.actions.add(new Pair(aiEntity, action));
                    }
                }
            }
        }
        return this.actions;
    }


    // Find the closest damageable-ctrl-entity, if any
    private static Entity scanForEnemy(TilePositionComponent location, int sightRange, ImmutableArray<Entity> enemies) {
        Entity closestDamageableEntity = null;
        for (Entity damageableEntity : enemies) {

            if (ECSMapper.state.get(damageableEntity).state != State.DEAD) {
                TilePositionComponent damageableTilePos = ECSMapper.tilePosition.get(damageableEntity);

                int distanceToAI = Math.abs(location.x - damageableTilePos.x) + Math.abs(location.y - damageableTilePos.y);

                if (distanceToAI < sightRange) {
                    sightRange = distanceToAI;
                    closestDamageableEntity = damageableEntity;
                }
            }
        }
        return closestDamageableEntity;
    }

    // Figure out whether to fire an action horizontally or vertically
    private static Directionality approachEnemy(TileBound ai, TileBound enemy, Entity entity, CollisionProcessor collisions) {
        boolean[] blocked = new boolean[4];
        boolean wantsUp = false;
        boolean wantsRight = false;
        for (int i = 0; i < Directionality.values().length - 1; i++) {
            blocked[i] = collisions.checkMove(ai.x(), ai.y(), entity, Directionality.values()[i]);
        }

        if (enemy.x() > ai.x()) {
            wantsRight = true;
        }
        if (enemy.y() > ai.y()) {
            wantsUp = true;
        }

        System.out.println(ECSMapper.tilePosition.get(entity).x + ", " + ECSMapper.tilePosition.get(entity).y + " wants up: " +
                wantsUp + ", wants right: " + wantsRight);

        if (Math.abs(enemy.x() - ai.x()) > Math.abs(enemy.y() - ai.y())) {
            if (wantsRight) {
                if (!blocked[Directionality.RIGHT.ordinal()]) {
                    return Directionality.RIGHT;
                }
            } else {
                if (!blocked[Directionality.LEFT.ordinal()]) {
                    return Directionality.LEFT;
                }
            }
        }
        if (wantsUp) {
            if (!blocked[Directionality.UP.ordinal()]) {
                return Directionality.UP;
            }
        } else {
            if (!blocked[Directionality.DOWN.ordinal()]) {
                return Directionality.DOWN;
            }
        }
        return Directionality.NONE;
    }

    private static boolean isInAttackRange(TileBound ai, TileBound enemy, int reach) {
        boolean canAttack = false;
        if (enemy.x() - ai.x() == 0) { // Chance for vertical attack?
            if (Math.abs(enemy.y() - ai.y()) <= reach) {
                canAttack = true;
            }
        } else if (enemy.y() - ai.y() == 0) { // Chance for horizontal attack?
            if (Math.abs(enemy.x() - ai.x()) <= reach) {
                canAttack = true;
            }
        }

        return canAttack;
    }

    private static TileBound simulateAIMovement(TileBound aiPos, Directionality dir) {
        TilePositionComponent t = new TilePositionComponent();
        t.x = aiPos.x();
        t.y = aiPos.y();
        switch (dir) {
            case UP:
                t.y(aiPos.y() + 1);
                break;
            case DOWN:
                t.y(aiPos.y() - 1);
                break;
            case LEFT:
                t.x(aiPos.x() - 1);
                break;
            case RIGHT:
                t.x(aiPos.x() + 1);
                break;
        }
        return t;
    }


}
