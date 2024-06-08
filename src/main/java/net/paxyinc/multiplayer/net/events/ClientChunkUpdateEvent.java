package net.paxyinc.multiplayer.net.events;

import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.World;
import finalforeach.cosmicreach.world.Zone;
import io.netty.channel.Channel;
import net.paxyinc.multiplayer.GameServer;
import net.paxyinc.multiplayer.interfaces.ChunkInterface;
import net.paxyinc.multiplayer.net.ChunkSerializer;
import net.paxyinc.multiplayer.net.bus.ClientEvent;
import net.querz.nbt.tag.ByteArrayTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.Tag;

public class ClientChunkUpdateEvent extends ClientEvent {

    private final String zoneId;
    private final int[] chunkCoords;
    private final ByteArrayTag chunkData;

    public ClientChunkUpdateEvent(Chunk chunk) {
        super();
        this.zoneId = chunk.region.zone.zoneId;
        this.chunkCoords = new int[] { chunk.chunkX, chunk.chunkY, chunk.chunkZ };
        this.chunkData = ChunkSerializer.serialize(chunk);
    }

    public ClientChunkUpdateEvent(GameServer server, Channel client, Tag<?> tag) {
        super(server, client, tag);
        CompoundTag data = (CompoundTag) tag;
        this.zoneId = data.getString("zone");
        this.chunkCoords = data.getIntArray("cXYZ");
        this.chunkData = data.getByteArrayTag("data");
    }

    public Tag<?> getData() {
        CompoundTag data = new CompoundTag();
        data.putString("zone", zoneId);
        data.putIntArray("cXYZ", chunkCoords);
        data.put("data", chunkData);
        return data;
    }

    public Chunk getChunk(World world) {
        Zone zone = world.getZone(zoneId);
        return zone.getChunkAtChunkCoords(chunkCoords[0], chunkCoords[1], chunkCoords[2]);
    }

    public void deserialize(Chunk chunk) {
        ChunkSerializer.deserialize(chunkData, chunk);
    }

}
