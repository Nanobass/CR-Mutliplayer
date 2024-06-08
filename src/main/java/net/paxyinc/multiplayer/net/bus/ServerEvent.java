package net.paxyinc.multiplayer.net.bus;

import io.netty.channel.Channel;
import net.paxyinc.multiplayer.GameClient;
import net.paxyinc.multiplayer.GameServer;
import net.paxyinc.multiplayer.net.events.ChunkResponse;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.Tag;

public abstract class ServerEvent {

    private Channel client;

    public ServerEvent(Channel client) {
        this.client = client;
    }

    public ServerEvent(GameClient client, Tag<?> data) {

    }

    public abstract Tag<?> getData();

    public Channel getClient() {
        if(client == null) throw new RuntimeException("No Client Access... on the client you dumb ass");
        return client;
    }

}
