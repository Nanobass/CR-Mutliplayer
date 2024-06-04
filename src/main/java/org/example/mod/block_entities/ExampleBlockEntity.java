package org.example.mod.block_entities;

import com.badlogic.gdx.graphics.Camera;
import dev.crmodders.flux.entities.TickableBlockEntity;
import dev.crmodders.flux.entities.interfaces.IRenderable;
import dev.crmodders.flux.tags.Identifier;
import finalforeach.cosmicreach.blockentities.BlockEntityCreator;
import finalforeach.cosmicreach.blocks.Block;
import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;
import org.example.mod.ExampleMod;

public class ExampleBlockEntity extends TickableBlockEntity implements IRenderable {

    public static void register() {
        BlockEntityCreator.registerBlockEntityCreator(new Identifier(ExampleMod.MOD_ID, "example_entity").toString(), (block, x, y, z) -> new ExampleBlockEntity(block.getStringId(), x, y, z));
    }

    public ExampleBlockEntity(String id, int x, int y, int z) {
        super(id, x, y, z);
    }

    @Override
    public void onTick(float tps) {
        BlockPosition above = position.getOffsetBlockPos(position.chunk.region.zone, 0, 1, 0);
        BlockState current = above.getBlockState();
        if(current.getBlock() == Block.AIR) {
            above.setBlockState(Block.GRASS.getDefaultBlockState());
            above.flagTouchingChunksForRemeshing(position.chunk.region.zone, false);
        }
    }

    @Override
    public void onRender(Camera camera, float dt) {
        // add custom rendering logic here
    }
}