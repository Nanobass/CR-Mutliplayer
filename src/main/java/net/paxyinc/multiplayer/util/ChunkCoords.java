package net.paxyinc.multiplayer.util;

import com.badlogic.gdx.math.Vector3;

public record ChunkCoords(int x, int y, int z) {

    public static int compare(ChunkCoords o1, ChunkCoords o2, Vector3 cameraPos) {
        int cx = Math.floorDiv((int)cameraPos.x, 16);
        int cy = Math.floorDiv((int)cameraPos.y, 16);
        int cz = Math.floorDiv((int)cameraPos.z, 16);
        ChunkCoords camera = new ChunkCoords(cx, cy, cz);
        float dst1 = o1.distance2(camera);
        float dst2 = o2.distance2(camera);
        return Float.compare(dst1, dst2);
    }

    @Override
    public int hashCode() {
        return x * 16777216 * 16777216 + y * 16777216 + z;
    }

    public float distance2(ChunkCoords o) {
        return Vector3.dst2(x, y, z, o.x, o.y, o.z);
    }
}
