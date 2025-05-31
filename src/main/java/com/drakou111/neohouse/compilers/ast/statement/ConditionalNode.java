package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ConditionExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class ConditionalNode extends StatementNode {
    public ConditionExpressionNode condition;
    public final StatementListNode thenBlock;
    public final StatementListNode elseBlock;

    public ConditionalNode(final ConditionExpressionNode condition, final StatementListNode thenBlock, final StatementListNode elseBlock, final Token token) {
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
