package com.drakou111.neohouse.compiler.ast.statement;

import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class AssignationNode extends StatementNode {
    public final VariableNode variable;
    public final AssignmentOperator operator;
    public NormalExpressionNode value;

    public AssignationNode(final VariableNode variable, final AssignmentOperator operator, final NormalExpressionNode value, final Token token) {
        this.variable = variable;
        this.operator = operator;
        this.value = value;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitAssignation(this);
    }

    @Override
    public String toString() {
        return "AssignationNode[variable=" + variable + ", operator=" + operator + ", value=" + value + "]";
    }

    @Override
    public String toShortString() {
        return "Agn[var=" + variable.toShortString() + ", op=" + operator + ", v=" + value.toShortString() + "]";
    }
}
