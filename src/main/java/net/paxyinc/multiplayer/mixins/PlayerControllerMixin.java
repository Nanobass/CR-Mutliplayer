package net.paxyinc.multiplayer.mixins;

import finalforeach.cosmicreach.entities.PlayerController;
import finalforeach.cosmicreach.items.ItemCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerController.class)
public class PlayerControllerMixin {

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lfinalforeach/cosmicreach/items/ItemCatalog;isShown()Z"))
    private boolean isShown(ItemCatalog instance) {
        return false;
    }

}
