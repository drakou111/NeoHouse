package com.drakou111.neohouse.compiler.ast.expr.var;

import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.main.ConstantPrimitiveList;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class PlaceholderNode extends NormalExpressionNode {

    public final String placeholder;
    public final ConstantPrimitiveList constants;

    public PlaceholderNode(final String placeholder, final ConstantPrimitiveList constants, VariableType type, final Token token) {
        super(type);
        this.placeholder = placeholder;
        this.constants = constants;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitPlaceholder(this);
    }

    @Override
    public String toString() {
        return "PlaceholderNode [placeholder=" + placeholder + ", type=" + type + ", constants=" + constants + "]";
    }

    @Override
    public String toShortString() {
        return "PH [ph=" + placeholder + ", type=" + type + ", consts=" + constants.toShortString() + "]";
    }
}
