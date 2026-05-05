package com.drakou111.neohouse.platform;

public final class Platforms {
    private static Platform instance;

    private Platforms() {}

    public static void init(Platform platform) {
        if (instance != null) throw new IllegalStateException("Platform already initialized");
        instance = platform;
    }

    public static Platform get() {
        if (instance == null) throw new IllegalStateException("Platform not initialized");
        return instance;
    }
}
