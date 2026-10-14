package com.drakou111.neohouse.compat;

import com.mojang.blaze3d.Blaze3D;

import java.nio.file.Path;

/** Opens folders in the system file manager (Blaze3D, 26.3+ where SDL replaced the Util.OS helpers). */
public final class FileOpener {
    private FileOpener() {}

    public static void openFolder(Path folder) {
        Blaze3D.openPath(folder);
    }
}
