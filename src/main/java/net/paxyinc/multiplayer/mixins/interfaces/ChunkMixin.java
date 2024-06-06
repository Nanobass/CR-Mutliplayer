package net.paxyinc.multiplayer.mixins.interfaces;

import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import net.paxyinc.multiplayer.entities.BetterEntity;
import net.paxyinc.multiplayer.interfaces.ChunkInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(Chunk.class)
public class ChunkMixin implements ChunkInterface {

    @Shadow public Region region;

    private final Map<UUID, BetterEntity> entities = new HashMap<>();

    @Override
    public void update(float deltaTime) {
        synchronized (entities) {
            for(Map.Entry<UUID, BetterEntity> entry : entities.entrySet()) {
                BetterEntity entity = entry.getValue();
                entity.update(region.zone, deltaTime);
            }
        }
    }

    @Override
    public void addEntity(BetterEntity entity) {
        synchronized (entities) {
            entities.put(entity.uuid, entity);
        }
    }

    @Override
    public void removeEntity(UUID uuid) {
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

}
