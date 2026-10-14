package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.*;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compiler.ast.statement.RandomNode;
import com.drakou111.neohouse.compiler.ast.statement.RepeatNode;
import com.drakou111.neohouse.compiler.ast.statement.ReturnNode;
import com.drakou111.neohouse.exceptions.SemanticException;

public class DeadCodeCheckVisitor extends EmptyVisitor<Void> {

    public Void analyze(MainNode main) {
        main.accept(this);
        return null;
    }

    @Override public Void visitTypedFunction(TypedFunctionNode f) { checkBlock(f.statements); return null; }
    @Override public Void visitTupleFunction(TupleFunctionNode f) { checkBlock(f.statements); return null; }
    @Override public Void visitVoidFunction(VoidFunctionNode f)   { checkBlock(f.statements); return null; }
    @Override public Void visitLoop(LoopNode l)                   { checkBlock(l.statements); return null; }
    @Override public Void visitEvent(EventNode e)                 { checkBlock(e.statements); return null; }
    @Override public Void visitCommand(CommandNode c)             { checkBlock(c.statements); return null; }
    @Override public Void visitMenu(MenuNode m)                   { for (SlotNode s : m.slots) s.accept(this); return null; }
    @Override public Void visitSlot(SlotNode s)                   { checkBlock(s.statements); return null; }

    private void checkBlock(StatementListNode block) {
        checkBlockTerminates(block);
    }

    // Checks for dead code within the block and returns true if the block always terminates.
    private boolean checkBlockTerminates(StatementListNode block) {
        boolean terminated = false;
        for (StatementNode stmt : block.statements) {
            if (terminated) {
                throw new SemanticException(stmt.token,
                        "Unreachable code - this statement can never be executed.");
            }
            if (stmt instanceof ReturnNode) {
                terminated = true;
            } else if (stmt instanceof ConditionalNode cond) {
                boolean thenTerminates = checkBlockTerminates(cond.thenBlock);
                boolean elseTerminates = cond.elseBlock != null && checkBlockTerminates(cond.elseBlock);
                // Both branches must terminate (and else must exist) for the if to terminate the outer block
                if (thenTerminates && elseTerminates) {
                    terminated = true;
                }
            } else if (stmt instanceof RandomNode rnd) {
                boolean allTerminate = !rnd.statements.isEmpty();
                for (StatementListNode opt : rnd.statements) {
                    if (!checkBlockTerminates(opt)) allTerminate = false;
                }
                if (allTerminate) terminated = true;
            } else if (stmt instanceof RepeatNode rep) {
                checkBlockTerminates(rep.statements);
            }
        }
        return terminated;
    }
}
