package com.drakou111.neohouse.compilers.ast.main_statements;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.main.MainStatementNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class GlobalDeclarationNode extends MainStatementNode {
    public VariableNode variable;

    public GlobalDeclarationNode(final VariableNode variable, final Token token) {
        this.variable = variable;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitGlobalDeclaration(this);
    }

    @Override
    public String toString() {
        return "GlobalDeclarationNode[variable=" + variable + "]";
    }

    @Override
    public String toShortString() {
        return "GDecl[var=" + variable.toShortString() + "]";
    }
}
