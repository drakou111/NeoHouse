package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Text typed into an action's input has to fit Housing's 256-character chat line. */
class ActionTextLengthTest {

    @Test
    void aMessageOverTheLimitIsAnError() {
        assertThatThrownBy(() -> TestCompiler.analyze("event onJoin { message(\"" + "x".repeat(257) + "\"); }"))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("Text is too long: it would be 257 characters");
    }

    @Test
    void aMessageAtTheLimitIsFine() {
        assertThatCode(() -> TestCompiler.analyze("event onJoin { message(\"" + "x".repeat(256) + "\"); }"))
                .doesNotThrowAnyException();
    }

    @Test
    void itemDataIsNotTypedSoItCanBeLonger() {
        String lore = "a".repeat(300);
        assertThatCode(() -> TestCompiler.analyze(
                "event onJoin { giveItem({id:\"minecraft:stone\",components:{\"minecraft:lore\":[\"" + lore + "\"]}}, false, \"HELMET\", false); }"))
                .doesNotThrowAnyException();
    }
}
