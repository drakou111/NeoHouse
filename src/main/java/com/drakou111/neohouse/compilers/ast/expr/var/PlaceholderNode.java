package com.drakou111.neohouse.compilers.ast.expr.var;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.ConstantPrimitiveList;
import com.drakou111.neohouse.compilers.lexer.Token;

public class PlaceholderNode extends ExpressionNode {

    public final String placeholder;
    public final ConstantPrimitiveList constants;

    public PlaceholderNode(final String placeholder, final ConstantPrimitiveList constants, final Token token) {
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
        return "PlaceholderNode [placeholder=" + placeholder + ", constants=" + constants + "]";
    }

    @Override
    public String toShortString() {
        return "PH [ph=" + placeholder + ", consts=" + constants.toShortString() + "]";
    }
}
