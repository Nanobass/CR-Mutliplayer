package net.paxyinc.multiplayer.net;

import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.io.ChunkSaver;
import finalforeach.cosmicreach.savelib.IChunkByteWriter;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

public class DataOutputChunkByteWriter implements IChunkByteWriter {

    private final DataOutput output;

    public DataOutputChunkByteWriter(DataOutput output) {
        this.output = output;
    }

    @Override
    public <T> void writeBlockValue(T t) {
        if (t instanceof BlockState blockState) {
            String saveKey = blockState.getSaveKey();
            try {
                output.writeUTF(saveKey);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("writeBlockValue() not implemented for " + t.getClass().getSimpleName());
        }
    }

    @Override
    public void writeInt(int i) {
        try {
            output.writeInt(i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeByte(int i) {
        try {
            output.writeByte(i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeBytes(byte[] bytes) {
        try {
            output.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeShorts(short[] shorts) {
        try {
            for(short i : shorts) {
                output.writeShort(i);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
