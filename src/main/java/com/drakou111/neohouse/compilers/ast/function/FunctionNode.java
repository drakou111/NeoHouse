package com.drakou111.neohouse.compilers.ast.function;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.FunctionType;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.statement.MainStatementNode;
import com.drakou111.neohouse.compilers.ast.statement.StatementNode;

import java.util.ArrayList;
import java.util.List;

public class FunctionNode extends MainStatementNode {
    public final String name;
    public final String description;
    public final FunctionType returnType;
    public final List<VariableNode> parameters;
    public final List<StatementNode> statements;

    public FunctionNode(String name, String description, FunctionType returnType, List<VariableNode> parameters, List<StatementNode> statements) {
        this.name = name;
        this.description = description;
        this.returnType = returnType;
        this.parameters = new ArrayList<>(parameters);
        this.statements = new ArrayList<>(statements);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFunction(this);
    }
}
