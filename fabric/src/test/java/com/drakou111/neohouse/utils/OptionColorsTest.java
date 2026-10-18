package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.UnknownCallMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OptionColorsTest {

    private static final int FALLBACK = 0xFF123456;

    @Test
    void offIsGreenWarnYellowErrorRed() {
        assertThat(OptionColors.argbFor(UnknownCallMode.OFF.displayName, FALLBACK)).isEqualTo(0xFF55FF55);
        assertThat(OptionColors.argbFor(UnknownCallMode.WARN.displayName, FALLBACK)).isEqualTo(0xFFFFFF55);
        assertThat(OptionColors.argbFor(UnknownCallMode.ERROR.displayName, FALLBACK)).isEqualTo(0xFFFF5555);
    }

    @Test
    void everyMultiCallAndOverrideOptionHasAColor() {
        for (MultiCallMode mode : MultiCallMode.values())
            assertThat(OptionColors.formattingFor(mode.displayName)).as(mode.displayName).isPresent();
        for (OverrideMode mode : OverrideMode.values())
            assertThat(OptionColors.formattingFor(mode.displayName)).as(mode.displayName).isPresent();
    }

    @Test
    void otherNamesKeepTheFallback() {
        assertThat(OptionColors.argbFor("Dracula", FALLBACK)).isEqualTo(FALLBACK);
    }
}
