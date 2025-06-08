package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.DeclarationNode;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Pass 1: collect all global‐scope declarations and all local‐only declarations.
 *   - globalDecls: maps fullName ("p.x", "g.foo", etc) → VariableType
 *   - allLocalNames: a set of every local‐declared name (to catch ambiguity later)
 * <p>
 * Any duplicate global declaration (same "p.x" twice) throws immediately.
 * We do *not* resolve or insert undeclared assignments here.
 */
public class DeclarationCollectorVisitor extends EmptyVisitor<Void> {
    public final Map<String, VariableType> globalDecls = new HashMap<>();
    public final Set<String> allLocalNames = new HashSet<>();
    private boolean inFunction = false;

    public void analyze(MainNode main) {
        visitMain(main);
    }

    @Override public Void visitGlobalDeclaration(GlobalDeclarationNode node) {
        String fullname = node.variable.holder + "." + node.variable.name;
        VariableType vt = node.variable.type;
        if (globalDecls.containsKey(fullname)) {
            throw new SemanticException(node.token, "Duplicate global declaration of '" + fullname + "'");
        }
        globalDecls.put(fullname, vt);
        return null;
    }

    @Override public Void visitTypedFunction(TypedFunctionNode fn) {
        boolean saved = inFunction;
        inFunction = true;
        fn.parameters.accept(this);
        fn.statements.accept(this);
        inFunction = saved;
        return null;
    }

    @Override public Void visitVoidFunction(VoidFunctionNode fn) {
        boolean saved = inFunction;
        inFunction = true;
        fn.parameters.accept(this);
        fn.statements.accept(this);
        inFunction = saved;
        return null;
    }

    // 3) Any local declaration anywhere (function body, conditional, etc.)
    @Override public Void visitDeclaration(DeclarationNode decl) {
        // decl.variable() holds the VariableNode with holder/name and type
        String fullname = decl.variable.holder + "." + decl.variable.name;
        if (!inFunction) {
            // That would be a non‐function‐scoped “var” too early (shouldn't happen),
            // but if it does, treat it as a "local" too.
        }
        if (allLocalNames.contains(fullname)) {
            throw new SemanticException(decl.token, "Duplicate local declaration of '" + fullname + "' in same or nested scope");
        }
        allLocalNames.add(fullname);
        return null;
    }
}
