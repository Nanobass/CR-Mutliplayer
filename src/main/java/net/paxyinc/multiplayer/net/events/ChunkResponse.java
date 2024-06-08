package net.paxyinc.multiplayer.net.events;

import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.World;
import finalforeach.cosmicreach.world.Zone;
import io.netty.channel.Channel;
import net.paxyinc.multiplayer.GameClient;
import net.paxyinc.multiplayer.net.ChunkSerializer;
import net.paxyinc.multiplayer.net.bus.ServerEvent;
import net.paxyinc.multiplayer.util.ChunkCoords;
import net.querz.nbt.tag.ByteArrayTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.Tag;

public class ChunkResponse extends ServerEvent {

    private final boolean present;
    private final String zoneId;
    private final int[] chunkCoords;
    private final ByteArrayTag chunkData;

    public ChunkResponse(Channel client, Zone zone, ChunkCoords coords) {
        super(client);
        this.present = false;
        this.zoneId = zone.zoneId;
        this.chunkCoords = new int[] { coords.x(), coords.y(), coords.z() };
        this.chunkData = null;
    }

    public ChunkResponse(Channel client, Chunk chunk) {
        super(client);
        this.present = true;
        this.zoneId = chunk.region.zone.zoneId;
        this.chunkCoords = new int[] { chunk.chunkX, chunk.chunkY, chunk.chunkZ };
        this.chunkData = ChunkSerializer.serialize(chunk);
    }

    public ChunkResponse(GameClient server, Tag<?> tag) {
        super(server, tag);
        CompoundTag data = (CompoundTag) tag;
        this.present = data.getBoolean("present");
        this.zoneId = data.getString("zone");
        this.chunkCoords = data.getIntArray("cXYZ");
        this.chunkData = present ? data.getByteArrayTag("data") : null;
    }

    @Override
    public Tag<?> getData() {
        CompoundTag message = new CompoundTag();
        message.putBoolean("present", present);
        message.putString("zone", zoneId);
        message.putIntArray("cXYZ", chunkCoords);
        if(present) {
            message.put("data", chunkData);
        }
        return message;
    }

    public boolean isPresent() {
        return present;
    }

    public Zone getZone(World world) {
        return world.getZone(zoneId);
    }

    public ChunkCoords getChunkCoords() {
        return new ChunkCoords(chunkCoords[0], chunkCoords[1], chunkCoords[2]);
    }

    public Chunk getChunk() {
        Chunk chunk = new Chunk(chunkCoords[0], chunkCoords[1], chunkCoords[2]);
        ChunkSerializer.deserialize(chunkData, chunk);
        chunk.setGenerated(true);
        return chunk;
    }
}
