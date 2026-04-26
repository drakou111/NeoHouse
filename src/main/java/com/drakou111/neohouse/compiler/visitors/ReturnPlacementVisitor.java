package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidReturnNode;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.List;

public class ReturnPlacementVisitor extends DefaultVisitor {

    @Override
    public ASTNode visitVoidFunction(VoidFunctionNode fn) {
        return super.visitVoidFunction(new VoidFunctionNode(fn.name, fn.description, fn.parameters, stripReturn(fn.statements), fn.token));
    }

    @Override
    public ASTNode visitEvent(EventNode event) {
        return super.visitEvent(new EventNode(event.event, stripReturn(event.statements), event.token));
    }

    @Override
    public ASTNode visitLoop(LoopNode loop) {
        return super.visitLoop(new LoopNode(loop.name, loop.description, stripReturn(loop.statements), loop.tickDelay, loop.token));
    }

    private StatementListNode stripReturn(StatementListNode stmts) {
        List<StatementNode> list = stmts.statements;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof VoidReturnNode ret) {
                if (i < list.size() - 1) {
                    throw new SemanticException(ret.token,
                            "return can only appear inside a conditional/random block or as the very last statement");
                }
                return new StatementListNode(new ArrayList<>(list.subList(0, list.size() - 1)));
            }
        }
        return stmts;
    }
}
