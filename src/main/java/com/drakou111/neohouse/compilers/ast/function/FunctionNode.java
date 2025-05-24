package com.drakou111.neohouse.compilers.ast.function;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.ast.main.MainStatementNode;
import com.drakou111.neohouse.compilers.ast.main.ParameterListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;

public class FunctionNode extends MainStatementNode {
    public final String name;
    public final String description;
    public final VariableType returnType;
    public final ParameterListNode parameters;
    public final StatementListNode statements;

    public FunctionNode(final String name, final String description, final VariableType returnType, ParameterListNode parameters, final StatementListNode statements) {
        this.name = name;
        this.description = description;
        this.returnType = returnType;
        this.parameters = parameters;
        this.statements = statements;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFunction(this);
    }
}
