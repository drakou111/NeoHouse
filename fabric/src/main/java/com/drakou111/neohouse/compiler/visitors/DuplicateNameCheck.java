package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.CommandNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compiler.ast.main_statements.MenuNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Rejects two functions, commands or menus with the same name, since Housing keeps one entry per
 * name and the second would overwrite the first. Loops are functions in Housing, so they share the
 * functions' names. Two blocks for the same event are rejected too: writing the second one would
 * replace (or, when keeping existing content, skip) the first.
 */
public final class DuplicateNameCheck {

    private record Definition(String kind, Token token) {}

    private DuplicateNameCheck() {}

    public static void check(MainNode main) {
        Map<String, Definition> functions = new HashMap<>();
        Map<String, Definition> commands = new HashMap<>();
        Map<String, Definition> menus = new HashMap<>();
        Map<Event, Token> events = new EnumMap<>(Event.class);
        for (MainStatementNode statement : main.statements) {
            switch (statement) {
                case LoopNode loop -> add(functions, loop.name, new Definition("loop", loop.token));
                case FunctionNode function -> add(functions, function.name, new Definition("function", function.token));
                case CommandNode command -> add(commands, command.name, new Definition("command", command.token));
                case MenuNode menu -> add(menus, menu.name, new Definition("menu", menu.token));
                case EventNode event -> addEvent(events, event);
                default -> { }
            }
        }
    }

    private static void addEvent(Map<Event, Token> seen, EventNode event) {
        Token first = seen.putIfAbsent(event.event, event.token);
        if (first == null) return;
        throw new SemanticException(event.token, "There's already an %s event block%s. Put all of its actions in one block."
                .formatted(event.event.name, where(first, event.token)));
    }

    private static String where(Token first, Token second) {
        if (first == null) return "";
        if (second != null && !first.file.equals(second.file)) return " (in " + first.file + ", line " + first.line + ")";
        return " (line " + first.line + ")";
    }

    private static void add(Map<String, Definition> seen, String name, Definition definition) {
        Definition first = seen.putIfAbsent(name, definition);
        if (first == null) return;

        String where = where(first.token(), definition.token());
        String shared = first.kind().equals(definition.kind()) ? "" : " Loops are functions in Housing, so they can't share a name.";
        throw new SemanticException(definition.token(),
                "There's already a %s named '%s'%s.%s".formatted(first.kind(), name, where, shared));
    }
}
