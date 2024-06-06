package net.paxyinc.multiplayer.util;

public record ChunkCoords(int x, int y, int z) {

    @Override
    public int hashCode() {
        return x * 16777216 * 16777216 + y * 16777216 + z;
    }
}
