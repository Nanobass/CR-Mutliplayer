package net.paxyinc.multiplayer.net;

import finalforeach.cosmicreach.savelib.IChunkByteReader;

import java.io.DataInput;
import java.io.IOException;

public class DataInputChunkByteReader implements IChunkByteReader {

    private final DataInput input;

    public DataInputChunkByteReader(DataInput input) {
        this.input = input;
    }

    @Override
    public int readInt() throws IOException {
        return input.readInt();
    }

    @Override
    public byte readByte() throws IOException {
        return input.readByte();
    }

    @Override
    public String readString() throws IOException {
        return input.readUTF();
    }

    @Override
    public void readFully(byte[] bytes) throws IOException {
        input.readFully(bytes);
    }

    @Override
    public short readShort() throws IOException {
        return input.readShort();
    }
}
