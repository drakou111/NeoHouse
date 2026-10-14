package com.drakou111.neohouse.factories.params;

import com.drakou111.neohouse.constants.Sound;
import com.drakou111.neohouse.constants.Teleport;

public class PlaySoundParams {
    public Sound sound = null;
    /** A Minecraft sound id used when {@link #sound} is null. */
    public String customSound = null;
    public double volume = 0.7;
    public double pitch = 1.0;
    public Teleport teleport = null;
    public String customLocation = null;
    public String note = null;

    public PlaySoundParams sound(Sound v)           { this.sound = v; return this; }
    public PlaySoundParams customSound(String v)    { this.customSound = v; return this; }
    public PlaySoundParams volume(double v)         { this.volume = v; return this; }
    public PlaySoundParams pitch(double v)          { this.pitch = v; return this; }
    public PlaySoundParams teleport(Teleport v)     { this.teleport = v; return this; }
    public PlaySoundParams customLocation(String v) { this.customLocation = v; return this; }
    public PlaySoundParams note(String v)           { this.note = v; return this; }
}
