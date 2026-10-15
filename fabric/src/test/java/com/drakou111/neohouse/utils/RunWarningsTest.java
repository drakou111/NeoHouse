package com.drakou111.neohouse.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RunWarningsTest {

    @AfterEach
    void stopLogging() {
        RunWarnings.stop();
    }

    @Test
    void onlyWarningsDuringARunAreKept() {
        RunWarnings.record(() -> { });
        RunWarnings.start();
        RunWarnings.record(() -> { });
        RunWarnings.record(() -> { });
        RunWarnings.stop();
        RunWarnings.record(() -> { });

        assertThat(RunWarnings.count()).isEqualTo(2);
    }

    @Test
    void aNewRunStartsAFreshLog() {
        RunWarnings.start();
        RunWarnings.record(() -> { });
        RunWarnings.stop();
        RunWarnings.start();

        assertThat(RunWarnings.count()).isZero();
    }

    @Test
    void compileWarningsSurviveTheRunStarting() {
        // A script run opens the log before compiling; starting its actions must not clear it.
        RunWarnings.start();
        RunWarnings.record(() -> { });
        RunWarnings.startIfIdle();

        assertThat(RunWarnings.count()).isEqualTo(1);
    }
}
