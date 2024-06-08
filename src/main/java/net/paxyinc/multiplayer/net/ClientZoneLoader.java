package net.paxyinc.multiplayer.net;

import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import finalforeach.cosmicreach.world.Zone;
import net.paxyinc.multiplayer.GameClient;
import net.paxyinc.multiplayer.interfaces.ChunkInterface;
import net.paxyinc.multiplayer.net.events.ChunkRequestEvent;
import net.paxyinc.multiplayer.net.events.ClientChunkModifyEvent;
import net.paxyinc.multiplayer.net.events.ClientChunkUpdateEvent;
import net.paxyinc.multiplayer.util.ChunkCoords;
import net.paxyinc.multiplayer.util.ChunkDelta;

import java.util.*;
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
        List<ChunkCoords> loaded = getLoadedChunks();
        List<ChunkCoords> toRequest = new ArrayList<>();
        List<ChunkCoords> toUnload = new ArrayList<>();
        calculateChunksToLoadAndUnload(toRequest, toUnload, loaded);

        Vector3 cameraPos = new Vector3(client.worldCamera.position);
        toRequest.sort((o1, o2) -> ChunkCoords.compare(o1, o2, cameraPos));
        toUnload.sort((o1, o2) -> -ChunkCoords.compare(o1, o2, cameraPos));

        for(Map.Entry<Region, List<Chunk>> entry : getRegionChunkBuckets(toUnload).entrySet()) {
            List<Chunk> chunks = entry.getValue();
            chunks.forEach(zone::removeChunk);
        }

        toRequest.removeAll(requested);
        for(ChunkCoords coords : toRequest) {
            requestChunk(coords);
        }
    }

    public void requestChunk(ChunkCoords coords) {
        client.handler.send(new ChunkRequestEvent(zone, coords));
        requested.add(coords);
    }

    public void onChunkFailed(ChunkCoords coords) {
        requested.remove(coords);
    }

    public void onChunkReceived(ChunkCoords coords, Chunk chunk) {
        zone.addChunk(chunk);
        requested.remove(coords);
    }

    @Override
    public void onChunkModify(Chunk chunk) {
        ChunkInterface ci = (ChunkInterface) chunk;
        if(ci.hasTooManyChanges()) {
            ClientChunkUpdateEvent update = new ClientChunkUpdateEvent(chunk);
            client.handler.send(update);
        } else {
            ClientChunkModifyEvent modify = new ClientChunkModifyEvent(chunk, ci.pollChunkChanges());
            client.handler.send(modify);
        }
    }

}
