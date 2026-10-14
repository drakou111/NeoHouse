package com.drakou111.neohouse.music;

import com.drakou111.neohouse.constants.Sound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

public final class MusicPlayer {

    private static final double MS_PER_TICK = 1000.0 / MusicProject.TICKS_PER_SECOND;

    private MusicProject project;
    private boolean playing = false;
    private boolean loop = false;
    private long startNanos;
    private int startTick;
    private int lastPlayedTick;

    public void setProject(MusicProject project) {
        this.project = project;
    }

    public boolean isPlaying() { return playing; }

    public void setLoop(boolean loop) { this.loop = loop; }

    public boolean isLooping() { return loop; }

    /** Plays from {@code fromTick}, or from the start when that is at or past the end of the song. */
    public void play(int fromTick) {
        if (project == null) return;
        this.startTick = fromTick >= project.lengthTicks() ? 0 : Math.max(0, fromTick);
        this.lastPlayedTick = startTick - 1;
        this.startNanos = System.nanoTime();
        this.playing = true;
    }

    public void stop() {
        playing = false;
    }

    /** Stops, or plays from the start cursor; a stopped player rests on its start cursor. */
    public void toggle() {
        if (playing) stop();
        else play(startTick);
    }

    /** The tick playback starts from (and returns to when stopped). */
    public int getStartTick() {
        return startTick;
    }

    /** Moves the start cursor; while playing, playback jumps there. */
    public void seek(int tick) {
        int clamped = Math.max(0, tick);
        if (playing && clamped < project.lengthTicks()) {
            play(clamped);
            return;
        }
        playing = false;
        startTick = clamped;
    }

    public double currentTick() {
        if (!playing) return startTick;
        double elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0;
        return startTick + elapsedMs / MS_PER_TICK;
    }

    public void update() {
        if (!playing || project == null) return;

        int end = project.lengthTicks();
        double head = currentTick();
        int upTo = (int) Math.floor(head);

        for (int tick = lastPlayedTick + 1; tick <= upTo && tick < end; tick++) {
            playTick(tick);
        }
        lastPlayedTick = Math.max(lastPlayedTick, upTo);

        if (head >= end) {
            if (loop) play(startTick);
            else stop();
        }
    }

    private void playTick(int tick) {
        for (MusicTrack track : project.tracks) {
            if (track.muted) continue;
            for (MusicNote note : track.notes) {
                if (note.tick == tick) {
                    playNote(track.soundFor(note), track.volumeFor(note), (float) track.pitchFor(note));
                }
            }
        }
    }

    public static void playNote(Sound sound, float volume, float pitch) {
        SoundEvent event = SoundEventResolver.soundEvent(sound);
        if (event == null) return;
        Minecraft mc = Minecraft.getInstance();
        float p = Math.max(0.0f, Math.min(2.0f, pitch));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(event, p, Math.max(0.0f, volume)));
    }
}
