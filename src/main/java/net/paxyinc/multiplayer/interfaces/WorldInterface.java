package net.paxyinc.multiplayer.interfaces;

import finalforeach.cosmicreach.entities.Player;
import finalforeach.cosmicreach.world.Zone;
import net.paxyinc.multiplayer.net.BetterWorldLoader;
import net.paxyinc.multiplayer.net.ZoneLoader;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public interface WorldInterface {

    Player loadPlayer(UUID uuid);
    void unloadPlayer(UUID uuid);
    Player getPlayer(UUID uuid);
    Map<UUID, Player> getPlayers();

    void createWorldLoader(Function<Zone, ZoneLoader> factory);
    BetterWorldLoader getWorldLoader();

}
