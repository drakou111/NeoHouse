package com.drakou111.neohouse.compat;

import net.minecraft.util.Util;

import java.nio.file.Path;

/** Opens folders in the system file manager (net.minecraft.util.Util, 1.21.11 to 26.2). */
public final class FileOpener {
    private FileOpener() {}

    public static void openFolder(Path folder) {
        Util.getPlatform().openPath(folder);
    }
}
