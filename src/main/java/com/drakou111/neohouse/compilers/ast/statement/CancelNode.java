package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;

public class CancelNode extends StatementNode {
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitCancel(this);
    }
}
