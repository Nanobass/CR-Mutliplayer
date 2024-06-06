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
    private final Map<UUID, BetterEntity> entities = new HashMap<>();

    private Chunk getBottomMostChunk(Chunk chunk) {
        return getChunkAtChunkCoords(chunk.chunkX, 0, chunk.chunkZ);
    }

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

        List<BetterEntity> toUnload = new ArrayList<>();
        synchronized (entities) {
            for (Map.Entry<UUID, BetterEntity> entry : entities.entrySet()) {
                BetterEntity entity = entry.getValue();
                int entityLastChunkX = Math.floorDiv((int) entity.lastPosition.x, 16);
                int entityLastChunkZ = Math.floorDiv((int) entity.lastPosition.z, 16);
                int entityChunkX = Math.floorDiv((int) entity.position.x, 16);
                int entityChunkZ = Math.floorDiv((int) entity.position.z, 16);
                // entity moved between chunks
                if (entityChunkX != entityLastChunkX || entityChunkZ != entityLastChunkZ || entity.chunk == null) {
                    Chunk oldChunk = entity.chunk;
                    Chunk newChunk = getChunkAtChunkCoords(entityChunkX, 0, entityChunkZ);
                    if (oldChunk == null && newChunk != null) {
                        // entity went back into still loaded chunks, by teleporting, or by loading
                        ChunkInterface newCi = (ChunkInterface) newChunk;
                        newCi.addEntity(entity);
                        entity.chunk = newChunk;
                    }
                    if (oldChunk != null && newChunk != null) {
                        // entity went between chunks
                        ChunkInterface oldCi = (ChunkInterface) oldChunk;
                        ChunkInterface newCi = (ChunkInterface) newChunk;
                        oldCi.removeEntity(entity.uuid);
                        newCi.addEntity(entity);
                        entity.chunk = newChunk;
                    }
                    if (oldChunk != null && newChunk == null) {
                        // entity went into unloaded chunks
                        ChunkInterface oldCi = (ChunkInterface) oldChunk;
                        oldCi.removeEntity(entity.uuid);
                        entity.chunk = null;
                        toUnload.add(entity);
                    }
                }
            }

        }

        for (BetterEntity entity : toUnload) {
            entity.update((Zone)(Object)this, deltaTime);
            // TODO do this, region file has to change, nope it doesn't have to, well, idk when ima do dat
        }

        synchronized (chunks) {
            // TODO reduce iteration count here
            chunks.forEach(chunk -> {
                ChunkInterface ci = (ChunkInterface) chunk;
                ci.update(deltaTime);
            });
        }


    }


    @Inject(method = "addChunk", at = @At("TAIL"))
    private void addChunk(Chunk chunk, CallbackInfo ci_) {
        Chunk bottom = getBottomMostChunk(chunk);
        ChunkInterface ci = (ChunkInterface) chunk;
        Map<UUID, BetterEntity> loaded = ci.getEntities();
        for (Map.Entry<UUID, BetterEntity> entry : loaded.entrySet()) {
            BetterEntity entity = entry.getValue();
            entity.chunk = bottom;
        }
        synchronized (entities) {
            entities.putAll(loaded);
        }
    }

    @Inject(method = "removeChunk", at = @At("TAIL"))
    private void removeChunk(Chunk chunk, CallbackInfo ci_) {
        Chunk bottom = getBottomMostChunk(chunk);
        if(bottom != null) {
            ChunkInterface ci = (ChunkInterface) bottom;
            Map<UUID, BetterEntity> loaded = ci.getEntities();
            synchronized (entities) {
                loaded.forEach(entities::remove);
            }
            for (Map.Entry<UUID, BetterEntity> entry : loaded.entrySet()) {
                BetterEntity entity = entry.getValue();
                entity.chunk = null;
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
            BetterEntity entity = entities.remove(uuid);
            if (entity.chunk != null) {
                ChunkInterface ci = (ChunkInterface) entity.chunk;
                ci.removeEntity(uuid);
                entity.chunk = null;
            }
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

    @Shadow
    protected abstract void runScheduledTriggers();

    @Shadow
    protected abstract Chunk getChunkAtChunkCoords(int cx, int cy, int cz);

}
