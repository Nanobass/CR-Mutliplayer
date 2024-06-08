package net.paxyinc.multiplayer.net.bus;

import io.netty.channel.Channel;
import net.paxyinc.multiplayer.GameServer;
import net.querz.nbt.tag.Tag;

public abstract class ClientEvent {

    private Channel client;

    public ClientEvent() {
    }

    public ClientEvent(GameServer server, Channel client, Tag<?> data) {
        this.client = client;
    }

    public abstract Tag<?> getData();

    public Channel getClient() {
        if(client == null) throw new RuntimeException("No Client Access... on the client you dumb ass");
        return client;
    }
}
