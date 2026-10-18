package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.DeclarationNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.config.UnsetVariableMode;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reports reads of a variable the script never sets. An unset variable has no value in Housing: a
 * condition on it is false whatever it compares against, and other reads get an empty value, unless
 * the read has a fallback ({@code p.x?0}). Another script or the house may still set the variable,
 * so this is a warning by default. Runs on the final tree, where parameters and return values are
 * compiler-made slots that are always set before they're read.
 */
public class UnsetVariableCheckVisitor extends DefaultVisitor {
    private record VarKey(Holder holder, String name, String team) {
        static VarKey of(VariableNode variable) {
            return new VarKey(variable.holder, variable.name, variable.teamName);
        }
    }

    private final UnsetVariableMode mode;
    private final List<CompileWarning> warnings;
    private final Set<VarKey> assigned = new HashSet<>();
    private final List<VariableNode> reads = new ArrayList<>();

    public UnsetVariableCheckVisitor(UnsetVariableMode mode, List<CompileWarning> warnings) {
        this.mode = mode;
        this.warnings = warnings;
    }

    @Override
    public MainNode analyze(MainNode main) {
        if (this.mode == UnsetVariableMode.OFF) return main;
        main.accept(this);

        Set<VarKey> reported = new HashSet<>();
        for (VariableNode read : this.reads) {
            VarKey key = VarKey.of(read);
            if (this.assigned.contains(key) || !reported.add(key)) continue;

            String name = read.sourceName();
            String message = ("'%s' is used here but never given a value with = in this script. While it's unset, any "
                    + "condition that uses it is false. Set it first, or add a fallback such as %s?%s.")
                    .formatted(name, name, exampleFallback(read.type));
            if (this.mode == UnsetVariableMode.ERROR) throw new SemanticException(read.token, message);
            this.warnings.add(new CompileWarning(read.token, message));
        }
        return main;
    }

    /** A fallback the variable's type accepts; an untyped var takes any, so it gets the common 0. */
    private static String exampleFallback(VariableType type) {
        if (type == null) return "0";
        return switch (type) {
            case STRING -> "\"\"";
            case DOUBLE -> "0.0";
            case BOOL -> "false";
            default -> "0";
        };
    }

    @Override
    public ASTNode visitVariable(VariableNode variable) {
        if (variable.fallback == null && !CompilerTemps.isTemp(variable)) this.reads.add(variable);
        return variable;
    }

    /**
     * Only a plain {@code =} sets a variable. A compound assignment like {@code +=} starts from the
     * current value, so on an unset variable it reads nothing, like any other read.
     */
    @Override
    public ASTNode visitAssignation(AssignationNode assignation) {
        if (assignation.operator == AssignmentOperator.ASSIGN) this.assigned.add(VarKey.of(assignation.variable));
        else visitVariable(assignation.variable);
        assignation.value.accept(this);
        return assignation;
    }

    @Override
    public ASTNode visitGlobalDeclaration(GlobalDeclarationNode declaration) {
        if (declaration.initializer != null) this.assigned.add(VarKey.of(declaration.variable));
        return declaration;
    }

    @Override
    public ASTNode visitDeclaration(DeclarationNode declaration) {
        return declaration;
    }
}
