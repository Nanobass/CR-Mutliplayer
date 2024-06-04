package org.example.mod;

import dev.crmodders.flux.FluxRegistries;
import dev.crmodders.flux.block.DataModBlock;
import dev.crmodders.flux.events.OnRegisterBlockEvent;
import dev.crmodders.flux.events.OnRegisterLanguageEvent;
import dev.crmodders.flux.tags.Identifier;
import dev.crmodders.flux.tags.ResourceLocation;
import net.fabricmc.api.ModInitializer;
import org.example.mod.block_entities.ExampleBlockEntity;
import org.example.mod.blocks.Bedrock;
import org.greenrobot.eventbus.Subscribe;

public class ExampleMod implements ModInitializer {

    public static String MOD_ID = "examplemod";
    public static Identifier MOD_NAME = new Identifier(MOD_ID, "name");

    @Override
    public void onInitialize() {
        FluxRegistries.EVENT_BUS.register(this);
        FluxRegistries.ON_INITIALIZE.register(MOD_NAME, this::onInit);
    }

    public void onInit() {
        ExampleBlockEntity.register();
    }

    @Subscribe
    public void onEvent(OnRegisterBlockEvent event) {
        event.registerBlock(() -> new DataModBlock("diamond_block", new ResourceLocation(MOD_ID, "blocks/diamond_block.json")));
        event.registerBlock(Bedrock::new);
    }

    @Subscribe
    public void onEvent(OnRegisterLanguageEvent event) {
        event.registerLanguage(new ResourceLocation(MOD_ID, "languages/en-US.json").load());
    }

}
