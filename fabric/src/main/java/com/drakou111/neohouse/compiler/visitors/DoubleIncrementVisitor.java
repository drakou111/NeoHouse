package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.DoubleValue;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.lexer.TokenType;

/**
 * {@code d++} and {@code d--} on a double step by 1.0. The parser writes them as {@code += 1} before
 * the variable's type is known, which would be a long added to a double; once types are known (after
 * scope checking) the step becomes a double. Only ++ and -- are widened: {@code d += 1} still needs 1.0.
 */
public class DoubleIncrementVisitor extends DefaultVisitor {

    @Override
    public MainNode analyze(MainNode main) {
        return (MainNode) main.accept(this);
    }

    @Override
    public ASTNode visitAssignation(AssignationNode assignment) {
        boolean step = assignment.token != null
                && (assignment.token.type == TokenType.PLUS_PLUS || assignment.token.type == TokenType.MINUS_MINUS);
        if (!step || assignment.variable.type != VariableType.DOUBLE) return super.visitAssignation(assignment);
        ConstantPrimitiveNode one = new ConstantPrimitiveNode(new DoubleValue(1.0), VariableType.DOUBLE, assignment.token);
        return new AssignationNode(assignment.variable, assignment.operator, one, assignment.token);
    }
}
