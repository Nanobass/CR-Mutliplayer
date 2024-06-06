package net.paxyinc.multiplayer.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.io.NamedTag;

import java.util.List;

public class CosmicReachProtocolDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes() < 4) return;
        int nbtSize = in.getInt(in.readerIndex());
        if(in.readableBytes() < 4 + nbtSize) return;
        byte[] data = new byte[nbtSize];
        in.skipBytes(4);
        in.readBytes(data);
        NBTDeserializer deserializer = new NBTDeserializer();
        NamedTag tag = deserializer.fromBytes(data);
        out.add(tag);
    }

}
