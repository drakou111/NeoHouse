package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main_statements.CommandNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compiler.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compiler.ast.main_statements.MenuNode;
import com.drakou111.neohouse.compiler.ast.main_statements.SlotNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TupleFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.DeclarationNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    @Override public Void visitTupleFunction(TupleFunctionNode fn) {
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

    @Override public Void visitEvent(EventNode node) {
        boolean saved = inFunction;
        inFunction = true;
        super.visitEvent(node);
        inFunction = saved;
        return null;
    }

    @Override public Void visitLoop(LoopNode node) {
        boolean saved = inFunction;
        inFunction = true;
        super.visitLoop(node);
        inFunction = saved;
        return null;
    }

    @Override public Void visitCommand(CommandNode node) {
        boolean saved = inFunction;
        inFunction = true;
        node.statements.accept(this);
        inFunction = saved;
        return null;
    }

    @Override public Void visitMenu(MenuNode menu) {
        for (SlotNode slot : menu.slots) {
            slot.accept(this);
        }
        return null;
    }

    @Override public Void visitSlot(SlotNode slot) {
        boolean saved = inFunction;
        inFunction = true;
        slot.statements.accept(this);
        inFunction = saved;
        return null;
    }

    @Override public Void visitDeclaration(DeclarationNode decl) {
        if (inFunction) {
            throw new SemanticException(decl.token, "Variable declarations must be at top-level scope, not inside functions, events, or loops");
        }
        String fullname = decl.variable.holder + "." + decl.variable.name;
        if (allLocalNames.contains(fullname)) {
            throw new SemanticException(decl.token, "Duplicate local declaration of '" + fullname + "' in same or nested scope");
        }
        allLocalNames.add(fullname);
        return null;
    }

}
