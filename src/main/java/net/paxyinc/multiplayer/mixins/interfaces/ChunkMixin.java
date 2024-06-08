package net.paxyinc.multiplayer.mixins.interfaces;

import finalforeach.cosmicreach.blockentities.BlockEntity;
import finalforeach.cosmicreach.blocks.Block;
import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.savelib.blockdata.IBlockData;
import finalforeach.cosmicreach.util.IPoint3DMap;
import finalforeach.cosmicreach.world.Chunk;
import net.paxyinc.multiplayer.interfaces.ChunkInterface;
import net.paxyinc.multiplayer.util.ChunkDelta;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Mixin(Chunk.class)
public abstract class ChunkMixin implements ChunkInterface {

    @Shadow public IBlockData<BlockState> blockData;

    @Shadow public abstract BlockEntity setBlockEntity(BlockState blockState, int localX, int localY, int localZ);

    @Shadow private IPoint3DMap<BlockEntity> blockEntities;

    @Shadow public abstract void setBlockEntityDirect(BlockState blockState, BlockEntity blockEntity, int localX, int localY, int localZ);

    @Shadow public transient boolean isSaved;

    @Shadow public int chunkX;
    private boolean isChunkModified = false;
    private int numberOfChunkModifications = 0;
    private List<ChunkDelta> changes = new ArrayList<>();

    @Override
    public void setBlockStateDirect(BlockState blockState, int x, int y, int z) {
        this.blockData = this.blockData.setBlockValue(blockState, x, y, z);
        Block block = blockState.getBlock();
        if (block.blockEntityId != null) {
            BlockEntity be = this.setBlockEntity(blockState, x, y, z);
            be.onBlockStateReplaced(blockState);
        } else if (this.blockEntities != null) {
            this.setBlockEntityDirect(blockState, (BlockEntity)null, x, y, z);
        }

        this.isSaved = false;
    }

    @Inject(method = "setBlockState(Lfinalforeach/cosmicreach/blocks/BlockState;III)V", at = @At("TAIL"))
    private void setBlockState(BlockState blockState, int x, int y, int z, CallbackInfo ci) {
        if(!hasTooManyChanges()) {
            BlockPosition position = new BlockPosition((Chunk) (Object) this, x, y, z);
            BlockState oldState = blockState;
            BlockState newState = blockState;
            changes.add(new ChunkDelta(position, oldState, newState));
        }
        numberOfChunkModifications++;
        isChunkModified = true;
    }

    @Override
    public boolean isChunkModified() {
        return isChunkModified;
    }

    @Override
    public void setChunkModified(boolean chunkModified) {
        isChunkModified = chunkModified;
    }

    @Override
    public boolean hasTooManyChanges() {
        return numberOfChunkModifications > 8;
    }

    @Override
    public List<ChunkDelta> pollChunkChanges() {
        List<ChunkDelta> copy = new ArrayList<>(changes);
        numberOfChunkModifications = 0;
        changes.clear();
        return copy;
    }

}
