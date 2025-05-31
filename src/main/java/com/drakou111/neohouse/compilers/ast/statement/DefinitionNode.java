package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class DefinitionNode extends StatementNode {
     public final String name;
     public final String definition;

     public DefinitionNode(final String name, final String definition, final Token token) {
         this.name = name;
         this.definition = definition;
         this.token = token;
     }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitDefinition(this);
    }

    @Override
    public String toString() {
         return "DefinitionNode[name=" + name + ", definition=" + definition + "]";
    }

    @Override
    public String toShortString() {
        return "Def[name=" + name + ", def=" + definition + "]";
    }
}
