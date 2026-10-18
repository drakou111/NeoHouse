package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.BooleanExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.LogicalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.UnaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.CommandNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compiler.ast.main_statements.MenuNode;
import com.drakou111.neohouse.compiler.ast.main_statements.SlotNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TupleFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.ActionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.RandomNode;
import com.drakou111.neohouse.compiler.ast.statement.RepeatNode;
import com.drakou111.neohouse.compiler.ast.statement.ReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.TupleDestructureNode;
import com.drakou111.neohouse.compiler.ast.statement.TupleReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.TypedReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Housing won't run the same function twice within one execution (one event, command, loop, menu
 * click or function run, including everything it triggers). This pass finds such multi-calls.
 *
 * <p>WARN reports them as warnings, ERROR as errors. FIX gives every call site a function
 * "instance" (the original or a {@code name$N} copy) whose whole call closure is disjoint from what
 * that execution has already run. Copies are built from the original body with the same rule, so a
 * copy calls its own copies of whatever it needs. Branches of an if/else or random are exclusive,
 * and a branch that ends in a return doesn't carry its calls into the code after it.</p>
 *
 * <p>A call that closes a recursion loop (f calling f, or f calling g calling f) is left to the
 * Recursion setting instead of being reported or copied here; what it runs still counts for the
 * calls around it.</p>
 */
public class FunctionDeduplicationVisitor {
    private static final int MAX_COPY_BASE_LENGTH = 47; // Housing names are at most 50 characters

    private final MultiCallMode mode;
    private final List<CompileWarning> warnings;
    /** Callable functions as written, by name. Loops can't be called, so they're only roots. */
    private final Map<String, FunctionNode> originals = new LinkedHashMap<>();
    private final Map<String, FunctionNode> rewritten = new HashMap<>();
    /** Every function name (concrete, copies included) that may run when the key runs, the key included. */
    private final Map<String, Set<String>> closures = new HashMap<>();
    /** Base name -> concrete instances in creation order, the original first. */
    private final Map<String, List<String>> instances = new HashMap<>();
    private final Map<String, Integer> nextCopyIndex = new HashMap<>();
    private final Set<String> inProgress = new HashSet<>();
    private final List<FunctionNode> copies = new ArrayList<>();
    /** Direct callees of each callable function, for finding recursion loops. */
    private final Map<String, Set<String>> calls = new HashMap<>();

    /** Call state while walking one body. {@code live} only holds calls on paths that can still continue. */
    private static final class Context {
        final Set<String> avoid;
        final Set<String> all;
        final String owner;
        /** The function (base name) whose body is walked, or null for an event, command, loop or menu. */
        final String function;
        Set<String> live;
        boolean returned = false;

        Context(Set<String> avoid, Set<String> live, Set<String> all, String owner, String function) {
            this.avoid = avoid;
            this.live = live;
            this.all = all;
            this.owner = owner;
            this.function = function;
        }

        Context branch() {
            return new Context(this.avoid, new HashSet<>(this.live), this.all, this.owner, this.function);
        }

        boolean allows(Set<String> closure) {
            for (String name : closure) if (this.avoid.contains(name) || this.live.contains(name)) return false;
            return true;
        }

        void run(Set<String> closure) {
            this.live.addAll(closure);
            this.all.addAll(closure);
        }
    }

    public FunctionDeduplicationVisitor(MultiCallMode mode, List<CompileWarning> warnings) {
        this.mode = Objects.requireNonNull(mode);
        this.warnings = Objects.requireNonNull(warnings);
    }

    public MainNode analyze(MainNode main) {
        if (this.mode == MultiCallMode.OFF) return main;

        for (MainStatementNode statement : main.statements) {
            if (statement instanceof FunctionNode function && !(statement instanceof LoopNode)) {
                this.originals.put(function.name, function);
                this.instances.put(function.name, new ArrayList<>(List.of(function.name)));
                this.calls.put(function.name, RecursionCheckVisitor.calleesOf(function.statements));
            }
        }

        List<MainStatementNode> result = new ArrayList<>();
        for (MainStatementNode statement : main.statements) {
            result.add(switch (statement) {
                case LoopNode loop -> new LoopNode(loop.name, loop.description, loop.icon,
                        walkRoot(loop.statements, "loop '" + loop.name + "'"), loop.tickDelay, loop.token);
                case FunctionNode function -> {
                    ensureRewritten(function.name);
                    yield this.rewritten.get(function.name);
                }
                case EventNode event -> new EventNode(event.event, walkRoot(event.statements, "event " + event.event.name()), event.token);
                case CommandNode command -> new CommandNode(command.name, command.targeted, command.priority, command.listed,
                        walkRoot(command.statements, "command /" + command.name), command.token);
                case MenuNode menu -> {
                    List<SlotNode> slots = new ArrayList<>();
                    for (SlotNode slot : menu.slots)
                        slots.add(new SlotNode(slot.slotId, slot.item,
                                walkRoot(slot.statements, "menu '" + menu.name + "' slot " + slot.slotId), slot.token));
                    yield new MenuNode(menu.name, menu.rows, slots, menu.token);
                }
                default -> statement;
            });
        }

        if (this.mode != MultiCallMode.FIX) return main;
        result.addAll(this.copies);
        return new MainNode(result);
    }

    private StatementListNode walkRoot(StatementListNode body, String owner) {
        return walkStatements(body, new Context(Set.of(), new HashSet<>(), new HashSet<>(), owner, null));
    }

    /** Rewrites an original function's body (as its own execution) once, recording its closure. */
    private void ensureRewritten(String name) {
        if (this.rewritten.containsKey(name) || !this.inProgress.add(name)) return;

        FunctionNode function = this.originals.get(name);
        Set<String> all = new HashSet<>(Set.of(name));
        Context context = new Context(Set.of(), new HashSet<>(Set.of(name)), all, "function '" + name + "'", name);
        this.rewritten.put(name, withBody(function, name, walkStatements(function.statements, context)));
        this.closures.put(name, all);
        this.inProgress.remove(name);
    }

    private Set<String> closureOf(String name) {
        return this.closures.getOrDefault(name, Set.of(name));
    }

    /** Picks the instance a call to {@code base} should use in this context, creating a copy in FIX mode. */
    private String resolveCall(String base, Token token, Context context) {
        if (!this.originals.containsKey(base)) return base;
        ensureRewritten(base);

        if (context.function != null && reaches(base, context.function)) {
            context.all.addAll(closureOf(base));
            return base;
        }

        for (String candidate : this.instances.get(base)) {
            if (context.allows(closureOf(candidate))) {
                context.run(closureOf(candidate));
                return candidate;
            }
        }

        if (this.mode != MultiCallMode.FIX) {
            report(token, base, context);
            context.run(closureOf(base));
            return base;
        }

        String copy = createCopy(base, context);
        context.run(closureOf(copy));
        return copy;
    }

    private String createCopy(String base, Context caller) {
        String prefix = base.length() > MAX_COPY_BASE_LENGTH ? base.substring(0, MAX_COPY_BASE_LENGTH) : base;
        String copyName;
        do {
            int index = this.nextCopyIndex.merge(base, 1, Integer::sum) - 1;
            copyName = prefix + "$" + index;
        } while (this.originals.containsKey(copyName));
        this.instances.get(base).add(copyName);

        Set<String> avoid = new HashSet<>(caller.avoid);
        avoid.addAll(caller.live);
        Set<String> all = new HashSet<>(Set.of(copyName));
        Context context = new Context(avoid, new HashSet<>(Set.of(copyName)), all, "function '" + copyName + "'", base);

        FunctionNode original = this.originals.get(base);
        this.copies.add(withBody(original, copyName, walkStatements(original.statements, context)));
        this.closures.put(copyName, all);
        return copyName;
    }

    /** Whether calling {@code from} can lead back to {@code target}, so a call to it from target is recursion. */
    private boolean reaches(String from, String target) {
        Set<String> seen = new HashSet<>();
        List<String> pending = new ArrayList<>(List.of(from));
        while (!pending.isEmpty()) {
            String name = pending.removeLast();
            if (name.equals(target)) return true;
            if (!seen.add(name)) continue;
            pending.addAll(this.calls.getOrDefault(name, Set.of()));
        }
        return false;
    }

    private void report(Token token, String base, Context context) {
        String message = ("Function '%s' would run more than once in one run of %s, which Housing doesn't allow. "
                + "Set Multi-call mode to Fix to create copies automatically.").formatted(base, context.owner);
        if (this.mode == MultiCallMode.ERROR) throw new SemanticException(token, message);
        this.warnings.add(new CompileWarning(token, message));
    }

    private StatementListNode walkStatements(StatementListNode body, Context context) {
        List<StatementNode> result = new ArrayList<>();
        for (StatementNode statement : body.statements) {
            result.add(walkStatement(statement, context));
            if (statement instanceof ReturnNode) context.returned = true;
        }
        return new StatementListNode(result);
    }

    private StatementNode walkStatement(StatementNode statement, Context context) {
        return switch (statement) {
            case VoidFunctionCallNode call -> {
                ArgumentListNode arguments = walkArguments(call.arguments, context);
                yield new VoidFunctionCallNode(resolveCall(call.functionName, call.token, context), arguments, call.token);
            }
            case GlobalFunctionCallNode call -> {
                ArgumentListNode arguments = walkArguments(call.arguments, context);
                yield new GlobalFunctionCallNode(resolveCall(call.functionName, call.token, context), arguments, call.token);
            }
            case TupleDestructureNode destructure -> {
                ArgumentListNode arguments = walkArguments(destructure.arguments, context);
                String target = resolveCall(destructure.functionName, destructure.token, context);
                yield new TupleDestructureNode(destructure.targets, destructure.operator, target, arguments, destructure.token);
            }
            case AssignationNode assignment -> new AssignationNode(assignment.variable, assignment.operator,
                    walkExpression(assignment.value, context), assignment.token);
            case TypedReturnNode ret -> ret.expression == null ? ret : new TypedReturnNode(walkExpression(ret.expression, context), ret.token);
            case TupleReturnNode ret -> {
                List<NormalExpressionNode> expressions = new ArrayList<>();
                for (NormalExpressionNode expression : ret.expressions) expressions.add(walkExpression(expression, context));
                yield new TupleReturnNode(expressions, ret.token);
            }
            case ActionCallNode action -> new ActionCallNode(action.name, walkArguments(action.arguments, context), action.token);
            case ConditionalNode conditional -> walkConditional(conditional, context);
            case RandomNode random -> walkRandom(random, context);
            // Repeats are unrolled before this pass; kept for safety, the body is one execution path.
            case RepeatNode repeat -> new RepeatNode(repeat.count, walkStatements(repeat.statements, context), repeat.token);
            default -> statement;
        };
    }

    private StatementNode walkConditional(ConditionalNode conditional, Context context) {
        ExpressionNode condition = conditional.condition == null ? null : walkCondition(conditional.condition, context);

        Context thenContext = context.branch();
        StatementListNode thenBlock = walkStatements(conditional.thenBlock, thenContext);
        Context elseContext = context.branch();
        StatementListNode elseBlock = conditional.elseBlock == null ? null : walkStatements(conditional.elseBlock, elseContext);

        mergeBranches(context, List.of(thenContext, elseContext));
        return new ConditionalNode(condition, thenBlock, elseBlock, conditional.token);
    }

    private StatementNode walkRandom(RandomNode random, Context context) {
        List<StatementListNode> options = new ArrayList<>();
        List<Context> branches = new ArrayList<>();
        for (StatementListNode option : random.statements) {
            Context branch = context.branch();
            options.add(walkStatements(option, branch));
            branches.add(branch);
        }
        mergeBranches(context, branches);
        return new RandomNode(options, random.weights, random.token);
    }

    /** After exclusive branches, the calls that may have run are those of every branch that didn't return. */
    private static void mergeBranches(Context context, List<Context> branches) {
        Set<String> live = new HashSet<>();
        boolean allReturned = true;
        for (Context branch : branches) {
            if (branch.returned) continue;
            allReturned = false;
            live.addAll(branch.live);
        }
        context.live = live;
        context.returned = allReturned;
    }

    private ArgumentListNode walkArguments(ArgumentListNode arguments, Context context) {
        List<NormalExpressionNode> result = new ArrayList<>();
        for (NormalExpressionNode argument : arguments.expressions) result.add(walkExpression(argument, context));
        return new ArgumentListNode(result);
    }

    private ExpressionNode walkCondition(ExpressionNode condition, Context context) {
        return switch (condition) {
            case LogicalExpressionNode logical -> {
                List<ExpressionNode> conditions = new ArrayList<>();
                for (ExpressionNode single : logical.conditions) conditions.add(walkCondition(single, context));
                yield new LogicalExpressionNode(conditions, logical.operator, logical.token);
            }
            case BooleanExpressionNode comparison -> new BooleanExpressionNode(walkExpression(comparison.left, context),
                    walkExpression(comparison.right, context), comparison.operator, comparison.token);
            case NormalExpressionNode expression -> walkExpression(expression, context);
            default -> condition;
        };
    }

    private NormalExpressionNode walkExpression(NormalExpressionNode expression, Context context) {
        return switch (expression) {
            case FunctionCallNode call -> {
                ArgumentListNode arguments = walkArguments(call.arguments, context);
                yield new FunctionCallNode(resolveCall(call.functionName, call.token, context), arguments, call.type, call.token);
            }
            case BinaryExpressionNode binary -> new BinaryExpressionNode(walkExpression(binary.left, context),
                    walkExpression(binary.right, context), binary.operator, binary.type, binary.token);
            case UnaryExpressionNode unary -> new UnaryExpressionNode(unary.operator,
                    walkExpression(unary.operand, context), unary.type, unary.token);
            default -> expression;
        };
    }

    private static FunctionNode withBody(FunctionNode function, String name, StatementListNode body) {
        return switch (function) {
            case VoidFunctionNode v -> new VoidFunctionNode(name, v.description, v.icon, v.parameters, body, v.token);
            case TypedFunctionNode t -> new TypedFunctionNode(name, t.description, t.icon, t.returnType, t.parameters, body, t.token);
            case TupleFunctionNode t -> new TupleFunctionNode(name, t.description, t.icon, t.returnTypes, t.parameters, body, t.token);
            default -> throw new IllegalStateException("Unexpected function kind: " + function.getClass().getSimpleName());
        };
    }
}
