package com.drakou111.neohouse.music;

import com.drakou111.neohouse.constants.Sound;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports a Standard MIDI File into a {@link MusicProject}. Each distinct
 * instrument (channel + General-MIDI program, plus the percussion channel) maps
 * to the best-fitting note-block instrument; every {@code note-on} becomes a
 * one-shot {@link MusicNote} at the correct tick, pitch and velocity.
 *
 * <p>Uses only the JDK's {@code javax.sound.midi}, which is available in the
 * Minecraft runtime.</p>
 */
public final class MidiImporter {

    private MidiImporter() {}

    private static final int DEFAULT_US_PER_QUARTER = 500_000; // 120 BPM

    private static final int NB_LOW = 54;
    private static final int NB_HIGH = 78;

    private static final Sound[] FAMILY_SOUND = {
            Sound.BLOCK_NOTE_BLOCK_HARP,       // 0  Piano
            Sound.BLOCK_NOTE_BLOCK_XYLOPHONE,  // 1  Chromatic Percussion
            Sound.BLOCK_NOTE_BLOCK_BELL,       // 2  Organ
            Sound.BLOCK_NOTE_BLOCK_GUITAR,     // 3  Guitar
            Sound.BLOCK_NOTE_BLOCK_BASS,       // 4  Bass
            Sound.BLOCK_NOTE_BLOCK_HARP,       // 5  Strings
            Sound.BLOCK_NOTE_BLOCK_HARP,       // 6  Ensemble
            Sound.BLOCK_NOTE_BLOCK_PLING,      // 7  Brass
            Sound.BLOCK_NOTE_BLOCK_FLUTE,      // 8  Reed
            Sound.BLOCK_NOTE_BLOCK_FLUTE,      // 9  Pipe
            Sound.BLOCK_NOTE_BLOCK_PLING,      // 10 Synth Lead
            Sound.BLOCK_NOTE_BLOCK_BELL,       // 11 Synth Pad
            Sound.BLOCK_NOTE_BLOCK_CHIME,      // 12 Synth Effects
            Sound.BLOCK_NOTE_BLOCK_GUITAR,     // 13 Ethnic
            Sound.BLOCK_NOTE_BLOCK_XYLOPHONE,  // 14 Percussive
            Sound.BLOCK_NOTE_BLOCK_HAT         // 15 Sound Effects
    };

    private static final String[] FAMILY_NAME = {
            "Piano", "Chromatic Perc", "Organ", "Guitar", "Bass", "Strings", "Ensemble", "Brass",
            "Reed", "Pipe", "Synth Lead", "Synth Pad", "Synth FX", "Ethnic", "Percussive", "Sound FX"
    };

    private record Ev(long tick, MidiMessage msg) {}

    /**
     * Imports the MIDI file, inferring tempo from its first tempo meta-event. Timing is snapped
     * to the nearest tempo whose beat is a whole number of game ticks, so every note lands
     * cleanly on the 20 tps grid (fast even runs stay evenly spaced; only the tempo may shift).
     */
    public static MusicProject importMidi(Path midiFile, String projectName)
            throws InvalidMidiDataException, IOException {
        Sequence seq = MidiSystem.getSequence(midiFile.toFile());

        boolean ppq = seq.getDivisionType() == Sequence.PPQ;
        double resolution = seq.getResolution();
        double smpteTicksPerSec = ppq ? 0 : resolution * seq.getDivisionType();

        // merge every event across tracks, ordered by tick
        List<Ev> events = new ArrayList<>();
        for (Track track : seq.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent e = track.get(i);
                events.add(new Ev(e.getTick(), e.getMessage()));
            }
        }
        events.sort((a, b) -> Long.compare(a.tick, b.tick));

        MusicProject project = new MusicProject();
        project.name = projectName;
        project.tracks.clear();

        Map<String, MusicTrack> byKey = new LinkedHashMap<>();
        int[] programByChannel = new int[16];

        int firstTempo = DEFAULT_US_PER_QUARTER;
        boolean sawTempo = false;

        // Notes are captured at their RAW midi tick; timing is resolved below once the whole
        // song is known, so we can lock the grid to musical beats rather than wall-clock time.
        for (Ev ev : events) {
            MidiMessage m = ev.msg;
            if (m instanceof MetaMessage meta && meta.getType() == 0x51) {
                byte[] d = meta.getData();
                if (d.length == 3 && !sawTempo) {
                    firstTempo = ((d[0] & 0xFF) << 16) | ((d[1] & 0xFF) << 8) | (d[2] & 0xFF);
                    sawTempo = true;
                }
            } else if (m instanceof ShortMessage sm) {
                int cmd = sm.getCommand();
                int channel = sm.getChannel();
                if (cmd == ShortMessage.PROGRAM_CHANGE) {
                    programByChannel[channel] = sm.getData1();
                } else if (cmd == ShortMessage.NOTE_ON && sm.getData2() > 0) {
                    addNote(byKey, channel, programByChannel[channel], sm.getData1(), sm.getData2(), ev.tick);
                }
            }
        }

        // ---- map every note onto the game-tick grid at the song's real tempo ----
        // Place notes by MUSICAL position (beats = midiTick / PPQ) and make one beat a whole
        // number of game ticks, so the beat grid stays aligned and the tempo is preserved
        // (rounded to the nearest whole-tick beat). We deliberately do NOT snap onsets to a
        // detected subdivision: many MIDIs (e.g. the original DOOM tracks, which are a 140 Hz
        // event stream, not musically quantized) have no clean subdivision, and forcing one
        // both drags the tempo off and makes steady runs uneven. Each note simply rounds to the
        // nearest game tick — the most faithful the 20 tps engine allows.
        int ticksPerBeat = 10; // neutral default (used for gridDivision on SMPTE files)
        if (ppq) {
            double idealTicksPerBeat = MusicProject.TICKS_PER_SECOND * (firstTempo / 1_000_000.0);
            ticksPerBeat = Math.max(1, (int) Math.round(idealTicksPerBeat));
        }

        for (MusicTrack t : byKey.values()) {
            for (MusicNote n : t.notes) {
                if (ppq) {
                    n.tick = Math.max(0, (int) Math.round(n.tick / resolution * ticksPerBeat));
                } else {
                    // SMPTE (absolute-time) files carry no beat grid — fall back to seconds.
                    n.tick = Math.max(0, (int) Math.round(n.tick / smpteTicksPerSec * MusicProject.TICKS_PER_SECOND));
                }
            }
        }

        for (Map.Entry<String, MusicTrack> e : byKey.entrySet()) {
            finalizeTrack(e.getValue(), e.getKey().equals("drums"));
            project.tracks.add(e.getValue());
        }
        if (project.tracks.isEmpty()) {
            project.tracks.add(new MusicTrack("Harp", Sound.BLOCK_NOTE_BLOCK_HARP));
        }
        project.gridDivision = Math.max(1, ticksPerBeat);
        project.bpm = 60.0 * MusicProject.TICKS_PER_SECOND / project.gridDivision;
        return project;
    }

    private static void addNote(Map<String, MusicTrack> byKey, int channel, int program,
                                int midiNote, int velocity, long rawTick) {
        boolean drums = channel == 9;
        String key = drums ? "drums" : channel + "|" + program;

        MusicTrack track = byKey.get(key);
        if (track == null) {
            Sound sound = drums ? Sound.BLOCK_NOTE_BLOCK_BASEDRUM : FAMILY_SOUND[program / 8];
            String name = drums ? "Drums" : FAMILY_NAME[program / 8] + " (ch" + (channel + 1) + ")";
            track = new MusicTrack(name, sound);
            byKey.put(key, track);
        }

        MusicNote note = new MusicNote((int) Math.max(0, rawTick));
        note.volume = velocityToVolume(velocity);
        if (drums) {
            note.sound = drumSound(midiNote).name();
            note.semitone = 12;
        } else {
            note.semitone = midiNote;
        }
        track.notes.add(note);
    }

    private static void finalizeTrack(MusicTrack track, boolean drums) {
        java.util.LinkedHashMap<Long, MusicNote> keep = new java.util.LinkedHashMap<>();
        if (drums) {
            for (MusicNote n : track.notes) {
                long k = ((long) n.tick << 8) | (n.sound == null ? 0 : n.sound.hashCode() & 0xFF);
                MusicNote ex = keep.get(k);
                if (ex == null || n.volume > ex.volume) keep.put(k, n);
            }
        } else {
            int shift = bestOctaveShift(track.notes);
            for (MusicNote n : track.notes) {
                n.semitone = foldToRange(n.semitone + shift) - NB_LOW; // now 0..24
                long k = ((long) n.tick << 8) | n.semitone;
                MusicNote ex = keep.get(k);
                if (ex == null || n.volume > ex.volume) keep.put(k, n);
            }
        }
        track.notes = new java.util.ArrayList<>(keep.values());
        track.notes.sort(java.util.Comparator.comparingInt(a -> a.tick));
    }

    private static int bestOctaveShift(java.util.List<MusicNote> notes) {
        int best = 0, bestCount = -1;
        for (int shift = -48; shift <= 48; shift += 12) {
            int c = 0;
            for (MusicNote n : notes) {
                int v = n.semitone + shift;
                if (v >= NB_LOW && v <= NB_HIGH) c++;
            }
            if (c > bestCount || (c == bestCount && Math.abs(shift) < Math.abs(best))) {
                bestCount = c;
                best = shift;
            }
        }
        return best;
    }

    private static double velocityToVolume(int velocity) {
        double v = Math.round(velocity / 100.0 * 0.7 * 10.0) / 10.0;
        return Math.max(0.1, Math.min(2.0, v));
    }

    private static int foldToRange(int midiNote) {
        int n = midiNote;
        while (n < NB_LOW) n += 12;
        while (n > NB_HIGH) n -= 12;
        return n;
    }

    private static Sound drumSound(int midiNote) {
        return switch (midiNote) {
            case 35, 36, 41, 43, 45, 47 -> Sound.BLOCK_NOTE_BLOCK_BASEDRUM; // kick / low toms
            case 42, 44, 46, 49, 51, 52, 53, 55, 57, 59 -> Sound.BLOCK_NOTE_BLOCK_HAT; // hats / cymbals
            default -> Sound.BLOCK_NOTE_BLOCK_SNARE; // snare / claps / everything else
        };
    }
}
