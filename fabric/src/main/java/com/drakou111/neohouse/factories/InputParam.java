package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.constants.Others;
import com.drakou111.neohouse.utils.ActionBuilder;

@FunctionalInterface
public interface InputParam {
    void apply(ActionBuilder b);

    static InputParam toggle(String label, boolean active) {
        return b -> { if (active) b.search(label); };
    }

    /** A text field, filled through its chat prompt unless the value is the field's default. */
    static InputParam text(String label, String value, String skipValue) {
        return b -> { if (value != null && !value.equals(skipValue)) b.searchForText(label, value); };
    }

    static InputParam textIfSet(String label, String value) {
        return b -> { if (value != null && !value.trim().isEmpty()) b.searchForText(label, value); };
    }

    static InputParam choice(String label, int clicks) {
        return b -> { for (int i = 0; i < clicks; i++) b.search(label); };
    }

    static InputParam listSearch(String label, String value) {
        return b -> { if (value != null && !value.trim().isEmpty()) { b.search(label); b.search(value); } };
    }

    /** Picks {@code value} from a long list by typing it into the list's search bar first. */
    static InputParam listSearchWithSearchBar(String label, String value) {
        return b -> {
            if (value != null && !value.trim().isEmpty()) {
                b.search(label);
                b.searchBarInput(value, Others.GENERAL_SEARCH_H6);
                b.search(value);
            }
        };
    }

    static InputParam item(String label, String snbt) {
        return b -> { if (snbt != null && !snbt.isEmpty()) { b.search(label); b.customItem(snbt); } };
    }

    static InputParam note(String value) {
        return b -> { if (value != null && !value.trim().isEmpty()) b.textInput(value, Others.GENERAL_NOTE_H4); };
    }

    static InputParam predefined(String label, boolean active, String displayName) {
        return b -> { if (active) { b.search(label); b.search(displayName); } };
    }

    /**
     * Opens {@code label} and enters a free-form value through its {@code customLabel} item. That prompt
     * may be an anvil even with chat inputs on (e.g. Custom Sound), so either kind is accepted.
     */
    static InputParam customValue(String label, String customLabel, String customValue) {
        return b -> { if (customValue != null && !customValue.isBlank()) { b.search(label); b.searchForText(customLabel, customValue, true); } };
    }

    static InputParam withCustom(String label, boolean active, String customLabel, String customValue) {
        return b -> { if (active) { b.search(label); b.searchForText(customLabel, customValue); } };
    }
}
