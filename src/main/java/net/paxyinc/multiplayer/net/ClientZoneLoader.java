package net.paxyinc.multiplayer.net;

import finalforeach.cosmicreach.blocks.Block;
import finalforeach.cosmicreach.entities.Player;
import finalforeach.cosmicreach.io.ChunkLoader;
import finalforeach.cosmicreach.util.Point3DMap;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import finalforeach.cosmicreach.world.Zone;
import net.paxyinc.multiplayer.GameClient;
import net.paxyinc.multiplayer.entities.BetterEntity;
import net.paxyinc.multiplayer.interfaces.ZoneInterface;
import net.paxyinc.multiplayer.util.ChunkCoords;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArraySet;

public class ClientZoneLoader extends ZoneLoader {

    public GameClient client;
    public Set<ChunkCoords> requested = new CopyOnWriteArraySet<>();

    public ClientZoneLoader(GameClient client, Zone zone) {
        super(zone);
        this.client = client;
    }

    @Override
    protected void loop() {
        ZoneInterface zi = (ZoneInterface) zone;
        Map<UUID, Player> players = zi.getPlayers();

        Set<ChunkCoords> toRequest = new HashSet<>();
        for(Player player : players.values()) {
            BetterEntity playerEntity = (BetterEntity) player.getEntity();
            for(int localChunkX = -playerDistance; localChunkX < playerDistance; localChunkX++) {
                for(int localChunkZ = -playerDistance; localChunkZ < playerDistance; localChunkZ++) {
                    for(int localChunkY = -playerDistance; localChunkY < playerDistance; localChunkY++) {
                        int chunkX = Math.floorDiv((int) playerEntity.position.x, 16) + localChunkX;
                        int chunkY = Math.floorDiv((int) playerEntity.position.y, 16) + localChunkY;
                        int chunkZ = Math.floorDiv((int) playerEntity.position.z, 16) + localChunkZ;
                        toRequest.add(new ChunkCoords(chunkX, chunkY, chunkZ));
                    }
                }
            }
        }

        Point3DMap<Chunk> zoneChunks = zi.getChunks();

        Set<ChunkCoords> loaded = new HashSet<>();
        synchronized (zoneChunks) {
            zoneChunks.forEach((c, x, y, z) -> {
                loaded.add(new ChunkCoords(x, y, z));
            });
        }

        Set<ChunkCoords> toUnload = new HashSet<>(loaded);
        toUnload.removeAll(toRequest);
        toRequest.removeAll(loaded);
        toRequest.removeAll(requested);

        Map<Region, List<Chunk>> chunkUnloadBuckets = new HashMap<>();
        for(ChunkCoords coords : toUnload) {
            Chunk chunk = zoneChunks.get(coords.x(), coords.y(), coords.z());
            if(chunkUnloadBuckets.containsKey(chunk.region)) {
                List<Chunk> bucket = chunkUnloadBuckets.get(chunk.region);
                bucket.add(chunk);
            } else {
                List<Chunk> bucket = new ArrayList<>();
                bucket.add(chunk);
                chunkUnloadBuckets.put(chunk.region, bucket);
            }
        }

        for(Map.Entry<Region, List<Chunk>> entry : chunkUnloadBuckets.entrySet()) {
            List<Chunk> chunks = entry.getValue();
            chunks.forEach(zone::removeChunk);
        }

        for(ChunkCoords coords : toRequest) {
            CompoundTag tag = new CompoundTag();
            tag.putString("zone", zone.zoneId);
            tag.putIntArray("xyz", new int[] {coords.x(), coords.y(), coords.z()});
            client.sendToServer(new NamedTag("onChunkRequest", tag));
            requested.add(coords);
        }
    }

    public void onChunkReceived(Chunk chunk) {
        ChunkCoords coords = new ChunkCoords(chunk.chunkX, chunk.chunkY, chunk.chunkZ);
        requested.remove(coords);
    }

}
