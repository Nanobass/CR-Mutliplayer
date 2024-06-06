package net.paxyinc.multiplayer.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.paxyinc.multiplayer.GameServer;
import net.querz.nbt.io.NamedTag;

public class CosmicReachProtocolServerHandler extends SimpleChannelInboundHandler<NamedTag> {

    private final GameServer server;

    public CosmicReachProtocolServerHandler(GameServer server) {
        this.server = server;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        server.onClientJoined(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        server.onClientLeft(ctx.channel());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, NamedTag tag) throws Exception {
        server.onMessageReceived(ctx, tag.getName(), tag.getTag());
    }

}
