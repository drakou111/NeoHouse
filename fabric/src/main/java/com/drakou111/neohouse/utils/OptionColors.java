package com.drakou111.neohouse.utils;

import net.minecraft.ChatFormatting;

import java.util.Map;
import java.util.Optional;

/**
 * Colors for setting options by how strict or risky they are: green lets things through, yellow
 * reports, red stops (or overwrites), aqua fixes things up. Keyed by the option's display name.
 */
public final class OptionColors {
    private record OptionColor(ChatFormatting formatting, int argb) {}

    // ARGB kept here because ChatFormatting.getColor() isn't available on every Minecraft version.
    private static final OptionColor GREEN = new OptionColor(ChatFormatting.GREEN, 0xFF55FF55);
    private static final OptionColor YELLOW = new OptionColor(ChatFormatting.YELLOW, 0xFFFFFF55);
    private static final OptionColor RED = new OptionColor(ChatFormatting.RED, 0xFFFF5555);
    private static final OptionColor AQUA = new OptionColor(ChatFormatting.AQUA, 0xFF55FFFF);

    private static final Map<String, OptionColor> BY_NAME = Map.ofEntries(
            Map.entry("Off", GREEN),
            Map.entry("Allow", GREEN),
            Map.entry("Warn", YELLOW),
            Map.entry("Error", RED),
            Map.entry("Fix", AQUA),
            Map.entry("Fix (auto-copy)", AQUA),
            Map.entry("Never Override", GREEN),
            Map.entry("Override If Empty", YELLOW),
            Map.entry("Append", AQUA),
            Map.entry("Always Override", RED));

    private OptionColors() {}

    public static Optional<ChatFormatting> formattingFor(String optionName) {
        OptionColor color = BY_NAME.get(optionName);
        return color == null ? Optional.empty() : Optional.of(color.formatting());
    }

    /** The option's color as ARGB, or {@code fallback} for an option without one. */
    public static int argbFor(String optionName, int fallback) {
        OptionColor color = BY_NAME.get(optionName);
        return color == null ? fallback : color.argb();
    }
}
