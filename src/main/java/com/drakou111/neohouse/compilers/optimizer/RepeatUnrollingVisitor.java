package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.MainNode;
import com.drakou111.neohouse.compilers.ast.main.MainStatementNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.ast.statement.RepeatNode;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.List;

/**
 * A visitor that replaces each "repeat (count) { ... }" with exactly `count` copies
 * of its inner statements.  If count ≤ 0, it emits no statements.
 */
public class RepeatUnrollingVisitor extends DefaultVisitor {

    // Unroll a RepeatNode into multiple copies of its body
    @Override
    public ASTNode visitRepeat(RepeatNode repeat) {
        // First, recursively unroll any inner repeats inside the body
        StatementListNode rewrittenBody = (StatementListNode) repeat.statements.accept(this);

        long count = repeat.count;
        if (count < 0) {
            throw new SemanticException(
                    repeat.token,
                    "Repeat count cannot be negative: " + count
            );
        }

        // Now duplicate the body 'count' times
        List<StatementNode> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // Make sure to clone or re‐visit each statement so nested repeats inside get expanded
            for (StatementNode stmt : rewrittenBody.statements) {
                // stmt.accept(this) is not needed here because we've already visited the whole block above.
                out.add(stmt);
            }
        }

        return new StatementListNode(out);
    }
}
