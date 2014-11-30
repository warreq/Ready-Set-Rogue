package com.warsheep.scamp.components;
import com.badlogic.ashley.core.ComponentMapper;

/**
 * The ECSMapper is a convenient container for all of the ComponentMappers in the game.
 *
 * ComponentMappers provide O(1) access to their designated ComponentType for the given entity,
 * making them the most efficient way to look up a Component of an Entity.
 */
public class ECSMapper {
    public static final ComponentMapper<VisibleComponent> visible =
            ComponentMapper.getFor(VisibleComponent.class);

    public static final ComponentMapper<TransformComponent> transform =
            ComponentMapper.getFor(TransformComponent.class);

    public static final ComponentMapper<MovementComponent> movement =
            ComponentMapper.getFor(MovementComponent.class);

    public static final ComponentMapper<ControllableComponent> control =
            ComponentMapper.getFor(ControllableComponent.class);
}
