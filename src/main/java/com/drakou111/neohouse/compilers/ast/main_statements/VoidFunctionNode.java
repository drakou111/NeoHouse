package com.drakou111.neohouse.compilers.ast.main_statements;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.ast.main.MainStatementNode;
import com.drakou111.neohouse.compilers.ast.main.ParameterListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class VoidFunctionNode extends FunctionNode {
    public final ParameterListNode parameters;

    public VoidFunctionNode(final String name, final String description, ParameterListNode parameters, final StatementListNode statements, final Token token) {
        super(name, description, statements, token);
        this.parameters = parameters;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVoidFunction(this);
    }

    @Override
    public String toString() {
        return "VoidFunctionNode[name=" + name + ", description=" + description + ", parameters=" + parameters + ", statements=" + statements + "]";
    }

    @Override
    public String toShortString() {
        return "VoidFun[name=" + name + ", desc=" + description + ", params=" + parameters.toShortString() + ", stmts=" + statements.toShortString() + "]";
    }
}
