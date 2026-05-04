package com.drakou111.neohouse.constants;

public enum Potion {
    SPEED("Speed"),
    SLOWNESS("Slowness"),
    HASTE("Haste"),
    MINING_FATIGUE("Mining Fatigue"),
    STRENGTH("Strength"),
    INSTANT_HEALTH("Instant Health"),
    INSTANT_DAMAGE("Instant Damage"),
    JUMP_BOOST("Jump Boost"),
    NAUSEA("Nausea"),
    REGENERATION("Regeneration"),
    RESISTANCE("Resistance"),
    FIRE_RESISTANCE("Fire Resistance"),
    WATER_BREATHING("Water Breathing"),
    INVISIBILITY("Invisibility"),
    BLINDNESS("Blindness"),
    NIGHT_VISION("Night Vision"),
    HUNGER("Hunger"),
    WEAKNESS("Weakness"),
    POISON("Poison"),
    WITHER("Wither"),
    HEALTH_BOOST("Health Boost"),
    ABSORPTION("Absorption"),;

    public final String displayName;

    Potion(String displayName) {
        this.displayName = displayName;
    }
}
