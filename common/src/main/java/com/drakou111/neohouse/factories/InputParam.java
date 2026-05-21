package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.constants.Others;
import com.drakou111.neohouse.utils.ActionBuilder;

@FunctionalInterface
public interface InputParam {
    void apply(ActionBuilder b);

    static InputParam toggle(String label, boolean active) {
        return b -> { if (active) b.search(label); };
    }

    static InputParam anvil(String label, String value, String skipValue) {
        return b -> { if (value != null && !value.equals(skipValue)) b.searchForAnvil(label, value); };
    }

    static InputParam anvilIfSet(String label, String value) {
        return b -> { if (value != null && !value.trim().isEmpty()) b.searchForAnvil(label, value); };
    }

    static InputParam chat(String label, String value, String skipValue) {
        return b -> { if (value != null && !value.equals(skipValue)) b.searchForChat(label, value); };
    }

    static InputParam choice(String label, int clicks) {
        return b -> { for (int i = 0; i < clicks; i++) b.search(label); };
    }

    static InputParam listSearch(String label, String value) {
        return b -> { if (value != null && !value.trim().isEmpty()) { b.search(label); b.search(value); } };
    }

    static InputParam item(String label, String snbt) {
        return b -> { if (snbt != null && !snbt.isEmpty()) { b.search(label); b.customItem(snbt); } };
    }

    static InputParam note(String value) {
        return b -> { if (value != null && !value.trim().isEmpty()) b.chatInput(value, Others.GENERAL_NOTE_H4); };
    }

    static InputParam predefined(String label, boolean active, String displayName) {
        return b -> { if (active) { b.search(label); b.search(displayName); } };
    }

    static InputParam withCustom(String label, boolean active, String customLabel, String customValue) {
        return b -> { if (active) { b.search(label); b.searchForAnvil(customLabel, customValue); } };
    }
}
