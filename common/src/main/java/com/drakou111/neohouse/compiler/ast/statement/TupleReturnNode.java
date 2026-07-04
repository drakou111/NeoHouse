package com.drakou111.neohouse.compiler.ast.statement;

import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

import java.util.List;

public class TupleReturnNode extends ReturnNode {
    public final List<NormalExpressionNode> expressions;

    public TupleReturnNode(final List<NormalExpressionNode> expressions, final Token token) {
        super(token);
        this.expressions = expressions;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitTupleReturn(this);
    }

    @Override
    public String toString() {
        return "TupleReturnNode[expressions=" + expressions + "]";
    }

    @Override
    public String toShortString() {
        return "TupleRet[" + expressions.size() + " exprs]";
    }
}
