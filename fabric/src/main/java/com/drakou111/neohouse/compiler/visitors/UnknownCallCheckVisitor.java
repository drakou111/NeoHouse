package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.StringValue;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.MenuNode;
import com.drakou111.neohouse.compiler.ast.statement.ActionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.config.UnknownCallMode;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Finds calls to functions, and displayMenu of menus, that the script doesn't define. They may
 * already exist in the house, so depending on the mode they are allowed silently, allowed with a
 * warning, or rejected; a run then checks they exist before changing anything. A call to an unknown
 * function can't pass arguments, since its parameter variables aren't known. Read-only.
 */
public class UnknownCallCheckVisitor extends DefaultVisitor {
    private final UnknownCallMode mode;
    private final List<CompileWarning> warnings;
    private final Set<String> functions = new HashSet<>();
    private final Set<String> menus = new HashSet<>();

    public UnknownCallCheckVisitor(UnknownCallMode mode, List<CompileWarning> warnings) {
        this.mode = mode;
        this.warnings = warnings;
    }

    @Override
    public MainNode analyze(MainNode main) {
        for (MainStatementNode statement : main.statements) {
            if (statement instanceof FunctionNode function) this.functions.add(function.name);
            else if (statement instanceof MenuNode menu) this.menus.add(menu.name);
        }
        main.accept(this);
        return main;
    }

    @Override
    public ASTNode visitVoidFunctionCall(VoidFunctionCallNode call) {
        checkFunction(call.functionName, call.arguments, call.token);
        return super.visitVoidFunctionCall(call);
    }

    @Override
    public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode call) {
        checkFunction(call.functionName, call.arguments, call.token);
        return super.visitGlobalFunctionCall(call);
    }

    @Override
    public ASTNode visitActionCall(ActionCallNode call) {
        if (AddAction.searchByName(call.name) == AddAction.DISPLAY_MENU) {
            Optional<String> menu = literalText(call.arguments.expressions);
            if (menu.isPresent() && !menu.get().contains("%") && !this.menus.contains(menu.get()))
                report(call.token, "Menu '%s' isn't in this script.".formatted(menu.get()));
        }
        return super.visitActionCall(call);
    }

    private void checkFunction(String name, ArgumentListNode arguments, Token token) {
        if (this.functions.contains(name)) return;
        if (!arguments.expressions.isEmpty())
            throw new SemanticException(token, ("Function '%s' isn't in this script, so it can't take arguments "
                    + "(its parameters aren't known).").formatted(name));
        report(token, "Function '%s' isn't in this script.".formatted(name));
    }

    private void report(Token token, String what) {
        switch (this.mode) {
            case OFF -> { }
            case WARN -> this.warnings.add(new CompileWarning(token, what
                    + " A run checks that it exists in the house before changing anything."));
            case ERROR -> throw new SemanticException(token, what
                    + " Define it here, or set Unknown calls to Warn or Off if it already exists in the house.");
        }
    }

    private static Optional<String> literalText(List<NormalExpressionNode> args) {
        if (!args.isEmpty() && args.getFirst() instanceof ConstantPrimitiveNode constant && constant.value instanceof StringValue text)
            return Optional.of(text.value);
        return Optional.empty();
    }
}
