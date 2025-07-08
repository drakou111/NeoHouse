package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.*;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.TypedReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionCallLoweringVisitor extends DefaultVisitor {

    private final Map<String, VoidFunctionNode> voidFunctions = new HashMap<>();
    private final Map<String, TypedFunctionNode> typedFunctions = new HashMap<>();


    public FunctionCallLoweringVisitor(MainNode main) {
        for (MainStatementNode top : main.statements) {
            if (top instanceof TypedFunctionNode tf) {
                String name = tf.name;
                if (typedFunctions.containsKey(name) || voidFunctions.containsKey(name)) {
                    throw new SemanticException(tf.token, "Duplicate function name '" + name + "'");
                }
                typedFunctions.put(name, tf);

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
            List<VariableNode> params = def.parameters.parameters;
            List<StatementNode> stmts = new ArrayList<>();

            List<NormalExpressionNode> realArgs = new ArrayList<>();
            for (int i = 0; i < tfc.arguments.expressions.size(); i++) {
                ExpressionNode rawArg = tfc.arguments.expressions.get(i);
                ArgLowerResult sub = lowerArgument(rawArg);
                stmts.addAll(sub.stmts());
                realArgs.add(sub.simple);
            }

            for (int i = 0; i < realArgs.size(); i++) {
                NormalExpressionNode argExpr = realArgs.get(i);
                VariableNode param = params.get(i);
                String newName = tfc.functionName + "_p$" + i;
                VariableNode target = new VariableNode(param.type, param.holder, newName, param.teamName, tfc.token);
                stmts.add(new AssignationNode(target, AssignmentOperator.ASSIGN, argExpr, tfc.token));
            }
            stmts.add(new VoidFunctionCallNode(tfc.functionName, new ArgumentListNode(List.of()), tfc.token));
            VariableNode resultVar = new VariableNode(def.returnType, Holder.PLAYER, "o$", null, tfc.token);
            return new ArgLowerResult(resultVar, stmts);
        }

        return new ArgLowerResult((NormalExpressionNode) expr, List.of());
    }

    @Override
    public ASTNode visitVoidFunctionCall(VoidFunctionCallNode call) {
        VoidFunctionNode def = voidFunctions.get(call.functionName);
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
            String newName = call.functionName + "_p$" + i;
            VariableNode target = new VariableNode(param.type, param.holder, newName, param.teamName, call.token);
            stmts.add(new AssignationNode(target, AssignmentOperator.ASSIGN, lowered.simple, call.token));
        }

        stmts.add(new VoidFunctionCallNode(call.functionName, new ArgumentListNode(List.of()), call.token));

        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode call) {
        TypedFunctionNode def = typedFunctions.get(call.functionName);
        if (def == null) {
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
            String newName = call.functionName + "_p$" + i;
            VariableNode target = new VariableNode(param.type, param.holder, newName, param.teamName, call.token);
            stmts.add(new AssignationNode(target, AssignmentOperator.ASSIGN, lowered.simple, call.token));
        }

        stmts.add(new GlobalFunctionCallNode(call.functionName,new ArgumentListNode(List.of()),call.token));
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
