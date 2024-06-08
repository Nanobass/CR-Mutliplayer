package net.paxyinc.multiplayer.interfaces;

import finalforeach.cosmicreach.entities.Player;
import finalforeach.cosmicreach.util.Point3DMap;
import finalforeach.cosmicreach.world.Chunk;
import net.paxyinc.multiplayer.entities.BetterEntity;
import net.paxyinc.multiplayer.net.ZoneLoader;

import java.util.Map;
import java.util.UUID;

public interface ZoneInterface {

    void update(float deltaTime);

    Map<UUID, Player> getPlayers();

    BetterEntity spawnEntity(Class<?> clazz, UUID uuid, float x, float y, float z);
    void spawnEntity(BetterEntity entity, float x, float y, float z);
    void addEntity(BetterEntity entity);
    void killEntity(UUID uuid);
    BetterEntity getEntity(UUID uuid);
    Map<UUID, BetterEntity> getEntities();

    Point3DMap<Chunk> getChunks();

    ZoneLoader getZoneLoader();

}
