package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.main.*;
import com.drakou111.neohouse.compiler.ast.main_statements.*;
import com.drakou111.neohouse.compiler.ast.statement.*;
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
        return new VoidFunctionNode(fn.name, fn.description, fn.icon, fn.parameters, body, fn.isConst, fn.token);
    }

    @Override
    public ASTNode visitEvent(EventNode ev) {
        currentScopeName = ev.event.name().toLowerCase();
        StatementListNode stmts = (StatementListNode)ev.statements.accept(this);
        StatementListNode body = splitIfNeeded(stmts, false);
        return new EventNode(ev.event, body, ev.isConst, ev.token);
    }

    @Override
    public ASTNode visitConditional(ConditionalNode c) {
        StatementListNode thenBlock = (StatementListNode)c.thenBlock.accept(this);
        StatementListNode elseBlock = null;
        if (c.elseBlock != null) {
            elseBlock = (StatementListNode)c.elseBlock.accept(this);
        }
        // Already inside a conditional — Housing forbids nesting, so helpers only.
        StatementListNode thenB = splitIfNeeded(thenBlock, true);
        StatementListNode elseB = elseBlock == null ? null : splitIfNeeded(elseBlock, true);
        return new ConditionalNode(c.condition, thenB, elseB, c.token);
    }

    @Override
    public ASTNode visitLoop(LoopNode loop) {
        currentScopeName = loop.name;
        StatementListNode statements = (StatementListNode)loop.statements.accept(this);
        StatementListNode body = splitIfNeeded(statements, false);
        return new LoopNode(loop.name, loop.description, loop.icon, body, loop.tickDelay, loop.isConst, loop.token);
    }

    @Override
    public ASTNode visitCommand(CommandNode cmd) {
        currentScopeName = cmd.name;
        StatementListNode stmts = (StatementListNode)cmd.statements.accept(this);
        StatementListNode body = splitIfNeeded(stmts, false);
        return new CommandNode(cmd.name, cmd.targeted, cmd.priority, cmd.listed, body, cmd.isConst, cmd.token);
    }

    @Override
    public ASTNode visitMenu(MenuNode menu) {
        List<SlotNode> slots = new ArrayList<>();
        for (SlotNode slot : menu.slots) {
            currentScopeName = menu.name + "_slot" + slot.slotId;
            slots.add((SlotNode) slot.accept(this));
        }
        return new MenuNode(menu.name, menu.rows, slots, menu.isConst, menu.token);
    }

    @Override
    public ASTNode visitSlot(SlotNode slot) {
        StatementListNode stmts = (StatementListNode) slot.statements.accept(this);
        StatementListNode body = splitIfNeeded(stmts, false);
        return new SlotNode(slot.slotId, slot.item, body, slot.token);
    }

    @Override
    public ASTNode visitRandom(RandomNode rand) {
        // Housing materialises a random block by duplicating each branch `weight` times,
        // so the block holds sum(weight) copies of each branch's action. Every action type
        // therefore has to stay within its per-block maxCount across the whole random block.
        Map<AddAction, Long> totals = new HashMap<>();
        for (int i = 0; i < rand.statements.size(); i++) {
            StatementListNode branch = rand.statements.get(i);
            long weight = rand.weights.get(i);

            // Every branch has exactly 1 statement after RandomUnfoldingVisitor
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

    /**
     * Distributes statements across the current scope while respecting Housing's per-scope limits.
     *
     * When inCondition=false (top-level scope):
     *   Greedily packs overflow into empty if(){} wrappers (each gets a fresh counter).
     *   Only falls back to a helper function once all 25 conditional slots are used up.
     *   This minimises helper functions: a single scope can hold up to
     *   maxCount (flat) + 25 * maxCount (in conditionals) of each action type.
     *
     * When inCondition=true (inside a user conditional):
     *   Housing forbids nested conditionals, so overflow goes straight to helpers.
     */
    private StatementListNode splitIfNeeded(StatementListNode stmts, boolean inCondition) {
        List<StatementNode> list = new ArrayList<>(stmts.statements);
        List<StatementNode> result = new ArrayList<>();
        Map<AddAction, Integer> outerCounts = new HashMap<>();

        // Currently open empty-if buffer (null = none open).
        List<StatementNode> condBuffer = null;
        Map<AddAction, Integer> condCounts = null;

        for (int i = 0; i < list.size(); i++) {
            StatementNode s = list.get(i);
            AddAction a = extractAction(s);
            // ConditionalNodes and RandomNodes must stay in the outer scope — no nesting.
            boolean mustGoOuter = s instanceof ConditionalNode || s instanceof RandomNode;

            // --- Try to place into the open conditional buffer ---
            if (!inCondition && condBuffer != null) {
                if (!mustGoOuter && (a == null || condCounts.getOrDefault(a, 0) < a.maxCount)) {
                    if (a != null) condCounts.merge(a, 1, Integer::sum);
                    condBuffer.add(s);
                    continue;
                }
                // Buffer full or mustGoOuter — flush it to the outer scope first.
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

            // Outer scope is full for this action type.
            // Open a new empty-if buffer if we still have conditional slots available.
            if (!inCondition && !mustGoOuter
                    && outerCounts.getOrDefault(AddAction.CONDITIONAL, 0) < AddAction.CONDITIONAL.maxCount) {
                outerCounts.merge(AddAction.CONDITIONAL, 1, Integer::sum);
                condBuffer = new ArrayList<>();
                condBuffer.add(s);
                condCounts = new HashMap<>();
                if (a != null) condCounts.put(a, 1);
                continue;
            }

            // No room anywhere — emit a helper function for the tail [i..end).
            if (condBuffer != null) {
                result.add(new ConditionalNode(null, new StatementListNode(condBuffer), null, condBuffer.getFirst().token));
                condBuffer = null;
            }
            List<StatementNode> tail = new ArrayList<>(list.subList(i, list.size()));
            String helperName = currentScopeName + "_s$" + nextHelperId++;
            StatementListNode helperBody = splitIfNeeded(new StatementListNode(tail), false);
            VoidFunctionNode helper = new VoidFunctionNode(helperName, "", null, new ParameterListNode(List.of()), helperBody, false, tail.getFirst().token);
            all.add(helper);
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
