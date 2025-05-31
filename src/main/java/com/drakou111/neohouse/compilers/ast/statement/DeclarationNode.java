package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class DeclarationNode extends StatementNode {
    public VariableNode variable;

    public DeclarationNode(final VariableNode variable, final Token token) {
        this.variable = variable;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitDeclaration(this);
    }

    @Override
    public String toString() {
        return "DeclarationNode[variable=" + variable + "]";
    }

    @Override
    public String toShortString() {
        return "Decl[var=" + variable.toShortString() + "]";
    }
}
