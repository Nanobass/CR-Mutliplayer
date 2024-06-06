package net.paxyinc.multiplayer.util;

import finalforeach.cosmicreach.blocks.BlockPosition;

public record RaycastResult(BlockPosition breakingPos, BlockPosition placingPos) {

    public boolean canBreak() { return breakingPos != null; }
    public boolean canPlace() { return placingPos != null; }

}
