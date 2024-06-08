package net.paxyinc.multiplayer.net.events;

import finalforeach.cosmicreach.world.World;
import finalforeach.cosmicreach.world.Zone;
import io.netty.channel.Channel;
import net.paxyinc.multiplayer.GameServer;
import net.paxyinc.multiplayer.net.bus.ClientEvent;
import net.paxyinc.multiplayer.util.ChunkCoords;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.Tag;

public class ChunkRequestEvent extends ClientEvent {

    private final String zoneId;
    private final int[] chunkCoords;

    public ChunkRequestEvent(Zone zone, ChunkCoords coords) {
        super();
        this.zoneId = zone.zoneId;
        this.chunkCoords = new int[] {coords.x(), coords.y(), coords.z()};
    }

    public ChunkRequestEvent(GameServer server, Channel client, Tag<?> data) {
        super(server, client, data);
        CompoundTag compound = (CompoundTag) data;
        this.zoneId = compound.getString("zone");
        this.chunkCoords = compound.getIntArray("xyz");
    }

    @Override
    public Tag<?> getData() {
        CompoundTag tag = new CompoundTag();
        tag.putString("zone", zoneId);
        tag.putIntArray("xyz", chunkCoords);
        return tag;
    }

    public Zone getZone(World world) {
        return world.getZone(zoneId);
    }

    public ChunkCoords getChunkCoords() {
        return new ChunkCoords(chunkCoords[0], chunkCoords[1], chunkCoords[2]);
    }

}
