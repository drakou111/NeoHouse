package com.drakou111.neohouse.compiler.ast.main_statements;

import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class GlobalDeclarationNode extends MainStatementNode {
    public final VariableNode variable;
    public final NormalExpressionNode initializer; // null if no initializer

    public GlobalDeclarationNode(final VariableNode variable, final NormalExpressionNode initializer, final Token token) {
        this.variable = variable;
        this.initializer = initializer;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitGlobalDeclaration(this);
    }

    @Override
    public String toString() {
        return "GlobalDeclarationNode[variable=" + variable + (initializer != null ? ", initializer=" + initializer : "") + "]";
    }

    @Override
    public String toShortString() {
        return "GDecl[var=" + variable.toShortString() + "]";
    }
}
