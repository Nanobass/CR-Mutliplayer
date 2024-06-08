package net.paxyinc.multiplayer.net.events;

import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.World;
import finalforeach.cosmicreach.world.Zone;
import io.netty.channel.Channel;
import net.paxyinc.multiplayer.GameClient;
import net.paxyinc.multiplayer.GameServer;
import net.paxyinc.multiplayer.net.bus.ClientEvent;
import net.paxyinc.multiplayer.net.bus.ServerEvent;
import net.paxyinc.multiplayer.util.ChunkDelta;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;

import java.util.ArrayList;
import java.util.List;

public class ChunkModifyEvent extends ServerEvent {

    private static class BlockPositionTuple {
        public final int[] localCoords;
        public final BlockState oldState;
        public final BlockState newState;

        private BlockPositionTuple(ChunkDelta delta) {
            BlockPosition position = delta.position();
            this.localCoords = new int[] { position.localX, position.localY, position.localZ };
            this.oldState = delta.oldState();
            this.newState = delta.newState();
        }

        private BlockPositionTuple(CompoundTag tag) {
            this.localCoords = tag.getIntArray("bXYZ");
            this.oldState = BlockState.getInstance(tag.getString("old"));
            this.newState = BlockState.getInstance(tag.getString("new"));
        }

        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putIntArray("bXYZ", localCoords);
            tag.putString("old", oldState.getSaveKey());
            tag.putString("new", newState.getSaveKey());
            return tag;
        }

        public ChunkDelta toDelta(Chunk chunk) {
            return new ChunkDelta(new BlockPosition(chunk, localCoords[0], localCoords[1], localCoords[2]), oldState, newState);
        }

    }

    private final String zoneId;
    private final int[] chunkCoords;
    private final List<BlockPositionTuple> changes;

    public ChunkModifyEvent(Channel client, Chunk chunk, List<ChunkDelta> deltas) {
        super(client);
        this.zoneId = chunk.region.zone.zoneId;
        this.chunkCoords = new int[] { chunk.chunkX, chunk.chunkY, chunk.chunkZ };
        this.changes = new ArrayList<>();
        for (ChunkDelta delta : deltas) {
            changes.add(new BlockPositionTuple(delta));
        }
    }

    public ChunkModifyEvent(GameClient client, Tag<?> tag) {
        super(client, tag);
        CompoundTag data = (CompoundTag) tag;
        this.zoneId = data.getString("zone");
        this.chunkCoords = data.getIntArray("cXYZ");
        this.changes = new ArrayList<>();
        for(CompoundTag delta : data.getListTag("changes").asCompoundTagList()) {
            changes.add(new BlockPositionTuple(delta));
        }
    }

    public Tag<?> getData() {
        CompoundTag data = new CompoundTag();
        data.putString("zone", zoneId);
        data.putIntArray("cXYZ", chunkCoords);
        ListTag<CompoundTag> deltas = new ListTag<>(CompoundTag.class);
        for(BlockPositionTuple change : changes) {
            deltas.add(change.toNBT());
        }
        data.put("changes", deltas);
        return data;
    }

    public Chunk getChunk(World world) {
        Zone zone = world.getZone(zoneId);
        return zone.getChunkAtChunkCoords(chunkCoords[0], chunkCoords[1], chunkCoords[2]);
    }

    public int getNumberOfChanges() {
        return changes.size();
    }

    public ChunkDelta getDelta(Chunk chunk, int i) {
        return changes.get(i).toDelta(chunk);
    }

}
