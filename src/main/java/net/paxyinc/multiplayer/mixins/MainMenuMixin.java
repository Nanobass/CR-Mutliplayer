package net.paxyinc.multiplayer.mixins;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import finalforeach.cosmicreach.RuntimeInfo;
import finalforeach.cosmicreach.gamestates.GameState;
import finalforeach.cosmicreach.gamestates.MainMenu;
import finalforeach.cosmicreach.lang.Lang;
import finalforeach.cosmicreach.settings.Controls;
import finalforeach.cosmicreach.ui.FontRenderer;
import finalforeach.cosmicreach.ui.HorizontalAnchor;
import finalforeach.cosmicreach.ui.UIElement;
import finalforeach.cosmicreach.ui.VerticalAnchor;
import net.paxyinc.multiplayer.GameClient;
import net.paxyinc.multiplayer.GameServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MainMenu.class)
public class MainMenuMixin extends GameState {

    @Shadow public static Texture textLogo;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void create() {
        super.create();

        UIElement clientButton = new UIElement(150.0F, 50.0F, 250.0F, 50.0F) {
            public void onClick() {
                super.onClick();
                GameState.switchToGameState(new GameClient());
            }
        };
        clientButton.setText("Client");
        clientButton.show();
        this.uiObjects.add(clientButton);

        UIElement serverButton = new UIElement(-150.0F, 50.0F, 250.0F, 50.0F) {
            public void onClick() {
                super.onClick();
                GameState.switchToGameState(new GameServer());
            }
        };
        serverButton.setText("Server");
        serverButton.show();
        this.uiObjects.add(serverButton);

    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void render(float partTick) {
        super.render(partTick);
        ScreenUtils.clear(0.0F, 0.0F, 0.0F, 1.0F, true);
        Gdx.gl.glEnable(2929);
        Gdx.gl.glDepthFunc(513);
        Gdx.gl.glEnable(2884);
        Gdx.gl.glCullFace(1029);
        Gdx.gl.glEnable(3042);
        Gdx.gl.glBlendFunc(770, 771);
        batch.setProjectionMatrix(this.uiCamera.combined);
        batch.begin();
        float scale = 4.0F;
        float logoW = 192.0F;
        float logoH = 64.0F;
        float logoX = -scale * logoW / 2.0F;
        float logoY = -this.uiViewport.getWorldHeight() / 2.0F;
        batch.draw(textLogo, logoX, logoY, 0.0F, 0.0F, logoW, logoH, scale, scale, 0.0F, 0, 0, textLogo.getWidth(), textLogo.getHeight(), false, true);
        Vector2 promoTextDim = new Vector2();
        float y = -8.0F;
        String promoText = Lang.get("YT_Channel");
        FontRenderer.getTextDimensions(this.uiViewport, promoText, promoTextDim);
        batch.setColor(Color.GRAY);
        FontRenderer.drawText(batch, this.uiViewport, promoText, -7.0F, y + 1.0F, HorizontalAnchor.RIGHT_ALIGNED, VerticalAnchor.BOTTOM_ALIGNED);
        batch.setColor(Color.WHITE);
        FontRenderer.drawText(batch, this.uiViewport, promoText, -8.0F, y, HorizontalAnchor.RIGHT_ALIGNED, VerticalAnchor.BOTTOM_ALIGNED);
        String macWarning;
        if (Controls.controllers.size > 0) {
            macWarning = Controls.controllers.size == 1 ? Lang.get("Controller") : Lang.get("Controllers");
            String controllerWarning = macWarning + Lang.get("Controller_info");
            FontRenderer.drawText(batch, this.uiViewport, controllerWarning, 8.0F, y, HorizontalAnchor.LEFT_ALIGNED, VerticalAnchor.BOTTOM_ALIGNED);
        }

        if (RuntimeInfo.isMac) {
            macWarning = Lang.get("MAC_warning");
            FontRenderer.drawText(batch, this.uiViewport, macWarning, 8.0F, y, HorizontalAnchor.CENTERED, VerticalAnchor.CENTERED);
        }

        y -= promoTextDim.y + 2.0F;
        promoText = "finalforeach.com";
        FontRenderer.getTextDimensions(this.uiViewport, promoText, promoTextDim);
        batch.setColor(Color.GRAY);
        FontRenderer.drawText(batch, this.uiViewport, promoText, -7.0F, y + 1.0F, HorizontalAnchor.RIGHT_ALIGNED, VerticalAnchor.BOTTOM_ALIGNED);
        batch.setColor(Color.WHITE);
        FontRenderer.drawText(batch, this.uiViewport, promoText, -8.0F, y, HorizontalAnchor.RIGHT_ALIGNED, VerticalAnchor.BOTTOM_ALIGNED);
        y -= promoTextDim.y;
        String var10000 = Lang.get("Game_version");
        promoText = var10000 + RuntimeInfo.version;
        FontRenderer.getTextDimensions(this.uiViewport, promoText, promoTextDim);
        batch.setColor(Color.GRAY);
        FontRenderer.drawText(batch, this.uiViewport, promoText, -7.0F, y + 1.0F, HorizontalAnchor.RIGHT_ALIGNED, VerticalAnchor.BOTTOM_ALIGNED);
        batch.setColor(Color.WHITE);
        FontRenderer.drawText(batch, this.uiViewport, promoText, -8.0F, y, HorizontalAnchor.RIGHT_ALIGNED, VerticalAnchor.BOTTOM_ALIGNED);
        float var12 = y - promoTextDim.y;
        batch.end();
        this.drawUIElements();
    }

}
