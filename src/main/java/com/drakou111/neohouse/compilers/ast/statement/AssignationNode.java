package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.AssignmentOperator;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class AssignationNode extends StatementNode {
    public final VariableNode variable;
    public final AssignmentOperator operator;
    public ExpressionNode value;

    public AssignationNode(final VariableNode variable, final AssignmentOperator operator, final ExpressionNode value, final Token token) {
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
