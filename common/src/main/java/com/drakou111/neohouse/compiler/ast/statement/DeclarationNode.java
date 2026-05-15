package com.drakou111.neohouse.compiler.ast.statement;

import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class DeclarationNode extends StatementNode {
    public final VariableNode variable;

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
