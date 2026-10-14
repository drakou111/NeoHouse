package com.drakou111.neohouse.config;

import com.drakou111.neohouse.utils.MessageUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NeoHouseConfig {
    public static final NeoHouseConfig INSTANCE = new NeoHouseConfig();

    public boolean verboseActions             = false;
    public int     clickDelay                 = 2;
    public int     actionTimeoutTicks         = 200;
    public int     tabWidth                   = 4;
    public int     errorBarMaxLines           = 5;
    public boolean showArgumentHints          = true;
    public boolean showArgumentRecommendations = true;
    public boolean autoIndent                 = true;
    public OverrideMode  overrideMode         = OverrideMode.IF_EMPTY;
    public MultiCallMode multiCallMode        = MultiCallMode.ERROR;
    public RecursionMode  recursionMode       = RecursionMode.WARN;
    public GlobalCallMode globalCallMode      = GlobalCallMode.ERROR;
    public SyntaxTheme   syntaxTheme          = SyntaxTheme.VSCODE_DARK;
    public UITheme       uiTheme              = UITheme.DARK_BLUE;
    public UIOpacity     uiOpacity            = UIOpacity.DEFAULT;
    public boolean showScriptMob              = false;
    public String  scriptMobId               = "minecraft:fox";

    private static boolean loaded = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Single source of truth for every persisted setting: the JSON key and how to read/write it.
     * Add a setting here once instead of editing a field declaration, load() and save() separately.
     */
    private final List<Setting<?>> settings = List.of(
            Setting.ofBool("verboseActions",              () -> verboseActions,              v -> verboseActions = v),
            Setting.ofInt ("clickDelay",                  () -> clickDelay,                  v -> clickDelay = v),
            Setting.ofInt ("actionTimeoutTicks",          () -> actionTimeoutTicks,          v -> actionTimeoutTicks = v),
            Setting.ofInt ("tabWidth",                    () -> tabWidth,                    v -> tabWidth = v),
            Setting.ofInt ("errorBarMaxLines",            () -> errorBarMaxLines,            v -> errorBarMaxLines = v),
            Setting.ofBool("showArgumentHints",           () -> showArgumentHints,           v -> showArgumentHints = v),
            Setting.ofBool("showArgumentRecommendations", () -> showArgumentRecommendations, v -> showArgumentRecommendations = v),
            Setting.ofBool("autoIndent",                  () -> autoIndent,                  v -> autoIndent = v),
            Setting.ofEnum("overrideMode",                () -> overrideMode,                v -> overrideMode = v,  OverrideMode::byId),
            Setting.ofEnum("multiCallMode",               () -> multiCallMode,               v -> multiCallMode = v, MultiCallMode::byId),
            Setting.ofEnum("recursionMode",               () -> recursionMode,               v -> recursionMode = v, RecursionMode::byId),
            Setting.ofEnum("globalCallMode",              () -> globalCallMode,              v -> globalCallMode = v, GlobalCallMode::byId),
            Setting.ofEnum("syntaxTheme",                 () -> syntaxTheme,                 v -> syntaxTheme = v,   SyntaxTheme::byId),
            Setting.ofEnum("uiTheme",                     () -> uiTheme,                     v -> uiTheme = v,       UITheme::byId),
            Setting.ofEnum("uiOpacity",                   () -> uiOpacity,                   v -> uiOpacity = v,     UIOpacity::byId),
            Setting.ofBool("showScriptMob",               () -> showScriptMob,               v -> showScriptMob = v),
            Setting.ofString("scriptMobId",              () -> scriptMobId,                 v -> scriptMobId = v)
    );

    private NeoHouseConfig() {}

    /** The loaded settings; without a running client (e.g. in tests) the defaults are used. */
    public static NeoHouseConfig get() {
        if (!loaded && Minecraft.getInstance() != null) { load(); loaded = true; }
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
            for (Setting<?> s : INSTANCE.settings) s.load(obj);
            // Legacy migration: pre-OverrideMode configs used a boolean "overrideExisting".
            if (!obj.has("overrideMode") && obj.has("overrideExisting")) {
                INSTANCE.overrideMode = obj.get("overrideExisting").getAsBoolean()
                        ? OverrideMode.ALWAYS : OverrideMode.NEVER;
            }
        } catch (Exception e) {
            MessageUtils.warningToChat("Config load failed (" + e.getClass().getSimpleName() + "): " + e.getMessage() + ". Using defaults.");
        }
    }

    public void save() {
        JsonObject obj = new JsonObject();
        for (Setting<?> s : settings) s.save(obj);
        try {
            Files.writeString(configPath(), GSON.toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    /** A single persisted setting: knows its JSON key and how to read/write its value. */
    private static final class Setting<T> {
        private final String key;
        private final Supplier<T> getter;
        private final Consumer<T> setter;
        private final Function<JsonObject, T> reader;
        private final BiSink<JsonObject, T> writer;

        private interface BiSink<A, B> { void accept(A a, B b); }

        private Setting(String key, Supplier<T> getter, Consumer<T> setter,
                        Function<JsonObject, T> reader, BiSink<JsonObject, T> writer) {
            this.key = key;
            this.getter = getter;
            this.setter = setter;
            this.reader = reader;
            this.writer = writer;
        }

        void load(JsonObject obj) {
            if (obj.has(key)) setter.accept(reader.apply(obj));
        }

        void save(JsonObject obj) {
            writer.accept(obj, getter.get());
        }

        static Setting<Boolean> ofBool(String key, Supplier<Boolean> g, Consumer<Boolean> s) {
            return new Setting<>(key, g, s, o -> o.get(key).getAsBoolean(), (o, v) -> o.addProperty(key, v));
        }

        static Setting<Integer> ofInt(String key, Supplier<Integer> g, Consumer<Integer> s) {
            return new Setting<>(key, g, s, o -> o.get(key).getAsInt(), (o, v) -> o.addProperty(key, v));
        }

        static Setting<String> ofString(String key, Supplier<String> g, Consumer<String> s) {
            return new Setting<>(key, g, s, o -> o.get(key).getAsString(), (o, v) -> o.addProperty(key, v));
        }

        static <E extends Enum<E>> Setting<E> ofEnum(String key, Supplier<E> g, Consumer<E> s, Function<String, E> byId) {
            return new Setting<>(key, g, s, o -> byId.apply(o.get(key).getAsString()), (o, v) -> o.addProperty(key, v.name()));
        }
    }
}
