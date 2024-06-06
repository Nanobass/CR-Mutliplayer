package net.paxyinc.multiplayer.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Queue;
import finalforeach.cosmicreach.BlockSelection;
import finalforeach.cosmicreach.blockentities.BlockEntity;
import finalforeach.cosmicreach.blockentities.BlockEntityItemContainer;
import finalforeach.cosmicreach.blockevents.BlockEventTrigger;
import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.blocks.PooledBlockPosition;
import finalforeach.cosmicreach.entities.Entity;
import finalforeach.cosmicreach.gamestates.InGame;
import finalforeach.cosmicreach.items.Item;
import finalforeach.cosmicreach.items.ItemBlock;
import finalforeach.cosmicreach.items.ItemSlot;
import finalforeach.cosmicreach.items.ItemStack;
import finalforeach.cosmicreach.lighting.LightPropagator;
import finalforeach.cosmicreach.settings.Controls;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Zone;

import java.util.HashMap;
import java.util.Map;

public class CustomBlockSelection {
    public static ShapeRenderer shapeRenderer = new ShapeRenderer();
    public static boolean enabled;
    private BlockState lastSelectedBlockState;
    private static BlockState selectedBlockState;
    private BlockPosition lastSelectedBlockPos;
    private static BlockPosition selectedBlockPos;
    private Array<BoundingBox> blockBoundingBoxes = new Array(false, 4, BoundingBox.class);
    private LightPropagator lightPropagator = new LightPropagator();
    private Ray ray = new Ray();
    private BoundingBox tmpBoundingBox = new BoundingBox();
    private Vector3 intersection = new Vector3();
    private float maximumRaycastDist = 6.0F;
    private Array<BlockPosition> toVisit = new Array();
    private Vector3 workingPos = new Vector3();
    private Queue<BlockPosition> blockQueue = new Queue();
    private double timeSinceBlockModify = 0.0;
    private Vector3 mouseCoords = new Vector3();
    private Vector3 mouseCoords2 = new Vector3();
    private Array<BoundingBox> tmpBoundingBoxes = new Array(BoundingBox.class);
    private static Array<BlockPosition> positionsToFree = new Array();
    static Pool<BlockPosition> positionPool = new Pool<BlockPosition>() {
        protected BlockPosition newObject() {
            PooledBlockPosition<BlockPosition> p = new PooledBlockPosition(CustomBlockSelection.positionPool, (Chunk)null, 0, 0, 0);
            CustomBlockSelection.positionsToFree.add(p);
            return p;
        }
    };

    public static BlockState getBlockLookingAt() {
        return !enabled ? null : selectedBlockState;
    }

    public static BlockPosition getBlockPositionLookingAt() {
        return !enabled ? null : selectedBlockPos;
    }

    public void render(Camera worldCamera) {
        if (enabled) {
            if (selectedBlockState != null) {
                Array.ArrayIterator var2;
                BoundingBox bb;
                if (selectedBlockState != this.lastSelectedBlockState || selectedBlockPos != this.lastSelectedBlockPos) {
                    selectedBlockState.getAllBoundingBoxes(this.blockBoundingBoxes, selectedBlockPos);
                    var2 = this.blockBoundingBoxes.iterator();

                    while(var2.hasNext()) {
                        bb = (BoundingBox)var2.next();
                        bb.min.sub(0.001F);
                        bb.max.add(0.001F);
                        bb.update();
                    }

                    this.lastSelectedBlockState = selectedBlockState;
                    this.lastSelectedBlockPos = selectedBlockPos;
                }

                shapeRenderer.setProjectionMatrix(worldCamera.combined);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(Color.BLACK);
                var2 = this.blockBoundingBoxes.iterator();

                while(var2.hasNext()) {
                    bb = (BoundingBox)var2.next();
                    shapeRenderer.box(bb.min.x, bb.min.y, bb.min.z, bb.getWidth(), bb.getHeight(), -bb.getDepth());
                }

                shapeRenderer.end();
            }

        }
    }

    private void addBlockToQueue(Zone zone, BlockPosition bp, int dx, int dy, int dz) {
        BlockPosition step = bp.getOffsetBlockPos(positionPool, zone, dx, dy, dz);
        if (step != null && !this.toVisit.contains(step, false)) {
            BlockState block = bp.getBlockState();
            if (block != null) {
                block.getBoundingBox(this.tmpBoundingBox, step);
                if (Intersector.intersectRayBounds(this.ray, this.tmpBoundingBox, this.intersection)) {
                    this.blockQueue.addLast(step);
                    this.toVisit.add(step);
                }
            }
        }

    }

    private boolean intersectsWithBlock(BlockState block, BlockPosition nextBlockPos) {
        block.getBoundingBox(this.tmpBoundingBox, nextBlockPos);
        if (!Intersector.intersectRayBounds(this.ray, this.tmpBoundingBox, this.intersection)) {
            return false;
        } else {
            block.getAllBoundingBoxes(this.tmpBoundingBoxes, nextBlockPos);
            Array.ArrayIterator var3 = this.tmpBoundingBoxes.iterator();

            BoundingBox bb;
            do {
                if (!var3.hasNext()) {
                    return false;
                }

                bb = (BoundingBox)var3.next();
            } while(!Intersector.intersectRayBounds(this.ray, bb, this.intersection));

            return true;
        }
    }

    public void raycast(Zone zone, Camera worldCamera) {
        enabled = false;
        BlockPosition lastBlockPosAtPoint = null;
        BlockPosition placingBlockPos = null;
        BlockPosition breakingBlockPos = null;
        BlockPosition lastBlockPosInQueue = null;
        this.toVisit.clear();
        this.blockQueue.clear();
        if (Gdx.input.isCursorCatched()) {
            this.ray.set(worldCamera.position, worldCamera.direction);
        } else {
            this.mouseCoords.set((float)Gdx.input.getX(), (float)Gdx.input.getY(), 0.0F);
            this.mouseCoords2.set((float)Gdx.input.getX(), (float)Gdx.input.getY(), 1.0F);
            worldCamera.unproject(this.mouseCoords);
            worldCamera.unproject(this.mouseCoords2);
            this.mouseCoords2.sub(this.mouseCoords).nor();
            this.ray.set(this.mouseCoords, this.mouseCoords2);
        }

        boolean breakPressed = this.timeSinceBlockModify <= 0.0 && Controls.breakPressed();
        boolean placePressed = this.timeSinceBlockModify <= 0.0 && Controls.placePressed();
        breakPressed |= Controls.breakJustPressed();
        placePressed |= Controls.placeJustPressed();
        boolean interactJustPressed = Controls.placeJustPressed();
        boolean interactHeld = placePressed;
        this.workingPos.set(this.ray.origin);

        for(; this.workingPos.dst(this.ray.origin) <= this.maximumRaycastDist; this.workingPos.add(this.ray.direction)) {
            int bx = (int)Math.floor((double)this.workingPos.x);
            int by = (int)Math.floor((double)this.workingPos.y);
            int bz = (int)Math.floor((double)this.workingPos.z);
            int dx = 0;
            int dy = 0;
            int dz = 0;
            if (lastBlockPosAtPoint != null) {
                if (lastBlockPosAtPoint.getGlobalX() == bx && lastBlockPosAtPoint.getGlobalY() == by && lastBlockPosAtPoint.getGlobalZ() == bz) {
                    continue;
                }

                dx = bx - lastBlockPosAtPoint.getGlobalX();
                dy = by - lastBlockPosAtPoint.getGlobalY();
                dz = bz - lastBlockPosAtPoint.getGlobalZ();
            }

            Chunk c = zone.getChunkAtBlock(bx, by, bz);
            if (c == null) {
                if (!breakPressed && !placePressed) {
                    continue;
                }

                int cx = Math.floorDiv(bx, 16);
                int cy = Math.floorDiv(by, 16);
                int cz = Math.floorDiv(bz, 16);
                c = new Chunk(cx, cy, cz);
                c.initChunkData();
                c.setGenerated(true);
                boolean isSky = c.chunkY > 0;
                if (isSky) {
                    for(int i = c.chunkY; i < c.chunkY + 16; ++i) {
                        if (zone.getChunkAtBlock(bx, by + i * 16, bz) != null) {
                            isSky = false;
                            break;
                        }
                    }
                }

                this.lightPropagator.calculateLightingForChunk(zone, c, isSky);
                zone.addChunk(c);
                c.region.setColumnGeneratedForChunk(c, true);
            }

            BlockPosition nextBlockPos = (BlockPosition)positionPool.obtain();
            nextBlockPos.set(c, bx - c.blockX, by - c.blockY, bz - c.blockZ);
            if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1) {
                if (dx != 0) {
                    this.addBlockToQueue(zone, lastBlockPosAtPoint, dx, 0, 0);
                }

                if (dy != 0) {
                    this.addBlockToQueue(zone, lastBlockPosAtPoint, 0, dy, 0);
                }

                if (dz != 0) {
                    this.addBlockToQueue(zone, lastBlockPosAtPoint, 0, 0, dz);
                }

                if (dx != 0 && dy != 0) {
                    this.addBlockToQueue(zone, lastBlockPosAtPoint, dx, dy, 0);
                }

                if (dx != 0 && dz != 0) {
                    this.addBlockToQueue(zone, lastBlockPosAtPoint, dx, 0, dz);
                }

                if (dy != 0 && dz != 0) {
                    this.addBlockToQueue(zone, lastBlockPosAtPoint, 0, dy, dz);
                }
            }

            if (nextBlockPos != null && !this.toVisit.contains(nextBlockPos, false)) {
                BlockState block = nextBlockPos.getBlockState();
                block.getBoundingBox(this.tmpBoundingBox, nextBlockPos);
                if (Intersector.intersectRayBounds(this.ray, this.tmpBoundingBox, this.intersection)) {
                    this.blockQueue.addLast(nextBlockPos);
                    this.toVisit.add(nextBlockPos);
                } else if (block.canRaycastForReplace()) {
                    this.tmpBoundingBox.min.set((float)nextBlockPos.getGlobalX(), (float)nextBlockPos.getGlobalY(), (float)nextBlockPos.getGlobalZ());
                    this.tmpBoundingBox.max.set(this.tmpBoundingBox.min).add(1.0F, 1.0F, 1.0F);
                    if (Intersector.intersectRayBounds(this.ray, this.tmpBoundingBox, this.intersection)) {
                        this.blockQueue.addLast(nextBlockPos);
                        this.toVisit.add(nextBlockPos);
                    }
                }
            }

            label193:
            while(true) {
                BlockPosition curBlockPos;
                BlockState blockState;
                do {
                    if (!this.blockQueue.notEmpty()) {
                        break label193;
                    }

                    curBlockPos = (BlockPosition)this.blockQueue.removeFirst();
                    blockState = curBlockPos.getBlockState();
                } while(!blockState.hasEmptyModel() && !this.intersectsWithBlock(blockState, curBlockPos));

                if (breakingBlockPos == null && blockState.canRaycastForBreak()) {
                    breakingBlockPos = curBlockPos;
                    enabled = true;
                    selectedBlockState = blockState;
                    selectedBlockPos = curBlockPos;
                }

                if (placingBlockPos == null && blockState.canRaycastForPlaceOn() && lastBlockPosInQueue != null) {
                    BlockState lastBlockStateInQueue = lastBlockPosInQueue.getBlockState();
                    if (lastBlockStateInQueue.canRaycastForReplace()) {
                        placingBlockPos = lastBlockPosInQueue;
                        enabled = true;
                        selectedBlockState = blockState;
                        selectedBlockPos = curBlockPos;
                    }
                }

                if (breakingBlockPos != null && placingBlockPos != null) {
                    break;
                }

                lastBlockPosInQueue = curBlockPos;
            }

            if (breakingBlockPos != null && placingBlockPos != null) {
                break;
            }

            lastBlockPosAtPoint = nextBlockPos;
        }

        BlockState targetBlockState = null;
        boolean targetFromSelectedSlot = false;
        ItemStack itemStack = null;
        if (itemStack != null) {
            Item item = itemStack.getItem();
            if (item instanceof ItemBlock) {
                ItemBlock itemBlock = (ItemBlock)item;
                targetBlockState = itemBlock.getBlockState();
                targetFromSelectedSlot = true;
            }
        }

        this.timeSinceBlockModify -= (double)Gdx.graphics.getDeltaTime();
        if (breakingBlockPos != null && Controls.pickBlockPressed()) {

        }

        if (breakingBlockPos != null && breakPressed) {
            this.breakBlock(zone, breakingBlockPos.copy(), this.timeSinceBlockModify);
            this.timeSinceBlockModify = 0.25;
        }

        if (placingBlockPos != null && placePressed && targetBlockState != null) {
            Entity playerEntity = InGame.getLocalPlayer().getEntity();
            boolean positionBlockedByPlayer = false;
            if (!targetBlockState.walkThrough) {
                BoundingBox blockBoundingBox = new BoundingBox();
                BoundingBox playerBoundingBox = new BoundingBox();
                playerBoundingBox.set(playerEntity.localBoundingBox);
                playerBoundingBox.min.add(playerEntity.position);
                playerBoundingBox.max.add(playerEntity.position);
                playerBoundingBox.update();
                targetBlockState.getBoundingBox(blockBoundingBox, placingBlockPos);
                if (blockBoundingBox.intersects(playerBoundingBox) && blockBoundingBox.max.y - playerBoundingBox.min.y > playerEntity.maxStepHeight) {
                    positionBlockedByPlayer = true;
                }
            }

            if (!positionBlockedByPlayer || playerEntity.noClip) {
                this.timeSinceBlockModify = 0.25;
                if (targetFromSelectedSlot) {
                }

                this.placeBlock(zone, targetBlockState, placingBlockPos.copy(), this.timeSinceBlockModify);
            }
        } else if (breakingBlockPos != null && (interactJustPressed || interactHeld)) {
            this.interactWith(zone, breakingBlockPos.copy(), interactJustPressed, interactHeld, this.timeSinceBlockModify);
            this.timeSinceBlockModify = 0.25;
        }

        positionPool.freeAll(positionsToFree);
    }

    private void breakBlock(Zone zone, BlockPosition blockPos, double timeSinceLastInteract) {
        BlockState blockState = blockPos.getBlockState();
        if (blockState != null) {
            ItemStack droppedItemStack = new ItemStack(blockState.getItem(), 1);
            InGame.getLocalPlayer().inventory.addItemStackWithSwapGroup(droppedItemStack);
            BlockEventTrigger[] triggers = blockState.getTrigger("onBreak");
            if (triggers != null) {
                Map<String, Object> args = new HashMap();
                args.put("blockPos", blockPos);
                args.put("timeSinceLastInteract", timeSinceLastInteract);

                for(int i = 0; i < triggers.length; ++i) {
                    triggers[i].act(blockState, zone, args);
                }

            }
        }
    }

    private void placeBlock(Zone zone, BlockState targetBlockState, BlockPosition blockPos, double timeSinceLastInteract) {
        BlockState blockState = blockPos.getBlockState();
        if (blockState != null) {
            BlockEventTrigger[] triggers = targetBlockState.getTrigger("onPlace");
            if (triggers != null) {
                String targetId = targetBlockState.getStateParamsStr();
                HashMap m;
                float xDiff;
                if (targetId.contains("slab_type=top") || targetId.contains("slab_type=bottom")) {
                    m = new HashMap();
                    xDiff = this.intersection.y - (float)blockPos.getGlobalY();
                    if ((double)xDiff < 0.5) {
                        m.put("slab_type", "bottom");
                    } else {
                        m.put("slab_type", "top");
                    }

                    targetBlockState = targetBlockState.getVariantWithParams(m);
                }

                float zDiff;
                if (targetId.contains("slab_type=vertical")) {
                    m = new HashMap();
                    xDiff = this.intersection.x - (float)blockPos.getGlobalX();
                    zDiff = this.intersection.z - (float)blockPos.getGlobalZ();
                    if (Math.abs((double)xDiff - 0.5) > Math.abs((double)zDiff - 0.5)) {
                        if ((double)xDiff < 0.5) {
                            m.put("slab_type", "verticalNegX");
                        } else {
                            m.put("slab_type", "verticalPosX");
                        }
                    } else if ((double)zDiff < 0.5) {
                        m.put("slab_type", "verticalNegZ");
                    } else {
                        m.put("slab_type", "verticalPosZ");
                    }

                    targetBlockState = targetBlockState.getVariantWithParams(m);
                }

                if (targetId.contains("stair_type")) {
                    m = new HashMap();
                    xDiff = this.intersection.x - (float)blockPos.getGlobalX();
                    zDiff = this.intersection.z - (float)blockPos.getGlobalZ();
                    if (Math.abs((double)xDiff - 0.5) > Math.abs((double)zDiff - 0.5)) {
                        if ((double)xDiff < 0.5) {
                            m.put("stair_type", "bottom_NegX");
                        } else {
                            m.put("stair_type", "bottom_PosX");
                        }
                    } else if ((double)zDiff < 0.5) {
                        m.put("stair_type", "bottom_NegZ");
                    } else {
                        m.put("stair_type", "bottom_PosZ");
                    }

                    targetBlockState = targetBlockState.getVariantWithParams(m);
                }

                m = new HashMap();
                m.put("blockPos", blockPos);
                m.put("targetBlockState", targetBlockState);
                m.put("timeSinceLastInteract", timeSinceLastInteract);

                for(int i = 0; i < triggers.length; ++i) {
                    triggers[i].act(targetBlockState, zone, m);
                }

            }
        }
    }

    private void interactWith(Zone zone, BlockPosition blockPos, boolean interactJustPressed, boolean interactHeld, double timeSinceLastInteract) {
        BlockState blockState = blockPos.getBlockState();
        if (blockState != null) {
            BlockEntity blockEntity = blockPos.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.onInteract(zone, blockPos, interactJustPressed, interactHeld, timeSinceLastInteract);
            }

            if (blockEntity instanceof BlockEntityItemContainer) {
                BlockEntityItemContainer c = (BlockEntityItemContainer)blockEntity;
            }

            BlockEventTrigger[] triggers = blockState.getTrigger("onInteract");
            if (triggers != null) {
                Map<String, Object> args = new HashMap();
                args.put("blockPos", blockPos);
                args.put("interactJustPressed", interactJustPressed);
                args.put("interactHeld", interactHeld);
                args.put("timeSinceLastInteract", timeSinceLastInteract);

                for(int i = 0; i < triggers.length; ++i) {
                    triggers[i].act(blockState, zone, args);
                }

            }
        }
    }
}
