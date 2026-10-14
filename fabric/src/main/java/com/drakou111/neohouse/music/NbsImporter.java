package com.drakou111.neohouse.music;

import com.drakou111.neohouse.constants.Sound;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a Note Block Studio song ({@code .nbs}, every format version from the original one to 5)
 * into a {@link MusicProject}: one track per instrument, notes placed on the 20 ticks/second grid.
 *
 * <p>Note Block Studio keys 33-57 are the vanilla note block range (F#3-F#5), which is exactly the
 * project's semitones 0-24; notes outside it are moved by octaves into range. Instruments Housing's
 * sound list doesn't have (iron xylophone, cow bell, didgeridoo, bit, banjo) and custom instruments
 * use the closest listed note block sound.</p>
 */
public final class NbsImporter {
    private static final int KEY_LOW = 33;            // F#3: semitone 0
    private static final int KEY_HIGH = 57;           // F#5: semitone 24
    private static final int OCTAVE = 12;
    private static final int TEMPO_SCALE = 100;       // tempo is stored as ticks per second * 100
    private static final int DEFAULT_TEMPO = 1000;    // 10 ticks per second
    private static final int FULL_VELOCITY = 100;
    private static final int CENTS_PER_SEMITONE = 100;
    private static final int TICKS_PER_BEAT = 4;      // Note Block Studio's grid: 4 ticks per beat
    private static final double DEFAULT_NOTE_VOLUME = 0.7;
    private static final double VOLUME_STEP = 10.0;   // snap volumes to 0.1 so the default (0.7) is exact
    private static final double MIN_VOLUME = 0.1;

    /** Vanilla instruments in Note Block Studio order. */
    private static final Sound[] INSTRUMENTS = {
            Sound.BLOCK_NOTE_BLOCK_HARP,       // 0 harp
            Sound.BLOCK_NOTE_BLOCK_BASS,       // 1 double bass
            Sound.BLOCK_NOTE_BLOCK_BASEDRUM,   // 2 bass drum
            Sound.BLOCK_NOTE_BLOCK_SNARE,      // 3 snare
            Sound.BLOCK_NOTE_BLOCK_HAT,        // 4 click
            Sound.BLOCK_NOTE_BLOCK_GUITAR,     // 5 guitar
            Sound.BLOCK_NOTE_BLOCK_FLUTE,      // 6 flute
            Sound.BLOCK_NOTE_BLOCK_BELL,       // 7 bell
            Sound.BLOCK_NOTE_BLOCK_CHIME,      // 8 chime
            Sound.BLOCK_NOTE_BLOCK_XYLOPHONE,  // 9 xylophone
            Sound.BLOCK_NOTE_BLOCK_XYLOPHONE,  // 10 iron xylophone (not in Housing's list)
            Sound.BLOCK_NOTE_BLOCK_BELL,       // 11 cow bell (not in Housing's list)
            Sound.BLOCK_NOTE_BLOCK_BASS,       // 12 didgeridoo (not in Housing's list)
            Sound.BLOCK_NOTE_BLOCK_PLING,      // 13 bit (not in Housing's list)
            Sound.BLOCK_NOTE_BLOCK_GUITAR,     // 14 banjo (not in Housing's list)
            Sound.BLOCK_NOTE_BLOCK_PLING       // 15 pling
    };

    private NbsImporter() {}

    /** A raw note as stored in the file, before it is placed on the project's grid. */
    private record RawNote(int tick, int layer, int instrument, int key, int velocity, int pitchCents) {}

    public static MusicProject importNbs(Path file, String projectName) throws IOException {
        return parse(Files.readAllBytes(file), projectName);
    }

    static MusicProject parse(byte[] data, String projectName) throws IOException {
        ByteBuffer in = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        try {
            return read(in, projectName);
        } catch (BufferUnderflowException e) {
            throw new IOException("Not a valid Note Block Studio file (it ends too early)");
        }
    }

    private static MusicProject read(ByteBuffer in, String projectName) throws IOException {
        // Version 0 files start with the song length; newer ones start with 0, then the version.
        int version = 0;
        int vanillaInstruments = 10;
        int firstShort = Short.toUnsignedInt(in.getShort());
        if (firstShort == 0) {
            version = Byte.toUnsignedInt(in.get());
            vanillaInstruments = Byte.toUnsignedInt(in.get());
            if (version >= 3) in.getShort(); // song length
        }
        int layerCount = Short.toUnsignedInt(in.getShort());
        readString(in); // song name
        readString(in); // author
        readString(in); // original author
        readString(in); // description
        int tempo = Short.toUnsignedInt(in.getShort());
        if (tempo <= 0) tempo = DEFAULT_TEMPO;
        in.get();      // auto-saving
        in.get();      // auto-saving duration
        in.get();      // time signature
        in.getInt();   // minutes spent
        in.getInt();   // left clicks
        in.getInt();   // right clicks
        in.getInt();   // note blocks added
        in.getInt();   // note blocks removed
        readString(in); // imported MIDI/schematic name
        if (version >= 4) {
            in.get();      // loop on/off
            in.get();      // max loop count
            in.getShort(); // loop start tick
        }

        List<RawNote> notes = new ArrayList<>();
        int tick = -1;
        for (int jump = in.getShort(); jump != 0; jump = in.getShort()) {
            tick += jump;
            int layer = -1;
            for (int layerJump = in.getShort(); layerJump != 0; layerJump = in.getShort()) {
                layer += layerJump;
                int instrument = Byte.toUnsignedInt(in.get());
                int key = Byte.toUnsignedInt(in.get());
                int velocity = FULL_VELOCITY;
                int pitchCents = 0;
                if (version >= 4) {
                    velocity = Byte.toUnsignedInt(in.get());
                    in.get(); // panning
                    pitchCents = in.getShort();
                }
                notes.add(new RawNote(tick, layer, instrument, key, velocity, pitchCents));
            }
        }

        // Layer volumes scale every note on the layer (missing in files cut short after the notes).
        Map<Integer, Integer> layerVolume = new HashMap<>();
        for (int layer = 0; layer < layerCount && in.remaining() >= Integer.BYTES; layer++) {
            readString(in); // layer name
            if (version >= 4) in.get(); // locked
            layerVolume.put(layer, Byte.toUnsignedInt(in.get()));
            if (version >= 2) in.get(); // stereo
        }

        return toProject(notes, layerVolume, vanillaInstruments, tempo / (double) TEMPO_SCALE, projectName);
    }

    private static MusicProject toProject(List<RawNote> notes, Map<Integer, Integer> layerVolume,
                                          int vanillaInstruments, double ticksPerSecond, String projectName) {
        double tickScale = MusicProject.TICKS_PER_SECOND / ticksPerSecond;
        Map<Sound, MusicTrack> tracks = new EnumMap<>(Sound.class);
        Map<Sound, Map<Long, MusicNote>> loudestAt = new EnumMap<>(Sound.class);

        for (RawNote raw : notes) {
            Sound sound = raw.instrument() < Math.min(vanillaInstruments, INSTRUMENTS.length)
                    ? INSTRUMENTS[raw.instrument()] : Sound.BLOCK_NOTE_BLOCK_HARP;
            MusicTrack track = tracks.computeIfAbsent(sound, s -> {
                MusicTrack created = new MusicTrack(trackName(s), s);
                created.melodic = true; // Note Block Studio pitches every instrument like a note block
                return created;
            });

            MusicNote note = new MusicNote((int) Math.round(raw.tick() * tickScale));
            int key = raw.key() + Math.round(raw.pitchCents() / (float) CENTS_PER_SEMITONE);
            note.semitone = foldIntoRange(key) - KEY_LOW;
            double volume = raw.velocity() / (double) FULL_VELOCITY
                    * layerVolume.getOrDefault(raw.layer(), FULL_VELOCITY) / FULL_VELOCITY;
            note.volume = Math.max(MIN_VOLUME, Math.round(volume * DEFAULT_NOTE_VOLUME * VOLUME_STEP) / VOLUME_STEP);

            // Two notes of one instrument on the same tick and pitch would play as one: keep the louder.
            long slot = ((long) note.tick << 8) | note.semitone;
            MusicNote existing = loudestAt.computeIfAbsent(sound, s -> new LinkedHashMap<>()).get(slot);
            if (existing == null || note.volume > existing.volume) {
                loudestAt.get(sound).put(slot, note);
                if (existing != null) track.notes.remove(existing);
                track.notes.add(note);
            }
        }

        MusicProject project = new MusicProject();
        project.name = projectName;
        project.gridDivision = Math.max(1, (int) Math.round(TICKS_PER_BEAT * tickScale));
        project.bpm = 60.0 * MusicProject.TICKS_PER_SECOND / project.gridDivision;
        for (MusicTrack track : tracks.values()) {
            track.notes.sort(Comparator.comparingInt(n -> n.tick));
            project.tracks.add(track);
        }
        if (project.tracks.isEmpty()) project.tracks.add(new MusicTrack(trackName(Sound.BLOCK_NOTE_BLOCK_HARP), Sound.BLOCK_NOTE_BLOCK_HARP));
        return project;
    }

    /** Moves a key by octaves into the note block range. */
    private static int foldIntoRange(int key) {
        int folded = key;
        while (folded < KEY_LOW) folded += OCTAVE;
        while (folded > KEY_HIGH) folded -= OCTAVE;
        return folded;
    }

    private static String trackName(Sound sound) {
        String id = sound.name().replace("BLOCK_NOTE_BLOCK_", "").toLowerCase();
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    private static String readString(ByteBuffer in) throws IOException {
        int length = in.getInt();
        if (length < 0 || length > in.remaining()) throw new IOException("Not a valid Note Block Studio file (bad text length)");
        byte[] bytes = new byte[length];
        in.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
