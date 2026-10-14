package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ImportGraph;
import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.ParameterListNode;
import com.drakou111.neohouse.compiler.ast.main_statements.CommandNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compiler.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compiler.ast.main_statements.MenuNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TupleFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.DeclarationNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.TupleDestructureNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Rejects uses of functions and top-level variable declarations that live in a file the using
 * file doesn't import (directly or transitively). Read-only: returns the tree unchanged.
 */
public class ImportVisibilityVisitor extends DefaultVisitor {
    private final ImportGraph imports;
    private final Map<String, Token> functionDefinitions = new HashMap<>();
    private final Map<String, Token> topLevelVariables = new HashMap<>();
    private final Set<String> localVariables = new HashSet<>();

    public ImportVisibilityVisitor(ImportGraph imports, MainNode main) {
        this.imports = imports;
        for (MainStatementNode statement : main.statements) {
            if (statement instanceof FunctionNode function) this.functionDefinitions.putIfAbsent(function.name, function.token);
            else if (statement instanceof GlobalDeclarationNode declaration)
                this.topLevelVariables.putIfAbsent(fullName(declaration.variable), declaration.variable.token);
        }
    }

    @Override
    public MainNode analyze(MainNode main) {
        main.accept(this);
        return main;
    }

    @Override
    public ASTNode visitVoidFunction(VoidFunctionNode function) {
        enterBody(function.parameters);
        return super.visitVoidFunction(function);
    }

    @Override
    public ASTNode visitTypedFunction(TypedFunctionNode function) {
        enterBody(function.parameters);
        return super.visitTypedFunction(function);
    }

    @Override
    public ASTNode visitTupleFunction(TupleFunctionNode function) {
        enterBody(function.parameters);
        return super.visitTupleFunction(function);
    }

    @Override
    public ASTNode visitLoop(LoopNode loop) {
        enterBody(null);
        return super.visitLoop(loop);
    }

    @Override
    public ASTNode visitEvent(EventNode event) {
        enterBody(null);
        return super.visitEvent(event);
    }

    @Override
    public ASTNode visitCommand(CommandNode command) {
        enterBody(null);
        return super.visitCommand(command);
    }

    @Override
    public ASTNode visitMenu(MenuNode menu) {
        enterBody(null);
        return super.visitMenu(menu);
    }

    @Override
    public ASTNode visitDeclaration(DeclarationNode declaration) {
        this.localVariables.add(fullName(declaration.variable));
        return declaration;
    }

    @Override
    public ASTNode visitGlobalDeclaration(GlobalDeclarationNode declaration) {
        if (declaration.initializer != null) declaration.initializer.accept(this);
        return declaration;
    }

    @Override
    public ASTNode visitVariable(VariableNode variable) {
        String name = fullName(variable);
        Token declaration = this.topLevelVariables.get(name);
        if (declaration != null && !this.localVariables.contains(name))
            requireVisible(variable.token, declaration, "Variable '" + name + "'");
        return variable;
    }

    @Override
    public ASTNode visitFunctionCall(FunctionCallNode call) {
        requireFunctionVisible(call.token, call.functionName);
        return super.visitFunctionCall(call);
    }

    @Override
    public ASTNode visitVoidFunctionCall(VoidFunctionCallNode call) {
        requireFunctionVisible(call.token, call.functionName);
        return super.visitVoidFunctionCall(call);
    }

    @Override
    public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode call) {
        requireFunctionVisible(call.token, call.functionName);
        return super.visitGlobalFunctionCall(call);
    }

    @Override
    public ASTNode visitTupleDestructure(TupleDestructureNode node) {
        requireFunctionVisible(node.token, node.functionName);
        for (VariableNode target : node.targets) visitVariable(target);
        return super.visitTupleDestructure(node);
    }

    private void enterBody(ParameterListNode parameters) {
        this.localVariables.clear();
        if (parameters == null) return;
        for (VariableNode parameter : parameters.parameters) this.localVariables.add(fullName(parameter));
    }

    private void requireFunctionVisible(Token use, String functionName) {
        Token definition = this.functionDefinitions.get(functionName);
        if (definition != null) requireVisible(use, definition, "Function '" + functionName + "'");
    }

    private void requireVisible(Token use, Token declaration, String what) {
        if (this.imports.canSee(use.file, declaration.file)) return;
        throw new SemanticException(use, "%s is defined in '%s', which '%s' does not import".formatted(what, declaration.file, use.file));
    }

    private static String fullName(VariableNode variable) {
        return variable.holder + "." + variable.name;
    }
}
