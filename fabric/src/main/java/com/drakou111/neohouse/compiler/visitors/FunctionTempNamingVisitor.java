package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gives every function its own expression temps: {@code t$N} becomes {@code f$tN} in function
 * {@code f} (see {@link CompilerNames#temp}). Temps are numbered from 0 in each statement and a
 * call inside an expression runs while the caller still holds partial results in its temps
 * ({@code p.r = p.a * 2 + sq(p.b)} keeps {@code p.a * 2} in a temp across the call), so a callee
 * sharing the caller's names would overwrite them. Events, commands, loops and menus are never
 * called and keep {@code t$N}. The names depend only on each function's own name, so changing one
 * function never renames anything in another.
 */
public class FunctionTempNamingVisitor extends DefaultVisitor {
    private static final Pattern TEMP = Pattern.compile("t\\$(\\d+)");

    private String function = null;

    @Override
    public MainNode analyze(MainNode main) {
        checkCodesAreUnique(main);
        List<MainStatementNode> result = new ArrayList<>(main.statements.size());
        for (MainStatementNode statement : main.statements) {
            if (!(statement instanceof FunctionNode f)) {
                result.add(statement);
                continue;
            }
            this.function = f.name;
            result.add((MainStatementNode) statement.accept(this));
        }
        return new MainNode(result);
    }

    @Override
    public ASTNode visitVariable(VariableNode variable) {
        Matcher m = TEMP.matcher(variable.name);
        if (!m.matches()) return variable;
        VariableNode renamed = new VariableNode(variable.type, variable.holder,
                CompilerNames.temp(this.function, Integer.parseInt(m.group(1))), variable.teamName, variable.token);
        renamed.fallback = variable.fallback;
        renamed.castSuffix = variable.castSuffix;
        return renamed;
    }

    /** Two long function names sharing a 6-character code would share temps and parameter slots. */
    private static void checkCodesAreUnique(MainNode main) {
        Map<String, String> byCode = new HashMap<>();
        for (MainStatementNode statement : main.statements) {
            if (!(statement instanceof FunctionNode f) || !CompilerNames.usesCode(f.name)) continue;
            String base = CompilerNames.baseName(f.name);
            String other = byCode.putIfAbsent(CompilerNames.code(base), base);
            if (other != null && !other.equals(base))
                throw new SemanticException(f.token, ("Functions '%s' and '%s' would share compiler variables (their names map "
                        + "to the same code); rename one of them.").formatted(other, base));
        }
    }
}
