package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.CommandNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compiler.ast.main_statements.MenuNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.HashMap;
import java.util.Map;

/**
 * Rejects two functions, commands or menus with the same name, since Housing keeps one entry per
 * name and the second would overwrite the first. Loops are functions in Housing, so they share the
 * functions' names.
 */
public final class DuplicateNameCheck {

    private record Definition(String kind, Token token) {}

    private DuplicateNameCheck() {}

    public static void check(MainNode main) {
        Map<String, Definition> functions = new HashMap<>();
        Map<String, Definition> commands = new HashMap<>();
        Map<String, Definition> menus = new HashMap<>();
        for (MainStatementNode statement : main.statements) {
            switch (statement) {
                case LoopNode loop -> add(functions, loop.name, new Definition("loop", loop.token));
                case FunctionNode function -> add(functions, function.name, new Definition("function", function.token));
                case CommandNode command -> add(commands, command.name, new Definition("command", command.token));
                case MenuNode menu -> add(menus, menu.name, new Definition("menu", menu.token));
                default -> { }
            }
        }
    }

    private static void add(Map<String, Definition> seen, String name, Definition definition) {
        Definition first = seen.putIfAbsent(name, definition);
        if (first == null) return;

        Token firstToken = first.token();
        String where = "";
        if (firstToken != null && definition.token() != null && !firstToken.file.equals(definition.token().file))
            where = " (in " + firstToken.file + ", line " + firstToken.line + ")";
        else if (firstToken != null)
            where = " (line " + firstToken.line + ")";
        String shared = first.kind().equals(definition.kind()) ? "" : " Loops are functions in Housing, so they can't share a name.";
        throw new SemanticException(definition.token(),
                "There's already a %s named '%s'%s.%s".formatted(first.kind(), name, where, shared));
    }
}
