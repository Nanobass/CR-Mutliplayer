package net.paxyinc.multiplayer.net;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ByteArray;
import finalforeach.cosmicreach.blockentities.BlockEntity;
import finalforeach.cosmicreach.blockentities.BlockEntityCreator;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.io.*;
import finalforeach.cosmicreach.savelib.IChunkByteReader;
import finalforeach.cosmicreach.savelib.ISaveFileConstant;
import finalforeach.cosmicreach.savelib.blockdata.IBlockData;
import finalforeach.cosmicreach.savelib.blockdata.LayeredBlockData;
import finalforeach.cosmicreach.savelib.blockdata.SingleBlockData;
import finalforeach.cosmicreach.savelib.lightdata.blocklight.BlockLightLayeredData;
import finalforeach.cosmicreach.savelib.lightdata.blocklight.IBlockLightData;
import finalforeach.cosmicreach.savelib.lightdata.blocklight.layers.BlockLightDataShortLayer;
import finalforeach.cosmicreach.savelib.lightdata.blocklight.layers.BlockLightDataSingleLayer;
import finalforeach.cosmicreach.savelib.lightdata.skylight.ISkylightData;
import finalforeach.cosmicreach.savelib.lightdata.skylight.SkylightLayeredData;
import finalforeach.cosmicreach.savelib.lightdata.skylight.SkylightSingleData;
import finalforeach.cosmicreach.savelib.lightdata.skylight.layers.ISkylightDataLayer;
import finalforeach.cosmicreach.savelib.lightdata.skylight.layers.SkylightDataNibbleLayer;
import finalforeach.cosmicreach.savelib.lightdata.skylight.layers.SkylightDataSingleLayer;
import finalforeach.cosmicreach.world.Chunk;
import net.paxyinc.multiplayer.nbt.NbtSerializable;
import net.querz.nbt.tag.ByteArrayTag;
import net.querz.nbt.tag.CompoundTag;

import java.io.*;
import java.nio.ByteBuffer;

public class ChunkSerializer {

    private static IBlockData<BlockState> readBlockData(DataInput reader) throws IOException {
        int chunkDataType = reader.readByte();
        return switch (chunkDataType) {
            case 1 -> SingleBlockData.readFrom(new DataInputChunkByteReader(reader), BlockState::getInstance);
            case 2 -> LayeredBlockData.readFrom(new DataInputChunkByteReader(reader), BlockState::getInstance);
            default -> throw new RuntimeException("Unknown chunkDataType: " + chunkDataType);
        };
    }

    private static ISkylightData readSkylightData(DataInput reader) throws IOException {
        int skylightDataType = reader.readByte();
        return switch (skylightDataType) {
            case 1 -> null;
            case 2 -> {
                SkylightLayeredData skyLayeredData = new SkylightLayeredData();
                for(int l = 0; l < 16; ++l) {
                    int layerType = reader.readByte();
                    switch (layerType) {
                        case 1:
                            ISkylightDataLayer skySingleLayer = SkylightDataSingleLayer.getForLightValue(reader.readByte());
                            skyLayeredData.setLayer(l, skySingleLayer);
                            break;
                        case 2:
                            byte[] bytes = new byte[128];
                            reader.readFully(bytes);
                            SkylightDataNibbleLayer layer = new SkylightDataNibbleLayer(bytes);
                            skyLayeredData.setLayer(l, layer);
                            break;
                        default:
                            throw new RuntimeException("Unknown layerType: " + layerType);
                    }
                }
                yield skyLayeredData;
            }
            case 3 -> SkylightSingleData.getForLightValue(reader.readByte());
            default -> throw new RuntimeException("Unknown skylightDataType: " + skylightDataType);
        };
    }

    private static IBlockLightData readBlockLightData(DataInput reader) throws IOException {
        int blockLightDataType = reader.readByte();
        return switch (blockLightDataType) {
            case 1 -> null;
            case 2 -> {
                BlockLightLayeredData blockLightLayeredData = new BlockLightLayeredData();
                for(int l = 0; l < 16; ++l) {
                    int layerType = reader.readByte();
                    switch (layerType) {
                        case 1:
                            int r = reader.readByte();
                            int g = reader.readByte();
                            int b = reader.readByte();
                            blockLightLayeredData.setLayer(l, new BlockLightDataSingleLayer(blockLightLayeredData, l, r, g, b));
                            break;
                        case 2:
                            BlockLightDataShortLayer layer = BlockLightDataShortLayer.readFrom(new DataInputChunkByteReader(reader));
                            blockLightLayeredData.setLayer(l, layer);
                            break;
                        default:
                            throw new RuntimeException("Unknown layerType: " + layerType);
                    }
                }
                yield  blockLightLayeredData;
            }
            default -> throw new RuntimeException("Unknown blockLightDataType: " + blockLightDataType);
        };
    }

    private static void readBlockEntities(DataInput reader, Chunk chunk) throws IOException {
        int blockEntityDataType = reader.readByte();
        switch (blockEntityDataType) {
            case 0:
                return;
            case 1:
                int numBytes = reader.readInt();
                byte[] bytes = new byte[numBytes];
                reader.readFully(bytes);
                CosmicReachBinaryDeserializer crbd = new CosmicReachBinaryDeserializer();
                crbd.prepareForRead(ByteBuffer.wrap(bytes));
                CosmicReachBinaryDeserializer[] objDeserializers = crbd.readRawObjArray("blockEntities");
                CosmicReachBinaryDeserializer[] var7 = objDeserializers;
                int var8 = objDeserializers.length;
                int var9 = 0;

                for(; var9 < var8; ++var9) {
                    CosmicReachBinaryDeserializer d = var7[var9];

                    try {
                        int globalX = d.readInt("x", Integer.MIN_VALUE);
                        int globalY = d.readInt("y", Integer.MIN_VALUE);
                        int globalZ = d.readInt("z", Integer.MIN_VALUE);
                        BlockState blockState = chunk.getBlockState(globalX - chunk.blockX, globalY - chunk.blockY, globalZ - chunk.blockZ);
                        BlockEntity blockEntity = BlockEntityCreator.get(blockState, globalX, globalY, globalZ);
                        if (blockEntity != null) {
                            try {
                                blockEntity.read(d);
                            } catch (Exception var17) {
                                Exception ex = var17;
                                ex.printStackTrace();
                            }

                            chunk.setBlockEntityDirect(blockState, blockEntity, blockEntity.getGlobalX() - chunk.blockX, blockEntity.getGlobalY() - chunk.blockY, blockEntity.getGlobalZ() - chunk.blockZ);
                        }
                    } catch (Exception var18) {
                        Exception ex = var18;
                        ex.printStackTrace();
                    }
                }

                return;
            default:
                throw new IllegalArgumentException("Unexpected value: " + blockEntityDataType);
        }
    }

    private static void readChunk(DataInput reader, Chunk chunk) throws IOException {
        chunk.blockData = readBlockData(reader);
        chunk.skyLightData = readSkylightData(reader);
        chunk.blockLightData = readBlockLightData(reader);
        readBlockEntities(reader, chunk);
        chunk.isSaved = true;
        chunk.setGenerated(true);
    }

    public static void deserialize(ByteArrayTag tag, Chunk chunk) throws IOException {
        readChunk(new DataInputStream(new ByteArrayInputStream(tag.getValue())), chunk);
    }

    public static void saveConstantByte(DataOutput output, ISaveFileConstant item, int nullConstant) throws IOException {
        if (item == null) {
            output.writeByte(nullConstant);
        } else {
            output.writeByte(item.getSaveFileConstant());
        }

    }

    private static void writeBlockData(DataOutput output, IBlockData<BlockState> data) throws IOException {
        saveConstantByte(output, data, 0);
        data.writeTo(new DataOutputChunkByteWriter(output));
    }

    private static void writeSkyLightData(DataOutput output, ISkylightData data) throws IOException {
        saveConstantByte(output, data, 1);
        if(data != null) data.writeTo(new DataOutputChunkByteWriter(output));
    }

    private static void writeBlockLightData(DataOutput output, IBlockLightData data) throws IOException {
        saveConstantByte(output, data, 1);
        if(data != null) data.writeTo(new DataOutputChunkByteWriter(output));
    }

    private static void writeBlockEntities(DataOutput output, Chunk chunk) throws IOException {
        if (chunk.hasBlockEntities()) {
            output.writeByte(1);
            CosmicReachBinarySerializer crbs = new CosmicReachBinarySerializer();
            Array<BlockEntity> blockEntities = new Array<>();
            chunk.forEachBlockEntity(blockEntities::add);
            crbs.writeObjArray("blockEntities", blockEntities);
            byte[] blockEntitiesBytes = crbs.toBytes();
            output.writeInt(blockEntitiesBytes.length);
            output.write(blockEntitiesBytes);
        } else {
            output.writeByte(0);
        }
    }

    public static ByteArrayTag serialize(Chunk chunk) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        writeBlockData(dos, chunk.blockData);
        writeSkyLightData(dos, chunk.skyLightData);
        writeBlockLightData(dos, chunk.blockLightData);
        writeBlockEntities(dos, chunk);
        return new ByteArrayTag(baos.toByteArray());
    }

}
