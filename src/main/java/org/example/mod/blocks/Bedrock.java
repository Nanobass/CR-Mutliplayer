package org.example.mod.blocks;

import dev.crmodders.flux.block.IModBlock;
import dev.crmodders.flux.generators.BlockEventGenerator;
import dev.crmodders.flux.generators.BlockGenerator;
import dev.crmodders.flux.generators.BlockModelGenerator;
import dev.crmodders.flux.tags.Identifier;
import dev.crmodders.flux.tags.ResourceLocation;
import finalforeach.cosmicreach.blockevents.actions.BlockActionReplaceBlockState;
import finalforeach.cosmicreach.blocks.Block;
import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.entities.Player;
import finalforeach.cosmicreach.items.Hotbar;
import finalforeach.cosmicreach.items.Item;
import finalforeach.cosmicreach.items.ItemSlot;
import finalforeach.cosmicreach.ui.UI;
import finalforeach.cosmicreach.world.Zone;
import org.example.mod.ExampleMod;

import java.util.List;
import java.util.Map;

public class Bedrock implements IModBlock {

    public static final Identifier BLOCK_ID = new Identifier(ExampleMod.MOD_ID, "bedrock");
    public static final String BLOCK_NAME = "bedrock";

    public static final ResourceLocation ALL_TEXTURE = new ResourceLocation("base", "textures/blocks/lunar_soil.png");

    @Override
    public void onBreak(Zone zone, Player player, BlockState blockState, BlockPosition position) {
        ItemSlot slot = UI.hotbar.getSelectedSlot();
        if(slot == null) return;
        if(slot.itemStack != null) {
            Item selected = slot.itemStack.getItem();
            String itemId = selected.getID();
            if(itemId.startsWith(BLOCK_ID.toString())) {
                // make the block breakable when the player holds bedrock
                IModBlock.super.onBreak(zone, player, blockState, position);
            }
        }
        // make the block unbreakable, by omitting the super call here
    }

    @Override
    public BlockGenerator getBlockGenerator() {
        BlockGenerator generator = new BlockGenerator(BLOCK_ID, BLOCK_NAME);
        generator.createBlockState("default", "model", true, "events", true);
        generator.addBlockEntity("examplemod:example_entity", Map.of());
        return generator;
    }

    @Override
    public List<BlockModelGenerator> getBlockModelGenerators(Identifier blockId) {
        BlockModelGenerator generator = new BlockModelGenerator(blockId, "model");
        generator.createTexture("all", ALL_TEXTURE);
        generator.createCuboid(0, 0, 0, 16, 16, 16, "all");
        return List.of(generator);
    }

    @Override
    public List<BlockEventGenerator> getBlockEventGenerators(Identifier blockId) {
        BlockEventGenerator generator = new BlockEventGenerator(blockId, "events");
        return List.of(generator);
    }
}
