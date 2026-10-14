package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.main.*;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.RandomNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;

import java.util.ArrayList;
import java.util.List;

public class RandomUnfoldingVisitor extends DefaultVisitor {
    private final List<VoidFunctionNode> generated = new ArrayList<>();
    private int nextId = 0;

    @Override
    public MainNode analyze(MainNode main) {
        MainNode rewritten = (MainNode) main.accept(this);
        List<MainStatementNode> all = new ArrayList<>(rewritten.statements);
        all.addAll(generated);
        return new MainNode(all);
    }

    @Override
    public ASTNode visitRandom(RandomNode random) {
        List<StatementListNode> newOptions = new ArrayList<>();
        for (StatementListNode arm : random.statements) {
            if (arm.statements.size() <= 1) {
                newOptions.add(arm);
            } else {
                String fnName = "random$" + (nextId++);
                StatementListNode bodyClone = new StatementListNode(new ArrayList<>(arm.statements));
                VoidFunctionNode helper = new VoidFunctionNode(fnName, "", null, new ParameterListNode(List.of()), bodyClone, random.token);
                generated.add(helper);

                VoidFunctionCallNode call = new VoidFunctionCallNode(fnName, new ArgumentListNode(List.of()), random.token);
                newOptions.add(new StatementListNode(List.of(call)));
            }
        }
        return new RandomNode(newOptions, random.weights, random.token);
    }
}
