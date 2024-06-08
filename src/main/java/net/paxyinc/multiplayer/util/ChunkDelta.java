package net.paxyinc.multiplayer.util;

import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;

public record ChunkDelta(BlockPosition position, BlockState oldState, BlockState newState) {

}
