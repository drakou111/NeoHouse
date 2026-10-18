package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.UnaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TupleFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.ActionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.TupleDestructureNode;
import com.drakou111.neohouse.compiler.ast.statement.TypedReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionCallLoweringVisitor extends DefaultVisitor {
    // Temps saving a call's result start here, above the ones expression lowering numbered from 0.
    private static final int SAVED_RESULT_TEMP_BASE = 100;

    private int savedResults = 0;

    private final Map<String, VoidFunctionNode> voidFunctions = new HashMap<>();
    private final Map<String, TypedFunctionNode> typedFunctions = new HashMap<>();
    private final Map<String, TupleFunctionNode> tupleFunctions = new HashMap<>();

    public FunctionCallLoweringVisitor(MainNode main) {
        for (MainStatementNode top : main.statements) {
            if (top instanceof TypedFunctionNode tf) {
                String name = tf.name;
                if (typedFunctions.containsKey(name) || voidFunctions.containsKey(name)) {
                    throw new SemanticException(tf.token, "Duplicate function name '" + name + "'");
                }
                typedFunctions.put(name, tf);
            } else if (top instanceof TupleFunctionNode tf) {
                String name = tf.name;
                if (tupleFunctions.containsKey(name) || voidFunctions.containsKey(name)) {
                    throw new SemanticException(tf.token, "Duplicate function name '" + name + "'");
                }
                tupleFunctions.put(name, tf);
            } else if (top instanceof VoidFunctionNode vf) {
                String name = vf.name;
                if (typedFunctions.containsKey(name) || voidFunctions.containsKey(name)) {
                    throw new SemanticException(vf.token, "Duplicate function name '" + name + "'");
                }
                voidFunctions.put(name, vf);
            }
        }
    }

    private record ArgLowerResult(NormalExpressionNode simple, List<StatementNode> stmts) {}

    private ArgLowerResult lowerArgument(ExpressionNode expr) {
        if (expr instanceof FunctionCallNode tfc) {
            TypedFunctionNode def = typedFunctions.get(tfc.functionName);
            if (def == null) throw new SemanticException(tfc.token, "Undefined typed function '" + tfc.functionName + "'");
            List<VariableNode> params = def.parameters.parameters;
            List<StatementNode> stmts = new ArrayList<>();

            List<NormalExpressionNode> realArgs = new ArrayList<>();
            int argumentCount = tfc.arguments.expressions.size();
            for (int i = 0; i < argumentCount; i++) {
                ExpressionNode rawArg = tfc.arguments.expressions.get(i);
                ArgLowerResult sub = lowerArgument(rawArg);
                stmts.addAll(sub.stmts());
                realArgs.add(i < argumentCount - 1 ? saved(sub.simple, stmts) : sub.simple);
            }

            for (int i = 0; i < realArgs.size(); i++) {
                NormalExpressionNode argExpr = realArgs.get(i);
                VariableNode param = params.get(i);
                String newName = CompilerNames.parameterSlot(tfc.functionName, i);
                VariableNode target = new VariableNode(param.type, param.holder, newName, param.teamName, tfc.token);
                stmts.add(new AssignationNode(target, AssignmentOperator.ASSIGN, argExpr, tfc.token));
            }
            stmts.add(new VoidFunctionCallNode(tfc.functionName, new ArgumentListNode(List.of()), tfc.token));
            VariableNode resultVar = new VariableNode(def.returnType, Holder.PLAYER, "o$", null, tfc.token);
            return new ArgLowerResult(resultVar, stmts);
        }

        if (expr instanceof BinaryExpressionNode bin) {
            ArgLowerResult leftR  = lowerArgument(bin.left);
            ArgLowerResult rightR = lowerArgument(bin.right);
            List<StatementNode> stmts = new ArrayList<>(leftR.stmts());
            NormalExpressionNode left = saved(leftR.simple, stmts);
            stmts.addAll(rightR.stmts());
            return new ArgLowerResult(
                new BinaryExpressionNode(left, rightR.simple, bin.operator, bin.type, bin.token),
                stmts);
        }

        if (expr instanceof UnaryExpressionNode un) {
            ArgLowerResult inner = lowerArgument(un.operand);
            return new ArgLowerResult(
                new UnaryExpressionNode(un.operator, inner.simple, un.type, un.token),
                inner.stmts());
        }

        return new ArgLowerResult((NormalExpressionNode) expr, List.of());
    }

    @Override
    public ASTNode visitStatements(StatementListNode statements) {
        List<StatementNode> result = new ArrayList<>();
        for (StatementNode statement : statements.statements) {
            this.savedResults = 0;
            result.add((StatementNode) statement.accept(this));
        }
        return new StatementListNode(result);
    }

    /**
     * A call's result ({@code o$}) copied into a temp, since a later call in the same expression or
     * argument list replaces it; anything else as it is. The temp optimizer drops copies nothing needs.
     */
    private NormalExpressionNode saved(NormalExpressionNode value, List<StatementNode> stmts) {
        if (!(value instanceof VariableNode result) || !CompilerTemps.isReturnSlot(result.name)) return value;
        VariableNode temp = new VariableNode(result.type, Holder.PLAYER, "t$" + (SAVED_RESULT_TEMP_BASE + this.savedResults++),
                null, result.token);
        stmts.add(new AssignationNode(temp, AssignmentOperator.ASSIGN, result, result.token));
        return temp;
    }

    @Override
    public ASTNode visitActionCall(ActionCallNode call) {
        List<StatementNode> stmts = new ArrayList<>();
        List<NormalExpressionNode> newArgs = new ArrayList<>();
        List<NormalExpressionNode> args = call.arguments.expressions;
        for (int i = 0; i < args.size(); i++) {
            ArgLowerResult lowered = lowerArgument(args.get(i));
            stmts.addAll(lowered.stmts());
            newArgs.add(i < args.size() - 1 ? saved(lowered.simple, stmts) : lowered.simple);
        }
        stmts.add(new ActionCallNode(call.name, new ArgumentListNode(newArgs), call.token));
        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitVoidFunctionCall(VoidFunctionCallNode call) {
        VoidFunctionNode def = voidFunctions.get(call.functionName);

        // Allow calling typed/tuple functions as void (return value discarded)
        if (def == null) {
            TypedFunctionNode typedDef = typedFunctions.get(call.functionName);
            if (typedDef != null) return lowerTypedCallAsVoid(call, typedDef);
            TupleFunctionNode tupleDef = tupleFunctions.get(call.functionName);
            if (tupleDef != null) return lowerTupleCallAsVoid(call, tupleDef);
            // A function outside the script (allowed by UnknownCallCheckVisitor) is just triggered.
            if (call.arguments.expressions.isEmpty()) return call;
            throw new SemanticException(call.token, "Undefined function '" + call.functionName + "'");
        }

        List<NormalExpressionNode> rawArgs = call.arguments.expressions;
        List<VariableNode> params = def.parameters.parameters;

        if (rawArgs.size() != params.size()) {
            throw new SemanticException(call.token, "Function " + call.functionName + " expects " + params.size() + " arguments but got " + rawArgs.size());
        }

        List<StatementNode> stmts = new ArrayList<>();
        for (int i = 0; i < rawArgs.size(); i++) {
            ArgLowerResult lowered = lowerArgument(rawArgs.get(i));
            stmts.addAll(lowered.stmts());
            VariableNode param = params.get(i);
            String newName = CompilerNames.parameterSlot(call.functionName, i);
            VariableNode target = new VariableNode(param.type, param.holder, newName, param.teamName, call.token);
            stmts.add(new AssignationNode(target, AssignmentOperator.ASSIGN, lowered.simple, call.token));
        }

        stmts.add(new VoidFunctionCallNode(call.functionName, new ArgumentListNode(List.of()), call.token));

        return new StatementListNode(stmts);
    }

    private ASTNode lowerTupleCallAsVoid(VoidFunctionCallNode call, TupleFunctionNode def) {
        List<NormalExpressionNode> rawArgs = call.arguments.expressions;
        List<VariableNode> params = def.parameters.parameters;
        if (rawArgs.size() != params.size())
            throw new SemanticException(call.token, "Function " + call.functionName + " expects " + params.size() + " arguments but got " + rawArgs.size());
        List<StatementNode> stmts = new ArrayList<>();
        for (int i = 0; i < rawArgs.size(); i++) {
            ArgLowerResult lowered = lowerArgument(rawArgs.get(i));
            stmts.addAll(lowered.stmts());
            VariableNode param = params.get(i);
            String newName = CompilerNames.parameterSlot(call.functionName, i);
            VariableNode target = new VariableNode(param.type, param.holder, newName, param.teamName, call.token);
            stmts.add(new AssignationNode(target, AssignmentOperator.ASSIGN, lowered.simple, call.token));
        }
        stmts.add(new VoidFunctionCallNode(call.functionName, new ArgumentListNode(List.of()), call.token));
        return new StatementListNode(stmts);
    }

    private ASTNode lowerTypedCallAsVoid(VoidFunctionCallNode call, TypedFunctionNode def) {
        List<NormalExpressionNode> rawArgs = call.arguments.expressions;
        List<VariableNode> params = def.parameters.parameters;

        if (rawArgs.size() != params.size()) {
            throw new SemanticException(call.token, "Function " + call.functionName + " expects " + params.size() + " arguments but got " + rawArgs.size());
        }

        List<StatementNode> stmts = new ArrayList<>();
        for (int i = 0; i < rawArgs.size(); i++) {
            ArgLowerResult lowered = lowerArgument(rawArgs.get(i));
            stmts.addAll(lowered.stmts());
            VariableNode param = params.get(i);
            String newName = CompilerNames.parameterSlot(call.functionName, i);
            VariableNode target = new VariableNode(param.type, param.holder, newName, param.teamName, call.token);
            stmts.add(new AssignationNode(target, AssignmentOperator.ASSIGN, lowered.simple, call.token));
        }
        // Call the function; the return value in o$ is simply discarded
        stmts.add(new VoidFunctionCallNode(call.functionName, new ArgumentListNode(List.of()), call.token));
        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitTupleDestructure(TupleDestructureNode node) {
        TupleFunctionNode def = tupleFunctions.get(node.functionName);
        if (def == null) throw new SemanticException(node.token, "Undefined tuple function '" + node.functionName + "'");

        List<VariableNode> params = def.parameters.parameters;
        List<StatementNode> stmts = new ArrayList<>();

        // Lower each argument (may contain nested typed function calls)
        List<NormalExpressionNode> realArgs = new ArrayList<>();
        List<NormalExpressionNode> rawArgs = node.arguments.expressions;
        for (int i = 0; i < rawArgs.size(); i++) {
            ArgLowerResult sub = lowerArgument(rawArgs.get(i));
            stmts.addAll(sub.stmts());
            realArgs.add(i < rawArgs.size() - 1 ? saved(sub.simple, stmts) : sub.simple);
        }
        if (realArgs.size() != params.size())
            throw new SemanticException(node.token, "Function '" + node.functionName + "' expects " +
                    params.size() + " arguments but got " + realArgs.size());

        // Assign parameters
        for (int i = 0; i < realArgs.size(); i++) {
            VariableNode param = params.get(i);
            String newName = CompilerNames.parameterSlot(node.functionName, i);
            VariableNode target = new VariableNode(param.type, param.holder, newName, param.teamName, node.token);
            stmts.add(new AssignationNode(target, AssignmentOperator.ASSIGN, realArgs.get(i), node.token));
        }

        // Call the function
        stmts.add(new VoidFunctionCallNode(node.functionName, new ArgumentListNode(List.of()), node.token));

        // Read outputs o$0, o$1, ... into targets using the declared operator
        for (int i = 0; i < node.targets.size(); i++) {
            VariableNode outVar = new VariableNode(def.returnTypes.get(i), Holder.PLAYER, "o$" + i, null, node.token);
            stmts.add(new AssignationNode(node.targets.get(i), node.operator, outVar, node.token));
        }

        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode call) {
        VoidFunctionNode def = voidFunctions.get(call.functionName);
        if (def == null && call.arguments.expressions.isEmpty()) return call;
        if (def == null) {
            throw new SemanticException(call.token, "Undefined void function '" + call.functionName + "'");
        }

        List<NormalExpressionNode> rawArgs = call.arguments.expressions;
        List<VariableNode> params = def.parameters.parameters;

        if (rawArgs.size() != params.size()) {
            throw new SemanticException(call.token, "Function " + call.functionName + " expects " + params.size() + " arguments but got " + rawArgs.size());
        }

        List<StatementNode> stmts = new ArrayList<>();
        for (int i = 0; i < rawArgs.size(); i++) {
            ArgLowerResult lowered = lowerArgument(rawArgs.get(i));
            stmts.addAll(lowered.stmts());

            VariableNode param = params.get(i);
            String newName = CompilerNames.parameterSlot(call.functionName, i);
            VariableNode target = new VariableNode(param.type, param.holder, newName, param.teamName, call.token);
            stmts.add(new AssignationNode(target, AssignmentOperator.ASSIGN, lowered.simple, call.token));
        }

        stmts.add(new GlobalFunctionCallNode(call.functionName, new ArgumentListNode(List.of()), call.token));
        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitAssignation(AssignationNode node) {
        ArgLowerResult lowered = lowerArgument(node.value);
        List<StatementNode> stmts = new ArrayList<>(lowered.stmts());
        stmts.add(new AssignationNode(node.variable, node.operator, lowered.simple, node.token));
        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitTypedReturn(TypedReturnNode returnNode) {
        ArgLowerResult lowered = lowerArgument(returnNode.expression);
        List<StatementNode> stmts = new ArrayList<>(lowered.stmts());
        stmts.add(new TypedReturnNode(lowered.simple, returnNode.token));
        return new StatementListNode(stmts);
    }
}
