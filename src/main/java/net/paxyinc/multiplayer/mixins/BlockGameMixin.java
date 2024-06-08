package net.paxyinc.multiplayer.mixins;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.utils.BufferUtils;
import finalforeach.cosmicreach.BlockGame;
import finalforeach.cosmicreach.GameSingletons;
import finalforeach.cosmicreach.WorldLoader;
import finalforeach.cosmicreach.audio.SoundManager;
import finalforeach.cosmicreach.blockentities.BlockEntity;
import finalforeach.cosmicreach.blockevents.BlockEvents;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.gamestates.GameState;
import finalforeach.cosmicreach.io.ChunkSaver;
import finalforeach.cosmicreach.lang.Lang;
import finalforeach.cosmicreach.rendering.BatchedZoneRenderer;
import finalforeach.cosmicreach.rendering.ExperimentalNaiveZoneRenderer;
import finalforeach.cosmicreach.rendering.WorldRenderingMeshGenThread;
import finalforeach.cosmicreach.rendering.items.ItemRenderer;
import finalforeach.cosmicreach.rendering.shaders.ChunkShader;
import finalforeach.cosmicreach.rendering.shaders.GameShader;
import finalforeach.cosmicreach.settings.Controls;
import finalforeach.cosmicreach.settings.GraphicsSettings;
import finalforeach.cosmicreach.worldgen.ZoneGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.IntBuffer;

@Mixin(value = BlockGame.class, priority = 500)
public class BlockGameMixin {

    @Shadow public static Lwjgl3Application lwjglApp;

    @Shadow private int defaultWindowWidth;

    @Shadow private int defaultWindowHeight;

    @Shadow public static boolean gameStarted;

    @Shadow private float fixedUpdateAccumulator;

    @Shadow private double lastUpdateTime;

    @Shadow private double secondsSinceLastUpdate;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void create() {
        lwjglApp = (Lwjgl3Application) Gdx.app;
        System.out.println("GL_VENDOR: " + Gdx.gl.glGetString(7936));
        System.out.println("GL_RENDERER: " + Gdx.gl.glGetString(7937));
        System.out.println("GL_VERSION: " + Gdx.gl.glGetString(7938));
        System.out.println(Gdx.graphics.getGLVersion().getDebugVersionString());
        Lang.loadLanguages(false);
        IntBuffer i = BufferUtils.newIntBuffer(1);
        Gdx.gl.glGetIntegerv(33309, i);
        System.out.println("GL_NUM_EXTENSIONS: " + i.get());
        Gdx.graphics.setForegroundFPS(GraphicsSettings.maxFPS.getValue());
        GameShader.initShaders();
        defaultWindowWidth = Gdx.graphics.getWidth();
        defaultWindowHeight = Gdx.graphics.getHeight();
        GameState.currentGameState.create();

        BlockEvents.initBlockEvents();

        GameSingletons.meshGenThread = new WorldRenderingMeshGenThread();
        GameSingletons.zoneRenderer = new BatchedZoneRenderer();
        GameSingletons.soundManager = SoundManager.INSTANCE;
        GameSingletons.blockModelInstantiator = null;
        ZoneGenerator.BLOCKSTATE_INSTANTIATOR = BlockState::getInstance;

        gameStarted = true;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void runTicks() {
        fixedUpdateAccumulator += Gdx.graphics.getDeltaTime();
        float fixedUpdateTimestep = 0.05F;

        double curUpdateTime;
        for(curUpdateTime = (double)System.currentTimeMillis(); fixedUpdateAccumulator >= fixedUpdateTimestep; lastUpdateTime = curUpdateTime) {
            GameState.currentGameState.update(fixedUpdateTimestep);
            fixedUpdateAccumulator -= fixedUpdateTimestep;
        }

        secondsSinceLastUpdate = lastUpdateTime == -1.0 ? 0.0 : (curUpdateTime - lastUpdateTime) / 1000.0;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void render() {
        Controls.update();
        GameState thisFrameGameState = GameState.currentGameState;
        this.runTicks();
        float partTick = (float)(this.secondsSinceLastUpdate / 0.05000000074505806);
        GameState.currentGameState.render(partTick);
        GameState.currentGameState.firstFrame = false;
        if (Controls.keyDebugReloadShadersJustPressed()) {
            ChunkShader.reloadAllShaders();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.F9)) GameSingletons.isAllFlaggedForRemeshing = true;

        if (Controls.keyFullscreenJustPressed()) {
            Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode(this.defaultWindowWidth, this.defaultWindowHeight);
                System.setProperty("org.lwjgl.opengl.Window.undecorated", "false");
                Gdx.graphics.setUndecorated(false);
            } else {
                Gdx.graphics.setFullscreenMode(displayMode);
            }
        }

        GameState.lastFrameGameState = thisFrameGameState;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void dispose() {
        try {
            GameState.currentGameState.dispose();
            SoundManager.INSTANCE.dispose();
        } finally {
            System.out.println("Dispose() called! Closing the game.");
            System.exit(0);
        }
    }

}
