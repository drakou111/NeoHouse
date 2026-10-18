package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.CommandNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compiler.ast.main_statements.MenuNode;

import java.util.ArrayList;
import java.util.List;

/**
 * What a script holds, and how many Housing functions it compiles into (helpers split off to stay
 * within the action limits and function copies included).
 */
public record ScriptSummary(int functions, int compiledFunctions, int loops, int commands, int menus, int events) {

    public static final ScriptSummary EMPTY = new ScriptSummary(0, 0, 0, 0, 0, 0);

    /** {@code parsed} is the script as written (imports included), {@code compiled} after every pass. */
    static ScriptSummary of(MainNode parsed, MainNode compiled) {
        int functions = 0;
        int loops = 0;
        int commands = 0;
        int menus = 0;
        int events = 0;
        for (MainStatementNode statement : parsed.statements) {
            switch (statement) {
                case LoopNode ignored -> loops++;
                case FunctionNode ignored -> functions++;
                case CommandNode ignored -> commands++;
                case MenuNode ignored -> menus++;
                case EventNode ignored -> events++;
                default -> { }
            }
        }

        int compiledFunctions = 0;
        for (MainStatementNode statement : compiled.statements)
            if (statement instanceof FunctionNode && !(statement instanceof LoopNode)) compiledFunctions++;
        return new ScriptSummary(functions, compiledFunctions, loops, commands, menus, events);
    }

    /** For example "3 functions (5 once compiled), 1 loop, 2 menus", or "nothing" for an empty script. */
    public String describe() {
        List<String> parts = new ArrayList<>();
        if (this.functions > 0 || this.compiledFunctions > 0) {
            String compiled = this.compiledFunctions == this.functions ? "" : " (" + this.compiledFunctions + " once compiled)";
            parts.add(count(this.functions, "function") + compiled);
        }
        if (this.loops > 0) parts.add(count(this.loops, "loop"));
        if (this.commands > 0) parts.add(count(this.commands, "command"));
        if (this.menus > 0) parts.add(count(this.menus, "menu"));
        if (this.events > 0) parts.add(count(this.events, "event"));
        return parts.isEmpty() ? "nothing" : String.join(", ", parts);
    }

    private static String count(int amount, String noun) {
        return amount + " " + noun + (amount == 1 ? "" : "s");
    }
}
