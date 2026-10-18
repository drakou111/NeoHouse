package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.BooleanExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.LogicalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.DoubleValue;
import com.drakou111.neohouse.compiler.ast.expr.constants.LongValue;
import com.drakou111.neohouse.compiler.ast.expr.constants.StringValue;
import com.drakou111.neohouse.compiler.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.RandomNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidReturnNode;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.enums.BinaryOperator;
import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.compiler.enums.VariableType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Saves actions spent on compiler-made variables ({@link CompilerTemps}); every write to a variable
 * the script names is kept as written. Works on the lowered bodies (one action per statement):
 * <ul>
 *   <li>constant steps on a temp are merged: {@code t = 3; t += 4} is {@code t = 7}, and for longs
 *       {@code t += 2; t -= 5} is {@code t -= 3} and {@code t *= -1; t *= -1} is {@code t *= 1};</li>
 *   <li>a value built in a temp only to be copied into another temp is built there directly:
 *       {@code t = x; t *= x; o$ = t} is {@code o$ = x; o$ *= x}, and a temp that is only a copy
 *       goes straight to its destination, the script's variables included: {@code t = x; v = t} is
 *       {@code v = x}, the same single write; steps that start from a script variable's own value are
 *       done on it directly: {@code t = v; t <<= 2; v = t} is {@code v <<= 2};</li>
 *   <li>a temp copied from another temp (or a call's result) that isn't needed afterwards is used
 *       in place: {@code t1 = o$; t1 += 2; r = t1} is {@code o$ += 2; r = o$};</li>
 *   <li>a long temp that is a variable plus a constant and only compared with a constant is
 *       replaced by the variable: {@code t = p.a; t += 1; if (t > 5)} is {@code if (p.a > 4)};</li>
 *   <li>steps that change nothing ({@code t += 0}, {@code t *= 1}) and writes to expression temps
 *       that are never read are dropped.</li>
 * </ul>
 * Whether a value is still needed is decided by scanning forward in execution order: a call reads
 * the parameter slots and replaces the return slot, and a function that returns values still
 * needs its return slots when it exits.
 */
public class TempOptimizationVisitor extends DefaultVisitor {
    private static final int MAX_ROUNDS = 50;

    /** A variable's identity. */
    private record VarKey(Holder holder, String name, String team) {
        static VarKey of(VariableNode v) {
            return new VarKey(v.holder, v.name, v.teamName);
        }
    }

    /** Where to continue scanning once a nested list ends: after {@code list.get(index)}. */
    private record Frame(List<StatementNode> list, int index) {}

    /** Optimizes one body (a function, event, command, loop or menu slot). */
    @Override
    public ASTNode visitStatements(StatementListNode body) {
        List<StatementNode> list = new ArrayList<>(body.statements);
        Set<VarKey> liveAtExit = returnSlotsWritten(list);
        for (int round = 0; round < MAX_ROUNDS && optimizeList(list, List.of(), liveAtExit); round++) {
            // repeat until nothing changes
        }
        return new StatementListNode(list);
    }

    private boolean optimizeList(List<StatementNode> list, List<Frame> outer, Set<VarKey> liveAtExit) {
        boolean changed = optimizeNested(list, outer, liveAtExit);
        changed |= mergeConstantSteps(list);
        changed |= compareWithoutTemp(list, outer, liveAtExit);
        changed |= computeIntoDestination(list, outer, liveAtExit);
        changed |= useSourceInPlace(list, outer, liveAtExit);
        changed |= removeDeadStores(list, outer, liveAtExit);
        return changed;
    }

    private boolean optimizeNested(List<StatementNode> list, List<Frame> outer, Set<VarKey> liveAtExit) {
        boolean changed = false;
        for (int i = 0; i < list.size(); i++) {
            List<Frame> frames = new ArrayList<>();
            frames.add(new Frame(list, i));
            frames.addAll(outer);
            if (list.get(i) instanceof ConditionalNode c) {
                List<StatementNode> then = new ArrayList<>(c.thenBlock.statements);
                List<StatementNode> otherwise = c.elseBlock == null ? null : new ArrayList<>(c.elseBlock.statements);
                boolean nestedChanged = optimizeList(then, frames, liveAtExit);
                if (otherwise != null) nestedChanged |= optimizeList(otherwise, frames, liveAtExit);
                if (nestedChanged) {
                    list.set(i, new ConditionalNode(c.condition, new StatementListNode(then),
                            otherwise == null ? null : new StatementListNode(otherwise), c.token));
                    changed = true;
                }
            } else if (list.get(i) instanceof RandomNode r) {
                List<StatementListNode> arms = new ArrayList<>();
                boolean nestedChanged = false;
                for (StatementListNode arm : r.statements) {
                    List<StatementNode> statements = new ArrayList<>(arm.statements);
                    nestedChanged |= optimizeList(statements, frames, liveAtExit);
                    arms.add(new StatementListNode(statements));
                }
                if (nestedChanged) {
                    list.set(i, new RandomNode(arms, r.weights, r.token));
                    changed = true;
                }
            }
        }
        return changed;
    }

    // ---- merging constant steps ----

    private boolean mergeConstantSteps(List<StatementNode> list) {
        boolean changed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) instanceof AssignationNode a && CompilerTemps.isTemp(a.variable) && isNeutralStep(a)) {
                list.remove(i);
                changed = true;
            }
        }
        for (int i = 0; i + 1 < list.size(); i++) {
            if (!(list.get(i) instanceof AssignationNode a) || !(list.get(i + 1) instanceof AssignationNode b)) continue;
            if (!CompilerTemps.isTemp(a.variable) || !VarKey.of(a.variable).equals(VarKey.of(b.variable))) continue;
            if (!(b.value instanceof ConstantPrimitiveNode second)) continue;
            Optional<List<StatementNode>> merged = merge(a, b, second);
            if (merged.isEmpty()) continue;
            list.remove(i + 1);
            list.remove(i);
            list.addAll(i, merged.get());
            changed = true;
            i = Math.max(-1, i - 2);
        }
        return changed;
    }

    /** The statements replacing {@code a; b}, or empty when they can't be merged exactly. */
    private static Optional<List<StatementNode>> merge(AssignationNode a, AssignationNode b, ConstantPrimitiveNode second) {
        if (a.operator == AssignmentOperator.ASSIGN && a.value instanceof ConstantPrimitiveNode first) {
            // t = c1; t op= c2 is exactly t = c1 op c2: the same single operation, done once now.
            Optional<BinaryOperator> op = toBinary(b.operator);
            if (op.isEmpty()) return Optional.empty();
            return ConstantFolding.fold(op.get(), first, second, b.token)
                    .map(folded -> List.of(new AssignationNode(a.variable, AssignmentOperator.ASSIGN, folded, a.token)));
        }
        if (a.variable.type != VariableType.LONG || !(a.value instanceof ConstantPrimitiveNode first)
                || !(first.value instanceof LongValue x) || !(second.value instanceof LongValue y)) return Optional.empty();

        // Long additions and multiplications combine exactly, overflow included (64-bit wrapping).
        if (isAddOrSub(a.operator) && isAddOrSub(b.operator)) {
            long sum = signed(a.operator, x.value) + signed(b.operator, y.value);
            if (sum == 0) return Optional.of(List.of());
            boolean subtract = sum < 0 && sum != Long.MIN_VALUE;
            return Optional.of(List.of(new AssignationNode(a.variable,
                    subtract ? AssignmentOperator.SUB_ASSIGN : AssignmentOperator.ADD_ASSIGN,
                    longConstant(subtract ? -sum : sum, a), a.token)));
        }
        if (a.operator == AssignmentOperator.MUL_ASSIGN && b.operator == AssignmentOperator.MUL_ASSIGN)
            return Optional.of(List.of(new AssignationNode(a.variable, AssignmentOperator.MUL_ASSIGN, longConstant(x.value * y.value, a), a.token)));
        return Optional.empty();
    }

    // ---- comparing a variable instead of a temp ----

    private boolean compareWithoutTemp(List<StatementNode> list, List<Frame> outer, Set<VarKey> liveAtExit) {
        boolean changed = false;
        for (int c = 0; c < list.size(); c++) {
            if (!(list.get(c) instanceof ConditionalNode conditional)) continue;
            List<ExpressionNode> parts = conditional.condition instanceof LogicalExpressionNode logical
                    ? logical.conditions : conditional.condition == null ? List.of() : List.of(conditional.condition);
            for (int p = 0; p < parts.size(); p++) {
                if (!(parts.get(p) instanceof BooleanExpressionNode comparison)) continue;
                Optional<Rewrite> rewrite = comparisonRewrite(list, c, comparison, conditional, outer, liveAtExit);
                if (rewrite.isEmpty()) continue;

                List<ExpressionNode> newParts = new ArrayList<>(parts);
                newParts.set(p, rewrite.get().comparison());
                ExpressionNode newCondition = conditional.condition instanceof LogicalExpressionNode logical
                        ? new LogicalExpressionNode(newParts, logical.operator, logical.token) : newParts.getFirst();
                list.set(c, new ConditionalNode(newCondition, conditional.thenBlock, conditional.elseBlock, conditional.token));
                list.remove(rewrite.get().firstStatement() + 1);
                list.remove(rewrite.get().firstStatement());
                changed = true;
                break;
            }
        }
        return changed;
    }

    /** A comparison replacing {@code t > k}, and the index of the two statements that built {@code t}. */
    private record Rewrite(BooleanExpressionNode comparison, int firstStatement) {}

    private Optional<Rewrite> comparisonRewrite(List<StatementNode> list, int c, BooleanExpressionNode comparison,
                                                ConditionalNode conditional, List<Frame> outer, Set<VarKey> liveAtExit) {
        if (!(comparison.left instanceof VariableNode temp) || !CompilerTemps.isExpressionTemp(temp.name)
                || temp.castSuffix != null || temp.type != VariableType.LONG) return Optional.empty();
        if (!(comparison.right instanceof ConstantPrimitiveNode bound) || !(bound.value instanceof LongValue k)) return Optional.empty();
        VarKey t = VarKey.of(temp);
        if (countReferences(conditional.condition, t) != 1 || references(conditional.thenBlock, t)
                || (conditional.elseBlock != null && references(conditional.elseBlock, t))
                || isLiveAfter(t, list, c + 1, outer, liveAtExit)) return Optional.empty();

        int step = c - 1;
        while (step >= 0 && !references(list.get(step), t)) step--;
        if (step < 1 || !(list.get(step) instanceof AssignationNode add) || !isAddOrSub(add.operator)
                || !VarKey.of(add.variable).equals(t) || !(add.value instanceof ConstantPrimitiveNode offset)
                || !(offset.value instanceof LongValue d)) return Optional.empty();
        if (!(list.get(step - 1) instanceof AssignationNode start) || start.operator != AssignmentOperator.ASSIGN
                || !VarKey.of(start.variable).equals(t)) return Optional.empty();

        NormalExpressionNode source = start.value;
        boolean plainVariable = source instanceof VariableNode v && v.castSuffix == null && v.fallback == null && !VarKey.of(v).equals(t);
        if (!plainVariable && !(source instanceof PlaceholderNode)) return Optional.empty();
        // The source is now read at the comparison instead: nothing in between may change it.
        for (int i = step + 1; i < c; i++) {
            if (!(list.get(i) instanceof AssignationNode between)) return Optional.empty();
            if (source instanceof VariableNode v && VarKey.of(between.variable).equals(VarKey.of(v))) return Optional.empty();
        }

        long newBound;
        try {
            newBound = add.operator == AssignmentOperator.ADD_ASSIGN ? Math.subtractExact(k.value, d.value) : Math.addExact(k.value, d.value);
        } catch (ArithmeticException overflow) {
            return Optional.empty();
        }
        BooleanExpressionNode rewritten = new BooleanExpressionNode(source,
                new ConstantPrimitiveNode(new LongValue(newBound), VariableType.LONG, bound.token), comparison.operator, comparison.token);
        return Optional.of(new Rewrite(rewritten, step - 1));
    }

    // ---- computing straight into the destination ----

    private boolean computeIntoDestination(List<StatementNode> list, List<Frame> outer, Set<VarKey> liveAtExit) {
        boolean changed = false;
        for (int j = 0; j < list.size(); j++) {
            if (!(list.get(j) instanceof AssignationNode copy) || copy.operator != AssignmentOperator.ASSIGN) continue;
            if (!(copy.value instanceof VariableNode temp) || !CompilerTemps.isExpressionTemp(temp.name)
                    || temp.castSuffix != null || temp.fallback != null) continue;
            VariableNode destination = copy.variable;
            VarKey t = VarKey.of(temp);
            VarKey d = VarKey.of(destination);
            if (d.equals(t)) continue;

            int start = chainStart(list, j, t);
            if (start < 0 || isLiveAfter(t, list, j + 1, outer, liveAtExit)) continue;
            // A script variable may take a plain copy (t = x; v = t is v = x: still one write of the same
            // value), or the steps when they start from its own value (t = v; t += 2; v = t is v += 2).
            // Otherwise building a value step by step stays in temps.
            boolean plainCopy = start == j - 1;
            boolean fromDestination = !plainCopy && startsFrom(list.get(start), d, temp.type);
            if (!CompilerTemps.isTemp(destination) ? !plainCopy && !fromDestination : destination.type != temp.type) continue;
            int firstStep = fromDestination ? start + 1 : start;
            boolean usesDestination = false;
            for (int i = firstStep; i < j; i++) usesDestination |= references(list.get(i), d);
            if (usesDestination || readInText(list, start, j, t)) continue;

            for (int i = firstStep; i < j; i++) {
                AssignationNode step = (AssignationNode) list.get(i);
                list.set(i, new AssignationNode(destination, step.operator, renamed(step.value, t, destination), step.token));
            }
            list.remove(j);
            if (fromDestination) list.remove(start);
            changed = true;
        }
        return changed;
    }

    /** Whether {@code statement} is {@code t = v}: a plain read of {@code v}, of the temp's type. */
    private static boolean startsFrom(StatementNode statement, VarKey v, VariableType type) {
        return statement instanceof AssignationNode first && first.value instanceof VariableNode source
                && VarKey.of(source).equals(v) && source.castSuffix == null && source.fallback == null && source.type == type;
    }

    /** Index of the {@code t = ...} starting the run of steps on {@code t} that ends just before {@code end}, or -1. */
    private static int chainStart(List<StatementNode> list, int end, VarKey t) {
        for (int i = end - 1; i >= 0; i--) {
            if (!(list.get(i) instanceof AssignationNode step) || !VarKey.of(step.variable).equals(t)) return -1;
            if (step.operator == AssignmentOperator.ASSIGN && !references(step.value, t)) return i;
        }
        return -1;
    }

    // ---- using the source in place ----

    private boolean useSourceInPlace(List<StatementNode> list, List<Frame> outer, Set<VarKey> liveAtExit) {
        boolean changed = false;
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof AssignationNode copy) || copy.operator != AssignmentOperator.ASSIGN
                    || !CompilerTemps.isExpressionTemp(copy.variable.name)) continue;
            if (!(copy.value instanceof VariableNode source) || source.castSuffix != null || source.fallback != null
                    || source.type != copy.variable.type) continue;
            if (!CompilerTemps.isExpressionTemp(source.name) && !CompilerTemps.isReturnSlot(source.name)) continue;
            VarKey t = VarKey.of(copy.variable);
            VarKey s = VarKey.of(source);
            if (s.equals(t)) continue;

            int end = lifetimeEnd(list, i, t, outer, liveAtExit);
            if (end < 0 || readInText(list, i + 1, end, t)) continue;
            boolean touchesSource = false;
            for (int k = i + 1; k < end; k++) touchesSource |= references(list.get(k), s);
            if (touchesSource || isLiveAfter(s, list, end, outer, liveAtExit)) continue;

            for (int k = i + 1; k < end; k++) {
                AssignationNode step = (AssignationNode) list.get(k);
                VariableNode target = VarKey.of(step.variable).equals(t) ? source : step.variable;
                list.set(k, new AssignationNode(target, step.operator, renamed(step.value, t, source), step.token));
            }
            list.remove(i);
            changed = true;
        }
        return changed;
    }

    /**
     * Index just past the last statement that still uses the value {@code list.get(i)} put in
     * {@code t}: the next {@code t = ...} that doesn't read it, or the first statement from which it
     * is no longer needed. Every statement before that index must be an assignment; -1 otherwise.
     */
    private int lifetimeEnd(List<StatementNode> list, int i, VarKey t, List<Frame> outer, Set<VarKey> liveAtExit) {
        for (int k = i + 1; k < list.size(); k++) {
            StatementNode statement = list.get(k);
            if (statement instanceof AssignationNode step && VarKey.of(step.variable).equals(t)
                    && step.operator == AssignmentOperator.ASSIGN && !references(step.value, t)) return k;
            if (!(statement instanceof AssignationNode))
                return isLiveAfter(t, list, k, outer, liveAtExit) ? -1 : k;
        }
        return isLiveAfter(t, list, list.size(), outer, liveAtExit) ? -1 : list.size();
    }

    // ---- dead stores ----

    private boolean removeDeadStores(List<StatementNode> list, List<Frame> outer, Set<VarKey> liveAtExit) {
        boolean changed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (!(list.get(i) instanceof AssignationNode a) || !CompilerTemps.isExpressionTemp(a.variable.name)) continue;
            if (isLiveAfter(VarKey.of(a.variable), list, i + 1, outer, liveAtExit)) continue;
            list.remove(i);
            changed = true;
        }
        return changed;
    }

    // ---- liveness ----

    /** Whether the value {@code v} holds before {@code list.get(from)} may still be read. */
    private boolean isLiveAfter(VarKey v, List<StatementNode> list, int from, List<Frame> outer, Set<VarKey> liveAtExit) {
        Optional<Boolean> decided = scan(v, list, from, liveAtExit);
        if (decided.isPresent()) return decided.get();
        for (Frame frame : outer) {
            decided = scan(v, frame.list(), frame.index() + 1, liveAtExit);
            if (decided.isPresent()) return decided.get();
        }
        return liveAtExit.contains(v);
    }

    /** Live (true), overwritten first (false), or undecided by the end of the list (empty). */
    private Optional<Boolean> scan(VarKey v, List<StatementNode> list, int from, Set<VarKey> liveAtExit) {
        for (int i = from; i < list.size(); i++) {
            StatementNode statement = list.get(i);
            if (statement instanceof AssignationNode a) {
                if (references(a.value, v) || (a.operator != AssignmentOperator.ASSIGN && VarKey.of(a.variable).equals(v)))
                    return Optional.of(true);
                if (VarKey.of(a.variable).equals(v)) return Optional.of(false);
                continue;
            }
            if (statement instanceof VoidReturnNode) return Optional.of(liveAtExit.contains(v));
            if (references(statement, v)) return Optional.of(true);
            if (statement instanceof VoidFunctionCallNode || statement instanceof GlobalFunctionCallNode) {
                if (CompilerTemps.isParameterSlot(v.name())) return Optional.of(true); // the callee reads its arguments
                if (statement instanceof VoidFunctionCallNode && CompilerTemps.isReturnSlot(v.name()))
                    return Optional.of(false); // the callee's return replaces it
                continue;
            }
            // Nested blocks may or may not run, so a call or return inside one only ever keeps a value alive.
            References inside = References.of(statement);
            if (inside.hasCall && CompilerTemps.isParameterSlot(v.name())) return Optional.of(true);
            if (inside.hasReturn && liveAtExit.contains(v)) return Optional.of(true);
        }
        return Optional.empty();
    }

    /** Return slots the body writes: a function returning values still needs them when it exits. */
    private static Set<VarKey> returnSlotsWritten(List<StatementNode> body) {
        Set<VarKey> slots = new HashSet<>();
        for (StatementNode statement : body)
            for (VarKey v : References.of(statement).written)
                if (CompilerTemps.isReturnSlot(v.name())) slots.add(v);
        return slots;
    }

    // ---- helpers ----

    /** Whether {@code v} is read inside text in any of {@code list}'s statements {@code from} to {@code to} (exclusive). */
    private static boolean readInText(List<StatementNode> list, int from, int to, VarKey v) {
        for (int i = from; i < to; i++) if (References.of(list.get(i)).inText.contains(v)) return true;
        return false;
    }

    private static boolean references(ASTNode node, VarKey v) {
        return countReferences(node, v) > 0;
    }

    private static int countReferences(ASTNode node, VarKey v) {
        int count = 0;
        for (VarKey seen : References.of(node).all) if (seen.equals(v)) count++;
        return count;
    }

    /** {@code value} with reads of {@code from} replaced by {@code to} (keeping the read's cast and fallback). */
    private static NormalExpressionNode renamed(NormalExpressionNode value, VarKey from, VariableNode to) {
        if (!(value instanceof VariableNode v) || !VarKey.of(v).equals(from)) return value;
        VariableNode read = new VariableNode(to.type, to.holder, to.name, to.teamName, v.token);
        read.castSuffix = v.castSuffix;
        read.fallback = v.fallback;
        return read;
    }

    private static Optional<BinaryOperator> toBinary(AssignmentOperator op) {
        return switch (op) {
            case ADD_ASSIGN -> Optional.of(BinaryOperator.ADD);
            case SUB_ASSIGN -> Optional.of(BinaryOperator.SUBTRACT);
            case MUL_ASSIGN -> Optional.of(BinaryOperator.MULTIPLY);
            case DIV_ASSIGN -> Optional.of(BinaryOperator.DIVIDE);
            default -> Optional.empty();
        };
    }

    /** {@code t += 0}, {@code t -= 0}, {@code t *= 1} or {@code t /= 1}. */
    private static boolean isNeutralStep(AssignationNode a) {
        if (!(a.value instanceof ConstantPrimitiveNode c)) return false;
        double value;
        if (c.value instanceof LongValue l) value = l.value;
        else if (c.value instanceof DoubleValue d) value = d.value;
        else return false;
        return switch (a.operator) {
            case ADD_ASSIGN, SUB_ASSIGN -> value == 0.0;
            case MUL_ASSIGN, DIV_ASSIGN -> value == 1.0;
            default -> false;
        };
    }

    private static boolean isAddOrSub(AssignmentOperator op) {
        return op == AssignmentOperator.ADD_ASSIGN || op == AssignmentOperator.SUB_ASSIGN;
    }

    private static long signed(AssignmentOperator op, long value) {
        return op == AssignmentOperator.SUB_ASSIGN ? -value : value;
    }

    private static ConstantPrimitiveNode longConstant(long value, AssignationNode at) {
        return new ConstantPrimitiveNode(new LongValue(value), VariableType.LONG, at.token);
    }

    /** Every variable a node mentions (with repeats), the ones it assigns, and whether it holds a call or a return. */
    private static final class References extends DefaultVisitor {
        // A variable read inside text, like "%var.player/t$0%" or "%var.team/x \"red team\" 0%".
        private static final Pattern TEXT_READ = Pattern.compile(
                "%var\\.(player|global|team)/([^\\s%\"]+)(?: (\"[^\"]*\"|[^\\s%\"]+))?");

        private final List<VarKey> all = new ArrayList<>();
        /** Variables read inside text: counted in {@link #all}, but renaming a read can't reach them. */
        private final List<VarKey> inText = new ArrayList<>();
        private final List<VarKey> written = new ArrayList<>();
        private boolean hasCall = false;
        private boolean hasReturn = false;

        static References of(ASTNode node) {
            References refs = new References();
            node.accept(refs);
            return refs;
        }

        @Override
        public ASTNode visitVariable(VariableNode variable) {
            this.all.add(VarKey.of(variable));
            return variable;
        }

        @Override
        public ASTNode visitConstantPrimitive(ConstantPrimitiveNode constant) {
            if (!(constant.value instanceof StringValue text)) return constant;
            Matcher read = TEXT_READ.matcher(text.value);
            while (read.find()) {
                Holder holder = switch (read.group(1)) {
                    case "player" -> Holder.PLAYER;
                    case "global" -> Holder.GLOBAL;
                    default -> Holder.TEAM;
                };
                String team = holder == Holder.TEAM && read.group(3) != null ? read.group(3).replace("\"", "") : null;
                VarKey key = new VarKey(holder, read.group(2), team);
                this.all.add(key);
                this.inText.add(key);
            }
            return constant;
        }

        @Override
        public ASTNode visitAssignation(AssignationNode assignation) {
            this.written.add(VarKey.of(assignation.variable));
            return super.visitAssignation(assignation);
        }

        @Override
        public ASTNode visitVoidFunctionCall(VoidFunctionCallNode call) {
            this.hasCall = true;
            return super.visitVoidFunctionCall(call);
        }

        @Override
        public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode call) {
            this.hasCall = true;
            return super.visitGlobalFunctionCall(call);
        }

        @Override
        public ASTNode visitVoidReturn(VoidReturnNode returnNode) {
            this.hasReturn = true;
            return super.visitVoidReturn(returnNode);
        }

        @Override
        public ASTNode visitStatements(StatementListNode statements) {
            for (StatementNode statement : statements.statements) statement.accept(this);
            return statements;
        }
    }
}
