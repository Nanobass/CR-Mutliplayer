package net.paxyinc.multiplayer.mixins.interfaces;

import finalforeach.cosmicreach.entities.Entity;
import finalforeach.cosmicreach.entities.Player;
import net.paxyinc.multiplayer.entities.BetterEntity;
import net.paxyinc.multiplayer.nbt.NbtSerializable;
import net.paxyinc.multiplayer.nbt.NbtSerializer;
import net.querz.nbt.tag.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public class PlayerMixin implements NbtSerializable {
    @Shadow public String zoneId;

    @Shadow private Entity entity;

    @Override
    public void write(CompoundTag nbt) {
        nbt.putString("zoneId", zoneId);
        nbt.put("entity", NbtSerializer.write((NbtSerializable) entity));
    }

    @Override
    public void read(CompoundTag nbt) {
        zoneId = nbt.getString("zoneId");
        entity = NbtSerializer.read(nbt.getCompoundTag("entity"), BetterEntity.class);
    }
}
