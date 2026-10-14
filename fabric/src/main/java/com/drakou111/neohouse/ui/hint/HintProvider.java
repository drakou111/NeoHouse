package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;

public interface HintProvider {
    boolean shouldShow(Minecraft mc);
    String getText(Minecraft mc);
}
