package net.paxyinc.multiplayer.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NamedTag;

public class CosmicReachProtocolEncoder extends MessageToByteEncoder<NamedTag> {
    @Override
    protected void encode(ChannelHandlerContext ctx, NamedTag tag, ByteBuf out) throws Exception {
        NBTSerializer serializer = new NBTSerializer();
        byte[] data = serializer.toBytes(tag);
        out.writeInt(data.length);
        out.writeBytes(data);
    }
}
