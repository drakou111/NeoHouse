package com.drakou111.neohouse.utils;

import net.minecraft.ChatFormatting;

import java.util.Map;
import java.util.Optional;

/**
 * Colors for setting options by how strict or risky they are: green lets things through, yellow
 * reports, red stops (or overwrites), aqua fixes things up. Keyed by the option's display name.
 */
public final class OptionColors {
    private static final Map<String, ChatFormatting> BY_NAME = Map.ofEntries(
            Map.entry("Off", ChatFormatting.GREEN),
            Map.entry("Allow", ChatFormatting.GREEN),
            Map.entry("Warn", ChatFormatting.YELLOW),
            Map.entry("Error", ChatFormatting.RED),
            Map.entry("Fix", ChatFormatting.AQUA),
            Map.entry("Fix (auto-copy)", ChatFormatting.AQUA),
            Map.entry("Never Override", ChatFormatting.GREEN),
            Map.entry("Override If Empty", ChatFormatting.YELLOW),
            Map.entry("Append", ChatFormatting.AQUA),
            Map.entry("Always Override", ChatFormatting.RED));

    private OptionColors() {}

    public static Optional<ChatFormatting> formattingFor(String optionName) {
        return Optional.ofNullable(BY_NAME.get(optionName));
    }

    /** The option's color as ARGB, or {@code fallback} for an option without one. */
    public static int argbFor(String optionName, int fallback) {
        ChatFormatting formatting = BY_NAME.get(optionName);
        return formatting == null || formatting.getColor() == null ? fallback : 0xFF000000 | formatting.getColor();
    }
}
