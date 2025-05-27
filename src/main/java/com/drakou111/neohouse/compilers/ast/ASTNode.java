package com.drakou111.neohouse.compilers.ast;

import com.drakou111.neohouse.compilers.lexer.Token;

public abstract class ASTNode {
    public Token token;
    public abstract <T> T accept(ASTVisitor<T> visitor);
    public abstract String toString();
    public abstract String toShortString();
}