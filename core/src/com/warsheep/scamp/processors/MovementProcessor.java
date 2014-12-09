package com.warsheep.scamp.processors;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector3;
import com.warsheep.scamp.components.*;
import com.warsheep.scamp.processors.StateProcessor.StateListener;

import java.util.ArrayList;
import java.util.Queue;

public class MovementProcessor extends IteratingSystem implements StateListener {
    private boolean pause = false;
    private ArrayList<MovementListener> listeners;

    public static interface MovementListener {

        default public void tileMove(Entity mover, int oldX, int oldY) {
            // Do nothing
        }

        default public void transformMove(Entity mover) {
            // Do nothing
        }
    }

    public MovementProcessor(ArrayList<MovementListener> listeners) {
        super(Family.all(MovementComponent.class).get());
        this.listeners = listeners;
    }

    @Override
    public void processEntity(Entity entity, float deltaTime) {
        TransformComponent trans = ECSMapper.transform.get(entity);
        MovementComponent mov = ECSMapper.movement.get(entity);
        StateComponent state = ECSMapper.state.get(entity);
//        TilePositionComponent tilePos = ECSMapper.tilePosition.get(entity);

/*        // Don't do anything if we're already at our target
        System.out.println(trans.position.y - mov.target.y);
        if ((Math.abs(trans.position.x - mov.target.x) < 5 &&
                Math.abs(trans.position.y - mov.target.y) < 5)
                || (Math.abs(trans.position.x - mov.target.x) > 24 ||
                Math.abs(trans.position.y - mov.target.y) > 24 )) {
            System.out.println("time to stop");
            entity.remove(MovementComponent.class);
            state.inProgress = false;
            state.previousState = state.state;
            state.state = StateComponent.State.IDLE;
        } else {
            mov.timeSinceMove += deltaTime; // Update how long we've been moving ...
            mov.alpha += MOVE_SPEED / mov.timeSinceMove; // ... And how far we've come

            if (mov.alpha > .80) {
                //    mov.alpha = 1.0f;
            }
            trans.position.interpolate(mov.target, mov.alpha, mov.interpolation);

        }
        */
        for (Vector3 pos : mov.target) {
            trans.position.x = pos.x;
            trans.position.y = pos.y;
        }
        entity.remove(MovementComponent.class);
        state.state = StateComponent.State.IDLE;
        state.inProgress = false;
    }


    @Override
    public boolean checkProcessing() {
        return !pause;
    }

    public void pause(boolean pause) {
        this.pause = pause;
    }

    public void moving(Entity entity, Queue<StateComponent.Directionality> direction) {
        MovementComponent mov = new MovementComponent();
        if (ECSMapper.movement.get(entity) != null) {
            mov = ECSMapper.movement.get(entity);
        }

        for (StateComponent.Directionality dir : direction) {
            TileComponent tilePos = ECSMapper.tile.get(entity);
            System.out.print(dir);
            int oldX = tilePos.x;
            int oldY = tilePos.y;
            int x = tilePos.x;
            int y = tilePos.y;
            switch (dir) {
                case UP:
                    y++;
                    break;
                case DOWN:
                    y--;
                    break;
                case LEFT:
                    x--;
                    break;
                case RIGHT:
                    x++;
                    break;
                default:
                    break;
            }
            mov.target.add(new Vector3(x * 24.0f, y * 24.0f, 10.0f));
            ECSMapper.tile.get(entity).x = x;
            ECSMapper.tile.get(entity).y = y;

            for (MovementListener listener : listeners) {
                listener.tileMove(entity, oldX, oldY);
            }
            System.out.println(entity.getId() + " moved from " + oldX + ", " + oldY + " to " + x + ", " + y);
        }
        entity.add(mov);
    }

    public void listen(MovementListener listener) {
        this.listeners.add(listener);
    }
}
