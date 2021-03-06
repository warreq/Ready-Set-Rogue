package com.warsheep.scamp.processors;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.warsheep.scamp.components.ECSMapper;
import com.warsheep.scamp.components.TileComponent;
import com.warsheep.scamp.components.TransformComponent;

import java.util.ArrayList;

public class TileProcessor extends EntitySystem implements EntityListener, MovementProcessor.MovementListener {

    private ImmutableArray<Entity> tileEntities;
    private TransformComponent trans;
    private TileComponent tile;
    private static final int TILE_SIZE = 24;
    private final int MAP_WIDTH;
    private final int MAP_HEIGHT;
    private ArrayList<Entity>[][] map;

    public TileProcessor(int mapWidth, int mapHeight) {
        MAP_WIDTH = mapWidth;
        MAP_HEIGHT = mapHeight;
        map = new ArrayList[MAP_WIDTH][MAP_HEIGHT];
    }

    public ArrayList<Entity> queryByPosition(int x, int y) {
        return map[x][y];
    }

    @Override
    public void addedToEngine(Engine engine) {
        Family family = Family.all(TileComponent.class).get();
        engine.addEntityListener(family, this);

        tileEntities = engine.getEntitiesFor(Family.all(TileComponent.class).get());
    }

    @Override
    public void entityAdded(Entity entity) {
        trans = ECSMapper.transform.get(entity);
        tile = ECSMapper.tile.get(entity);

        trans.position.x = tile.x * TILE_SIZE;
        trans.position.y = tile.y * TILE_SIZE;
        trans.position.z = tile.z;

        if (map[tile.x][tile.y] == null) {
            map[tile.x][tile.y] = new ArrayList<>();
        }

        map[tile.x][tile.y].add(entity);
    }

    @Override
    public void entityRemoved(Entity entity) {
        tile = ECSMapper.tile.get(entity);
        map[tile.x][tile.y].remove(entity);
    }

    @Override
    public void tileMove(Entity entity, int oldX, int oldY) {
        tile = ECSMapper.tile.get(entity);
        if (tile != null && tile.x > -1 && tile.y > -1
                && oldX > -1 && oldY > -1
                && tile.x < this.MAP_WIDTH && tile.y < this.MAP_HEIGHT
                && oldX < this.MAP_WIDTH && oldY < this.MAP_HEIGHT) {
            if (this.map[oldX][oldY] != null) {
                this.map[oldX][oldY].remove(entity);
            }
            if (this.map[tile.x][tile.y] != null) {
                this.map[tile.x][tile.y].add(entity);
            }
        }
    }
}
