package com.drakou111.neohouse.integration;

import com.drakou111.neohouse.ui.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu's config button for NeoHouse opens the NeoHouse settings, returning to Mod Menu on close. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigScreen(parent, true);
    }
}
