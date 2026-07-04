package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.ParameterListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main_statements.*;
import com.drakou111.neohouse.compiler.ast.statement.*;
import com.drakou111.neohouse.compiler.ast.statement.TupleDestructureNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.*;

public class ScopeCheckingVisitor extends DefaultVisitor {
    private final Map<String, VariableType> globalDecls;
    private final Set<String> allLocalNames;
    private final Deque<Map<String, VariableType>> scopeStack = new ArrayDeque<>();
    private boolean inFunction = false;

    public ScopeCheckingVisitor(Map<String, VariableType> globals, Set<String> locals) {
        this.globalDecls = globals;
        this.allLocalNames = locals;
    }

    public MainNode analyze(MainNode main) {
        scopeStack.push(new HashMap<>(globalDecls));
        ASTNode result = main.accept(this);
        scopeStack.pop();
        return (MainNode) result;
    }

    private VariableType resolveOrDeclareGlobal(String fullname, Token token, VariableNode varNode) {
        for (Map<String, VariableType> scope : scopeStack) {
            if (scope.containsKey(fullname)) {
                VariableType vt = scope.get(fullname);
                if (varNode != null) varNode.type = vt;
                return vt;
            }
        }
        if (allLocalNames.contains(fullname)) {
            throw new SemanticException(token, "Ambiguous reference to '" + fullname + "' (declared locally elsewhere, but not in this scope)");
        }
        if (globalDecls.containsKey(fullname)) {
            VariableType vt = globalDecls.get(fullname);
            scopeStack.getLast().put(fullname, vt);
            if (varNode != null) varNode.type = vt;
            return vt;
        }
        VariableType vt = VariableType.VAR;
        globalDecls.put(fullname, vt);
        scopeStack.getLast().put(fullname, vt);
        if (varNode != null) varNode.type = vt;
        return vt;
    }

    @Override
    public ASTNode visitGlobalDeclaration(GlobalDeclarationNode node) {
        String fullname = node.variable.holder + "." + node.variable.name;
        scopeStack.peek().put(fullname, node.variable.type);

        VariableNode varCopy = new VariableNode(node.variable.type, node.variable.holder, node.variable.name, null, node.variable.token);
        NormalExpressionNode newInit = node.initializer != null
                ? (NormalExpressionNode) node.initializer.accept(this)
                : null;
        return new GlobalDeclarationNode(varCopy, newInit, node.token);
    }

    @Override
    public ASTNode visitTypedFunction(TypedFunctionNode fn) {
        Map<String, VariableType> localScope = new HashMap<>();
        for (VariableNode param : fn.parameters.parameters) {
            String fullname = param.holder + "." + param.name;
            localScope.put(fullname, param.type);
        }
        scopeStack.push(localScope);
        StatementListNode newBody = (StatementListNode) fn.statements.accept(this);
        scopeStack.pop();

        List<VariableNode> newParams = new ArrayList<>();
        for (VariableNode param : fn.parameters.parameters) {
            newParams.add(new VariableNode(param.type, param.holder, param.name, param.teamName, param.token));
        }
        ParameterListNode newParamList = new ParameterListNode(newParams);
        return new TypedFunctionNode(fn.name, fn.description, fn.icon, fn.returnType, newParamList, newBody, fn.isConst, fn.token);
    }

    @Override
    public ASTNode visitTupleFunction(TupleFunctionNode fn) {
        Map<String, VariableType> localScope = new HashMap<>();
        for (VariableNode param : fn.parameters.parameters) {
            String fullname = param.holder + "." + param.name;
            localScope.put(fullname, param.type);
        }
        scopeStack.push(localScope);
        StatementListNode newBody = (StatementListNode) fn.statements.accept(this);
        scopeStack.pop();

        List<VariableNode> newParams = new ArrayList<>();
        for (VariableNode param : fn.parameters.parameters) {
            newParams.add(new VariableNode(param.type, param.holder, param.name, param.teamName, param.token));
        }
        return new TupleFunctionNode(fn.name, fn.description, fn.icon, fn.returnTypes, new ParameterListNode(newParams), newBody, fn.isConst, fn.token);
    }

    @Override
    public ASTNode visitVoidFunction(VoidFunctionNode fn) {
        Map<String, VariableType> localScope = new HashMap<>();
        for (VariableNode param : fn.parameters.parameters) {
            String fullname = param.holder + "." + param.name;
            localScope.put(fullname, param.type);
        }

        scopeStack.push(localScope);
        StatementListNode newBody = (StatementListNode) fn.statements.accept(this);
        scopeStack.pop();

        List<VariableNode> newParams = new ArrayList<>();
        for (VariableNode param : fn.parameters.parameters) {
            newParams.add(new VariableNode(param.type, param.holder, param.name, param.teamName, param.token));
        }
        ParameterListNode newParamList = new ParameterListNode(newParams);

        return new VoidFunctionNode(fn.name, fn.description, fn.icon, newParamList, newBody, fn.isConst, fn.token);
    }

    @Override public ASTNode visitDeclaration(DeclarationNode decl) {
        String fullname = decl.variable.holder + "." + decl.variable.name;
        Map<String, VariableType> current = scopeStack.peek();
        if (current.containsKey(fullname)) {
            throw new SemanticException(decl.token, "Duplicate declaration of '" + fullname + "' in this same scope");
        }
        current.put(fullname, decl.variable.type);

        VariableNode varCopy = new VariableNode(decl.variable.type, decl.variable.holder, decl.variable.name, decl.variable.teamName, decl.variable.token);
        return new DeclarationNode(varCopy, decl.token);
    }

    @Override public ASTNode visitAssignation(AssignationNode assign) {
        String fullname = assign.variable.holder + "." + assign.variable.name;
        VariableType vt = resolveOrDeclareGlobal(fullname, assign.token, assign.variable);
        NormalExpressionNode newRhs = (NormalExpressionNode) assign.value.accept(this);
        VariableNode newVar = new VariableNode(vt, assign.variable.holder, assign.variable.name, assign.variable.teamName, assign.variable.token);
        return new AssignationNode(newVar, assign.operator, newRhs, assign.token);
    }

    @Override
    public ASTNode visitVariable(VariableNode var) {
        String fullname = var.holder + "." + var.name;
        if (var.wasBare && !isDeclaredAnywhere(fullname)) {
            throw new SemanticException(var.token,
                    "Unknown identifier '" + var.name + "' — not a constant, function parameter, or declared variable. " +
                    "If you meant a player variable, use 'p." + var.name + "' or declare it at the top of the file.");
        }
        resolveOrDeclareGlobal(fullname, var.token, var);
        if (var.fallback != null && var.type != VariableType.VAR && var.fallback.type != var.type) {
            throw new SemanticException(var.token,
                    "Fallback value is a " + var.fallback.type + " but variable '" + var.name +
                    "' is a " + var.type + " — they must be the same type.");
        }
        VariableNode copy = new VariableNode(var.type, var.holder, var.name, var.teamName, var.token);
        copy.fallback = var.fallback;
        return copy;
    }

    /** Returns true if {@code fullname} is visible (param in any enclosing scope or top-level declaration). */
    private boolean isDeclaredAnywhere(String fullname) {
        for (Map<String, VariableType> scope : scopeStack) {
            if (scope.containsKey(fullname)) return true;
        }
        return globalDecls.containsKey(fullname) || allLocalNames.contains(fullname);
    }

    @Override
    public ASTNode visitConditional(ConditionalNode cond) {
        ExpressionNode newCond = (ExpressionNode) cond.condition.accept(this);

        scopeStack.push(new HashMap<>());
        StatementListNode newThen = (StatementListNode) cond.thenBlock.accept(this);
        scopeStack.pop();

        StatementListNode newElse = null;
        if (cond.elseBlock != null) {
            scopeStack.push(new HashMap<>());
            newElse = (StatementListNode) cond.elseBlock.accept(this);
            scopeStack.pop();
        }

        return new ConditionalNode(newCond, newThen, newElse, cond.token);
    }

    @Override
    public ASTNode visitLoop(LoopNode loop) {
        scopeStack.push(new HashMap<>());
        StatementListNode newBody = (StatementListNode) loop.statements.accept(this);
        scopeStack.pop();

        return new LoopNode(loop.name, loop.description, loop.icon, newBody, loop.tickDelay, loop.isConst, loop.token);
    }

    @Override
    public ASTNode visitEvent(EventNode event) {
        boolean saved = inFunction;
        inFunction = true;

        scopeStack.push(new HashMap<>());
        StatementListNode newBody = (StatementListNode) event.statements.accept(this);
        scopeStack.pop();

        inFunction = saved;
        return new EventNode(event.event, newBody, event.isConst, event.token);
    }

    @Override
    public ASTNode visitCommand(CommandNode cmd) {
        scopeStack.push(new HashMap<>());
        StatementListNode newBody = (StatementListNode) cmd.statements.accept(this);
        scopeStack.pop();
        return new CommandNode(cmd.name, cmd.targeted, cmd.priority, cmd.listed, newBody, cmd.isConst, cmd.token);
    }

    @Override
    public ASTNode visitMenu(MenuNode menu) {
        List<SlotNode> newSlots = new ArrayList<>();
        for (SlotNode slot : menu.slots) {
            newSlots.add((SlotNode) slot.accept(this));
        }
        return new MenuNode(menu.name, menu.rows, newSlots, menu.isConst, menu.token);
    }

    @Override
    public ASTNode visitSlot(SlotNode slot) {
        scopeStack.push(new HashMap<>());
        StatementListNode newBody = (StatementListNode) slot.statements.accept(this);
        scopeStack.pop();
        return new SlotNode(slot.slotId, slot.item, newBody, slot.token);
    }

    @Override
    public ASTNode visitRepeat(RepeatNode repeat) {
        scopeStack.push(new HashMap<>());
        StatementListNode newBody = (StatementListNode) repeat.statements.accept(this);
        scopeStack.pop();
        return new RepeatNode(repeat.count, newBody, repeat.token);
    }

    @Override
    public ASTNode visitRandom(RandomNode random) {
        List<StatementListNode> newOptions = new ArrayList<>();
        for (StatementListNode opt : random.statements) {
            scopeStack.push(new HashMap<>());
            StatementListNode rewritten = (StatementListNode) opt.accept(this);
            scopeStack.pop();
            newOptions.add(rewritten);
        }
        return new RandomNode(newOptions, random.weights, random.token);
    }

    @Override
    public ASTNode visitTupleDestructure(TupleDestructureNode node) {
        for (VariableNode target : node.targets) {
            VariableType expectedType = target.type; // set by LinkFunctionVisitor from function return type
            String fullname = target.holder + "." + target.name;
            VariableType resolvedType = resolveOrDeclareGlobal(fullname, target.token, null);
            if (resolvedType != VariableType.VAR && expectedType != null && resolvedType != expectedType)
                throw new SemanticException(target.token, "Type mismatch: '" + fullname + "' is declared as " +
                        resolvedType + " but function returns " + expectedType);
            // Keep the function's return type on the target variable
            target.type = expectedType;
        }
        ArgumentListNode newArgs = (ArgumentListNode) node.arguments.accept(this);
        return new TupleDestructureNode(node.targets, node.operator, node.functionName, newArgs, node.token);
    }
}
