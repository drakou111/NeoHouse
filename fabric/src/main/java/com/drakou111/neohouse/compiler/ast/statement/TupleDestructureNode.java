package com.drakou111.neohouse.compiler.ast.statement;

import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

import java.util.List;

/** Represents {@code (p.x, p.y) op= func(args);}. Assigns to existing variables. */
public class TupleDestructureNode extends StatementNode {
    public final List<VariableNode> targets;
    public final AssignmentOperator operator;
    public final String functionName;
    public final ArgumentListNode arguments;

    public TupleDestructureNode(final List<VariableNode> targets, final AssignmentOperator operator,
                                 final String functionName, final ArgumentListNode arguments,
                                 final Token token) {
        this.targets = targets;
        this.operator = operator;
        this.functionName = functionName;
        this.arguments = arguments;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitTupleDestructure(this);
    }

    @Override
    public String toString() {
        return "TupleDestructureNode[fn=" + functionName + ", op=" + operator + ", targets=" + targets + "]";
    }

    @Override
    public String toShortString() {
        return "TupleDestr[fn=" + functionName + ", op=" + operator + ", n=" + targets.size() + "]";
    }
}
