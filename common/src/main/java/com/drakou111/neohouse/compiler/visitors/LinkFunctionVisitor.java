package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.HashMap;
import java.util.Map;

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
    public ASTNode visitVoidFunctionCall(VoidFunctionCallNode call) {
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
            throw new SemanticException(call.token, "Undefined global function '" + fname + "')");
        }
        ArgumentListNode newArgs = (ArgumentListNode) call.arguments.accept(this);
        return new GlobalFunctionCallNode(fname, newArgs, call.token);
    }
}
