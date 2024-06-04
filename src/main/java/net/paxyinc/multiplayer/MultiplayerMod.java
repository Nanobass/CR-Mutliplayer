package net.paxyinc.multiplayer;

import dev.crmodders.cosmicquilt.api.entrypoint.ModInitializer;
import dev.crmodders.flux.FluxRegistries;
import dev.crmodders.flux.tags.Identifier;
import org.quiltmc.loader.api.ModContainer;

public class MultiplayerMod implements ModInitializer {

    public static String MOD_ID = "multiplayer";
    public static Identifier MOD_NAME = new Identifier(MOD_ID, "CR Multiplayer");

    @Override
    public void onInitialize(ModContainer mod) {
        //FluxRegistries.EVENT_BUS.register(this);
        FluxRegistries.ON_PRE_INITIALIZE.register(MOD_NAME, this::onPreInit);
        FluxRegistries.ON_INITIALIZE.register(MOD_NAME, this::onInit);
        FluxRegistries.ON_POST_INITIALIZE.register(MOD_NAME, this::onPostInit);
    }

    public void onPreInit() {

    }

    public void onInit() {

    }

    public void onPostInit() {

    }

}
