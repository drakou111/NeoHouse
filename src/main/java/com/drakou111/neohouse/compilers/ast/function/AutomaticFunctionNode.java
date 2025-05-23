package com.drakou111.neohouse.compilers.ast.function;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.FunctionType;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.statement.StatementNode;

import java.util.List;

public class AutomaticFunctionNode extends FunctionNode {
    public final int tickDelay;

    public AutomaticFunctionNode(String name, String description, FunctionType returnType, List<VariableNode> parameters, List<StatementNode> statements, int delay) {
        super(name, description, returnType, parameters, statements);
        this.tickDelay = delay;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitAutomaticFunction(this);
    }
}
