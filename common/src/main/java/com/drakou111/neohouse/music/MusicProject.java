package com.drakou111.neohouse.music;

import com.drakou111.neohouse.constants.Sound;

import java.util.ArrayList;
import java.util.List;

public class MusicProject {

    public static final int FORMAT = 1;
    public static final int TICKS_PER_SECOND = 20;

    public int format = FORMAT;

    public String name = "Untitled";

    public int gridDivision = 4;

    public boolean allowAllSounds = false;

    public List<MusicTrack> tracks = new ArrayList<>();

    public MusicProject() {}

    public static MusicProject fresh(String name) {
        MusicProject p = new MusicProject();
        p.name = name;
        p.tracks.add(new MusicTrack("Harp", Sound.BLOCK_NOTE_BLOCK_HARP));
        return p;
    }

    public int lengthTicks() {
        int max = 0;
        for (MusicTrack t : tracks) {
            for (MusicNote n : t.notes) {
                if (n.tick + 1 > max) max = n.tick + 1;
            }
        }
        return Math.max(1, max);
    }

    public int totalNotes() {
        int c = 0;
        for (MusicTrack t : tracks) c += t.notes.size();
        return c;
    }
}
