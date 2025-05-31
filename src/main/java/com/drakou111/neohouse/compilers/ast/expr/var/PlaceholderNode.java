package com.drakou111.neohouse.compilers.ast.expr.var;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.ConstantPrimitiveList;
import com.drakou111.neohouse.compilers.lexer.Token;

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
