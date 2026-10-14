package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.ParameterListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.CommandNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compiler.ast.main_statements.MenuNode;
import com.drakou111.neohouse.compiler.ast.main_statements.SlotNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.ActionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.CancelNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.RandomNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidReturnNode;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionCountSplittingVisitor extends DefaultVisitor {
    private final List<VoidFunctionNode> all = new ArrayList<>();
    private int nextHelperId = 0;
    private String currentScopeName = "";

    public int getHelpersCreated() { return all.size(); }

    public List<String> getHelperNames() {
        return all.stream().map(fn -> fn.name).toList();
    }

    @Override
    public MainNode analyze(MainNode main) {
        MainNode rewritten = (MainNode) main.accept(this);
        List<MainStatementNode> all = new ArrayList<>(rewritten.statements);
        all.addAll(this.all);
        return new MainNode(all);
    }

    @Override
    public ASTNode visitVoidFunction(VoidFunctionNode fn) {
        currentScopeName = fn.name;
        StatementListNode stmts = (StatementListNode)fn.statements.accept(this);
        StatementListNode body = splitIfNeeded(stmts, false);
        return new VoidFunctionNode(fn.name, fn.description, fn.icon, fn.parameters, body, fn.token);
    }

    @Override
    public ASTNode visitEvent(EventNode ev) {
        currentScopeName = ev.event.name().toLowerCase();
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
        currentScopeName = loop.name;
        StatementListNode statements = (StatementListNode)loop.statements.accept(this);
        StatementListNode body = splitIfNeeded(statements, false);
        return new LoopNode(loop.name, loop.description, loop.icon, body, loop.tickDelay, loop.token);
    }

    @Override
    public ASTNode visitCommand(CommandNode cmd) {
        currentScopeName = cmd.name;
        StatementListNode stmts = (StatementListNode)cmd.statements.accept(this);
        StatementListNode body = splitIfNeeded(stmts, false);
        return new CommandNode(cmd.name, cmd.targeted, cmd.priority, cmd.listed, body, cmd.token);
    }

    @Override
    public ASTNode visitMenu(MenuNode menu) {
        List<SlotNode> slots = new ArrayList<>();
        for (SlotNode slot : menu.slots) {
            currentScopeName = menu.name + "_slot" + slot.slotId;
            slots.add((SlotNode) slot.accept(this));
        }
        return new MenuNode(menu.name, menu.rows, slots, menu.token);
    }

    @Override
    public ASTNode visitSlot(SlotNode slot) {
        StatementListNode stmts = (StatementListNode) slot.statements.accept(this);
        StatementListNode body = splitIfNeeded(stmts, false);
        return new SlotNode(slot.slotId, slot.item, body, slot.token);
    }

    @Override
    public ASTNode visitRandom(RandomNode rand) {
        Map<AddAction, Long> totals = new HashMap<>();
        for (int i = 0; i < rand.statements.size(); i++) {
            StatementListNode branch = rand.statements.get(i);
            long weight = rand.weights.get(i);

            if (branch.statements.isEmpty()) continue;
            StatementNode stmt = branch.statements.get(0);
            AddAction action = extractAction(stmt);
            if (action == null) continue;

            long total = totals.merge(action, weight, Long::sum);
            if (total > action.maxCount) {
                throw new SemanticException(stmt.token,
                        "Random block contains " + total + " '" + action.displayName + "' actions"
                        + " (counting branch weights), which exceeds the maximum of " + action.maxCount
                        + ". Reduce the number of branches or their weights.");
            }
        }
        return super.visitRandom(rand);
    }

    private StatementListNode splitIfNeeded(StatementListNode stmts, boolean inCondition) {
        List<StatementNode> list = new ArrayList<>(stmts.statements);
        List<StatementNode> result = new ArrayList<>();
        Map<AddAction, Integer> outerCounts = new HashMap<>();

        List<StatementNode> condBuffer = null;
        Map<AddAction, Integer> condCounts = null;

        for (int i = 0; i < list.size(); i++) {
            StatementNode s = list.get(i);
            AddAction a = extractAction(s);
            // ConditionalNodes and RandomNodes must stay in the outer scope - no nesting.
            boolean mustGoOuter = s instanceof ConditionalNode || s instanceof RandomNode;

            if (!inCondition && condBuffer != null) {
                if (!mustGoOuter && (a == null || condCounts.getOrDefault(a, 0) < a.maxCount)) {
                    if (a != null) condCounts.merge(a, 1, Integer::sum);
                    condBuffer.add(s);
                    continue;
                }
                result.add(new ConditionalNode(null, new StatementListNode(condBuffer), null, condBuffer.getFirst().token));
                condBuffer = null;
                condCounts = null;
            }

            // --- Try to place into the outer scope ---
            if (a == null || outerCounts.getOrDefault(a, 0) < a.maxCount) {
                if (a != null) outerCounts.merge(a, 1, Integer::sum);
                result.add(s);
                continue;
            }

            if (!inCondition && !mustGoOuter
                    && outerCounts.getOrDefault(AddAction.CONDITIONAL, 0) < AddAction.CONDITIONAL.maxCount) {
                outerCounts.merge(AddAction.CONDITIONAL, 1, Integer::sum);
                condBuffer = new ArrayList<>();
                condBuffer.add(s);
                condCounts = new HashMap<>();
                condCounts.put(a, 1);
                continue;
            }

            List<StatementNode> tail = new ArrayList<>(list.subList(i, list.size()));
            String helperName = currentScopeName + "_s$" + nextHelperId++;
            int slot = all.size();
            all.add(null);
            StatementListNode helperBody = splitIfNeeded(new StatementListNode(tail), false);
            VoidFunctionNode helper = new VoidFunctionNode(helperName, "", null, new ParameterListNode(List.of()), helperBody, tail.getFirst().token);
            all.set(slot, helper);
            result.add(new VoidFunctionCallNode(helperName, new ArgumentListNode(List.of()), tail.getFirst().token));
            return new StatementListNode(result);
        }

        // Flush any remaining open buffer.
        if (condBuffer != null && !condBuffer.isEmpty()) {
            result.add(new ConditionalNode(null, new StatementListNode(condBuffer), null, condBuffer.getFirst().token));
        }

        return new StatementListNode(result);
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
