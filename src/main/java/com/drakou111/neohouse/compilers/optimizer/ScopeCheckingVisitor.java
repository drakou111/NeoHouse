package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.ast.expr.ConditionExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.main.MainNode;
import com.drakou111.neohouse.compilers.ast.main.ParameterListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.ast.main_statements.EventNode;
import com.drakou111.neohouse.compilers.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compilers.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compilers.ast.main_statements.LoopNode;
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
        // 1) Check every scope from top→bottom
        for (Map<String, VariableType> scope : scopeStack) {
            if (scope.containsKey(fullname)) {
                VariableType vt = scope.get(fullname);
                if (varNode != null) varNode.type = vt;
                return vt;
            }
        }
        // 2) If the name is known to be “local somewhere else” → ambiguous
        if (allLocalNames.contains(fullname)) {
            throw new SemanticException(token, "Ambiguous reference to '" + fullname + "' (declared locally elsewhere, but not in this scope)");
        }
        // 3) If it’s in globalDecls → insert into current map & return
        if (globalDecls.containsKey(fullname)) {
            VariableType vt = globalDecls.get(fullname);
            scopeStack.getLast().put(fullname, vt);
            if (varNode != null) varNode.type = vt;
            return vt;
        }
        // 4) Otherwise auto-declare in global as VAR
        VariableType vt = VariableType.VAR;
        globalDecls.put(fullname, vt);
        scopeStack.getLast().put(fullname, vt);
        if (varNode != null) varNode.type = vt;
        return vt;
    }

    // ───────────────────────────────────────────────────
    // 1) Top‐level global declaration
    @Override
    public ASTNode visitGlobalDeclaration(GlobalDeclarationNode node) {
        String fullname = node.variable.holder + "." + node.variable.name;
        scopeStack.peek().put(fullname, node.variable.type);

        // Return a fresh copy of that node:
        VariableNode varCopy = new VariableNode(
                node.variable.type,
                node.variable.holder,
                node.variable.name,
                node.variable.token
        );
        return new GlobalDeclarationNode(varCopy, node.token);
    }

    // 2) Entering a function → push a new local scope
    @Override
    public ASTNode visitFunction(FunctionNode fn) {
        // 1) Build new local scope, seeded with parameters
        Map<String, VariableType> localScope = new HashMap<>();
        for (VariableNode param : fn.parameters.parameters) {
            String fullname = param.holder + "." + param.name;
            localScope.put(fullname, param.type);
        }
        scopeStack.push(localScope);

        // 2) Recurse into statements/body
        StatementListNode newBody = (StatementListNode) fn.statements.accept(this);

        // 3) Pop local scope
        scopeStack.pop();

        // 4) Rebuild the parameter list (they were already typed by the parser)
        List<VariableNode> newParams = new ArrayList<>();
        for (VariableNode param : fn.parameters.parameters) {
            newParams.add(new VariableNode(
                    param.type,
                    param.holder,
                    param.name,
                    param.token
            ));
        }
        ParameterListNode newParamList = new ParameterListNode(newParams);

        // 5) Return new FunctionNode
        return new FunctionNode(
                fn.name,
                fn.description,
                fn.returnType,
                newParamList,
                newBody,
                fn.token
        );
    }

    @Override public ASTNode visitDeclaration(DeclarationNode decl) {
        String fullname = decl.variable.holder + "." + decl.variable.name;
        Map<String, VariableType> current = scopeStack.peek();
        if (current.containsKey(fullname)) {
            throw new SemanticException(decl.token, "Duplicate declaration of '" + fullname + "' in this same scope");
        }
        current.put(fullname, decl.variable.type);

        // The parser already set decl.variable.type, so simply return a fresh copy:
        VariableNode varCopy = new VariableNode(
                decl.variable.type,
                decl.variable.holder,
                decl.variable.name,
                decl.variable.token
        );
        return new DeclarationNode(varCopy, decl.token);
    }

    @Override public ASTNode visitAssignation(AssignationNode assign) {
        // Resolve the LHS (and update its type)
        String fullname = assign.variable.holder + "." + assign.variable.name;
        VariableType vt = resolveOrDeclareGlobal(fullname, assign.token, assign.variable);

        // Recurse on the RHS expression to fix any variable-types inside
        NormalExpressionNode newRhs = (NormalExpressionNode) assign.value.accept(this);

        // Return a new AssignationNode(varNodeWithType, operator, newRhs, token)
        VariableNode newVar = new VariableNode(
                vt,
                assign.variable.holder,
                assign.variable.name,
                assign.variable.token
        );
        return new AssignationNode(newVar, assign.operator, newRhs, assign.token);
    }

    @Override
    public ASTNode visitVariable(VariableNode var) {
        // Resolve or auto-declare, writing the type back into `var`
        String fullname = var.holder + "." + var.name;
        resolveOrDeclareGlobal(fullname, var.token, var);

        // Now return a new VariableNode with the updated type.
        // (Copy any other fields you need, or simply reuse `var` if mutable.)
        return new VariableNode(var.type, var.holder, var.name, var.token);
    }

    @Override
    public ASTNode visitConditional(ConditionalNode cond) {
        // 1) Rewrite condition itself
        ConditionExpressionNode newCond = (ConditionExpressionNode) cond.condition.accept(this);

        // 2) “Then” block in its own nested scope
        scopeStack.push(new HashMap<>());
        StatementListNode newThen = (StatementListNode) cond.thenBlock.accept(this);
        scopeStack.pop();

        // 3) “Else” block if present
        StatementListNode newElse = null;
        if (cond.elseBlock != null) {
            scopeStack.push(new HashMap<>());
            newElse = (StatementListNode) cond.elseBlock.accept(this);
            scopeStack.pop();
        }

        // 4) Return new ConditionalNode
        return new ConditionalNode(newCond, newThen, newElse, cond.token);
    }

    @Override
    public ASTNode visitLoop(LoopNode loop) {
        // 1) Rewrite body in a nested scope
        scopeStack.push(new HashMap<>());
        StatementListNode newBody = (StatementListNode) loop.statements.accept(this);
        scopeStack.pop();

        // 2) Return a fresh LoopNode
        return new LoopNode(loop.name, loop.description, newBody, loop.tickDelay, loop.token);
    }

    @Override
    public ASTNode visitEvent(EventNode event) {
        boolean saved = inFunction;
        inFunction = true;

        // Events don't have parameters, so just push an empty scope
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
