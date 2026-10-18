package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.MenuNode;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MenuTitleTest {

    private static MenuNode onlyMenu(MainNode main) {
        for (MainStatementNode statement : main.statements)
            if (statement instanceof MenuNode menu) return menu;
        throw new AssertionError("no menu in " + main);
    }

    @Test
    void aQuotedTitleKeepsSpacesAndAmpersandsAsTyped() {
        MenuNode menu = onlyMenu(TestCompiler.analyze("menu \"Item &aShop\"(3) { slot0: ({id:\"minecraft:stone\"}) { closeMenu(); } }"));

        assertThat(menu.name).isEqualTo("Item &aShop");
        assertThat(menu.rows).isEqualTo(3);
        new CodeGenerator(OverrideMode.ALWAYS).generate(TestCompiler.analyze("menu \"Item Shop\"(1) { }"));
    }

    @Test
    void aPlainNameStillWorks() {
        assertThat(onlyMenu(TestCompiler.analyze("menu shop(1) { }")).name).isEqualTo("shop");
    }

    @Test
    void anEmptyTitleIsRejected() {
        assertThatThrownBy(() -> TestCompiler.analyze("menu \"  \"(1) { }"))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("Menu title can't be empty");
    }

    @Test
    void helpersSplitOffAQuotedTitleMenuGetValidFunctionNames() {
        StringBuilder source = new StringBuilder("menu \"Big Shop!\"(1) { slot0: ({id:\"minecraft:stone\"}) {\n");
        // More messages than 25 conditionals can hold, so a helper function is needed.
        for (int i = 0; i < 600; i++) source.append("message(\"").append(i).append("\");\n");
        source.append("} }");

        NHCompiler compiler = new NHCompiler();
        compiler.analyze(source.toString(), null, TestCompiler.DEFAULT_OPTIONS);

        assertThat(compiler.getLastHelperNames()).isNotEmpty()
                .allSatisfy(name -> assertThat(name).startsWith("Big_Shop__slot0_s$"));
    }
}
