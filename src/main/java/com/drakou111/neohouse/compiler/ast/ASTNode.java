package com.drakou111.neohouse.compiler.ast;

import com.drakou111.neohouse.compiler.lexer.Token;

public abstract class ASTNode {
    public Token token;
    public abstract <T> T accept(ASTVisitor<T> visitor);
    public abstract String toString();
    public abstract String toShortString();
}