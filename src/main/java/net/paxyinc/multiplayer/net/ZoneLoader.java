package net.paxyinc.multiplayer.net;

import finalforeach.cosmicreach.world.Zone;

public abstract class ZoneLoader implements Runnable {

    protected Thread thread;

    protected final Zone zone;
    public int playerDistance = 10;
    public boolean saveRequested = false;
    public Object worldGenLock = new Object();

    public ZoneLoader(Zone zone) {
        this.thread = new Thread(this);
        this.zone = zone;
        this.thread.setName("ZoneLoader[" + zone.zoneId + "]-0");
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            loop();
        }
    }

    protected abstract void loop();

    public void start() {
        thread.start();
    }

    public void stop() {
        thread.interrupt();
        try {
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dispose() {

    }

}
