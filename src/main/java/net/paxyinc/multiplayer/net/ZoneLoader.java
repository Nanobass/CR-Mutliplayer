package net.paxyinc.multiplayer.net;

import finalforeach.cosmicreach.entities.Player;
import finalforeach.cosmicreach.util.Point3DMap;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import finalforeach.cosmicreach.world.Zone;
import net.paxyinc.multiplayer.entities.BetterEntity;
import net.paxyinc.multiplayer.interfaces.ZoneInterface;
import net.paxyinc.multiplayer.util.ChunkCoords;
import net.paxyinc.multiplayer.util.ChunkDelta;

import java.util.*;

public abstract class ZoneLoader implements Runnable {

    protected Thread thread;

    protected final Zone zone;
    public int playerDistance = 10;
    public boolean saveRequested = false;
    public Object worldGenLock = new Object();

    public ZoneLoader(Zone zone) {
        this.thread = new Thread(this);
        this.zone = zone;
        this.thread.setName("ZoneLoader[" + zone.zoneId + "]-0");
    }

    protected List<ChunkCoords> getLoadedChunks() {
        List<ChunkCoords> loaded = new ArrayList<>();
        Point3DMap<Chunk> zoneChunks = ((ZoneInterface) zone).getChunks();
        synchronized (zoneChunks) {
            zoneChunks.forEach((c, x, y, z) -> {
                loaded.add(new ChunkCoords(x, y, z));
            });
        }
        return loaded;
    }

    protected void calculateChunksToLoadAndUnload(List<ChunkCoords> toLoad, List<ChunkCoords> toUnload, List<ChunkCoords> loaded) {
        Set<ChunkCoords> toLoadSet = new LinkedHashSet<>();
        for(Player player : ((ZoneInterface) zone).getPlayers().values()) {
            BetterEntity playerEntity = (BetterEntity) player.getEntity();
            for(int localChunkX = -playerDistance; localChunkX < playerDistance; localChunkX++) {
                for(int localChunkZ = -playerDistance; localChunkZ < playerDistance; localChunkZ++) {
                    for(int localChunkY = -playerDistance; localChunkY < playerDistance; localChunkY++) {
                        int chunkX = Math.floorDiv((int) playerEntity.position.x, 16) + localChunkX;
                        int chunkY = Math.floorDiv((int) playerEntity.position.y, 16) + localChunkY;
                        int chunkZ = Math.floorDiv((int) playerEntity.position.z, 16) + localChunkZ;
                        toLoadSet.add(new ChunkCoords(chunkX, chunkY, chunkZ));
                    }
                }
            }
        }
        toLoad.addAll(toLoadSet);
        toUnload.addAll(loaded);
        toUnload.removeAll(toLoadSet);
        toLoad.removeAll(loaded);
    }

    protected Map<Region, List<Chunk>> getRegionChunkBuckets(List<ChunkCoords> coords) {
        Map<Region, List<Chunk>> chunkUnloadBuckets = new HashMap<>();
        for(ChunkCoords coord : coords) {
            Chunk chunk = ((ZoneInterface) zone).getChunks().get(coord.x(), coord.y(), coord.z());
            if(chunkUnloadBuckets.containsKey(chunk.region)) {
                List<Chunk> bucket = chunkUnloadBuckets.get(chunk.region);
                bucket.add(chunk);
            } else {
                List<Chunk> bucket = new ArrayList<>();
                bucket.add(chunk);
                chunkUnloadBuckets.put(chunk.region, bucket);
            }
        }
        return chunkUnloadBuckets;
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            loop();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public abstract void onChunkModify(Chunk chunk);

    protected abstract void loop();

    public void start() {
        thread.start();
    }

    public void stop() {
        thread.interrupt();
        try {
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dispose() {

    }

}
