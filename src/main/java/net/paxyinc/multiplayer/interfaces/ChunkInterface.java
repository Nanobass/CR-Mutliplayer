package net.paxyinc.multiplayer.interfaces;

import net.paxyinc.multiplayer.entities.BetterEntity;

import java.util.Map;
import java.util.UUID;

public interface ChunkInterface {
    void update(float deltaTime);

    void addEntity(BetterEntity entity);
    void removeEntity(UUID uuid);
    BetterEntity getEntity(UUID uuid);
    Map<UUID, BetterEntity> getEntities();

}
