package com.drakou111.neohouse.music;

public class MusicNote {

    public int tick;
    public String sound;
    public double volume = 0.7;
    public int semitone = 12;
    public double pitch = 1.0;

    public MusicNote() {}

    public MusicNote(int tick) {
        this.tick = tick;
    }

    public MusicNote copy() {
        MusicNote n = new MusicNote(tick);
        n.sound    = sound;
        n.volume   = volume;
        n.semitone = semitone;
        n.pitch    = pitch;
        return n;
    }

    public static double semitoneToPitch(int semitone) {
        return Math.pow(2.0, (clampSemitone(semitone) - 12) / 12.0);
    }

    public static int clampSemitone(int semitone) {
        return Math.max(0, Math.min(24, semitone));
    }
}
