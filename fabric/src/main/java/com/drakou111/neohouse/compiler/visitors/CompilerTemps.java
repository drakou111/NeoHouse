package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;

import java.util.regex.Pattern;

/**
 * Recognises the variables the compiler makes, which it may rewrite or remove freely: expression
 * temps ({@code t$0}), return slots ({@code o$}, {@code o$1}) and parameter slots ({@code f_p$0}).
 * Scripts can't use '$' in their own names, so everything else is the script's own variable and
 * keeps every write the script asked for.
 */
final class CompilerTemps {
    private static final Pattern EXPRESSION_TEMP = Pattern.compile("t\\$\\d+");
    private static final Pattern RETURN_SLOT = Pattern.compile("o\\$\\d*");
    // f_p$0, or $<code>p0 for a function name too long for that (see CompilerNames).
    private static final Pattern PARAMETER_SLOT = Pattern.compile(".+_p\\$\\d+|\\$[0-9a-z]{6}p\\d+");

    private CompilerTemps() {
    }

    /** Any compiler-made variable. */
    static boolean isTemp(VariableNode v) {
        return v.teamName == null && v.name.indexOf('$') >= 0;
    }

    /** A temp holding part of one statement's expression (before functions get their own names for them). */
    static boolean isExpressionTemp(String name) {
        return EXPRESSION_TEMP.matcher(name).matches();
    }

    /** Where a function leaves its return value for the caller. */
    static boolean isReturnSlot(String name) {
        return RETURN_SLOT.matcher(name).matches();
    }

    /** Where a caller leaves an argument for the function it calls. */
    static boolean isParameterSlot(String name) {
        return PARAMETER_SLOT.matcher(name).matches();
    }
}
