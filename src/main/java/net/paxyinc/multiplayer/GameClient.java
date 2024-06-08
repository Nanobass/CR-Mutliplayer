package net.paxyinc.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import dev.crmodders.flux.util.Reflection;
import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.entities.Player;
import finalforeach.cosmicreach.entities.PlayerController;
import finalforeach.cosmicreach.gamestates.GameState;
import finalforeach.cosmicreach.gamestates.InGame;
import finalforeach.cosmicreach.settings.GraphicsSettings;
import finalforeach.cosmicreach.ui.debug.DebugInfo;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Sky;
import finalforeach.cosmicreach.world.World;
import finalforeach.cosmicreach.world.Zone;
import finalforeach.cosmicreach.worldgen.ZoneGenerator;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.paxyinc.multiplayer.interfaces.ChunkInterface;
import net.paxyinc.multiplayer.interfaces.WorldInterface;
import net.paxyinc.multiplayer.interfaces.ZoneInterface;
import net.paxyinc.multiplayer.net.*;
import net.paxyinc.multiplayer.net.events.ChunkModifyEvent;
import net.paxyinc.multiplayer.net.events.ChunkResponse;
import net.paxyinc.multiplayer.util.ChunkDelta;
import net.paxyinc.multiplayer.util.ChunkDeltaInfo;
import net.paxyinc.multiplayer.util.CustomBlockSelection;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.UUID;

import static finalforeach.cosmicreach.GameSingletons.zoneRenderer;
import static finalforeach.cosmicreach.world.Sky.currentSky;

public class GameClient extends GameState {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameClient.class);
    public static String host = "127.0.0.1";
    public static int port = 6502;


    public static final UUID LOCAL_PLAYER_UUID = UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454");

    private EventLoopGroup workerGroup;
    private Bootstrap bootstrap;
    private ChannelFuture future;
    public Channel server;
    public CosmicReachProtocolClientHandler handler;

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

        workerGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        try {
            handler = new CosmicReachProtocolClientHandler(GameClient.this);
            bootstrap.group(workerGroup); // (2)
            bootstrap.channel(NioSocketChannel.class); // (3)
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(
                            new CosmicReachProtocolDecoder(),
                            new CosmicReachProtocolEncoder(),
                            handler);
                }
            });
            handler.bus.register(this);
            future = bootstrap.connect(host, port).sync(); // (5)
            server = future.channel();
            LOGGER.info("Client Started");
        } catch (Exception e) {
            e.printStackTrace();
        }

        String zoneId = "base:flat";
        ZoneGenerator flat = ZoneGenerator.getZoneGenerator(zoneId);
        world = World.createNew("ClientConnection", "Multiplayer", zoneId, flat);
        WorldInterface wi = (WorldInterface) world;
        wi.createWorldLoader(z -> new ClientZoneLoader(this, z));

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
        localPlayer = null;

        try {
            future.channel().closeFuture().sync();
            workerGroup.shutdownGracefully();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        worldViewport.update(width, height);
    }

    @Subscribe
    public void onEvent(ChunkResponse event) {
        WorldInterface wi = (WorldInterface) world;
        Zone zone = event.getZone(world);
        ClientZoneLoader loader = (ClientZoneLoader) wi.getWorldLoader().getZoneLoader(zone);
        if(event.isPresent()) {
            loader.onChunkReceived(event.getChunkCoords(), event.getChunk());
        } else {
            loader.onChunkFailed(event.getChunkCoords());
        }
    }

    @Subscribe
    public void onEvent(ChunkModifyEvent event) {
        Chunk chunk = event.getChunk(world);
        ChunkInterface ci = (ChunkInterface) chunk;
        for(int i = 0; i < event.getNumberOfChanges(); i++) {
            ChunkDelta delta = event.getDelta(chunk, i);
            BlockPosition position = delta.position();
            ci.setBlockStateDirect(delta.newState(), position.localX, position.localY, position.localZ);
        }
        chunk.flagForRemeshing(true);
    }

}
