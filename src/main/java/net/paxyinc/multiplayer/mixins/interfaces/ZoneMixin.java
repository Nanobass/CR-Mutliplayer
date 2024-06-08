package net.paxyinc.multiplayer.mixins.interfaces;

import finalforeach.cosmicreach.entities.Player;
import finalforeach.cosmicreach.util.Point3DMap;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.World;
import finalforeach.cosmicreach.world.Zone;
import net.paxyinc.multiplayer.entities.BetterEntity;
import net.paxyinc.multiplayer.interfaces.ChunkInterface;
import net.paxyinc.multiplayer.interfaces.WorldInterface;
import net.paxyinc.multiplayer.interfaces.ZoneInterface;
import net.paxyinc.multiplayer.net.ZoneLoader;
import net.paxyinc.multiplayer.util.ChunkDelta;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(Zone.class)
public abstract class ZoneMixin implements ZoneInterface {

    @Shadow @Final private Point3DMap<Chunk> chunks;
    @Shadow public String zoneId;
    @Shadow
    private transient World world;

    @Shadow public abstract void runScheduledTriggers();

    private final Map<UUID, BetterEntity> entities = new HashMap<>();

    @Override
    public Map<UUID, Player> getPlayers() {
        WorldInterface wi = (WorldInterface) world;
        Map<UUID, Player> players = new HashMap<>();
        for(Map.Entry<UUID, Player> entry : wi.getPlayers().entrySet()) {
            Player player = entry.getValue();
            if(player.zoneId.equals(zoneId)) {
                players.put(entry.getKey(), player);
            }
        }
        return players;
    }

    @Override
    public void update(float deltaTime) {
        runScheduledTriggers();
        ZoneLoader loader = getZoneLoader();
        synchronized (chunks) {
            chunks.forEach(chunk -> {
                ChunkInterface ci = (ChunkInterface) chunk;
                if(ci.isChunkModified()) {
                    loader.onChunkModify(chunk);
                    ci.setChunkModified(false);
                }
            });
        }
        synchronized (entities) {
            for (Map.Entry<UUID, BetterEntity> entry : entities.entrySet()) {
                BetterEntity entity = entry.getValue();
                entity.update((Zone) (Object) this, deltaTime);
            }
        }
    }

    @Override
    public BetterEntity spawnEntity(Class<?> clazz, UUID uuid, float x, float y, float z) {
        try {
            BetterEntity entity = (BetterEntity) clazz.getDeclaredConstructor().newInstance();
            entity.setPosition(x, y, z);
            addEntity(entity);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void spawnEntity(BetterEntity entity, float x, float y, float z) {
        try {
            entity.setPosition(x, y, z);
            addEntity(entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addEntity(BetterEntity entity) {
        synchronized (entities) {
            entities.put(entity.uuid, entity);
        }
    }

    @Override
    public void killEntity(UUID uuid) {
        synchronized (entities) {
            entities.remove(uuid);
        }
    }

    @Override
    public BetterEntity getEntity(UUID uuid) {
        synchronized (entities) {
            return entities.get(uuid);
        }
    }

    @Override
    public Map<UUID, BetterEntity> getEntities() {
        return entities;
    }

    @Override
    public Point3DMap<Chunk> getChunks() {
        return chunks;
    }

    @Override
    public ZoneLoader getZoneLoader() {
        return ((WorldInterface) world).getWorldLoader().getZoneLoader((Zone) (Object) this);
    }
}
