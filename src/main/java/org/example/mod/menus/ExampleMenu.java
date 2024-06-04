package org.example.mod.menus;

import dev.crmodders.flux.menus.BasicMenu;
import finalforeach.cosmicreach.gamestates.GameState;
import org.example.mod.ExampleMod;

public class ExampleMenu extends BasicMenu {

    public ExampleMenu(GameState previous) {
        super(previous);

        addBackButton();
    }

}
