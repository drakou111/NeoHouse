package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.ast.expr.ConditionExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.main.MainNode;
import com.drakou111.neohouse.compilers.ast.main.ParameterListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.ast.main_statements.*;
import com.drakou111.neohouse.compilers.ast.statement.*;
import com.drakou111.neohouse.compilers.lexer.Token;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.*;

/**
 * Pass 2: builds and checks symbol tables (scopes).
 *   - On declaration: insert into current scope (error if duplicate).
 *   - On assignment/use: resolve up the scope stack or global.
 *       * If found in any scope → OK.
 *       * If not found and name ∈ allLocalNames → ERROR (ambiguous).
 *       * If not found in allLocalNames or globalDecls → insert in globalDecls as type=VAR.
 */
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

        VariableNode varCopy = new VariableNode(node.variable.type, node.variable.holder, node.variable.name, node.variable.token);
        return new GlobalDeclarationNode(varCopy, node.token);
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
            newParams.add(new VariableNode(param.type, param.holder, param.name, param.token));
        }
        ParameterListNode newParamList = new ParameterListNode(newParams);
        return new TypedFunctionNode(fn.name, fn.description, fn.returnType, newParamList, newBody, fn.token);
    }

    @Override
    public ASTNode visitVoidFunction(VoidFunctionNode fn) {
        // 1) Build new local scope, seeded with parameters
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
            newParams.add(new VariableNode(param.type, param.holder, param.name, param.token));
        }
        ParameterListNode newParamList = new ParameterListNode(newParams);

        return new VoidFunctionNode(fn.name, fn.description, newParamList, newBody, fn.token);
    }

    @Override public ASTNode visitDeclaration(DeclarationNode decl) {
        String fullname = decl.variable.holder + "." + decl.variable.name;
        Map<String, VariableType> current = scopeStack.peek();
        if (current.containsKey(fullname)) {
            throw new SemanticException(decl.token, "Duplicate declaration of '" + fullname + "' in this same scope");
        }
        current.put(fullname, decl.variable.type);

        VariableNode varCopy = new VariableNode(decl.variable.type, decl.variable.holder, decl.variable.name, decl.variable.token);
        return new DeclarationNode(varCopy, decl.token);
    }

    @Override public ASTNode visitAssignation(AssignationNode assign) {
        String fullname = assign.variable.holder + "." + assign.variable.name;
        VariableType vt = resolveOrDeclareGlobal(fullname, assign.token, assign.variable);
        NormalExpressionNode newRhs = (NormalExpressionNode) assign.value.accept(this);
        VariableNode newVar = new VariableNode(vt, assign.variable.holder, assign.variable.name, assign.variable.token);
        return new AssignationNode(newVar, assign.operator, newRhs, assign.token);
    }

    @Override
    public ASTNode visitVariable(VariableNode var) {
        String fullname = var.holder + "." + var.name;
        resolveOrDeclareGlobal(fullname, var.token, var);
        return new VariableNode(var.type, var.holder, var.name, var.token);
    }

    @Override
    public ASTNode visitConditional(ConditionalNode cond) {
        ConditionExpressionNode newCond = (ConditionExpressionNode) cond.condition.accept(this);

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

        return new LoopNode(loop.name, loop.description, newBody, loop.tickDelay, loop.token);
    }

    @Override
    public ASTNode visitEvent(EventNode event) {
        boolean saved = inFunction;
        inFunction = true;

        scopeStack.push(new HashMap<>());

        event.statements.accept(this);

        scopeStack.pop();
        inFunction = saved;
        return new EventNode(event.event, event.statements, event.token);
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
        return new RandomNode(newOptions, random.token);
    }
}
