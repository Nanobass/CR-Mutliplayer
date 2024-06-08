package net.paxyinc.multiplayer.net;

import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.blocks.Block;
import finalforeach.cosmicreach.lighting.LightPropagator;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import finalforeach.cosmicreach.world.Zone;
import io.netty.channel.Channel;
import net.paxyinc.multiplayer.GameServer;
import net.paxyinc.multiplayer.interfaces.ChunkInterface;
import net.paxyinc.multiplayer.net.events.ChunkModifyEvent;
import net.paxyinc.multiplayer.net.events.ClientChunkModifyEvent;
import net.paxyinc.multiplayer.net.events.ClientChunkUpdateEvent;
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
        List<ChunkCoords> loaded = getLoadedChunks();
        List<ChunkCoords> toLoad = new ArrayList<>();
        List<ChunkCoords> toUnload = new ArrayList<>();
        calculateChunksToLoadAndUnload(toLoad, toUnload, loaded);

        Vector3 cameraPos = new Vector3(server.worldCamera.position);
        toLoad.sort((o1, o2) -> ChunkCoords.compare(o1, o2, cameraPos));
        toUnload.sort((o1, o2) -> -ChunkCoords.compare(o1, o2, cameraPos));

        for(Map.Entry<Region, List<Chunk>> entry : getRegionChunkBuckets(toUnload).entrySet()) {
            List<Chunk> chunks = entry.getValue();
            chunks.forEach(zone::removeChunk);
        }

        List<Chunk> newChunks = new ArrayList<>();

        int limit = 256;
        for(ChunkCoords coords : toLoad) {
            Chunk chunk = new Chunk(coords.x(), coords.y(), coords.z());
            chunk.initChunkData();
            chunk.fillLayer(Block.GRASS.getDefaultBlockState(), 0);
            chunk.setGenerated(true);
            zone.addChunk(chunk);

            newChunks.add(chunk);
            if(limit-- == 0) break;
        }

        for(Chunk chunk : newChunks) {
            lightProp.calculateLightingForChunk(zone, chunk, true);
            chunk.flagTouchingChunksForRemeshing(zone, false);
        }

    }

    @Override
    public void onChunkModify(Chunk chunk) {
        ChunkInterface ci = (ChunkInterface) chunk;
        for(Channel client : server.clients) {
            if(ci.hasTooManyChanges()) {

            } else {
                ChunkModifyEvent modify = new ChunkModifyEvent(client, chunk, ci.pollChunkChanges());
                server.handler.send(modify);
            }
        }
    }

}
