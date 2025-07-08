package com.drakou111.neohouse.compiler.ast.statement;

import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.lexer.Token;

public class ConditionalNode extends StatementNode {
    public ExpressionNode condition;
    public StatementListNode thenBlock;
    public StatementListNode elseBlock;

    public ConditionalNode(final ExpressionNode condition, final StatementListNode thenBlock, final StatementListNode elseBlock, final Token token) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConditional(this);
    }

    @Override
    public String toString() {
        return "ConditionalNode[condition=" + condition + ", thenBlock=" + thenBlock + ", elseBlock=" + elseBlock + "]";
    }

    @Override
    public String toShortString() {
        return "Cond[cond=" + condition.toShortString() + ", then=" + thenBlock.toShortString() + ", else=" + elseBlock.toShortString() + "]";
    }
}
