package org.example.mod.mixins;

import dev.crmodders.flux.gui.SwitchGameStateButtonElement;
import dev.crmodders.flux.gui.interfaces.GameStateInterface;
import dev.crmodders.flux.localization.TranslationKey;
import finalforeach.cosmicreach.gamestates.GameState;
import finalforeach.cosmicreach.gamestates.MainMenu;
import finalforeach.cosmicreach.ui.HorizontalAnchor;
import finalforeach.cosmicreach.ui.VerticalAnchor;
import org.example.mod.menus.ExampleMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MainMenu.class)
public class MainMenuMixin {

    @Inject(method = "create", at = @At("TAIL"))
    public void create(CallbackInfo ci) {
        GameStateInterface gi = (GameStateInterface) this;
        SwitchGameStateButtonElement button = new SwitchGameStateButtonElement(() -> new ExampleMenu(GameState.currentGameState));
        button.setBounds(-250, -100, 200, 75);
        button.setAnchors(HorizontalAnchor.CENTERED, VerticalAnchor.CENTERED);
        button.translation = new TranslationKey("examplemod:example.regular");
        button.updateText();
        gi.getComponents().add(button);
    }

}
