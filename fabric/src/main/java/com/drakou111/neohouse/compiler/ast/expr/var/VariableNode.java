package com.drakou111.neohouse.compiler.ast.expr.var;

import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.BoolValue;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.DoubleValue;
import com.drakou111.neohouse.compiler.ast.expr.constants.LongValue;
import com.drakou111.neohouse.compiler.ast.expr.constants.StringValue;
import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.exceptions.SemanticException;

public class VariableNode extends NormalExpressionNode {
    public final Holder holder;
    public final String name;
    public final String teamName;
    public String castSuffix = null;
    /** True iff this variable was written in source as a bare identifier (no p./g./t[].). Set by parseVariable; default false. */
    public boolean wasBare = false;
    /** Optional fallback constant given in source as {@code p.name?value}: the value the Housing
     *  placeholder uses when the variable is undefined. Always a constant literal after parsing. */
    public ConstantPrimitiveNode fallback = null;

    /** The space-prefixed fallback token embedded inside a {@code %var...%} placeholder,
     *  e.g. {@code " 0"}. Empty when there is no fallback. */
    public String fallbackToken() {
        if (fallback == null) return "";
        return " " + switch (fallback.value) {
            case LongValue l   -> l.toString();
            case DoubleValue d -> d.toString();
            case BoolValue b   -> b.value ? "1" : "0";
            case StringValue s -> s.value;
        };
    }

    public VariableNode(final VariableType type, final Holder holder, final String name, final String teamName, final Token token) {
        super(type);

        this.type = type;
        this.holder = holder;
        this.name = name;
        this.teamName = teamName;
        this.token = token;

        if (name.length() > 16)
            throw new SemanticException(token, "The variable named '" + name + "' is too long.");
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVariable(this);
    }

    @Override
    public String toString() {
        return "VariableNode[type=" + type + ", holder=" + holder + ", name=" + name + ", teamName=" + teamName + "]";
    }

    @Override
    public String toShortString() {
        return "Var[type=" + type + ", hold=" + holder + ", name=" + name + ", tname=" + teamName + "]";
    }
}
