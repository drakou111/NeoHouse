package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.DoubleValue;
import com.drakou111.neohouse.compiler.ast.expr.constants.LongValue;
import com.drakou111.neohouse.compiler.enums.BinaryOperator;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.lexer.Token;

import java.util.Optional;

/**
 * Computes arithmetic on two numeric constants at compile time ({@code 1.0 / 2.0} becomes
 * {@code 0.5}) and casts of constants ({@code (long) 2.7} is {@code 2}), so no temp variable and no
 * actions are spent on it. Longs wrap like Java's 64-bit arithmetic and doubles follow Java's IEEE
 * rules, as on the server. Division or modulo by a constant zero and the bitwise operators are
 * left for Housing to evaluate.
 */
final class ConstantFolding {

    private ConstantFolding() {
    }

    /** The folded constant, or empty when {@code left op right} isn't foldable. */
    static Optional<ConstantPrimitiveNode> fold(BinaryOperator op, ASTNode left, ASTNode right, Token token) {
        if (!(left instanceof ConstantPrimitiveNode l) || !(right instanceof ConstantPrimitiveNode r)) return Optional.empty();

        if (l.value instanceof LongValue a && r.value instanceof LongValue b) {
            Optional<Long> value = foldLong(op, a.value, b.value);
            return value.map(v -> new ConstantPrimitiveNode(new LongValue(v), VariableType.LONG, token));
        }
        if (l.value instanceof DoubleValue a && r.value instanceof DoubleValue b) {
            Optional<Double> value = foldDouble(op, a.value, b.value);
            return value.map(v -> new ConstantPrimitiveNode(new DoubleValue(v), VariableType.DOUBLE, token));
        }
        return Optional.empty();
    }

    /** A numeric constant cast to LONG (truncating) or DOUBLE, or empty when {@code value} isn't one. */
    static Optional<ConstantPrimitiveNode> cast(ASTNode value, VariableType target, Token token) {
        if (!(value instanceof ConstantPrimitiveNode c)) return Optional.empty();
        double asDouble;
        if (c.value instanceof LongValue l) asDouble = l.value;
        else if (c.value instanceof DoubleValue d) asDouble = d.value;
        else return Optional.empty();
        return switch (target) {
            case LONG -> Optional.of(new ConstantPrimitiveNode(new LongValue(c.value instanceof LongValue l ? l.value : (long) asDouble), VariableType.LONG, token));
            case DOUBLE -> Optional.of(new ConstantPrimitiveNode(new DoubleValue(asDouble), VariableType.DOUBLE, token));
            default -> Optional.empty();
        };
    }

    static Optional<Long> foldLong(BinaryOperator op, long a, long b) {
        return switch (op) {
            case ADD -> Optional.of(a + b);
            case SUBTRACT -> Optional.of(a - b);
            case MULTIPLY -> Optional.of(a * b);
            case DIVIDE -> b == 0 ? Optional.empty() : Optional.of(a / b);
            case MODULO -> b == 0 ? Optional.empty() : Optional.of(a % b);
            default -> Optional.empty();
        };
    }

    static Optional<Double> foldDouble(BinaryOperator op, double a, double b) {
        return switch (op) {
            case ADD -> Optional.of(a + b);
            case SUBTRACT -> Optional.of(a - b);
            case MULTIPLY -> Optional.of(a * b);
            case DIVIDE -> b == 0.0 ? Optional.empty() : Optional.of(a / b);
            case MODULO -> b == 0.0 ? Optional.empty() : Optional.of(a % b);
            default -> Optional.empty();
        };
    }
}
