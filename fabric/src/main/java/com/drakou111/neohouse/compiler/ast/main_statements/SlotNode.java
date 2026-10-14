package com.drakou111.neohouse.compiler.ast.main_statements;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class SlotNode extends ASTNode {
    public final int slotId;
    public final String item;
    public final StatementListNode statements;

    public SlotNode(int slotId, String item, StatementListNode statements, Token token) {
        this.slotId = slotId;
        this.item = item;
        this.statements = statements;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitSlot(this);
    }

    @Override
    public String toString() {
        return "SlotNode[slotId=" + slotId + ", statements=" + statements + "]";
    }

    @Override
    public String toShortString() {
        return "Slot[" + slotId + ", stmts=" + statements.toShortString() + "]";
    }
}
