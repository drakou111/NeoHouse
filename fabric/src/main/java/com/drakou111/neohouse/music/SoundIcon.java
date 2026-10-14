package com.drakou111.neohouse.music;

import com.drakou111.neohouse.compat.RegistryHelper;
import com.drakou111.neohouse.constants.Sound;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SoundIcon {

    private SoundIcon() {}

    private static final EnumMap<Sound, ItemStack> CACHE = new EnumMap<>(Sound.class);

    private static final Map<String, String> BLOCK_FIX = new HashMap<>();
    static {
        BLOCK_FIX.put("wood", "oak_planks");
        BLOCK_FIX.put("nether_wood", "warped_planks");
        BLOCK_FIX.put("wool", "white_wool");
        BLOCK_FIX.put("grass", "grass_block");
        BLOCK_FIX.put("stem", "warped_stem");
        BLOCK_FIX.put("wart_block", "nether_wart_block");
        BLOCK_FIX.put("metal", "iron_block");
        BLOCK_FIX.put("cobweb", "cobweb");
        BLOCK_FIX.put("nether_bricks", "nether_bricks");
    }

    private static final java.util.Properties EXPLICIT = loadExplicit();

    private static java.util.Properties loadExplicit() {
        java.util.Properties p = new java.util.Properties();
        try (var in = SoundIcon.class.getResourceAsStream("/assets/neohouse/sound_icons.properties")) {
            if (in != null) p.load(in);
        } catch (Exception ignored) {}
        return p;
    }

    public static ItemStack iconFor(Sound sound) {
        ItemStack cached = CACHE.get(sound);
        if (cached != null) return cached;
        ItemStack stack = resolve(sound);
        CACHE.put(sound, stack);
        return stack;
    }

    private static ItemStack resolve(Sound sound) {
        // 1) explicit mapping wins (if the id resolves in this version's registry)
        String explicit = EXPLICIT.getProperty(sound.name());
        if (explicit != null) {
            Optional<Item> item = RegistryHelper.lookupItem(explicit.trim());
            if (item.isPresent() && item.get() != Items.AIR) return new ItemStack(item.get());
        }
        // 2) fall back to the derived heuristic
        for (String candidate : candidates(sound)) {
            Optional<Item> item = RegistryHelper.lookupItem(candidate);
            if (item.isPresent() && item.get() != Items.AIR) {
                return new ItemStack(item.get());
            }
        }
        return new ItemStack(SoundEventResolver.isMelodic(sound) ? Items.NOTE_BLOCK : Items.MUSIC_DISC_13);
    }

    private static List<String> candidates(Sound sound) {
        List<String> out = new ArrayList<>();
        if (SoundEventResolver.isMelodic(sound)) {
            out.add("note_block");
            return out;
        }

        String path = SoundEventResolver.resolvePath(sound);
        if (path != null) {
            String[] p = path.split("\\.");
            String domain = p[0];
            String seg1 = p.length > 1 ? p[1] : "";
            switch (domain) {
                case "entity" -> out.add(seg1 + "_spawn_egg");
                case "block" -> {
                    if ("note_block".equals(seg1)) out.add("note_block");
                    else { out.add(BLOCK_FIX.getOrDefault(seg1, seg1)); out.add(seg1); }
                }
                case "item" -> out.add(seg1);
                case "music", "music_disc" -> {
                    if (p.length > 1) out.add("music_disc_" + p[p.length - 1]);
                    out.add("jukebox");
                }
                case "weather", "ambient" -> out.add("clock");
                case "ui" -> out.add("lever");
                default -> { }
            }
        }
        out.add("music_disc_13");
        out.add("note_block");
        return out;
    }
}
