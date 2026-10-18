package com.terraformersmc.modmenu.api;

/**
 * Compile-only stand-in for Mod Menu's API (the part NeoHouse uses). It is not packaged: when Mod
 * Menu is installed its own interface is loaded, and without it the entrypoint is never touched.
 */
public interface ModMenuApi {
    default ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> null;
    }
}
