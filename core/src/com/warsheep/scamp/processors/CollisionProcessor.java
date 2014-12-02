package com.warsheep.scamp.processors;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;
import com.warsheep.scamp.components.*;

public class CollisionProcessor extends EntitySystem {

    private ImmutableArray<Entity> controllableEntities;
    private ImmutableArray<Entity> colliableTilePosEntities;
    private ImmutableArray<Entity> colliableTileEntities;

    public CollisionProcessor(int order) {
        super(order);
    }

    public void addedToEngine(Engine engine) {
        controllableEntities = engine.getEntitiesFor(Family.getFor(CollidableComponent.class, TilePositionComponent.class, MovementComponent.class));
        colliableTilePosEntities = engine.getEntitiesFor(Family.getFor(CollidableComponent.class, TilePositionComponent.class));
        colliableTileEntities = engine.getEntitiesFor(Family.getFor(CollidableComponent.class, TileComponent.class));
    }

    public void update(float deltaTime) {
        super.update(deltaTime);

        for (int i = 0; i < controllableEntities.size(); i++) {
            Entity entityMain = controllableEntities.get(i);
            MovementComponent m = ECSMapper.movement.get(entityMain);
            TilePositionComponent tilePosMain = ECSMapper.tilePosition.get(entityMain);

            // TilePos + Collidable
            for (int k = 0; k < colliableTilePosEntities.size(); k++) {
                Entity entityCheck = colliableTilePosEntities.get(k);
                TilePositionComponent tilePosCheck = ECSMapper.tilePosition.get(entityCheck);

                if (entityMain.getId() != entityCheck.getId()) {
                    if (tilePosMain.x == tilePosCheck.x && tilePosMain.y == tilePosCheck.y) {
                        System.out.println("Block TilePos");

                        tilePosMain.x = tilePosMain.prevX;
                        tilePosMain.y = tilePosMain.prevY;
                        m.target = new Vector3(tilePosMain.x*24, tilePosMain.y*24, m.target.z);
                    }
                }
            }

            // Tile + Collidable
            for (int k = 0; k < colliableTileEntities.size(); k++) {
                Entity entityCheck = colliableTileEntities.get(k);
                TileComponent tileCheck = ECSMapper.tile.get(entityCheck);

                if (entityMain.getId() != entityCheck.getId()) {
                    if (tilePosMain.x == tileCheck.x && tilePosMain.y == tileCheck.y) {
                        System.out.println("Block Tile");

                        tilePosMain.x = tilePosMain.prevX;
                        tilePosMain.y = tilePosMain.prevY;
                        m.target = new Vector3(tilePosMain.x*24, tilePosMain.y*24, m.target.z);
                    }
                }
            }
        }
    }
}

