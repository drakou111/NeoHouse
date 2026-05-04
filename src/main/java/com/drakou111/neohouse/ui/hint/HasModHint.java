package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;

public class HasModHint implements HintProvider {
    private final String text;
    private final String modId;

    public HasModHint(String text, String modId) {
        this.text  = text;
        this.modId = modId;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
