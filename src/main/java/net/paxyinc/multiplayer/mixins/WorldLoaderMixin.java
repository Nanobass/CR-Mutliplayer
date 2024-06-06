package net.paxyinc.multiplayer.mixins;

import com.badlogic.gdx.utils.PauseableThread;
import finalforeach.cosmicreach.WorldLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldLoader.class)
public class WorldLoaderMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/badlogic/gdx/utils/PauseableThread;start()V"))
    private void noStart(PauseableThread instance) {

    }

}
