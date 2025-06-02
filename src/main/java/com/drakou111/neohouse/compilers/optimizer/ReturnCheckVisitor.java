package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.enums.Holder;
import com.drakou111.neohouse.compilers.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.MainNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compilers.ast.statement.*;
import com.drakou111.neohouse.exceptions.SemanticException;

/**
 * Ensures that every TypedFunctionNode has all its control-flow paths ending in a return.
 *   - An ordinary statement does not guarantee a return; we look for Return, full-coverage if/else,
 *     or a Random where every alternative ends in return.
 *   - If any path can fall through to the end without hitting a return, we throw.
 */
public class ReturnCheckVisitor extends EmptyVisitor<Void> {

    public Void analyze(MainNode main) {
        main.accept(this);
        return null;
    }

    @Override
    public Void visitTypedFunction(TypedFunctionNode function) {
        if (!allPathsReturn(function.statements)) {
            throw new SemanticException(function.token, "Function '" + function.name + "' may fall through without returning a value");
        }
        return null;
    }

    /**
     * Returns true if and only if every possible path through this StatementListNode
     * ends in a Return. Otherwise false.
     */
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
