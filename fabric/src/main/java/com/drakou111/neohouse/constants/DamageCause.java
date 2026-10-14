package com.drakou111.neohouse.constants;

public enum DamageCause {
    ENTITY_ATTACK("Entity Attack"),
    PROJECTILE("Projectile"),
    SUFFOCATION("Suffocation"),
    FALL("Fall"),
    LAVA("Lava"),
    FIRE("Fire"),
    FIRE_TICK("Fire Tick"),
    DROWNING("Drowning"),
    STARVATION("Starvation"),
    POISON("Poison"),
    THORNS("Thorns");

    public final String displayName;

    DamageCause(String displayName) {
        this.displayName = displayName;
    }
}
