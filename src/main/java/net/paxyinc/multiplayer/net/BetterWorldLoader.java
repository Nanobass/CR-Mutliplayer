package net.paxyinc.multiplayer.net;

import finalforeach.cosmicreach.world.Zone;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class BetterWorldLoader {

    private final Map<Zone, ZoneLoader> loaders = new HashMap<>();
    private final Function<Zone, ZoneLoader> factory;

    public BetterWorldLoader(Function<Zone, ZoneLoader> factory) {
        this.factory = factory;
    }

    public boolean hasZoneLoader(Zone zone) {
        return loaders.containsKey(zone);
    }

    public ZoneLoader getZoneLoader(Zone zone) {
        return loaders.get(zone);
    }

    public void addZoneLoader(Zone zone) {
        ZoneLoader loader = factory.apply(zone);
        loader.start();
        loaders.put(zone, loader);
    }

    public void removeZoneLoader(Zone zone) {
        ZoneLoader loader = loaders.remove(zone);
        loader.stop();
        loader.dispose();
    }

}
