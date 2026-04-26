package com.drakou111.neohouse;

import com.google.gson.*;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class NeoHouseConfig {
    public static final NeoHouseConfig INSTANCE = new NeoHouseConfig();

    public int clickDelay  = 2;
    public int retryAmount = 3;
    public int retryDelay  = 5;

    private static boolean loaded = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private NeoHouseConfig() {}

    public static NeoHouseConfig get() {
        if (!loaded) { load(); loaded = true; }
        return INSTANCE;
    }

    private static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("neohouse-config.json");
    }

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) { INSTANCE.save(); return; }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("clickDelay"))  INSTANCE.clickDelay  = obj.get("clickDelay").getAsInt();
            if (obj.has("retryAmount")) INSTANCE.retryAmount = obj.get("retryAmount").getAsInt();
            if (obj.has("retryDelay"))  INSTANCE.retryDelay  = obj.get("retryDelay").getAsInt();
        } catch (Exception ignored) {}
    }

    public void save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("clickDelay",  clickDelay);
        obj.addProperty("retryAmount", retryAmount);
        obj.addProperty("retryDelay",  retryDelay);
        try {
            Files.writeString(configPath(), GSON.toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }
}
