package com.drakou111.neohouse.compat;

import org.lwjgl.glfw.GLFW;

/** Keyboard keys and modifier masks the UI handles, as this version's GLFW key codes (1.21.x to 26.2). */
public final class Keys {
    public static final int A = GLFW.GLFW_KEY_A;
    public static final int C = GLFW.GLFW_KEY_C;
    public static final int F = GLFW.GLFW_KEY_F;
    public static final int H = GLFW.GLFW_KEY_H;
    public static final int K = GLFW.GLFW_KEY_K;
    public static final int N = GLFW.GLFW_KEY_N;
    public static final int R = GLFW.GLFW_KEY_R;
    public static final int S = GLFW.GLFW_KEY_S;
    public static final int V = GLFW.GLFW_KEY_V;
    public static final int X = GLFW.GLFW_KEY_X;
    public static final int Y = GLFW.GLFW_KEY_Y;
    public static final int Z = GLFW.GLFW_KEY_Z;
    public static final int BACKSPACE = GLFW.GLFW_KEY_BACKSPACE;
    public static final int DELETE = GLFW.GLFW_KEY_DELETE;
    public static final int DOWN = GLFW.GLFW_KEY_DOWN;
    public static final int END = GLFW.GLFW_KEY_END;
    public static final int ENTER = GLFW.GLFW_KEY_ENTER;
    public static final int ESCAPE = GLFW.GLFW_KEY_ESCAPE;
    public static final int HOME = GLFW.GLFW_KEY_HOME;
    public static final int KP_ENTER = GLFW.GLFW_KEY_KP_ENTER;
    public static final int LEFT = GLFW.GLFW_KEY_LEFT;
    public static final int PAGE_DOWN = GLFW.GLFW_KEY_PAGE_DOWN;
    public static final int PAGE_UP = GLFW.GLFW_KEY_PAGE_UP;
    public static final int RIGHT = GLFW.GLFW_KEY_RIGHT;
    public static final int SPACE = GLFW.GLFW_KEY_SPACE;
    public static final int TAB = GLFW.GLFW_KEY_TAB;
    public static final int UP = GLFW.GLFW_KEY_UP;

    /** Modifier masks for the {@code mods} argument of key handlers. */
    public static final int MOD_SHIFT = GLFW.GLFW_MOD_SHIFT;
    public static final int MOD_CONTROL = GLFW.GLFW_MOD_CONTROL;

    private Keys() {}

    /** Whether {@code key} is held right now (for hotkeys polled every tick). */
    public static boolean isDown(int key) {
        long window = GLFW.glfwGetCurrentContext();
        return window != 0L && key >= 0 && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }
}
