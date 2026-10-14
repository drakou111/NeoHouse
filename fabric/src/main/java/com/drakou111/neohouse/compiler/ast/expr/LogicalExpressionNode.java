package com.drakou111.neohouse.compiler.ast.expr;

import com.drakou111.neohouse.compiler.enums.LogicalOperator;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class LogicalExpressionNode extends ConditionExpressionNode {
    public final LogicalOperator operator;
    public final List<ExpressionNode> conditions;

    public LogicalExpressionNode(final List<ExpressionNode> conditions, final LogicalOperator operator, final Token token) {
        super(VariableType.BOOL);
        if (conditions == null || conditions.isEmpty())
            this.conditions = new ArrayList<>();
        else
            this.conditions = new ArrayList<>(conditions);

        this.operator = operator;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitLogicalExpr(this);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("LogicalExpressionNode[operator=" + operator + ", type=" + type + ", conditions=[");
        for (int i = 0; i < conditions.size(); i++) {
            ret.append(conditions.get(i).toString());
            if (i < conditions.size() - 1)
                ret.append(", ");
        }
        ret.append("]]");
        return ret.toString();
    }

    @Override
    public String toShortString() {
        StringBuilder ret = new StringBuilder("LExpr[op=" + operator + ", type=" + type + ", conds=[");
        for (int i = 0; i < conditions.size(); i++) {
            ret.append(conditions.get(i).toShortString());
            if (i < conditions.size() - 1)
                ret.append(", ");
        }
        ret.append("]]");
        return ret.toString();
    }
}
