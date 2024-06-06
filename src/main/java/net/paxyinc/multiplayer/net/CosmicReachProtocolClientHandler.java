package net.paxyinc.multiplayer.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.paxyinc.multiplayer.GameClient;
import net.querz.nbt.io.NamedTag;

public class CosmicReachProtocolClientHandler extends ChannelInboundHandlerAdapter {

    private final GameClient client;

    public CosmicReachProtocolClientHandler(GameClient client) {
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NamedTag tag = (NamedTag) msg;
        client.onMessageReceived(tag.getName(), tag.getTag());
    }

}
