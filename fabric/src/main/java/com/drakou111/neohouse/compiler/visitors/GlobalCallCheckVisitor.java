package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.*;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlobalCallCheckVisitor {

    private final GlobalCallMode mode;
    private final List<CompileWarning> warnings;

    public GlobalCallCheckVisitor() {
        this(GlobalCallMode.ERROR, new java.util.ArrayList<>());
    }

    public GlobalCallCheckVisitor(GlobalCallMode mode, List<CompileWarning> warnings) {
        this.mode = mode;
        this.warnings = warnings;
    }

    /** Either records a warning or throws, depending on the configured mode. */
    private void report(com.drakou111.neohouse.compiler.lexer.Token token, String msg) {
        if (mode == GlobalCallMode.WARN) warnings.add(new CompileWarning(token, msg));
        else throw new SemanticException(token, msg);
    }

    public void analyze(MainNode main) {
        if (mode == GlobalCallMode.OFF) return;

        // Phase 1: find all function names that are called via global
        Set<String> globallyCalled = new HashSet<>();
        new GlobalNameCollector(globallyCalled).visitMain(main);

        // Phase 2: for each globally-called void function, check its direct body
        //          for any GlobalFunctionCallNode
        for (MainStatementNode stmt : main.statements) {
            if (stmt instanceof VoidFunctionNode vf && globallyCalled.contains(vf.name)) {
                GlobalCallInBodyChecker bodyChecker = new GlobalCallInBodyChecker(vf.name);
                bodyChecker.visitStatements(vf.statements);
            }
        }

        // Phase 3: for each LoopNode, check its body for GlobalFunctionCallNode
        for (MainStatementNode stmt : main.statements) {
            if (stmt instanceof LoopNode loop) {
                LoopGlobalCallChecker loopChecker = new LoopGlobalCallChecker(loop.name);
                loopChecker.visitStatements(loop.statements);
            }
        }
    }

    /**
     * Scans the entire AST (including function bodies) to find all function names
     * that are targets of at least one GlobalFunctionCallNode.
     */
    private static class GlobalNameCollector extends EmptyVisitor<Void> {
        private final Set<String> names;

        GlobalNameCollector(Set<String> names) {
            this.names = names;
        }

        @Override
        public Void visitGlobalFunctionCall(GlobalFunctionCallNode node) {
            names.add(node.functionName);
            node.arguments.accept(this);
            return null;
        }
    }

    /**
     * Checks the DIRECT body of a globally-called function (does not recurse
     * into nested function definitions) for GlobalFunctionCallNode.
     */
    private class GlobalCallInBodyChecker extends EmptyVisitor<Void> {
        private final String ownerName;

        GlobalCallInBodyChecker(String ownerName) {
            this.ownerName = ownerName;
        }

        @Override
        public Void visitGlobalFunctionCall(GlobalFunctionCallNode node) {
            report(node.token,
                    "Function '" + ownerName + "' is globally called and cannot contain global calls " +
                    "(use 'global " + node.functionName + "()' inside it)");
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

    /**
     * Checks a loop's body for GlobalFunctionCallNode.
     */
    private class LoopGlobalCallChecker extends EmptyVisitor<Void> {
        private final String loopName;

        LoopGlobalCallChecker(String loopName) {
            this.loopName = loopName;
        }

        @Override
        public Void visitGlobalFunctionCall(GlobalFunctionCallNode node) {
            report(node.token,
                    "Loop '" + loopName + "' runs on all players and cannot contain global calls");
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
