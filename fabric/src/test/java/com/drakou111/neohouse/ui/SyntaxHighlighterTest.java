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

    @Test
    void otherStringsStillShowTheirColorCodes() {
        List<Span> spans = spansInside("function f() { message(\"&aShop &lNow\"); }", "\"&aShop &lNow\"");

        assertThat(spans.size()).isGreaterThan(1);
    }
}
