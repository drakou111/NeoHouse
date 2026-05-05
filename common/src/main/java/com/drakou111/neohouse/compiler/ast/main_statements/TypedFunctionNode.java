package com.drakou111.neohouse.compiler.ast.main_statements;

import com.drakou111.neohouse.compiler.ast.main.ParameterListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class TypedFunctionNode extends FunctionNode {
    public final VariableType returnType;
    public final ParameterListNode parameters;

    public TypedFunctionNode(final String name, final String description, final VariableType returnType, ParameterListNode parameters, final StatementListNode statements, final Token token) {
        super(name, description, statements, token);
        this.returnType = returnType;
        this.parameters = parameters;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitTypedFunction(this);
    }

    @Override
    public String toString() {
        return "TypedFunctionNode[name=" + name + ", description=" + description + ", returnType=" + returnType + ", parameters=" + parameters + ", statements=" + statements + "]";
    }

    @Override
    public String toShortString() {
        return "Fun[name=" + name + ", desc=" + description + ", return=" + returnType + ", params=" + parameters.toShortString() + ", stmts=" + statements.toShortString() + "]";
    }
}
