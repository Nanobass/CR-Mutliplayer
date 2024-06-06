package net.paxyinc.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import dev.crmodders.flux.util.Reflection;
import finalforeach.cosmicreach.BlockSelection;
import finalforeach.cosmicreach.GameSingletons;
import finalforeach.cosmicreach.WorldLoader;
import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.entities.Player;
import finalforeach.cosmicreach.entities.PlayerController;
import finalforeach.cosmicreach.gamestates.GameState;
import finalforeach.cosmicreach.gamestates.InGame;
import finalforeach.cosmicreach.settings.GraphicsSettings;
import finalforeach.cosmicreach.ui.UI;
import finalforeach.cosmicreach.ui.debug.DebugInfo;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Sky;
import finalforeach.cosmicreach.world.World;
import finalforeach.cosmicreach.world.Zone;
import finalforeach.cosmicreach.worldgen.ZoneGenerator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.paxyinc.multiplayer.interfaces.WorldInterface;
import net.paxyinc.multiplayer.interfaces.ZoneInterface;
import net.paxyinc.multiplayer.net.*;
import net.paxyinc.multiplayer.util.CustomBlockSelection;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.ByteArrayTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static finalforeach.cosmicreach.GameSingletons.zoneRenderer;
import static finalforeach.cosmicreach.world.Sky.currentSky;

public class GameServer extends GameState {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameServer.class);

    public static final UUID LOCAL_PLAYER_UUID = UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454");

    public static String worldDisplayName = "";
    public static int port = 6502;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap bootstrap;
    private ChannelFuture future;

    public PlayerController playerController;
    public Player localPlayer;

    public World world;

    public CustomBlockSelection blockSelection;

    public PerspectiveCamera worldCamera;
    public Viewport worldViewport;

    @Override
    public void create() {
        super.create();

        worldCamera = new PerspectiveCamera(GraphicsSettings.fieldOfView.getValue(), Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        worldCamera.near = 0.1F;
        worldCamera.far = 2500.0F;
        worldViewport = new ExtendViewport(1.0F, 1.0F, worldCamera);

        currentSky = Sky.skyChoices.get(2);
        blockSelection = new CustomBlockSelection();

        String zoneId = "base:flat";
        ZoneGenerator flat = ZoneGenerator.getZoneGenerator(zoneId);
        world = World.createNew(worldDisplayName, "Multiplayer", zoneId, flat);
        WorldInterface wi = (WorldInterface) world;
        wi.createWorldLoader(z -> new ServerZoneLoader(this, z));

        localPlayer = wi.loadPlayer(LOCAL_PLAYER_UUID);
        playerController = new PlayerController(localPlayer);
        playerController.updateCamera(worldCamera, 0.0F);

        try {
            // TODO remove references to InGame
            Reflection.setField(InGame.class, "world", world);
            Reflection.setField(InGame.class, "player", localPlayer);
            Reflection.setField(InGame.class, "rawWorldCamera", worldCamera);
            Reflection.setField(IN_GAME, "viewport", worldViewport);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        bootstrap = new ServerBootstrap();
        try {
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new CosmicReachProtocolDecoder(),
                                    new CosmicReachProtocolEncoder(),
                                    new CosmicReachProtocolServerHandler(GameServer.this));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            future = bootstrap.bind(port).sync(); // (7)
            LOGGER.info("Server Started");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        worldCamera.fieldOfView = GraphicsSettings.fieldOfView.getValue();
        Collection<Zone> zones = world.getZones();
        synchronized (zones) {
            for(Zone zone : zones) {
                ZoneInterface zi = (ZoneInterface) zone;
                zi.update(deltaTime);
            }
        }
    }

    @Override
    public void render(float partTick) {
        super.render(partTick);

        Zone playerZone = localPlayer.getZone(world);
        lastFrameGameState = IN_GAME;
        currentGameState = IN_GAME;
        playerController.update(playerZone);
        blockSelection.raycast(playerZone, worldCamera);
        lastFrameGameState = this;
        currentGameState = this;
        playerController.updateCamera(worldCamera, partTick);

        ScreenUtils.clear(currentSky.currentSkyColor, true);

        worldViewport.apply();
        currentSky.update();
        currentSky.drawSky(worldCamera);
        zoneRenderer.render(playerZone, worldCamera);
        blockSelection.render(worldCamera);

        uiViewport.apply();
        DebugInfo.drawDebugText(uiViewport);
    }

    @Override
    public void dispose() {
        super.dispose();

        zoneRenderer.unload();

        WorldInterface wi = (WorldInterface) world;
        for(UUID playerUUID : wi.getPlayers().keySet()) {
            wi.unloadPlayer(playerUUID);
        }
        localPlayer = null;

        try {
            future.channel().closeFuture().sync();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        worldViewport.update(width, height);
    }

    private List<Channel> clients = new ArrayList<>();

    public void onClientJoined(Channel client) {
        clients.add(client);
    }

    public void onClientLeft(Channel client) {
        clients.remove(client);
    }

    public void onMessageReceived(ChannelHandlerContext ctx, String id, Tag<?> tag) {
        Channel client = ctx.channel();
        switch (id) {

            case "onChunkRequest" -> {
                CompoundTag compound = (CompoundTag) tag;
                String zoneId = compound.getString("zone");
                int[] chunkCoords = compound.getIntArray("xyz");
                Chunk chunk = world.getZone(zoneId).getChunkAtChunkCoords(chunkCoords[0], chunkCoords[1], chunkCoords[2]);
                try {
                    CompoundTag message = new CompoundTag();
                    message.putString("zone", zoneId);
                    message.putIntArray("xyz", chunkCoords);
                    message.put("data", ChunkSerializer.serialize(chunk));
                    client.writeAndFlush(new NamedTag("onChunkReceive", message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            case "onBlockStateModify" -> {
                CompoundTag compound = (CompoundTag) tag;
                String zoneId = compound.getString("zone");
                int[] chunkCoords = compound.getIntArray("cXYZ");
                int[] localCoords = compound.getIntArray("bXYZ");
                BlockState oldState = BlockState.getInstance(compound.getString("old"));
                BlockState newState = BlockState.getInstance(compound.getString("new"));
                Zone zone = world.getZone(zoneId);
                Chunk chunk = zone.getChunkAtChunkCoords(chunkCoords[0], chunkCoords[1], chunkCoords[2]);
                BlockPosition position = new BlockPosition(chunk, localCoords[0], localCoords[1], localCoords[2]);
                position.setBlockState(newState);
                position.flagTouchingChunksForRemeshing(zone, true);

                for(Channel broadcast : clients) {
                    if (broadcast != client) {
                        broadcast.writeAndFlush(new NamedTag("onBlockStateModify", compound));
                    }
                }

            }

        }
    }

}
