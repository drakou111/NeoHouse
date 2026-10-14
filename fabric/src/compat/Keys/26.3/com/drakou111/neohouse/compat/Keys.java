package com.drakou111.neohouse.compat;

import com.mojang.blaze3d.platform.InputConstants;

/** Keyboard keys and modifier masks the UI handles, as this version's SDL key codes (26.3+, which replaced GLFW input with SDL). */
public final class Keys {
    public static final int A = InputConstants.KEY_A;
    public static final int C = InputConstants.KEY_C;
    public static final int F = InputConstants.KEY_F;
    public static final int H = InputConstants.KEY_H;
    public static final int K = InputConstants.KEY_K;
    public static final int N = InputConstants.KEY_N;
    public static final int R = InputConstants.KEY_R;
    public static final int S = InputConstants.KEY_S;
    public static final int V = InputConstants.KEY_V;
    public static final int X = InputConstants.KEY_X;
    public static final int Y = InputConstants.KEY_Y;
    public static final int Z = InputConstants.KEY_Z;
    public static final int BACKSPACE = InputConstants.KEY_BACKSPACE;
    public static final int DELETE = InputConstants.KEY_DELETE;
    public static final int DOWN = InputConstants.KEY_DOWN;
    public static final int END = InputConstants.KEY_END;
    public static final int ENTER = InputConstants.KEY_RETURN;
    public static final int ESCAPE = InputConstants.KEY_ESCAPE;
    public static final int HOME = InputConstants.KEY_HOME;
    public static final int KP_ENTER = InputConstants.KEY_NUMPADENTER;
    public static final int LEFT = InputConstants.KEY_LEFT;
    public static final int PAGE_DOWN = InputConstants.KEY_PAGEDOWN;
    public static final int PAGE_UP = InputConstants.KEY_PAGEUP;
    public static final int RIGHT = InputConstants.KEY_RIGHT;
    public static final int SPACE = InputConstants.KEY_SPACE;
    public static final int TAB = InputConstants.KEY_TAB;
    public static final int UP = InputConstants.KEY_UP;

    /** Modifier masks for the {@code mods} argument of key handlers. */
    public static final int MOD_SHIFT = InputConstants.MOD_SHIFT;
    public static final int MOD_CONTROL = InputConstants.MOD_CONTROL;

    private Keys() {}

    /** Whether {@code key} is held right now (for hotkeys polled every tick). */
    public static boolean isDown(int key) {
        return key >= 0 && InputConstants.isKeyDown(key);
    }
}
