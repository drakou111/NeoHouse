package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.main.MainNode;
import com.drakou111.neohouse.compilers.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compilers.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compilers.ast.statement.DeclarationNode;
import com.drakou111.neohouse.compilers.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.exceptions.SemanticException;
import java.util.*;

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

    @Override public Void visitFunction(FunctionNode fn) {
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

    @Override public Void visitConditional(ConditionalNode cond) {
        cond.condition.accept(this);
        cond.thenBlock.accept(this);
        if (cond.elseBlock != null) cond.elseBlock.accept(this);
        return null;
    }
}
