package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.StringValue;
import com.drakou111.neohouse.compiler.enums.Holder;

import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Variables read inside text through {@code %var.player/name%} placeholders. Text is built from an
 * expression before later passes rename parameters and temps, so those passes rename the
 * placeholders too, or the text would read the old name.
 */
final class PlaceholderText {
    private static final Pattern VARIABLE = Pattern.compile("%var\\.(player|global|team)/([^\\s%\"]+)");

    private PlaceholderText() {
    }

    /**
     * {@code constant} with each variable placeholder in its text renamed to what {@code rename}
     * gives for (holder, name), or kept where it gives null. Anything but text is returned as is.
     */
    static ASTNode renamed(ConstantPrimitiveNode constant, BiFunction<Holder, String, String> rename) {
        if (!(constant.value instanceof StringValue text)) return constant;
        Matcher matcher = VARIABLE.matcher(text.value);
        StringBuilder result = new StringBuilder();
        boolean changed = false;
        while (matcher.find()) {
            String newName = rename.apply(holderOf(matcher.group(1)), matcher.group(2));
            String replacement = newName == null ? matcher.group() : "%var." + matcher.group(1) + "/" + newName;
            changed |= newName != null;
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        if (!changed) return constant;
        matcher.appendTail(result);
        return new ConstantPrimitiveNode(new StringValue(result.toString()), constant.type, constant.token);
    }

    private static Holder holderOf(String placeholderHolder) {
        return switch (placeholderHolder) {
            case "player" -> Holder.PLAYER;
            case "global" -> Holder.GLOBAL;
            default -> Holder.TEAM;
        };
    }
}
