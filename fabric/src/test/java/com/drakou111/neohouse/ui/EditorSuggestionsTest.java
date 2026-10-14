package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Teleport;
import com.drakou111.neohouse.ui.EditorSuggestions.Suggestions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EditorSuggestionsTest {

    @Test
    void eventNamesAfterTheEventKeyword() {
        Optional<Suggestions> s = EditorSuggestions.at("event onJ");

        assertThat(s).isPresent();
        assertThat(s.get().items()).contains("onJoin").allMatch(name -> name.startsWith("onJ"));
        assertThat(s.get().typedLength()).isEqualTo(3);
        assertThat(s.get().insideString()).isFalse();
    }

    @Test
    void everyEventRightAfterTheEventKeyword() {
        Optional<Suggestions> s = EditorSuggestions.at("    event ");

        assertThat(s).isPresent();
        assertThat(s.get().items()).hasSize(Event.values().length);
        assertThat(s.get().typedLength()).isZero();
    }

    @Test
    void noEventsForAWordThatOnlyEndsInEvent() {
        assertThat(EditorSuggestions.at("myevent ")).isEmpty();
        assertThat(EditorSuggestions.at("event onJoin { x")).isEmpty();
    }

    @Test
    void placeholderNamesAfterPh() {
        Optional<Suggestions> s = EditorSuggestions.at("x = ph.date");

        assertThat(s).isPresent();
        assertThat(s.get().items()).isNotEmpty().allMatch(name -> name.startsWith("date"));
        assertThat(s.get().typedLength()).isEqualTo(4);
    }

    @Test
    void locationPresetsForALocationArgument() {
        Optional<Suggestions> s = EditorSuggestions.at("teleport(INV");

        assertThat(s).isPresent();
        assertThat(s.get().items()).containsExactly(Teleport.INVOKER.name());
        assertThat(s.get().typedLength()).isEqualTo(3);
    }

    @Test
    void nothingWhenTheOnlySuggestionIsAlreadyTyped() {
        assertThat(EditorSuggestions.at("teleport(" + Teleport.INVOKER.name())).isEmpty();
    }

    @Test
    void noPresetsOutsideALocationArgument() {
        assertThat(EditorSuggestions.at("teleport(INVOKER, INV")).isEmpty();
        assertThat(EditorSuggestions.at("x = INV")).isEmpty();
    }
}
