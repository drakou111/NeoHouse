package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.statement.RepeatNode;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.List;


public class RepeatUnrollingVisitor extends DefaultVisitor {

    @Override
    public ASTNode visitRepeat(RepeatNode repeat) {
        StatementListNode rewrittenBody = (StatementListNode) repeat.statements.accept(this);

        long count = repeat.count;
        if (count < 0) {
            throw new SemanticException(
                    repeat.token,
                    "Repeat count cannot be negative: " + count
            );
        }

        List<StatementNode> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            for (StatementNode stmt : rewrittenBody.statements) {
                out.add(stmt);
            }
        }

        return new StatementListNode(out);
    }
}
