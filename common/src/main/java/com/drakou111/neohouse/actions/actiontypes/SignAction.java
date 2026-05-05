package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;

import java.lang.reflect.Field;

public class SignAction extends Action {
    private final String text;
    private static final Field MESSAGES_FIELD;

    static {
        Field found = null;
        for (Field f : AbstractSignEditScreen.class.getDeclaredFields()) {
            if (f.getType() == String[].class) {
                f.setAccessible(true);
                found = f;
                break;
            }
        }
        MESSAGES_FIELD = found;
    }

    public SignAction(String text) {
        this.text = text;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!(mc.screen instanceof AbstractSignEditScreen sign))
            throw new InterruptActionException("Not in sign edit screen.");

        if (MESSAGES_FIELD == null)
            throw new InterruptActionException("Could not locate sign messages field via reflection.");

        try {
            String[] msgs = (String[]) MESSAGES_FIELD.get(sign);
            if (msgs != null && msgs.length > 0) msgs[0] = text;
        } catch (IllegalAccessException e) {
            throw new InterruptActionException("Could not write sign text: " + e.getMessage());
        }

        sign.onClose();
        return true;
    }
}
