package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.BooleanExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.LogicalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.config.UnsetVariableMode;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reports conditions on a variable the script never sets. Housing treats a condition on an unset
 * variable as false whatever it compares against, unless the variable has a fallback ({@code p.x?0}),
 * so such a condition may never pass. Another script or the house may still set the variable, so
 * this is a warning by default. Runs on the final tree, where every condition compares a variable
 * and parameters and return values are compiler-made slots that callers always set.
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
    private final List<VariableNode> compared = new ArrayList<>();

    public UnsetVariableCheckVisitor(UnsetVariableMode mode, List<CompileWarning> warnings) {
        this.mode = mode;
        this.warnings = warnings;
    }

    @Override
    public MainNode analyze(MainNode main) {
        if (this.mode == UnsetVariableMode.OFF) return main;
        main.accept(this);

        Set<VarKey> reported = new HashSet<>();
        for (VariableNode variable : this.compared) {
            VarKey key = VarKey.of(variable);
            if (this.assigned.contains(key) || !reported.add(key)) continue;

            String name = variable.sourceName();
            String message = ("'%s' is never set in this script, and a condition on an unset variable is always false. "
                    + "Set it first, or give it a fallback such as %s?0.").formatted(name, name);
            if (this.mode == UnsetVariableMode.ERROR) throw new SemanticException(variable.token, message);
            this.warnings.add(new CompileWarning(variable.token, message));
        }
        return main;
    }

    @Override
    public ASTNode visitAssignation(AssignationNode assignation) {
        this.assigned.add(VarKey.of(assignation.variable));
        return super.visitAssignation(assignation);
    }

    @Override
    public ASTNode visitGlobalDeclaration(GlobalDeclarationNode declaration) {
        if (declaration.initializer != null) this.assigned.add(VarKey.of(declaration.variable));
        return super.visitGlobalDeclaration(declaration);
    }

    @Override
    public ASTNode visitConditional(ConditionalNode conditional) {
        if (conditional.condition != null) collectCompared(conditional.condition);
        return super.visitConditional(conditional);
    }

    private void collectCompared(ExpressionNode condition) {
        List<ExpressionNode> conditions = condition instanceof LogicalExpressionNode logical ? logical.conditions : List.of(condition);
        for (ExpressionNode single : conditions) {
            if (single instanceof BooleanExpressionNode comparison && comparison.left instanceof VariableNode variable
                    && variable.fallback == null && !CompilerTemps.isTemp(variable))
                this.compared.add(variable);
        }
    }
}
