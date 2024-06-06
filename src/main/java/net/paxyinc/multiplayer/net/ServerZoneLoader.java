package net.paxyinc.multiplayer.net;

import finalforeach.cosmicreach.blocks.Block;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.entities.Player;
import finalforeach.cosmicreach.io.ChunkSaver;
import finalforeach.cosmicreach.lighting.LightPropagator;
import finalforeach.cosmicreach.savelib.blockdata.IBlockData;
import finalforeach.cosmicreach.savelib.blockdata.SingleBlockData;
import finalforeach.cosmicreach.savelib.blocks.IBlockDataFactory;
import finalforeach.cosmicreach.util.Point3DMap;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import finalforeach.cosmicreach.world.Zone;
import net.paxyinc.multiplayer.GameServer;
import net.paxyinc.multiplayer.entities.BetterEntity;
import net.paxyinc.multiplayer.interfaces.ZoneInterface;
import net.paxyinc.multiplayer.util.ChunkCoords;

import java.util.*;

public class ServerZoneLoader extends ZoneLoader {

    public GameServer server;
    LightPropagator lightProp = new LightPropagator();

    public ServerZoneLoader(GameServer server, Zone zone) {
        super(zone);
        this.server = server;
    }

    @Override
    protected void loop() {
        ZoneInterface zi = (ZoneInterface) zone;
        Map<UUID, Player> players = zi.getPlayers();

        Set<ChunkCoords> toLoad = new HashSet<>();
        for(Player player : players.values()) {
            BetterEntity playerEntity = (BetterEntity) player.getEntity();
            for(int localChunkX = -playerDistance; localChunkX < playerDistance; localChunkX++) {
                for(int localChunkZ = -playerDistance; localChunkZ < playerDistance; localChunkZ++) {
                    for(int localChunkY = -playerDistance; localChunkY < playerDistance; localChunkY++) {
                        int chunkX = Math.floorDiv((int) playerEntity.position.x, 16) + localChunkX;
                        int chunkY = Math.floorDiv((int) playerEntity.position.y, 16) + localChunkY;
                        int chunkZ = Math.floorDiv((int) playerEntity.position.z, 16) + localChunkZ;
                        toLoad.add(new ChunkCoords(chunkX, chunkY, chunkZ));
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
        toUnload.removeAll(toLoad);
        toLoad.removeAll(loaded);

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
            Region region = entry.getKey();
            List<Chunk> chunks = entry.getValue();
            //ChunkSaver.saveRegion(zone, region);
            chunks.forEach(zone::removeChunk);
        }

        List<Chunk> chunkLoadBuckets = new ArrayList<>();
        List<ChunkCoords> toGenerate = new ArrayList<>();
        toGenerate.addAll(toLoad);

        for(ChunkCoords coords : toGenerate) {

            Chunk chunk = new Chunk(coords.x(), coords.y(), coords.z());
            chunk.initChunkData();
            chunk.fillLayer(Block.GRASS.getDefaultBlockState(), 0);
            chunk.setGenerated(true);
            zone.addChunk(chunk);

            chunkLoadBuckets.add(chunk);
        }

        for(Chunk chunk : chunkLoadBuckets) {
            lightProp.calculateLightingForChunk(zone, chunk, true);
            chunk.flagTouchingChunksForRemeshing(zone, false);
        }

    }

}
