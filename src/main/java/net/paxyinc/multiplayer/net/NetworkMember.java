package net.paxyinc.multiplayer.net;

import finalforeach.cosmicreach.world.Zone;

public abstract class NetworkMember {

    public abstract ZoneLoader createZoneLoader(Zone zone);
    public abstract void start();
    public abstract void stop();

}
