package com.drakou111.neohouse.music;

import com.drakou111.neohouse.constants.Sound;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NbsImporterTest {

    /** Builds Note Block Studio files in memory, little endian like the real format. */
    private static final class NbsWriter {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        NbsWriter b(int value) { out.write(value); return this; }
        NbsWriter s(int value) { return bytes(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) value).array()); }
        NbsWriter i(int value) { return bytes(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()); }
        NbsWriter str(String value) { byte[] b = value.getBytes(StandardCharsets.UTF_8); i(b.length); return bytes(b); }
        NbsWriter bytes(byte[] b) { out.write(b, 0, b.length); return this; }
        byte[] done() { return out.toByteArray(); }
    }

    /** Version 5 header with one layer at the given volume and tempo (ticks per second * 100). */
    private static NbsWriter v5Header(int tempo) {
        return new NbsWriter().s(0).b(5).b(16).s(20).s(1)
                .str("Song").str("Me").str("").str("")
                .s(tempo).b(0).b(10).b(4).i(0).i(0).i(0).i(0).i(0).str("")
                .b(0).b(0).s(0);
    }

    @Test
    void readsVersion5Notes() throws IOException {
        byte[] file = v5Header(1000)
                .s(1).s(1).b(0).b(45).b(100).b(100).s(0)          // tick 0: harp F#4
                .s(0)
                .s(4).s(1).b(1).b(33).b(100).b(100).s(0)          // tick 4: bass F#3
                .s(1).b(0).b(57).b(50).b(100).s(0)                //         harp F#5 at half velocity
                .s(0)
                .s(0)
                .str("Layer").b(0).b(100).b(100)                  // layer 0 at full volume
                .done();

        MusicProject project = NbsImporter.parse(file, "song");

        MusicTrack harp = project.tracks.stream().filter(t -> t.sound.equals(Sound.BLOCK_NOTE_BLOCK_HARP.name())).findFirst().orElseThrow();
        MusicTrack bass = project.tracks.stream().filter(t -> t.sound.equals(Sound.BLOCK_NOTE_BLOCK_BASS.name())).findFirst().orElseThrow();
        assertThat(harp.notes).hasSize(2);
        assertThat(harp.notes.get(0).tick).isZero();
        assertThat(harp.notes.get(0).semitone).isEqualTo(12);
        assertThat(harp.notes.get(0).volume).isEqualTo(0.7);
        assertThat(harp.notes.get(1).tick).isEqualTo(8);          // 10 tps -> 20 tps doubles ticks
        assertThat(harp.notes.get(1).semitone).isEqualTo(24);
        assertThat(harp.notes.get(1).volume).isEqualTo(0.4);
        assertThat(bass.notes.getFirst().semitone).isZero();
        assertThat(project.gridDivision).isEqualTo(8);
    }

    @Test
    void readsTheOriginalFormat() throws IOException {
        byte[] file = new NbsWriter().s(10).s(1)                   // song length, layer count
                .str("Old").str("").str("").str("")
                .s(2000).b(0).b(10).b(4).i(0).i(0).i(0).i(0).i(0).str("")
                .s(1).s(1).b(6).b(70).s(0)                         // tick 0: flute, key above range
                .s(0)
                .done();

        MusicProject project = NbsImporter.parse(file, "old");

        MusicNote note = project.tracks.getFirst().notes.getFirst();
        assertThat(project.tracks.getFirst().sound).isEqualTo(Sound.BLOCK_NOTE_BLOCK_FLUTE.name());
        assertThat(note.semitone).isEqualTo(70 - 24 - 33);         // folded down two octaves
        assertThat(project.gridDivision).isEqualTo(4);             // 20 tps already
    }

    @Test
    void rejectsTruncatedFiles() {
        assertThatThrownBy(() -> NbsImporter.parse(new byte[]{0, 0, 5}, "x")).isInstanceOf(IOException.class);
    }
}
