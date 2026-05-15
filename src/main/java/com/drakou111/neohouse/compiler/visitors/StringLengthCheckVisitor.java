package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.UnaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitive;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.StringValue;
import com.drakou111.neohouse.compiler.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.TypedReturnNode;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.enums.BinaryOperator;
import com.drakou111.neohouse.compiler.enums.UnaryOperator;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.constants.Placeholder;
import com.drakou111.neohouse.constants.StringLimits;
import com.drakou111.neohouse.exceptions.SemanticException;

public class StringLengthCheckVisitor extends EmptyVisitor<Void> {

    private VariableType currentReturnType = null;

    public void analyze(MainNode main) {
        main.accept(this);
    }

    @Override
    public Void visitAssignation(AssignationNode assignation) {
        if (assignation.variable.type == VariableType.STRING
                && assignation.operator == AssignmentOperator.ASSIGN) {
            int len = housingLength(assignation.value);
            if (len > StringLimits.STRING_TEXT) {
                throw new SemanticException(assignation.token,
                        "String value is too long: it would expand to " + len
                        + " characters in Hypixel Housing, but the limit is " + StringLimits.STRING_TEXT + ".");
            }
        }
        return null;
    }

    @Override
    public Void visitTypedFunction(TypedFunctionNode function) {
        VariableType prev = currentReturnType;
        currentReturnType = function.returnType;
        function.parameters.accept(this);
        function.statements.accept(this);
        currentReturnType = prev;
        return null;
    }

    @Override
    public Void visitTypedReturn(TypedReturnNode returnNode) {
        if (currentReturnType == VariableType.STRING) {
            int len = housingLength(returnNode.expression);
            if (len > StringLimits.STRING_TEXT) {
                throw new SemanticException(returnNode.token,
                        "Returned string is too long: it would expand to " + len
                        + " characters in Hypixel Housing, but the limit is " + StringLimits.STRING_TEXT + ".");
            }
        }
        return null;
    }

    @Override
    public Void visitGlobalDeclaration(GlobalDeclarationNode declaration) {
        if (declaration.variable.type == VariableType.STRING && declaration.initializer != null) {
            int len = housingLength(declaration.initializer);
            if (len > StringLimits.STRING_TEXT) {
                throw new SemanticException(declaration.token,
                        "String initializer is too long: it would expand to " + len
                        + " characters in Hypixel Housing, but the limit is " + StringLimits.STRING_TEXT + ".");
            }
        }
        return null;
    }

    private int housingLength(NormalExpressionNode expr) {
        return switch (expr) {
            case ConstantPrimitiveNode c -> primitiveLength(c.value);
            case VariableNode v -> switch (v.holder) {
                case PLAYER -> 13 + v.name.length(); // %var.player/name%
                case GLOBAL -> 13 + v.name.length(); // %var.global/name%
                case TEAM   -> 12 + v.name.length() + 1 + v.teamName.length(); // %var.team/name teamname%
            };
            case PlaceholderNode p -> placeholderLength(p);
            case BinaryExpressionNode b when b.operator == BinaryOperator.ADD ->
                housingLength(b.left) + housingLength(b.right);
            case UnaryExpressionNode u when u.operator == UnaryOperator.STRING_CAST ->
                housingLength(u.operand);
            default -> 0;
        };
    }

    private int primitiveLength(ConstantPrimitive prim) {
        return switch (prim) {
            case StringValue s -> s.value.length();
            default -> prim.toString().length();
        };
    }

    private int placeholderLength(PlaceholderNode p) {
        Placeholder ph = Placeholder.requireByName(p.placeholder);
        // %inGameName% or %inGameName/arg0 arg1 ...%
        int len = 1 + ph.inGameName.length() + 1; // % + name + %
        if (!p.constants.constants.isEmpty()) {
            len += 1; // the '/'
            for (int i = 0; i < p.constants.constants.size(); i++) {
                if (i > 0) len += 1; // space between args
                len += primitiveLength(p.constants.constants.get(i).value);
            }
        }
        return len;
    }
}
