package com.drakou111.neohouse.music;

import com.drakou111.neohouse.constants.Sound;

import java.util.ArrayList;
import java.util.List;

public class MusicTrack {

    public String name = "Track";
    public String sound = Sound.BLOCK_NOTE_BLOCK_HARP.name();
    public boolean melodic = true;
    public double volume = 1.0;
    public boolean muted = false;

    public List<MusicNote> notes = new ArrayList<>();

    public MusicTrack() {}

    public MusicTrack(String name, Sound sound) {
        this.name    = name;
        this.sound   = sound.name();
        this.melodic = SoundEventResolver.isMelodic(sound);
    }

    public Sound soundFor(MusicNote note) {
        if (note.sound != null) {
            Sound s = parseSound(note.sound);
            if (s != null) return s;
        }
        Sound s = parseSound(sound);
        return s != null ? s : Sound.BLOCK_NOTE_BLOCK_HARP;
    }

    public double pitchFor(MusicNote note) {
        if (melodic) return MusicNote.semitoneToPitch(note.semitone);
        return Math.max(0.0, Math.min(2.0, MusicNote.clampSemitone(note.semitone) / 24.0 * 2.0));
    }

    public float volumeFor(MusicNote note) {
        return (float) Math.max(0.0, Math.min(2.0, note.volume * volume));
    }

    private static Sound parseSound(String name) {
        if (name == null) return null;
        try {
            return Sound.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
