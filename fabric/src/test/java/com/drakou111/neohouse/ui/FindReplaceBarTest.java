package com.drakou111.neohouse.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FindReplaceBarTest {

    @Test
    void matchesIgnoreCaseAndMayOverlap() {
        assertThat(FindReplaceBar.findMatches("Aaa-AA", "aa")).containsExactly(0, 1, 4);
        assertThat(FindReplaceBar.findMatches("abc", "")).isEmpty();
        assertThat(FindReplaceBar.findMatches("abc", "x")).isEmpty();
    }

    @Test
    void replaceAllIgnoresCaseAndDoesNotOverlap() {
        assertThat(FindReplaceBar.replaceAllIgnoringCase("Aaa-AA", "aa", "b")).isEqualTo("ba-b");
    }

    @Test
    void replaceAllWhenTheReplacementContainsTheQuery() {
        assertThat(FindReplaceBar.replaceAllIgnoringCase("a.a", "a", "aa")).isEqualTo("aa.aa");
    }

    @Test
    void replaceAllWithAnEmptyQueryChangesNothing() {
        assertThat(FindReplaceBar.replaceAllIgnoringCase("abc", "", "x")).isEqualTo("abc");
    }
}
