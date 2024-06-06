package net.paxyinc.multiplayer.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import finalforeach.cosmicreach.entities.Player;
import finalforeach.cosmicreach.world.World;
import net.paxyinc.multiplayer.interfaces.WorldInterface;
import net.paxyinc.multiplayer.nbt.NbtSerializable;
import net.paxyinc.multiplayer.nbt.NbtSerializer;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class PlayerSaveSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerSaveSystem.class);

    private static FileHandle getPlayerFile(World world, UUID uuid) {
        return Gdx.files.absolute(world.getFullSaveFolder() + "/players/" + uuid.toString() + ".dat");
    }

    public static void savePlayers(World world) {
        WorldInterface wi = (WorldInterface) world;
        for(UUID uuid : wi.getPlayers().keySet()) {
            Player player = wi.getPlayer(uuid);
            savePlayer(world, uuid, player);
        }
    }

    public static void savePlayer(World world, UUID uuid, Player player) {
        FileHandle file = getPlayerFile(world, uuid);
        try {
            file.file().getParentFile().mkdirs();
            file.file().createNewFile();
            CompoundTag tag = NbtSerializer.write((NbtSerializable) player);
            NBTUtil.write(tag, file.file());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Player loadPlayer(World world, UUID uuid) {
        FileHandle file = getPlayerFile(world, uuid);
        try {
            NamedTag tag = NBTUtil.read(file.file());
            if(tag.getTag() instanceof CompoundTag compound) {
                return NbtSerializer.read(compound);
            } else {
                throw new RuntimeException("Unknown entity file");
            }
        } catch (Exception e) {
            LOGGER.error("Player Save File Corrupted");
        }
        return null;
    }

}
