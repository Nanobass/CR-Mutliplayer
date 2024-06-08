package net.paxyinc.multiplayer.interfaces;

import finalforeach.cosmicreach.blocks.BlockState;
import net.paxyinc.multiplayer.util.ChunkDelta;

import java.util.List;

public interface ChunkInterface {

    void setBlockStateDirect(BlockState blockState, int localX, int localY, int localZ);

    boolean isChunkModified();

    void setChunkModified(boolean chunkModified);

    boolean hasTooManyChanges();

    List<ChunkDelta> pollChunkChanges();

}
