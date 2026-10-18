package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.ui.SyntaxHighlighter.Span;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SyntaxHighlighterTest {

    private static List<Span> spansInside(String source, String part) {
        int start = source.indexOf(part);
        int end = start + part.length();
        return new SyntaxHighlighter().buildSpans(source).stream()
                .filter(span -> span.start() < end && span.end() > start).toList();
    }

    @Test
    void aMenuTitleIsOneStringColorDespiteColorCodes() {
        List<Span> spans = spansInside("menu \"&aShop &lNow\"(1) { }", "\"&aShop &lNow\"");

        assertThat(spans).hasSize(1);
    }

    private static Span spanOf(String source, String text) {
        int start = source.indexOf(text);
        return new SyntaxHighlighter().buildSpans(source).stream()
                .filter(span -> span.start() == start && span.end() == start + text.length())
                .findFirst().orElseThrow();
    }

    @Test
    void formattingCodesAddUpUntilAColorOrReset() {
        String source = "message(\"&lA&oB&aC&nD&rE&mF\");";

        assertThat(spanOf(source, "A").style()).isEqualTo(StyledText.BOLD);
        assertThat(spanOf(source, "B").style()).isEqualTo(StyledText.BOLD | StyledText.ITALIC);
        assertThat(spanOf(source, "C").style()).isZero();
        assertThat(spanOf(source, "D").style()).isEqualTo(StyledText.UNDERLINE);
        assertThat(spanOf(source, "E").style()).isZero();
        assertThat(spanOf(source, "F").style()).isEqualTo(StyledText.STRIKETHROUGH);
    }

    @Test
    void theCodesThemselvesAndObfuscatedTextStayPlain() {
        String source = "message(\"&lA&kB\");";

        assertThat(spanOf(source, "&l").style()).isZero();
        assertThat(spanOf(source, "B").style()).isEqualTo(StyledText.BOLD);
    }

    @Test
    void otherStringsStillShowTheirColorCodes() {
        List<Span> spans = spansInside("function f() { message(\"&aShop &lNow\"); }", "\"&aShop &lNow\"");

        assertThat(spans.size()).isGreaterThan(1);
    }
}
