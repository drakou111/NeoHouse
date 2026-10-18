package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.StringValue;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.ActionCallNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringEscapeTest {

    private static String messageText(String literal) {
        VoidFunctionNode function = (VoidFunctionNode) TestCompiler.analyze("function f() { message(" + literal + "); }")
                .statements.getFirst();
        ActionCallNode call = (ActionCallNode) function.statements.statements.getFirst();
        return ((StringValue) ((ConstantPrimitiveNode) call.arguments.expressions.getFirst()).value).value;
    }

    @Test
    void escapedQuotesAndBackslashesAreUnescaped() {
        // "say \"hi\" \\ ok"  ->  say "hi" \ ok
        assertThat(messageText("\"say \\\"hi\\\" \\\\ ok\"")).isEqualTo("say \"hi\" \\ ok");
    }

    @Test
    void otherBackslashesAreKept() {
        // "a\nb" keeps its backslash
        assertThat(messageText("\"a\\nb\"")).isEqualTo("a\\nb");
    }
}
