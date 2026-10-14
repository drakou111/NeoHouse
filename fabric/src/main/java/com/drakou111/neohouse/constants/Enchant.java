package com.drakou111.neohouse.constants;

public enum Enchant {
    PROTECTION("Protection"),
    FIRE_PROTECTION("Fire Protection"),
    FEATHER_FALLING("Feather Falling"),
    BLAST_PROTECTION("Blast Protection"),
    PROJECTILE_PROTECTION("Projectile Protection"),
    RESPIRATION("Respiration"),
    AQUA_AFFINITY("Aqua Affinity"),
    THORNS("Thorns"),
    DEPTH_STRIDER("Depth Strider"),
    SHARPNESS("Sharpness"),
    SMITE("Smite"),
    BANE_OF_ARTHROPODS("Bane Of Arthropods"),
    KNOCKBACK("Knockback"),
    FIRE_ASPECT("Fire Aspect"),
    LOOTING("Looting"),
    EFFICIENCY("Efficiency"),
    SILK_TOUCH("Silk Touch"),
    UNBREAKING("Unbreaking"),
    FORTUNE("Fortune"),
    POWER("Power"),
    PUNCH("Punch"),
    FLAME("Flame"),
    INFINITY("Infinity"),
    LUCK_OF_THE_SEA("Luck Of The Sea"),
    LURE("Lure");

    public final String displayName;

    Enchant(String displayName) {
        this.displayName = displayName;
    }
}
