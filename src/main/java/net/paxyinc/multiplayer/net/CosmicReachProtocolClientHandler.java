package net.paxyinc.multiplayer.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import net.paxyinc.multiplayer.GameClient;
import net.paxyinc.multiplayer.net.bus.*;
import net.paxyinc.multiplayer.net.events.ChunkRequestEvent;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.Tag;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CosmicReachProtocolClientHandler extends SimpleChannelInboundHandler<NamedTag> {

    private final GameClient client;
    public final EventBus bus = EventBus.builder().build();

    public CosmicReachProtocolClientHandler(GameClient client) {
        this.client = client;
    }

    public void send(ClientEvent event) {
        NamedTag packet = new NamedTag(event.getClass().getName(), event.getData());
        client.server.writeAndFlush(packet);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, NamedTag tag) throws Exception {
        Class<? extends ServerEvent> eventClass = (Class<? extends ServerEvent>) Class.forName(tag.getName());
        Constructor<? extends ServerEvent> constructor = eventClass.getConstructor(GameClient.class, Tag.class);
        constructor.setAccessible(true);
        ServerEvent event = constructor.newInstance(client, tag.getTag());
        bus.post(event);
    }

}
