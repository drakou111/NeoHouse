package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.ast.expr.ConditionExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compilers.ast.main.*;
import com.drakou111.neohouse.compilers.ast.main_statements.*;
import com.drakou111.neohouse.compilers.ast.statement.*;
import com.drakou111.neohouse.compilers.lexer.Token;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.*;

/**
 * 1) In the constructor, we scan the MainNode to collect every TypedFunctionNode
 *    into typedDefs, and every VoidFunctionNode into voidDefs.
 * 2) Then analyze(main) calls main.accept(this), and each visitXxx replaces
 *    every FunctionCallNode/ VoidFunctionCallNode/ GlobalFunctionCallNode
 *    with a new node whose .type is taken from the matching definition.
 *
 * If a call name is not found in the appropriate map, we throw SemanticException.
 */
public class LinkFunctionVisitor extends DefaultVisitor {
    private final Map<String, TypedFunctionNode> typedDefs = new HashMap<>();
    private final Map<String, VoidFunctionNode> voidDefs = new HashMap<>();

    public LinkFunctionVisitor(MainNode main) {
        for (MainStatementNode top : main.statements) {
            if (top instanceof TypedFunctionNode tf) {
                String name = tf.name;
                if (typedDefs.containsKey(name) || voidDefs.containsKey(name)) {
                    throw new SemanticException(tf.token, "Duplicate function name '" + name + "'");
                }
                typedDefs.put(name, tf);

            } else if (top instanceof VoidFunctionNode vf) {
                String name = vf.name;
                if (typedDefs.containsKey(name) || voidDefs.containsKey(name)) {
                    throw new SemanticException(vf.token, "Duplicate function name '" + name + "'");
                }
                voidDefs.put(name, vf);
            }
        }
    }

    @Override
    public MainNode analyze(MainNode main) {
        return (MainNode) main.accept(this);
    }

    @Override
    public ASTNode visitFunctionCall(FunctionCallNode call) {
        String fname = call.functionName;
        TypedFunctionNode def = typedDefs.get(fname);
        if (def == null) {
            throw new SemanticException(call.token, "Undefined typed function '" + fname + "'");
        }
        ArgumentListNode newArgs = (ArgumentListNode) call.arguments.accept(this);
        VariableType retType = def.returnType;
        return new FunctionCallNode(fname, newArgs, retType, call.token);
    }

    @Override
    public ASTNode visitVoidFunctionCallStatement(VoidFunctionCallNode call) {
        String fname = call.functionName;
        VoidFunctionNode def = voidDefs.get(fname);
        if (def == null) {
            throw new SemanticException(call.token, "Undefined void function '" + fname + "'");
        }
        ArgumentListNode newArgs = (ArgumentListNode) call.arguments.accept(this);
        return new VoidFunctionCallNode(fname, newArgs, call.token);
    }

    @Override
    public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode call) {
        String fname = call.functionName;
        VoidFunctionNode def = voidDefs.get(fname);
        if (def == null) {
            throw new SemanticException(
                    call.token,
                    "Undefined global function '" + fname + "'"
            );
        }
        ArgumentListNode newArgs = (ArgumentListNode) call.arguments.accept(this);
        return new GlobalFunctionCallNode(fname, newArgs, call.token);
    }
}
