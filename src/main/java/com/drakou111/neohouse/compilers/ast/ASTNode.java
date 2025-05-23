package com.drakou111.neohouse.compilers.ast;

public abstract class ASTNode {
    public abstract <T> T accept(ASTVisitor<T> visitor);
}