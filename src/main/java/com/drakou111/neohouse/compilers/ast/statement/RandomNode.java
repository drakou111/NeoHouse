package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.helpers.StringHelper;
import com.drakou111.neohouse.compilers.lexer.Token;

import java.util.ArrayList;
import java.util.List;

public class RandomNode extends StatementNode {
    public final List<StatementListNode> statements;

    public RandomNode(final List<StatementListNode> statements, final Token token) {
        this.statements = new ArrayList<>(statements);
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitRandom(this);
    }

    @Override
    public String toString() {
        return "RandomNode [statements=" + statements + "]";
    }

    @Override
    public String toShortString() {


        return "Rand[stmts=" + StringHelper.nodeListToString(statements) + "]";
    }
}
