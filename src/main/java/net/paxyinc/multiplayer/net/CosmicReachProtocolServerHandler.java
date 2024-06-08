package net.paxyinc.multiplayer.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.paxyinc.multiplayer.GameClient;
import net.paxyinc.multiplayer.GameServer;
import net.paxyinc.multiplayer.net.bus.ClientEvent;
import net.paxyinc.multiplayer.net.bus.ServerEvent;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.Tag;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class CosmicReachProtocolServerHandler extends SimpleChannelInboundHandler<NamedTag> {

    private final GameServer server;
    public final EventBus bus = EventBus.builder().build();

    public CosmicReachProtocolServerHandler(GameServer server) {
        this.server = server;
    }

    public void send(ServerEvent event) {
        NamedTag packet = new NamedTag(event.getClass().getName(), event.getData());
        event.getClient().writeAndFlush(packet);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        server.clients.add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        server.clients.remove(ctx.channel());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, NamedTag tag) throws Exception {
        Class<? extends ClientEvent> eventClass = (Class<? extends ClientEvent>) Class.forName(tag.getName());
        Constructor<? extends ClientEvent> constructor = eventClass.getConstructor(GameServer.class, Channel.class, Tag.class);
        constructor.setAccessible(true);
        ClientEvent event = constructor.newInstance(server, ctx.channel(), tag.getTag());
        bus.post(event);
    }

}
