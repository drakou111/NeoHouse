package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.OverrideMode;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.*;
import com.drakou111.neohouse.compiler.ast.expr.*;
import com.drakou111.neohouse.compiler.ast.expr.constants.*;
import com.drakou111.neohouse.compiler.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.*;
import com.drakou111.neohouse.compiler.ast.statement.*;
import com.drakou111.neohouse.compiler.enums.*;
import com.drakou111.neohouse.constants.*;
import com.drakou111.neohouse.constants.Comparator;
import com.drakou111.neohouse.constants.others.CustomMenuInputs;
import com.drakou111.neohouse.constants.others.FunctionInputs;
import com.drakou111.neohouse.factories.ActionFactory;
import com.drakou111.neohouse.factories.ConditionFactory;
import com.drakou111.neohouse.factories.MovementFactory;
import com.drakou111.neohouse.factories.params.DropItemParams;
import com.drakou111.neohouse.factories.params.PlaySoundParams;
import com.drakou111.neohouse.factories.params.TitleParams;
import com.drakou111.neohouse.factories.params.VariableParams;
import com.drakou111.neohouse.utils.ActionBuilder;

import java.util.*;

public class CodeGenerator {

    public Action generate(MainNode main) {
        List<Action> parts = new ArrayList<>();
        OverrideMode mode = NeoHouseConfig.get().overrideMode;

        // Pass 0: emit global/team variable initializers
        for (MainStatementNode stmt : main.statements) {
            if (stmt instanceof GlobalDeclarationNode decl && decl.initializer != null) {
                parts.add(generateGlobalInitializer(decl));
                // Wait for "variable" confirmation in chat, then an extra 1.5 s (30 ticks)
                parts.add(new WaitForChatAction("variable", 30));
            }
        }

        // NEVER has its own two-pass flow: create shells for missing functions, then fill
        // only the ones that were empty (i.e. the shells we just added), preserving the rest.
        if (mode == OverrideMode.NEVER) {
            generateNeverMode(main, parts);
            return new GroupAction(parts);
        }

        // Pass 1: prepare shells (mode here is ALWAYS or SKIP_CONST; NEVER already returned).
        // First read every existing function name once, so an overridden function is deleted only
        // when it's actually present (skipping a wasted list search otherwise). Overridden functions
        // are then recreated as bare stubs — their description/icon is applied in Pass 2 right before
        // the body, so stub creation stays a cheap list operation. Const items keep shell-if-absent.
        parts.add(new ActionBuilder().closeAll().sendChatAndWait(Commands.FUNCTIONS).get());
        parts.add(new ReadFunctionNamesAction());
        for (MainStatementNode stmt : main.statements) {
            boolean override = mode == OverrideMode.ALWAYS || !stmtIsConst(stmt);
            if (override) {
                if (stmt instanceof VoidFunctionNode fn) {
                    parts.add(recreateFunctionStub(fn.name));
                } else if (stmt instanceof LoopNode loop) {
                    parts.add(recreateFunctionStub(loop.name));
                } else if (stmt instanceof CommandNode cmd) {
                    parts.add(MovementFactory.goAndRecreateCommand(cmd.name, cmd.targeted, cmd.priority, cmd.listed));
                    parts.add(MovementFactory.exit(true));
                } else if (stmt instanceof MenuNode menu) {
                    parts.add(MovementFactory.goAndRecreateMenu(menu.name, menu.rows));
                    parts.add(MovementFactory.exit(true));
                }
            } else if (stmt instanceof VoidFunctionNode fn) {
                parts.add(createShellUsingRead(fn.name, "function"));
            } else if (stmt instanceof LoopNode loop) {
                parts.add(createShellUsingRead(loop.name, "loop"));
            } else if (stmt instanceof MenuNode menu) {
                parts.add(createMenuShellIfAbsent(menu.name, menu.rows));
            }
        }

        Map<String, VoidFunctionNode> fnByName = collectFunctions(main);
        FunctionCopies fnCopies = findFunctionCopies(main, fnByName.keySet());
        Map<String, List<String>> copiesOf = fnCopies.copiesOf();
        Set<String> isCopy = fnCopies.isCopy();

        // Pass 2: fill in bodies. Overridden items are (re)filled unconditionally; const items in
        // SKIP_CONST are filled only if Pass 1 created them (pre-existing ones are left as-is, and
        // Pass 1 already warned about the skip). All shells exist by now, so any reference resolves.
        for (MainStatementNode stmt : main.statements) {
            boolean override = mode == OverrideMode.ALWAYS || !stmtIsConst(stmt);
            if (stmt instanceof VoidFunctionNode fn) {
                if (override) {
                    if (isCopy.contains(fn.name)) continue; // filled by pasting from its original
                    List<String> copies = copiesOf.getOrDefault(fn.name, List.of());
                    parts.add(!copies.isEmpty() ? generateFunctionWithCopies(fn, copies) : generateFunction(fn));
                } else {
                    // const: fill only what we created this run (each copy handled on its own).
                    parts.add(new RunIfCreatedAction(fnKey(fn.name), generateFunction(fn)));
                }
            } else if (stmt instanceof EventNode event) {
                parts.add(override ? generateEvent(event) : generateEventIfEmpty(event));
            } else if (stmt instanceof LoopNode loop) {
                parts.add(override ? generateLoop(loop) : new RunIfCreatedAction(fnKey(loop.name), generateLoop(loop)));
            } else if (stmt instanceof CommandNode cmd) {
                parts.add(override ? generateCommand(cmd) : generateCommandIfAbsent(cmd));
            } else if (stmt instanceof MenuNode menu) {
                parts.add(override ? generateMenu(menu) : new RunIfCreatedAction(menuKey(menu.name), generateMenu(menu)));
            }
        }
        return new GroupAction(parts);
    }

    /**
     * "Never Override" generation. Pass 1 searches the Functions list for each function/loop: if
     * it's missing we create an empty shell AND note its name; if it's already there we leave it
     * completely untouched. Creating every missing shell first means references between functions
     * (including forward references and mutual calls) resolve when bodies are written in Pass 2.
     * Pass 2 then fills in ONLY the entities we noted as created this run — anything that already
     * existed keeps its content. Detection is purely by list search (no entering functions to
     * inspect them); the created-set is remembered client-side via {@link SequenceManager}.
     * Events keep their fill-if-empty handling; commands and menus create-if-absent.
     */
    private void generateNeverMode(MainNode main, List<Action> parts) {
        // Read every existing function name once so shell creation can decide create-vs-warn from
        // that set instead of searching the list per function.
        parts.add(new ActionBuilder().closeAll().sendChatAndWait(Commands.FUNCTIONS).get());
        parts.add(new ReadFunctionNamesAction());
        // Pass 1: create a shell for every missing function/loop/menu and record which we created.
        for (MainStatementNode stmt : main.statements) {
            if (stmt instanceof VoidFunctionNode fn) {
                parts.add(createShellUsingRead(fn.name, "function"));
            } else if (stmt instanceof LoopNode loop) {
                parts.add(createShellUsingRead(loop.name, "loop"));
            } else if (stmt instanceof MenuNode menu) {
                parts.add(createMenuShellIfAbsent(menu.name, menu.rows));
            }
        }
        // Pass 2: fill only the entities Pass 1 created (never the pre-existing ones).
        for (MainStatementNode stmt : main.statements) {
            if (stmt instanceof VoidFunctionNode fn) {
                parts.add(new RunIfCreatedAction(fnKey(fn.name), generateFunction(fn)));
            } else if (stmt instanceof LoopNode loop) {
                parts.add(new RunIfCreatedAction(fnKey(loop.name), generateLoop(loop)));
            } else if (stmt instanceof EventNode event) {
                parts.add(generateEventIfEmpty(event));
            } else if (stmt instanceof CommandNode cmd) {
                parts.add(generateCommandIfAbsent(cmd));
            } else if (stmt instanceof MenuNode menu) {
                parts.add(new RunIfCreatedAction(menuKey(menu.name), generateMenu(menu)));
            }
        }
    }

    /**
     * Deletes {@code name} (only if it existed when the run started — see {@link ReadFunctionNamesAction})
     * and creates a fresh empty stub. The description/icon are applied in Pass 2 right before the
     * body, so this stays a lean list operation.
     */
    private Action recreateFunctionStub(String name) {
        return new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.FUNCTIONS)
                .append(new IfFunctionExistsAction(name, new ActionBuilder().removeFunctionIfPresent(name).get(), null))
                .anvilInput(name, Others.GENERAL_ADD_H6)
                .get();
    }

    /**
     * Shell-creation that decides existence from the pre-read function set
     * ({@link ReadFunctionNamesAction}) instead of searching the list. If the function already
     * exists, just warn (and leave it for Pass 2 to skip); otherwise create a bare stub and record
     * it. Used by NEVER (all functions) and SKIP_CONST (const functions) — both run after a read pass.
     */
    private Action createShellUsingRead(String name, String kind) {
        Action createStub = new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.FUNCTIONS)
                .anvilInput(name, Others.GENERAL_ADD_H6)
                .append(new MarkCreatedAction(fnKey(name)))
                .get();
        Action warn = new ClientMessageAction("Skipped " + kind + " '" + name + "' — already exists.", true);
        return new IfFunctionExistsAction(name, warn, createStub);
    }

    /**
     * Applies a function/loop's description and icon via its config menu (right-click), if any;
     * a no-op when there's neither. Used at the start of Pass 2 body generation so metadata is set
     * right before editing rather than during the (batched) stub-creation pass.
     */
    private Action setFunctionMeta(String name, String description, String icon) {
        boolean hasDesc = description != null && !description.trim().isEmpty();
        if (!hasDesc && icon == null) return new GroupAction(List.of());
        return new ActionBuilder()
                .gotoFunction(true, name, false)
                .onlyIf(hasDesc, () -> new ActionBuilder().searchForChat(FunctionInputs.DESCRIPTION, description))
                .onlyIf(icon != null, () -> new ActionBuilder().search(FunctionInputs.ICON).customItem(icon))
                .closeAll()
                .get();
    }

    /**
     * Searches the Custom Menus list; if the menu already exists, leaves it untouched and warns; if
     * missing, creates an empty menu of the right size and records it so Pass 2 fills its slots.
     * Shell-creating menus up front lets a function's {@code displayMenu(...)} resolve even when the
     * menu is defined later. (Menus have no read-pass equivalent, so this still searches.)
     */
    private Action createMenuShellIfAbsent(String name, int rows) {
        String rowsLabel = rows == 1 ? "1 Row" : rows + " Rows";
        ActionBuilder create = new ActionBuilder()
                .anvilInput(name, Others.GENERAL_ADD_H6)
                .onlyIf(rows != 1, () -> new ActionBuilder()
                        .gotoCustomMenu(name, true)
                        .search(CustomMenuInputs.CHANGE_SIZE)
                        .search(rowsLabel))
                .closeAll()
                .append(new MarkCreatedAction(menuKey(name)));
        ActionBuilder skip = new ActionBuilder()
                .closeAll()
                .append(new ClientMessageAction("Skipped menu '" + name + "' — already exists.", true));
        return new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.CUSTOM_MENUS)
                .search(name, null, false,
                        skip,   // found → skip + warn
                        create) // missing → create shell + note it
                .get();
    }

    // Registry keys for "created this run" are namespaced so a function and a menu that happen to
    // share a name (they live in separate Housing lists) don't get conflated.
    private static String fnKey(String name)   { return "fn:" + name; }
    private static String menuKey(String name) { return "menu:" + name; }

    /** Collects all top-level void functions, keyed by name, preserving source order. */
    private static Map<String, VoidFunctionNode> collectFunctions(MainNode main) {
        Map<String, VoidFunctionNode> fnByName = new LinkedHashMap<>();
        for (MainStatementNode s : main.statements) {
            if (s instanceof VoidFunctionNode vf) fnByName.put(vf.name, vf);
        }
        return fnByName;
    }

    /** Result of {@link #findFunctionCopies}: which originals have copies, and which names are copies. */
    private record FunctionCopies(Map<String, List<String>> copiesOf, Set<String> isCopy) {}

    /**
     * Detects compiler-created copies: functions named {@code baseName$N} where {@code baseName} is
     * also a defined function. These should be paste-filled rather than manually rebuilt.
     */
    private static FunctionCopies findFunctionCopies(MainNode main, Set<String> definedFunctions) {
        Map<String, List<String>> copiesOf = new LinkedHashMap<>();
        Set<String> isCopy = new HashSet<>();
        for (MainStatementNode s : main.statements) {
            if (s instanceof VoidFunctionNode vf) {
                int dollar = vf.name.lastIndexOf('$');
                if (dollar > 0) {
                    String tail = vf.name.substring(dollar + 1);
                    String base = vf.name.substring(0, dollar);
                    if (!tail.isEmpty() && tail.chars().allMatch(Character::isDigit) && definedFunctions.contains(base)) {
                        copiesOf.computeIfAbsent(base, k -> new ArrayList<>()).add(vf.name);
                        isCopy.add(vf.name);
                    }
                }
            }
        }
        return new FunctionCopies(copiesOf, isCopy);
    }

    private static boolean stmtIsConst(MainStatementNode stmt) {
        return switch (stmt) {
            case VoidFunctionNode fn -> fn.isConst;
            case EventNode ev        -> ev.isConst;
            case LoopNode loop       -> loop.isConst;
            case CommandNode cmd     -> cmd.isConst;
            case MenuNode menu       -> menu.isConst;
            default                  -> false;
        };
    }

    private Action generateFunction(VoidFunctionNode fn) {
        List<Action> parts = new ArrayList<>();
        parts.add(setFunctionMeta(fn.name, fn.description, fn.icon));
        parts.add(MovementFactory.goToFunction(false, fn.name));
        parts.add(generateStatements(fn.statements, RestrictionAction.NONE, RestrictionCondition.NONE));
        parts.add(MovementFactory.exit(true));
        return new GroupAction(parts);
    }

    private Action generateFunctionWithCopies(VoidFunctionNode fn, List<String> copyNames) {
        List<Action> parts = new ArrayList<>();
        parts.add(setFunctionMeta(fn.name, fn.description, fn.icon));
        parts.add(MovementFactory.goToFunction(false, fn.name));
        parts.add(generateStatements(fn.statements, RestrictionAction.NONE, RestrictionCondition.NONE));
        parts.add(new ActionBuilder().copyContext().get());
        parts.add(MovementFactory.exit(true));
        for (String copyName : copyNames) {
            parts.add(MovementFactory.goToFunction(false, copyName));
            parts.add(new ActionBuilder().append(new DeleteAllAction(10)).get());
            parts.add(new ActionBuilder().pasteContext().get());
            parts.add(MovementFactory.exit(true));
        }
        return new GroupAction(parts);
    }

    private Action generateEvent(EventNode event) {
        String key = "EVENT_" + event.event.name();
        RestrictionAction ra = RestrictionAction.valueOf(key);
        RestrictionCondition rc = ra.conditionContext();
        List<Action> parts = new ArrayList<>();
        parts.add(MovementFactory.goToEvent(event.event));
        parts.add(new ActionBuilder().append(new DeleteAllAction(10)).get());
        parts.add(generateStatements(event.statements, ra, rc));
        parts.add(MovementFactory.exit(true));
        return new GroupAction(parts);
    }

    private Action generateLoop(LoopNode loop) {
        boolean hasDesc = loop.description != null && !loop.description.trim().isEmpty();
        List<Action> parts = new ArrayList<>();
        parts.add(MovementFactory.goToFunction(false, loop.name));
        parts.add(generateStatements(loop.statements, RestrictionAction.NONE, RestrictionCondition.NONE));
        // One config-menu visit sets description, icon and the automatic-execution delay.
        parts.add(new ActionBuilder()
                .gotoFunction(true, loop.name, false)
                .onlyIf(hasDesc, () -> new ActionBuilder().searchForChat(FunctionInputs.DESCRIPTION, loop.description))
                .onlyIf(loop.icon != null, () -> new ActionBuilder().search(FunctionInputs.ICON).customItem(loop.icon))
                .searchForAnvil(FunctionInputs.AUTOMATIC_EXECUTION, String.valueOf(loop.tickDelay))
                .closeAll()
                .get());
        return new GroupAction(parts);
    }

    private Action generateCommand(CommandNode cmd) {
        List<Action> parts = new ArrayList<>();
        parts.add(new ActionBuilder().gotoCommand(false, cmd.name, true).get());
        parts.add(generateStatements(cmd.statements, RestrictionAction.NONE, RestrictionCondition.NONE));
        parts.add(MovementFactory.exit(true));
        return new GroupAction(parts);
    }

    private Action generateMenu(MenuNode menu) {
        List<Action> parts = new ArrayList<>();
        parts.add(MovementFactory.goToMenuGrid(menu.name));
        for (SlotNode slot : menu.slots) {
            parts.add(generateSlot(slot));
        }
        parts.add(MovementFactory.exit(true));
        return new GroupAction(parts);
    }

    private Action generateCommandIfAbsent(CommandNode cmd) {
        Action warn = new GroupAction(List.of(
            new ActionBuilder().closeAll().get(),
            new ClientMessageAction("Skipped command '/" + cmd.name + "' — already exists.", true)
        ));
        Action createAndFill = new ActionBuilder()
                .anvilInput(cmd.name, Others.GENERAL_ADD_H6)
                .append(new ActionBuilder().gotoCommand(true, cmd.name, true).get())
                .append(generateStatements(cmd.statements, RestrictionAction.NONE, RestrictionCondition.NONE))
                .closeAll()
                .get();
        return new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.COMMANDS)
                .search("/" + cmd.name, null, false, new ActionBuilder().append(warn), new ActionBuilder().append(createAndFill))
                .get();
    }

    private Action generateEventIfEmpty(EventNode event) {
        String key = "EVENT_" + event.event.name();
        RestrictionAction ra = RestrictionAction.valueOf(key);
        RestrictionCondition rc = ra.conditionContext();
        Action warn = new GroupAction(List.of(
            new ActionBuilder().closeAll().get(),
            new ClientMessageAction("Skipped event '" + event.event.name() + "' — already has content.", true)
        ));
        Action fill = new GroupAction(List.of(
            new DeleteAllAction(10),
            generateStatements(event.statements, ra, rc),
            MovementFactory.exit(true)
        ));
        return new GroupAction(List.of(
            MovementFactory.goToEvent(event.event),
            new CheckSlotEmptyAction(10, fill, warn)
        ));
    }

    private Action generateSlot(SlotNode slot) {
        List<Action> parts = new ArrayList<>();
        parts.add(new ActionBuilder()
                .clickSlotAndWait(slot.slotId, false)
                .customItem(slot.item)
                .clickSlotAndWait(slot.slotId, true)
                .get());
        parts.add(generateStatements(slot.statements, RestrictionAction.MENU, RestrictionCondition.NONE));
        parts.add(new ActionBuilder().clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true).get());
        return new GroupAction(parts);
    }

    private Action generateGlobalInitializer(GlobalDeclarationNode decl) {
        if (!(decl.initializer instanceof ConstantPrimitiveNode c)) {
            throw new IllegalStateException("Variable initializer must be a constant value (e.g. 0, 1.5, \"text\")");
        }
        String value = primitiveToInitString(c.value);
        String cmd = switch (decl.variable.holder) {
            case GLOBAL -> "/variable global set " + decl.variable.name + " " + value;
            case TEAM   -> "/variable team set " + decl.variable.teamName + " " + decl.variable.name + " " + value;
            case PLAYER -> throw new IllegalStateException("Player variables cannot have initializers");
        };
        return new ChatAction(cmd);
    }

    private String primitiveToInitString(ConstantPrimitive prim) {
        return switch (prim) {
            case StringValue s -> s.value;
            case LongValue l   -> l.toString();
            case DoubleValue d -> d.toString();
            case BoolValue b   -> b.value ? "1" : "0";
        };
    }

    private record RepeatPattern(int period, int count) {}

    private RepeatPattern findRepeat(List<StatementNode> stmts, int startIdx) {
        int n = stmts.size() - startIdx;
        for (int period = 1; period <= n / 2; period++) {
            int maxReps = n / period;
            int count = 1;
            for (int rep = 1; rep < maxReps; rep++) {
                boolean match = true;
                for (int j = 0; j < period; j++) {
                    String a = stmts.get(startIdx + j).toShortString();
                    String b = stmts.get(startIdx + rep * period + j).toShortString();
                    if (!a.equals(b)) { match = false; break; }
                }
                if (match) count = rep + 1;
                else break;
            }
            if (count > 1) return new RepeatPattern(period, count);
        }
        return null;
    }

    private Action generateStatements(StatementListNode stmts, RestrictionAction ra, RestrictionCondition rc) {
        List<StatementNode> list = stmts.statements;
        List<Action> actions = new ArrayList<>();
        boolean containerEmpty = true;
        int i = 0;
        while (i < list.size()) {
            if (containerEmpty) {
                RepeatPattern rp = findRepeat(list, i);
                if (rp != null) {
                    for (int j = i; j < i + rp.period; j++)
                        actions.add(generateStatement(list.get(j), ra, rc));
                    actions.add(new ActionBuilder().copyContext().get());
                    for (int k = 1; k < rp.count; k++)
                        actions.add(new ActionBuilder().pasteContext().get());
                    i += rp.period * rp.count;
                    containerEmpty = false;
                    continue;
                }
            }
            actions.add(generateStatement(list.get(i), ra, rc));
            containerEmpty = false;
            i++;
        }
        return new GroupAction(actions);
    }

    private Action generateStatement(StatementNode stmt, RestrictionAction ra, RestrictionCondition rc) {
        return switch (stmt) {
            case AssignationNode assign -> generateAssignation(assign, ra);
            case ConditionalNode cond -> generateConditional(cond, ra, rc);
            case RandomNode rand -> generateRandom(rand, ra, rc);
            case CancelNode ignored -> ActionFactory.addActionCancelEvent(ra);
            case VoidReturnNode ignored -> ra == RestrictionAction.NONE
                    ? new GroupAction(List.of())
                    : ActionFactory.addActionExit(ra);
            case VoidFunctionCallNode call -> ActionFactory.addActionTriggerFunction(ra, call.functionName, false, null);
            case GlobalFunctionCallNode call -> ActionFactory.addActionTriggerFunction(ra, call.functionName, true, null);
            case ActionCallNode call -> generateActionCall(call, ra);
            case DeclarationNode ignored -> new GroupAction(List.of()); // type annotation only, no Housing action
            default -> throw new IllegalStateException("Unhandled statement in code generation: " + stmt.getClass().getSimpleName());
        };
    }

    private Action generateAssignation(AssignationNode assign, RestrictionAction ra) {
        Holder holder = toHolder(assign.variable.holder);
        String variable = assign.variable.name;
        String teamName = assign.variable.teamName;
        Operator operator = toOperator(assign.operator);
        String value = exprToValue(assign.value);
        return ActionFactory.addActionVariable(ra, new VariableParams()
                .holder(holder)
                .variable(variable)
                .teamName(teamName)
                .operator(operator)
                .value(value));
    }

    private Action generateConditional(ConditionalNode cond, RestrictionAction ra, RestrictionCondition rc) {
        RestrictionAction innerRa = ra.conditionalVariant();
        if (cond.condition == null) {
            Action thenAction = generateStatements(cond.thenBlock, innerRa, rc);
            return ActionFactory.addActionConditional(ra, false, null, thenAction, null, null);
        }
        boolean matchAny = cond.condition instanceof LogicalExpressionNode log
                && log.operator == LogicalOperator.OR;
        Action conditions = generateConditionExpr(cond.condition, rc);
        Action thenAction = generateStatements(cond.thenBlock, innerRa, rc);
        Action elseAction = cond.elseBlock != null && !cond.elseBlock.statements.isEmpty() ? generateStatements(cond.elseBlock, innerRa, rc) : null;
        return ActionFactory.addActionConditional(ra, matchAny, conditions, thenAction, elseAction, null);
    }

    private Action generateRandom(RandomNode rand, RestrictionAction ra, RestrictionCondition rc) {
        RestrictionAction innerRa = ra.conditionalVariant();
        List<Action> branches = new ArrayList<>();
        for (int i = 0; i < rand.statements.size(); i++) {
            long weight = rand.weights.get(i);
            Action branchContent = generateStatements(rand.statements.get(i), innerRa, rc);
            for (long k = 0; k < weight; k++) {
                branches.add(branchContent);
            }
        }
        return ActionFactory.addActionRandom(ra, new GroupAction(branches), null);
    }

    private Action generateConditionExpr(ExpressionNode expr, RestrictionCondition rc) {
        return switch (expr) {
            case BooleanExpressionNode b -> generateBooleanCondition(b, rc);
            case ConditionCallNode c -> generateConditionCall(c, rc);
            case LogicalExpressionNode l -> {
                List<Action> condActions = new ArrayList<>();
                for (ExpressionNode child : l.conditions) {
                    condActions.add(generateConditionExpr(child, rc));
                }
                yield new GroupAction(condActions);
            }
            default -> throw new IllegalStateException("Unexpected condition expression: " + expr.getClass().getSimpleName());
        };
    }

    private Action generateBooleanCondition(BooleanExpressionNode b, RestrictionCondition rc) {
        boolean invert = b.operator == BooleanOperator.NOT_EQUALS;
        Comparator comparator = toComparator(b.operator);
        String value = exprToValue(b.right);
        NormalExpressionNode left = b.left;
        if (left instanceof VariableNode v) {
            return ConditionFactory.addConditionVariable(rc, invert, toHolder(v.holder), v.teamName ,v.name, comparator, value, null, null);
        } else if (left instanceof PlaceholderNode p) {
            return ConditionFactory.addConditionPlaceholder(rc, invert, placeholderToString(p), comparator, value, null);
        }
        throw new IllegalStateException("Unexpected left-hand side of condition: " + left.getClass().getSimpleName());
    }

    private Action generateConditionCall(ConditionCallNode call, RestrictionCondition rc) {
        AddCondition kind = AddCondition.requireByName(call.name);
        List<NormalExpressionNode> args = call.arguments.expressions;
        return switch (kind) {
            case GROUP ->
                    ConditionFactory.addConditionGroup(rc, getBool(args, 0), getString(args, 1), getBool(args, 2), null);
            case PERMISSION ->
                    ConditionFactory.addConditionPermission(rc, getBool(args, 0), Permission.valueOf(getString(args, 1)), null);
            case REGION ->
                    ConditionFactory.addConditionRegion(rc, getBool(args, 0), getString(args, 1), null);
            case HAS_ITEM -> {
                String data = getString(args, 1);
                yield ConditionFactory.addConditionHasItem(rc, getBool(args, 0), data, WhatCheck.valueOf(getString(args, 2)), WhereCheck.valueOf(getString(args, 3)), AmountCheck.valueOf(getString(args, 4)), null);
            }
            case DOING_PARKOUR ->
                    ConditionFactory.addConditionDoingParkour(rc, getBool(args, 0), null);
            case POTION ->
                    ConditionFactory.addConditionPotion(rc, getBool(args, 0), Potion.valueOf(getString(args, 1)), null);
            case SNEAKING ->
                    ConditionFactory.addConditionSneaking(rc, getBool(args, 0), null);
            case FLYING ->
                    ConditionFactory.addConditionFlying(rc, getBool(args, 0), null);
            case HEALTH ->
                    ConditionFactory.addConditionHealth(rc, getBool(args, 0), Comparator.valueOf(getString(args, 1)), exprToValue(args.get(2)), null);
            case MAX_HEALTH ->
                    ConditionFactory.addConditionMaxHealth(rc, getBool(args, 0), Comparator.valueOf(getString(args, 1)), exprToValue(args.get(2)), null);
            case HUNGER ->
                    ConditionFactory.addConditionHunger(rc, getBool(args, 0), Comparator.valueOf(getString(args, 1)), exprToValue(args.get(2)), null);
            case GAME_MODE ->
                    ConditionFactory.addConditionGamemode(rc, getBool(args, 0), Gamemode.valueOf(getString(args, 1)), null);
            case TEAM ->
                    ConditionFactory.addConditionTeam(rc, getBool(args, 0), getString(args, 1), null);
            case PORTAL_TYPE ->
                    ConditionFactory.addConditionPortalType(rc, getBool(args, 0), PortalType.valueOf(getString(args, 1)), null);
            case PVP_ENABLED ->
                    ConditionFactory.addConditionPVPEnabled(rc, getBool(args, 0), null);
            case DAMAGE_CAUSE ->
                    ConditionFactory.addConditionDamageCause(rc, getBool(args, 0), DamageCause.valueOf(getString(args, 1)), null);
            case DAMAGE_AMOUNT ->
                    ConditionFactory.addConditionDamageAmount(rc, getBool(args, 0), Comparator.valueOf(getString(args, 1)), exprToValue(args.get(2)), null);
            case BLOCK_TYPE -> {
                String data = getString(args, 1);
                yield ConditionFactory.addConditionBlockType(rc, getBool(args, 0), data, getBool(args, 2), null);
            }
            case IS_ITEM -> {
                String data = getString(args, 1);
                yield ConditionFactory.addConditionIsItem(rc, getBool(args, 0), data, WhatCheck.valueOf(getString(args, 2)), WhereCheck.valueOf(getString(args, 3)), AmountCheck.valueOf(getString(args, 4)), null);
            }
            case FISHING_ENVIRONMENT ->
                    ConditionFactory.addConditionFishingEnvironment(rc, getBool(args, 0), FishingEnvironment.valueOf(getString(args, 1)), null);
            default ->
                    throw new IllegalStateException("Unhandled condition in code generation: " + kind);
        };
    }

    private Action generateActionCall(ActionCallNode call, RestrictionAction ra) {
        AddAction kind = AddAction.requireByName(call.name);
        List<NormalExpressionNode> args = call.arguments.expressions;
        return switch (kind) {
            case CHANGE_GROUP ->
                    ActionFactory.addActionChangeGroup(ra, getString(args, 0), getBool(args, 1), null);
            case KILL ->
                    ActionFactory.addActionKillPlayer(ra);
            case FULL_HEAL ->
                    ActionFactory.addActionFullHeal(ra);
            case TITLE ->
                    ActionFactory.addActionTitle(ra, new TitleParams()
                            .title(getString(args, 0))
                            .subtitle(getString(args, 1))
                            .fadeIn((int) getLong(args, 2))
                            .stay((int) getLong(args, 3))
                            .fadeOut((int) getLong(args, 4)));
            case ACTION_BAR ->
                    ActionFactory.addActionActionBar(ra, getString(args, 0), null);
            case CLEAR_INVENTORY ->
                    ActionFactory.addActionResetInventory(ra);
            case MAX_HEALTH ->
                    ActionFactory.addActionMaxHealth(ra, exprToValue(args.get(0)), RestrictedOperator.valueOf(getString(args, 1)), getBool(args, 2), null);
            case PARKOUR_CHECKPOINT ->
                    ActionFactory.addActionParkourCheckpoint(ra);
            case GIVE_ITEM -> {
                String data = getString(args, 0);
                yield ActionFactory.addActionGiveItem(ra, data, getBool(args, 1), InventorySlots.valueOf(getString(args, 2)), getBool(args, 3), null);
            }
            case REMOVE_ITEM -> {
                String data = getString(args, 0);
                yield ActionFactory.addActionRemoveItem(ra, data, null);
            }
            case CHAT_MESSAGE ->
                    ActionFactory.addActionSendMessage(ra, getString(args, 0), null);
            case APPLY_POTION ->
                    ActionFactory.addActionApplyPotion(ra, Potion.valueOf(getString(args, 0)), (int) getLong(args, 1), (int) getLong(args, 2), getBool(args, 3), getBool(args, 4), null);
            case CLEAR_POTION ->
                    ActionFactory.addActionClearPotion(ra);
            case GIVE_XP ->
                    ActionFactory.addActionGiveXP(ra, (int) getLong(args, 0), null);
            case SEND_LOBBY ->
                    ActionFactory.addActionSendLobby(ra, Lobby.valueOf(getString(args, 0)), null);
            case TELEPORT -> {
                String loc = getString(args, 0);
                Teleport tp = parseTeleport(loc);
                yield ActionFactory.addActionTeleport(ra, tp, tp == Teleport.CUSTOM ? loc : null, getBool(args, 1), null);
            }
            case FAIL_PARKOUR ->
                    ActionFactory.addActionFailParkour(ra, getString(args, 0), null);
            case PLAY_SOUND -> {
                String loc = getString(args, 3);
                Teleport tp = parseTeleport(loc);
                yield ActionFactory.addActionPlaySound(ra, new PlaySoundParams()
                        .sound(Sound.valueOf(getString(args, 0)))
                        .volume(getDouble(args, 1))
                        .pitch(getDouble(args, 2))
                        .teleport(tp)
                        .customLocation(tp == Teleport.CUSTOM ? loc : null));
            }
            case COMPASS_TARGET -> {
                String loc = getString(args, 0);
                Teleport tp = parseTeleport(loc);
                yield ActionFactory.addActionCompassTarget(ra, tp, tp == Teleport.CUSTOM ? loc : null, null);
            }
            case SET_GAME_MODE ->
                    ActionFactory.addActionGamemode(ra, Gamemode.valueOf(getString(args, 0)), null);
            case CHANGE_HEALTH ->
                    ActionFactory.addActionChangeHealth(ra, exprToValue(args.get(0)), RestrictedOperator.valueOf(getString(args, 1)), null);
            case CHANGE_HUNGER ->
                    ActionFactory.addActionChangeHunger(ra, exprToValue(args.get(0)), RestrictedOperator.valueOf(getString(args, 1)), null);
            case APPLY_LAYOUT ->
                    ActionFactory.addActionApplyLayout(ra, getString(args, 0), null);
            case ENCHANT_HELD_ITEM ->
                    ActionFactory.addActionEnchantHeldItem(ra, Enchant.valueOf(getString(args, 0)), (int) getLong(args, 1), null);
            case PAUSE ->
                    ActionFactory.addActionPause(ra, (int) getLong(args, 0), null);
            case SET_TEAM ->
                    ActionFactory.addActionTeam(ra, getString(args, 0), null);
            case DISPLAY_MENU ->
                    ActionFactory.addActionDisplayMenu(ra, getString(args, 0), null);
            case CLOSE_MENU ->
                    ActionFactory.addActionCloseMenu(ra);
            case DROP_ITEM -> {
                String data = getString(args, 0);
                String loc = getString(args, 1);
                Teleport tp = parseTeleport(loc);
                yield ActionFactory.addActionDropItem(ra, new DropItemParams()
                        .snbtData(data)
                        .teleport(tp)
                        .customLocation(tp == Teleport.CUSTOM ? loc : null)
                        .dropNaturally(getBool(args, 2))
                        .disableItemMerging(getBool(args, 3))
                        .despawnDuration(getLong(args, 4))
                        .pickupDelay(getLong(args, 5))
                        .prioritizePlayer(getBool(args, 6))
                        .fallbackToInventory(getBool(args, 7)));
            }
            case CHANGE_VELOCITY ->
                    ActionFactory.addActionChangeVelocity(ra, exprToValue(args.get(0)), exprToValue(args.get(1)), exprToValue(args.get(2)), null);
            case LAUNCH_TO_TARGET -> {
                String loc = getString(args, 0);
                Teleport tp = parseTeleport(loc);
                yield ActionFactory.addActionLaunchTarget(ra, tp, tp == Teleport.CUSTOM ? loc : null, exprToValue(args.get(1)), null);
            }
            case SET_WEATHER ->
                    ActionFactory.addActionWeather(ra, Weather.valueOf(getString(args, 0)), null);
            case SET_TIME -> {
                String timeStr = getString(args, 0);
                Time time = parseTime(timeStr);
                yield ActionFactory.addActionTime(ra, time, time == Time.CUSTOM ? timeStr : null, null);
            }
            case NAME_TAG_DISPLAY ->
                    ActionFactory.addActionNametag(ra, getBool(args, 0), null);
            case CANCEL_EVENT ->
                    ActionFactory.addActionCancelEvent(ra);
            default ->
                    throw new IllegalStateException("Unhandled action in code generation: " + kind);
        };
    }

    private String exprToValue(NormalExpressionNode expr) {
        return switch (expr) {
            case ConstantPrimitiveNode c -> primitiveToString(c.value);
            case VariableNode v -> variableToString(v);
            case PlaceholderNode p -> placeholderToString(p);
            default -> throw new IllegalStateException("Unexpected expression in value position: " + expr.getClass().getSimpleName());
        };
    }

    private String primitiveToString(ConstantPrimitive prim) {
        return switch (prim) {
            case StringValue s -> s.value;
            case LongValue l -> l.toString();
            case DoubleValue d -> d.toString();
            // Bools are stored in Housing as the number 1/0 — must match
            // primitiveToInitString so declared and assigned/compared bools agree.
            case BoolValue b -> b.value ? "1" : "0";
        };
    }

    private String variableToString(VariableNode v) {
        String suffix = v.castSuffix != null ? v.castSuffix : "";
        return switch (v.holder) {
            case PLAYER -> "%var.player/" + v.name + v.fallbackToken() + "%" + suffix;
            case GLOBAL -> "%var.global/" + v.name + v.fallbackToken() + "%" + suffix;
            case TEAM -> "%var.team/" + v.name + " " + v.teamName + v.fallbackToken() + "%" + suffix;
        };
    }

    private String placeholderToString(PlaceholderNode p) {
        Placeholder ph = Placeholder.requireByName(p.placeholder);
        StringBuilder sb = new StringBuilder("%").append(ph.inGameName);
        List<ConstantPrimitiveNode> consts = p.constants.constants;
        if (!consts.isEmpty()) {
            sb.append("/");
            for (int i = 0; i < consts.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(primitiveToString(consts.get(i).value));
            }
        }
        sb.append("%");
        return sb.toString();
    }

    private Holder toHolder(com.drakou111.neohouse.compiler.enums.Holder h) {
        return switch (h) {
            case PLAYER -> Holder.PLAYER;
            case GLOBAL -> Holder.GLOBAL;
            case TEAM -> Holder.TEAM;
        };
    }

    private Operator toOperator(AssignmentOperator op) {
        return switch (op) {
            case ASSIGN -> Operator.SET;
            case ADD_ASSIGN -> Operator.INCREMENT;
            case SUB_ASSIGN -> Operator.DECREMENT;
            case MUL_ASSIGN -> Operator.MULTIPLY;
            case DIV_ASSIGN -> Operator.DIVIDE;
            case BITWISE_OR_ASSIGN -> Operator.BITWISE_OR;
            case BITWISE_AND_ASSIGN -> Operator.BITWISE_AND;
            case BITWISE_XOR_ASSIGN -> Operator.BITWISE_XOR;
            case LEFT_SHIFT_ASSIGN -> Operator.LEFT_SHIFT;
            case RIGHT_SHIFT_ASSIGN -> Operator.ARITHMETIC_RIGHT_SHIFT;
            case UNSIGNED_RIGHT_SHIFT_ASSIGN -> Operator.LOGICAL_RIGHT_SHIFT;
        };
    }

    private Comparator toComparator(BooleanOperator op) {
        return switch (op) {
            case EQUALS, NOT_EQUALS -> Comparator.EQUAL;
            case LESS_THAN -> Comparator.LESS_THAN;
            case GREATER_THAN -> Comparator.GREATER_THAN;
            case LESS_THAN_EQUALS -> Comparator.LESS_THAN_OR_EQUAL;
            case GREATER_THAN_EQUALS -> Comparator.GREATER_THAN_OR_EQUAL;
        };
    }

    private boolean getBool(List<NormalExpressionNode> args, int idx) {
        return ((BoolValue) ((ConstantPrimitiveNode) args.get(idx)).value).value;
    }

    private String getString(List<NormalExpressionNode> args, int idx) {
        return exprToString(args.get(idx));
    }

    private String exprToString(NormalExpressionNode expr) {
        return switch (expr) {
            case ConstantPrimitiveNode c -> primitiveToString(c.value);
            case VariableNode v -> variableToString(v);
            case PlaceholderNode p -> placeholderToString(p);
            case BinaryExpressionNode b when b.operator == BinaryOperator.ADD ->
                exprToString(b.left) + exprToString(b.right);
            case UnaryExpressionNode u when u.operator == UnaryOperator.STRING_CAST ->
                exprToString(u.operand);
            default -> throw new IllegalStateException("Cannot use expression of type "
                + expr.getClass().getSimpleName() + " as a string value in this context");
        };
    }

    private long getLong(List<NormalExpressionNode> args, int idx) {
        return ((LongValue) ((ConstantPrimitiveNode) args.get(idx)).value).value;
    }

    private double getDouble(List<NormalExpressionNode> args, int idx) {
        return ((DoubleValue) ((ConstantPrimitiveNode) args.get(idx)).value).value;
    }

    private Teleport parseTeleport(String s) {
        try {
            return Teleport.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Teleport.CUSTOM;
        }
    }

    private Time parseTime(String s) {
        try {
            return Time.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Time.CUSTOM;
        }
    }
}
