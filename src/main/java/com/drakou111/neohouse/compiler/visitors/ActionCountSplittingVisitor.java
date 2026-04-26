package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.main.*;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.*;
import com.drakou111.neohouse.constants.AddAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionCountSplittingVisitor extends DefaultVisitor {
    private final List<VoidFunctionNode> all = new ArrayList<>();
    private int nextHelperId = 0;

    @Override
    public MainNode analyze(MainNode main) {
        // Rewrite the bodies of all top‐level statements:
        MainNode rewritten = (MainNode) main.accept(this);
        // Append all generated helpers at the end:
        List<MainStatementNode> all = new ArrayList<>(rewritten.statements);
        all.addAll(this.all);
        return new MainNode(all);
    }

    @Override
    public ASTNode visitVoidFunction(VoidFunctionNode fn) {
        StatementListNode stmts = (StatementListNode)fn.statements.accept(this);
        StatementListNode body = splitIfNeeded(stmts, false);
        return new VoidFunctionNode(fn.name, fn.description, fn.parameters, body, fn.token);
    }

    @Override
    public ASTNode visitEvent(EventNode ev) {
        StatementListNode stmts = (StatementListNode)ev.statements.accept(this);
        StatementListNode body = splitIfNeeded(stmts, false);
        return new EventNode(ev.event, body, ev.token);
    }

    @Override
    public ASTNode visitConditional(ConditionalNode c) {

        StatementListNode thenBlock = (StatementListNode)c.thenBlock.accept(this);
        StatementListNode elseBlock = null;
        if (c.elseBlock != null) {
            elseBlock = (StatementListNode)c.elseBlock.accept(this);
        }
        StatementListNode thenB = splitIfNeeded(thenBlock, true);
        StatementListNode elseB = elseBlock == null ? null : splitIfNeeded(elseBlock, true);
        return new ConditionalNode(c.condition, thenB, elseB, c.token);
    }

    @Override
    public ASTNode visitLoop(LoopNode loop) {
        StatementListNode statements = (StatementListNode)loop.statements.accept(this);
        StatementListNode body = splitIfNeeded(statements, false);
        return new LoopNode(loop.name, loop.description, body, loop.tickDelay, loop.token);
    }

    private StatementListNode splitIfNeeded(StatementListNode stmts, boolean inCondition) {
        // Work on a modifiable copy:
        List<StatementNode> list = new ArrayList<>(stmts.statements);

        // ──────────────── Phase 1 ────────────────
        // Wrap each overflow segment in an empty if() { … } block, unless we're already in one
        if (!inCondition) {
            boolean didWrap;
            do {
                didWrap = false;
                Map<AddAction, Integer> counts = new HashMap<>();
                int lastDelimIdx = -1;

                for (int i = 0; i < list.size(); i++) {
                    StatementNode s = list.get(i);

                    // delimiters reset counts
                    if (s instanceof ConditionalNode || s instanceof RandomNode) {
                        counts.clear();
                        lastDelimIdx = i;
                        continue;
                    }

                    AddAction a = extractAction(s);
                    if (a == null) continue;

                    int c = counts.getOrDefault(a, 0) + 1;
                    counts.put(a, c);

                    if (c > a.maxCount) {
                        // wrap [lastDelimIdx+1 .. i) in an empty if()
                        List<StatementNode> segment = new ArrayList<>(list.subList(lastDelimIdx + 1, i));
                        StatementListNode body = new StatementListNode(segment);
                        ConditionalNode wrapper = new ConditionalNode(null, body, null, segment.getFirst().token);

                        // rebuild:
                        List<StatementNode> rebuilt = new ArrayList<>(list.subList(0, lastDelimIdx + 1));
                        rebuilt.add(wrapper);
                        rebuilt.addAll(list.subList(i, list.size()));
                        list = rebuilt;

                        didWrap = true;
                        break;
                    }
                }
            } while (didWrap);
        }

        // ──────────────── Phase 2 ────────────────
        // Peel off any remaining overflow *tails* into helper functions
        Map<AddAction, Integer> counts = new HashMap<>();
        int overflowIdx = -1;

        for (int i = 0; i < list.size(); i++) {
            StatementNode s = list.get(i);
            AddAction a = extractAction(s);
            if (a == null) continue;

            int c = counts.getOrDefault(a, 0) + 1;
            counts.put(a, c);

            if (c > a.maxCount) {
                overflowIdx = i;
                break;
            }
        }

        // no overflow → done
        if (overflowIdx < 0) return new StatementListNode(list);

        // split off tail [overflowIdx..end) into a helper
        List<StatementNode> tail = new ArrayList<>(list.subList(overflowIdx, list.size()));
        String helperName = "split$" + (nextHelperId++);
        StatementListNode helperBody = splitIfNeeded(new StatementListNode(tail), false);
        VoidFunctionNode helper = new VoidFunctionNode(helperName, "", new ParameterListNode(List.of()), helperBody, tail.getFirst().token);
        all.add(helper);

        // replace tail with helper call
        VoidFunctionCallNode call = new VoidFunctionCallNode(
                helperName, new ArgumentListNode(List.of()), tail.getFirst().token
        );
        List<StatementNode> prefix = new ArrayList<>(list.subList(0, overflowIdx));
        prefix.add(call);
        list = prefix;

        return new StatementListNode(list);
    }



    private AddAction extractAction(StatementNode stmt) {
        return switch (stmt) {
            case ActionCallNode f -> AddAction.requireByName(f.name);
            case VoidFunctionCallNode ignore -> AddAction.TRIGGER_FUNCTION;
            case GlobalFunctionCallNode ignore -> AddAction.TRIGGER_FUNCTION;
            case CancelNode ignore -> AddAction.CANCEL_EVENT;
            case VoidReturnNode ignore -> AddAction.EXIT;
            case RandomNode ignore -> AddAction.RANDOM;
            case AssignationNode ignore -> AddAction.VARIABLE;
            case ConditionalNode ignore -> AddAction.CONDITIONAL;
            default -> null;
        };
    }
}
