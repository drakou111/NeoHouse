package com.drakou111.neohouse.compiler.ast.main_statements;

import com.drakou111.neohouse.compiler.ast.main.ParameterListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class VoidFunctionNode extends FunctionNode {
    public final ParameterListNode parameters;

    public VoidFunctionNode(final String name, final String description, final String icon, ParameterListNode parameters, final StatementListNode statements, final boolean isConst, final Token token) {
        super(name, description, icon, statements, isConst, token);
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
