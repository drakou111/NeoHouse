package com.drakou111.neohouse.compilers.ast.main_statements;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.ast.main.MainStatementNode;
import com.drakou111.neohouse.compilers.ast.main.ParameterListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class FunctionNode extends MainStatementNode {
    public final String name;
    public final String description;
    public final VariableType returnType;
    public final ParameterListNode parameters;
    public final StatementListNode statements;

    public FunctionNode(final String name, final String description, final VariableType returnType, ParameterListNode parameters, final StatementListNode statements, final Token token) {
        this.name = name;
        this.description = description;
        this.returnType = returnType;
        this.parameters = parameters;
        this.statements = statements;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFunction(this);
    }

    @Override
    public String toString() {
        return "FunctionNode [name=" + name + ", description=" + description + ", returnType=" + returnType + ", parameters=" + parameters + ", statements=" + statements + "]";
    }

    @Override
    public String toShortString() {
        return "Fun[name=" + name + ", desc=" + description + ", return=" + returnType + ", params=" + parameters.toShortString() + ", stmts=" + statements.toShortString() + "]";
    }
}
