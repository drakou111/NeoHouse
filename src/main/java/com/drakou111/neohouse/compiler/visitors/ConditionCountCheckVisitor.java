package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.exceptions.SemanticException;

public class ConditionCountCheckVisitor extends EmptyVisitor<Void> {

    public void analyze(MainNode main) {
        main.accept(this);
    }

    @Override
    public Void visitStatements(StatementListNode stmts) {
        int count = 0;
        for (StatementNode stmt : stmts.statements) {
            if (stmt instanceof ConditionalNode cond) {
                count++;
                if (count > AddAction.CONDITIONAL.maxCount) {
                    throw new SemanticException(cond.token,
                            "Too many conditionals in one block (" + count + " exceeds the limit of " + AddAction.CONDITIONAL.maxCount + "). Split the logic into multiple functions.");
                }
                cond.thenBlock.accept(this);
                if (cond.elseBlock != null) cond.elseBlock.accept(this);
            } else {
                stmt.accept(this);
            }
        }
        return null;
    }
}
