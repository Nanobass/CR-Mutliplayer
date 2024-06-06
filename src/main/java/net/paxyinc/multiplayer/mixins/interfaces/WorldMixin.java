package net.paxyinc.multiplayer.mixins.interfaces;

import finalforeach.cosmicreach.entities.Player;
import finalforeach.cosmicreach.world.World;
import finalforeach.cosmicreach.world.Zone;
import finalforeach.cosmicreach.worldgen.ZoneGenerator;
import net.paxyinc.multiplayer.entities.BetterEntity;
import net.paxyinc.multiplayer.interfaces.WorldInterface;
import net.paxyinc.multiplayer.interfaces.ZoneInterface;
import net.paxyinc.multiplayer.net.BetterWorldLoader;
import net.paxyinc.multiplayer.net.ZoneLoader;
import net.paxyinc.multiplayer.util.PlayerSaveSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

@Mixin(World.class)
public class WorldMixin implements WorldInterface {

    @Shadow public String defaultZoneId;
    @Shadow private transient HashMap<String, Zone> zoneMap;
    @Shadow public transient String worldFolderName;
    @Shadow private String worldDisplayName;
    @Shadow public long worldSeed = (new Random()).nextLong();

    private final Map<UUID, Player> allPlayers = new HashMap<>();

    private BetterWorldLoader worldLoader;

    private World this0() {
        return (World) (Object) this;
    }

    @Override
    public void createWorldLoader(Function<Zone, ZoneLoader> factory) {
        worldLoader = new BetterWorldLoader(factory);
    }

    @Override
    public BetterWorldLoader getWorldLoader() {
        return worldLoader;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public Zone getZone(String zoneId) {
        synchronized(zoneMap) {
            Zone zone = zoneMap.get(zoneId);
            if (zone == null) {
                zone = Zone.loadZone(this0(), zoneId);
                zoneMap.put(zoneId, zone);
            }
            if(!worldLoader.hasZoneLoader(zone)) worldLoader.addZoneLoader(zone);
            return zone;
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void addNewZone(String zoneId, ZoneGenerator zoneGen) {
        zoneGen.seed = worldSeed + (long)zoneId.hashCode();
        Zone zone = new Zone(this0(), zoneId, zoneGen);
        zoneMap.put(zoneId, zone);
        //worldLoader.addZoneLoader(zone);
    }

    @Override @SuppressWarnings("all")
    public Player loadPlayer(UUID uuid) {
        World world = this0();
        Player player = PlayerSaveSystem.loadPlayer(world, uuid);
        if(player == null) {
            player = new Player();
            player.zoneId = defaultZoneId;
            BetterEntity entity = new BetterEntity();
            entity.savable = false;
            player.setEntity(entity);
            player.setPosition(0, 130, 0);

        }
        ZoneInterface zi = (ZoneInterface) player.getZone(world);
        zi.addEntity((BetterEntity) player.getEntity());
        allPlayers.put(uuid, player);
        return player;
    }

    @Override @SuppressWarnings("all")
    public void unloadPlayer(UUID uuid) {
        World world = this0();
        Player player = getPlayer(uuid);
        if(player != null) allPlayers.remove(uuid);
        else return;
        PlayerSaveSystem.savePlayer(world, uuid, player);
    }

    @Override
    public Player getPlayer(UUID uuid) {
        return allPlayers.get(uuid);
    }

    @Override
    public Map<UUID, Player> getPlayers() {
        return allPlayers;
    }

}
