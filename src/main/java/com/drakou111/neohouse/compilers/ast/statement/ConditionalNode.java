package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;

public class ConditionalNode extends StatementNode {
    public final ExpressionNode condition;
    public final StatementListNode thenBlock;
    public final StatementListNode elseBlock;

    public ConditionalNode(final ExpressionNode condition, final StatementListNode thenBlock, final StatementListNode elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConditional(this);
    }
}
