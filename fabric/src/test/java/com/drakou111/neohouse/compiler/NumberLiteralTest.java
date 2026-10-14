package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.LongValue;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NumberLiteralTest {

    private static long assignedValue(String literal) {
        MainNode main = TestCompiler.analyze("function f() { p.x = " + literal + "; }");
        VoidFunctionNode function = (VoidFunctionNode) main.statements.getFirst();
        AssignationNode assignment = (AssignationNode) function.statements.statements.getFirst();
        return ((LongValue) ((ConstantPrimitiveNode) assignment.value).value).value;
    }

    @ParameterizedTest
    @CsvSource({
            "0xFFFF00, 16776960",
            "0XffFF00, 16776960",
            "0b010101, 21",
            "0B11, 3",
            "1_000_000, 1000000",
            "0xFF_FF, 65535",
            "0xFFFFFFFFFFFFFFFF, -1",
            "-9223372036854775808, -9223372036854775808",
            "-0x10, -16"
    })
    void parsesLongLiterals(String literal, long expected) {
        assertThat(assignedValue(literal)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"9223372036854775808", "0x1FFFFFFFFFFFFFFFF", "0b11111111111111111111111111111111111111111111111111111111111111111"})
    void rejectsLiteralsThatDoNotFit(String literal) {
        assertThatThrownBy(() -> assignedValue(literal)).isInstanceOf(CompilingException.class);
    }
}
