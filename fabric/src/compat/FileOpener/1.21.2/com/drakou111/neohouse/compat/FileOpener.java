package com.drakou111.neohouse.compat;

import net.minecraft.Util;

import java.nio.file.Path;

/** Opens folders in the system file manager (net.minecraft.Util, before 1.21.11). */
public final class FileOpener {
    private FileOpener() {}

    public static void openFolder(Path folder) {
        Util.getPlatform().openPath(folder);
    }
}
