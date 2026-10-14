package com.drakou111.neohouse.music;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MusicPlayerTest {

    private static final int SONG_END_TICK = 50;
    // currentTick() advances with wall time; a test runs well within this many ticks
    private static final double TIMING_SLACK_TICKS = 5;

    private MusicPlayer player;

    @BeforeEach
    void setUp() {
        MusicProject project = MusicProject.fresh("test");
        project.tracks.getFirst().notes.add(new MusicNote(SONG_END_TICK - 1));
        this.player = new MusicPlayer();
        this.player.setProject(project);
    }

    @Test
    void stoppedPlayerRestsOnTheCursor() {
        this.player.seek(20);

        assertThat(this.player.isPlaying()).isFalse();
        assertThat(this.player.getStartTick()).isEqualTo(20);
        assertThat(this.player.currentTick()).isEqualTo(20);
    }

    @Test
    void playStartsFromTheCursorAndStopReturnsToIt() {
        this.player.seek(20);
        this.player.toggle();

        assertThat(this.player.isPlaying()).isTrue();
        assertThat(this.player.currentTick()).isCloseTo(20, within(TIMING_SLACK_TICKS));

        this.player.toggle();
        assertThat(this.player.isPlaying()).isFalse();
        assertThat(this.player.currentTick()).isEqualTo(20);
    }

    @Test
    void seekingWhilePlayingJumpsThere() {
        this.player.toggle();
        this.player.seek(30);

        assertThat(this.player.isPlaying()).isTrue();
        assertThat(this.player.getStartTick()).isEqualTo(30);
        assertThat(this.player.currentTick()).isCloseTo(30, within(TIMING_SLACK_TICKS));
    }

    @Test
    void seekingPastTheEndWhilePlayingStops() {
        this.player.toggle();
        this.player.seek(SONG_END_TICK + 10);

        assertThat(this.player.isPlaying()).isFalse();
        assertThat(this.player.getStartTick()).isEqualTo(SONG_END_TICK + 10);
    }

    @Test
    void playingFromPastTheEndStartsOver() {
        this.player.seek(SONG_END_TICK + 10);
        this.player.toggle();

        assertThat(this.player.isPlaying()).isTrue();
        assertThat(this.player.getStartTick()).isZero();
    }

    @Test
    void negativeSeekClampsToZero() {
        this.player.seek(-5);

        assertThat(this.player.getStartTick()).isZero();
    }
}
