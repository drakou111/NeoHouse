package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compiler.ast.statement.RandomNode;
import com.drakou111.neohouse.compiler.ast.statement.ReturnNode;
import com.drakou111.neohouse.exceptions.SemanticException;

public class ReturnCheckVisitor extends EmptyVisitor<Void> {

    public Void analyze(MainNode main) {
        main.accept(this);
        return null;
    }

    @Override
    public Void visitTypedFunction(TypedFunctionNode function) {
        if (!allPathsReturn(function.statements)) {
            throw new SemanticException(function.token, "Function '" + function.name + "' may fall through without returning a value. Make sure the function will always return a value.");
        }
        return null;
    }

    private boolean allPathsReturn(StatementListNode block) {
        // Iterate in order; once a statement is “guaranteed return,” no need to look further:
        for (StatementNode stmt : block.statements) {
            if (stmt instanceof StatementListNode list) {
                if (allPathsReturn(list))
                    return true;
            }
            if (stmt instanceof ReturnNode) {
                return true;
            }
            if (stmt instanceof ConditionalNode cond) {
                // Both branches must exist and both must guarantee return:
                StatementListNode thenBlock = cond.thenBlock;
                StatementListNode elseBlock = cond.elseBlock;
                if (elseBlock == null) {
                    // No else => some path falls through
                    continue;
                }
                boolean thenReturns = allPathsReturn(thenBlock);
                boolean elseReturns = allPathsReturn(elseBlock);
                if (thenReturns && elseReturns) {
                    // This if‐statement guarantees return regardless of condition
                    return true;
                } else {
                    // One branch or both might fall through; keep scanning next statements
                    continue;
                }
            }
            if (stmt instanceof RandomNode rnd) {
                // Every alternative block in “random” must guarantee a return:
                boolean everyOptionReturns = true;
                for (StatementListNode option : rnd.statements) {
                    if (!allPathsReturn(option)) {
                        everyOptionReturns = false;
                        break;
                    }
                }
                if (everyOptionReturns) {
                    return true;
                } else {
                    continue;
                }
            }
        }
        return false;
    }
}
