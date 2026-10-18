package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TupleFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.TupleDestructureNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkFunctionVisitor extends DefaultVisitor {
    private final Map<String, TypedFunctionNode> typedDefs = new HashMap<>();
    private final Map<String, VoidFunctionNode> voidDefs = new HashMap<>();
    private final Map<String, TupleFunctionNode> tupleDefs = new HashMap<>();

    public LinkFunctionVisitor(MainNode main) {
        for (MainStatementNode top : main.statements) {
            if (top instanceof TypedFunctionNode tf) {
                String name = tf.name;
                if (typedDefs.containsKey(name) || voidDefs.containsKey(name) || tupleDefs.containsKey(name)) {
                    throw new SemanticException(tf.token, "Duplicate function name '" + name + "'");
                }
                typedDefs.put(name, tf);
            } else if (top instanceof TupleFunctionNode tf) {
                String name = tf.name;
                if (typedDefs.containsKey(name) || voidDefs.containsKey(name) || tupleDefs.containsKey(name)) {
                    throw new SemanticException(tf.token, "Duplicate function name '" + name + "'");
                }
                tupleDefs.put(name, tf);
            } else if (top instanceof VoidFunctionNode vf) {
                String name = vf.name;
                if (typedDefs.containsKey(name) || voidDefs.containsKey(name) || tupleDefs.containsKey(name)) {
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
        ArgumentListNode newArgs = (ArgumentListNode) call.arguments.accept(this);
        boolean defined = voidDefs.containsKey(fname) || typedDefs.containsKey(fname) || tupleDefs.containsKey(fname);
        // UnknownCallCheckVisitor already allowed a call to a function outside the script, if it has no arguments.
        if (defined || newArgs.expressions.isEmpty()) return new VoidFunctionCallNode(fname, newArgs, call.token);
        throw new SemanticException(call.token, "Undefined function '" + fname + "'");
    }

    @Override
    public ASTNode visitTupleDestructure(TupleDestructureNode node) {
        TupleFunctionNode def = tupleDefs.get(node.functionName);
        if (def == null) throw new SemanticException(node.token, "Undefined tuple function '" + node.functionName + "'");
        if (node.targets.size() != def.returnTypes.size())
            throw new SemanticException(node.token, "Tuple function '" + node.functionName + "' returns " +
                    def.returnTypes.size() + " values but " + node.targets.size() + " targets given");
        ArgumentListNode newArgs = (ArgumentListNode) node.arguments.accept(this);
        // Fill in target types from the function definition
        List<VariableNode> typedTargets = new ArrayList<>();
        for (int i = 0; i < node.targets.size(); i++) {
            VariableNode t = node.targets.get(i);
            VariableType expectedType = def.returnTypes.get(i);
            if (t.type != null && t.type != expectedType)
                throw new SemanticException(t.token, "Target type mismatch at position " + i +
                        ": declared as " + t.type + " but function returns " + expectedType);
            typedTargets.add(new VariableNode(expectedType, t.holder, t.name, t.teamName, t.token));
        }
        return new TupleDestructureNode(typedTargets, node.operator, node.functionName, newArgs, node.token);
    }

    @Override
    public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode call) {
        String fname = call.functionName;
        VoidFunctionNode def = voidDefs.get(fname);
        if (def == null && !call.arguments.expressions.isEmpty()) {
            throw new SemanticException(call.token, "Undefined global function '" + fname + "'");
        }
        ArgumentListNode newArgs = (ArgumentListNode) call.arguments.accept(this);
        return new GlobalFunctionCallNode(fname, newArgs, call.token);
    }
}
