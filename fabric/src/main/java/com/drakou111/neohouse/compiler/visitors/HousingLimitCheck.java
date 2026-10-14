package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.CommandNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.MenuNode;
import com.drakou111.neohouse.constants.HousingList;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.EnumMap;
import java.util.Map;

/**
 * Rejects a script that on its own defines more functions (including loops and compiler-made
 * helpers), commands or menus than a house can hold. Run after every pass that adds helpers.
 */
public final class HousingLimitCheck {
    private HousingLimitCheck() {}

    public static void check(MainNode main) {
        Map<HousingList, Integer> counts = new EnumMap<>(HousingList.class);
        for (MainStatementNode statement : main.statements) {
            HousingList list = switch (statement) {
                case FunctionNode ignored -> HousingList.FUNCTIONS;
                case CommandNode ignored -> HousingList.COMMANDS;
                case MenuNode ignored -> HousingList.MENUS;
                default -> null;
            };
            if (list == null) continue;

            int count = counts.merge(list, 1, Integer::sum);
            if (count > list.maxEntries)
                throw new SemanticException(statement.token, "This script defines more than %d %ss (including generated helpers), which is the most Housing allows."
                        .formatted(list.maxEntries, list.singularName));
        }
    }
}
