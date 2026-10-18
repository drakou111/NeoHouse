package com.drakou111.neohouse.ui;

/**
 * Pasting into a string literal: the pasted text is escaped so it stays one string. Backslashes are
 * escaped too, so text that already holds escapes (like {@code "a \"b\""}) keeps them one level down.
 */
final class StringPaste {

    private StringPaste() {
    }

    /** {@code pasted} as it should be inserted at {@code offset} in {@code text}. */
    static String forOffset(CharSequence text, int offset, String pasted) {
        return isInsideString(text, offset) ? escape(pasted) : pasted;
    }

    static String escape(String pasted) {
        return pasted.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Whether {@code offset} sits between the quotes of a string literal (comments are skipped). */
    static boolean isInsideString(CharSequence text, int offset) {
        int i = 0;
        while (i < offset) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? text.charAt(i + 1) : 0;
            if (c == '/' && next == '/') {
                i = indexOf(text, "\n", i + 2);
            } else if (c == '/' && next == '*') {
                int end = indexOf(text, "*/", i + 2);
                i = end >= text.length() ? end : end + 2;
            } else if (c == '"') {
                int end = closingQuote(text, i + 1);
                if (end < 0 || end >= offset) return true;
                i = end + 1;
            } else {
                i++;
            }
        }
        return false;
    }

    /** The index of the quote closing a string whose content starts at {@code from}, or -1 if it isn't closed. */
    private static int closingQuote(CharSequence text, int from) {
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') i++;
            else if (c == '"') return i;
        }
        return -1;
    }

    /** The index of {@code needle} at or after {@code from}, or the text's length when it doesn't occur. */
    private static int indexOf(CharSequence text, String needle, int from) {
        // A plain scan: text may be a large builder, and copying it per comment would be quadratic.
        for (int i = from; i + needle.length() <= text.length(); i++) {
            boolean match = true;
            for (int k = 0; k < needle.length() && match; k++) match = text.charAt(i + k) == needle.charAt(k);
            if (match) return i;
        }
        return text.length();
    }
}
