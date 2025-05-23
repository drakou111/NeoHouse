package com.drakou111.neohouse.compilers.ast.function;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.FunctionType;
import com.drakou111.neohouse.compilers.ast.main.ParameterListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;

public class AutomaticFunctionNode extends FunctionNode {
    public final int tickDelay;

    public AutomaticFunctionNode(final String name, final String description, final FunctionType returnType, final ParameterListNode parameters, final StatementListNode statements, final int delay) {
        super(name, description, returnType, parameters, statements);
        this.tickDelay = delay;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitAutomaticFunction(this);
    }
}
