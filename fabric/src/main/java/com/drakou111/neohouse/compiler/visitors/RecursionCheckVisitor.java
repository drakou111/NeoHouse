package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TupleFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.TupleDestructureNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.config.RecursionMode;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecursionCheckVisitor {

    private final RecursionMode mode;
    private final List<CompileWarning> warnings;
    private final Set<Set<String>> warnedCycles = new HashSet<>();

    public RecursionCheckVisitor() {
        this(RecursionMode.ERROR, new ArrayList<>());
    }

    public RecursionCheckVisitor(RecursionMode mode, List<CompileWarning> warnings) {
        this.mode = mode;
        this.warnings = warnings;
    }

    public void analyze(MainNode main) {
        if (mode == RecursionMode.OFF) return;

        // Build call graph: function name -> set of direct callee names
        Map<String, Set<String>> callGraph = new HashMap<>();
        Map<String, VoidFunctionNode>    voidDefs   = new HashMap<>();
        Map<String, TypedFunctionNode>   typedDefs  = new HashMap<>();
        Map<String, TupleFunctionNode>   tupleDefs  = new HashMap<>();

        for (MainStatementNode stmt : main.statements) {
            if (stmt instanceof VoidFunctionNode vf) {
                voidDefs.put(vf.name, vf);
            } else if (stmt instanceof TypedFunctionNode tf) {
                typedDefs.put(tf.name, tf);
            } else if (stmt instanceof TupleFunctionNode tf) {
                tupleDefs.put(tf.name, tf);
            }
        }

        for (VoidFunctionNode vf : voidDefs.values()) {
            Set<String> callees = new HashSet<>();
            new CallCollector(callees).visitStatements(vf.statements);
            callGraph.put(vf.name, callees);
        }
        for (TypedFunctionNode tf : typedDefs.values()) {
            Set<String> callees = new HashSet<>();
            new CallCollector(callees).visitStatements(tf.statements);
            callGraph.put(tf.name, callees);
        }
        for (TupleFunctionNode tf : tupleDefs.values()) {
            Set<String> callees = new HashSet<>();
            new CallCollector(callees).visitStatements(tf.statements);
            callGraph.put(tf.name, callees);
        }

        // DFS cycle detection using LinkedHashSet for ordered path
        Set<String> allNames = callGraph.keySet();
        Set<String> visited  = new HashSet<>();

        for (String start : allNames) {
            if (!visited.contains(start)) {
                LinkedHashSet<String> path = new LinkedHashSet<>();
                dfs(start, callGraph, path, visited, main, voidDefs, typedDefs, tupleDefs);
            }
        }
    }

    private void dfs(String node,
                     Map<String, Set<String>> callGraph,
                     LinkedHashSet<String> path,
                     Set<String> visited,
                     MainNode main,
                     Map<String, VoidFunctionNode> voidDefs,
                     Map<String, TypedFunctionNode> typedDefs,
                     Map<String, TupleFunctionNode> tupleDefs) {
        if (path.contains(node)) {
            // Build cycle string from where node appears in path
            List<String> pathList = new ArrayList<>(path);
            int cycleStart = pathList.indexOf(node);
            List<String> cycle = pathList.subList(cycleStart, pathList.size());
            String msg = "Recursive call: " + String.join(" -> ", cycle) + " -> " + node;
            // Find a token from the function definition
            com.drakou111.neohouse.compiler.lexer.Token token = null;
            if (voidDefs.containsKey(node)) token = voidDefs.get(node).token;
            else if (typedDefs.containsKey(node)) token = typedDefs.get(node).token;
            else if (tupleDefs.containsKey(node)) token = tupleDefs.get(node).token;
            if (mode == RecursionMode.WARN) {
                if (warnedCycles.add(new HashSet<>(cycle)))
                    warnings.add(new CompileWarning(token, msg));
                return; // break the cycle so DFS terminates
            }
            throw new SemanticException(token, msg);
        }
        if (visited.contains(node)) return;

        path.add(node);

        Set<String> callees = callGraph.getOrDefault(node, Collections.emptySet());
        for (String callee : callees) {
            if (callGraph.containsKey(callee)) {
                dfs(callee, callGraph, path, visited, main, voidDefs, typedDefs, tupleDefs);
            }
        }

        path.remove(node);
        visited.add(node);
    }

    /** Visits a statement list (and nested control flow) to collect called function names.
     *  Does NOT recurse into nested function definitions. */
    private static class CallCollector extends EmptyVisitor<Void> {
        private final Set<String> callees;

        CallCollector(Set<String> callees) {
            this.callees = callees;
        }

        @Override
        public Void visitVoidFunctionCall(VoidFunctionCallNode node) {
            callees.add(node.functionName);
            node.arguments.accept(this);
            return null;
        }

        @Override
        public Void visitGlobalFunctionCall(GlobalFunctionCallNode node) {
            callees.add(node.functionName);
            node.arguments.accept(this);
            return null;
        }

        @Override
        public Void visitFunctionCall(FunctionCallNode node) {
            callees.add(node.functionName);
            node.arguments.accept(this);
            return null;
        }

        @Override
        public Void visitTupleDestructure(TupleDestructureNode node) {
            callees.add(node.functionName);
            node.arguments.accept(this);
            return null;
        }

        // Do NOT recurse into nested function definitions
        @Override
        public Void visitVoidFunction(VoidFunctionNode function) {
            return null;
        }

        @Override
        public Void visitTypedFunction(TypedFunctionNode function) {
            return null;
        }

        @Override
        public Void visitTupleFunction(TupleFunctionNode function) {
            return null;
        }
    }
}
