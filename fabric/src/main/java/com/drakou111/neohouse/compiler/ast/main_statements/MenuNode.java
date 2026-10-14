package com.drakou111.neohouse.compiler.ast.main_statements;

import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

import java.util.List;

public class MenuNode extends MainStatementNode {
    public final String name;
    public final int rows;
    public final List<SlotNode> slots;

    public MenuNode(String name, int rows, List<SlotNode> slots, Token token) {
        this.name = name;
        this.rows = rows;
        this.slots = slots;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitMenu(this);
    }

    @Override
    public String toString() {
        return "MenuNode[name=" + name + ", rows=" + rows + ", slots=" + slots + "]";
    }

    @Override
    public String toShortString() {
        return "Menu[" + name + ", rows=" + rows + "]";
    }
}
