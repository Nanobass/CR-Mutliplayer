package net.paxyinc.multiplayer.util;

import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.World;
import finalforeach.cosmicreach.world.Zone;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.Tag;

public record ChunkDeltaInfo(String zoneId, int[] chunkCoords, int[] localCoords, BlockState oldState, BlockState newState) {

    public static ChunkDeltaInfo fromNBT(CompoundTag tag) {
        String zoneId = tag.getString("zone");
        int[] chunkCoords = tag.getIntArray("cXYZ");
        int[] localCoords = tag.getIntArray("bXYZ");
        BlockState oldState = BlockState.getInstance(tag.getString("oldState"));
        BlockState newState = BlockState.getInstance(tag.getString("newState"));
        return new ChunkDeltaInfo(zoneId, chunkCoords, localCoords, oldState, newState);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("zone", zoneId);
        tag.putIntArray("cXYZ", chunkCoords);
        tag.putIntArray("bXYZ", localCoords);
        tag.putString("oldState", oldState.getSaveKey());
        tag.putString("newState", newState.getSaveKey());
        return tag;
    }

    public ChunkDelta toDelta(World world) {
        Zone zone = world.getZone(zoneId);
        Chunk chunk = zone.getChunkAtChunkCoords(chunkCoords[0], chunkCoords[1], chunkCoords[2]);
        BlockPosition position = new BlockPosition(chunk, localCoords[0], localCoords[1], localCoords[2]);
        return new ChunkDelta(position, oldState, newState);
    }

}
