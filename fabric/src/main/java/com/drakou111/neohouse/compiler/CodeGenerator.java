package com.drakou111.neohouse.compiler;

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
import com.drakou111.neohouse.compiler.visitors.HousingNameCollector;
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
    private final OverrideMode mode;

    public CodeGenerator(OverrideMode mode) {
        this.mode = Objects.requireNonNull(mode);
    }

    public Action generate(MainNode main) {
        List<Action> parts = new ArrayList<>();

        // First of all: the teams, layouts and groups the script names must already exist, since
        // Housing can't create them from a script. Checking up front fails before anything changes.
        for (Map.Entry<HousingList, Set<String>> entry : new HousingNameCollector().collect(main).entrySet())
            parts.add(checkNamesExist(entry.getKey(), List.copyOf(entry.getValue())));

        // Pass 0: emit global/team variable initializers
        for (MainStatementNode stmt : main.statements) {
            if (stmt instanceof GlobalDeclarationNode decl && decl.initializer != null) {
                parts.add(generateGlobalInitializer(decl));
                // Wait for "variable" confirmation in chat, then an extra 1.5 s (30 ticks)
                parts.add(new WaitForChatAction("variable", 30));
            }
        }

        // Pass 1a: read the existing entries of each list the script adds to (lists it doesn't touch
        // are never opened), and stop before changing anything if Housing's limit would be exceeded.
        for (Map.Entry<HousingList, List<String>> entry : collectListEntries(main).entrySet()) {
            parts.add(new ActionBuilder().closeAll().sendChatAndWait(entry.getKey().openCommand).get());
            parts.add(new ReadListNamesAction(entry.getKey()));
            parts.add(new CheckListLimitAction(entry.getKey(), entry.getValue()));
        }

        // Pass 1b: make sure every function, loop, command and menu exists before any body is
        // written, so references between them (forward references, mutual calls, displayMenu) resolve.
        for (MainStatementNode stmt : main.statements) {
            switch (stmt) {
                case VoidFunctionNode fn -> parts.add(prepareFunction(fn.name));
                case LoopNode loop -> parts.add(prepareFunction(loop.name));
                case CommandNode cmd -> parts.add(prepareCommand(cmd));
                case MenuNode menu -> parts.add(prepareMenu(menu));
                default -> { }
            }
        }

        // Pass 2: write the bodies. ALWAYS writes everything; NEVER only what Pass 1 created;
        // IF_EMPTY also fills existing entries that turn out to be empty.
        Map<String, VoidFunctionNode> fnByName = collectFunctions(main);
        FunctionCopies fnCopies = findFunctionCopies(main, fnByName);
        for (MainStatementNode stmt : main.statements) {
            Action fill = switch (stmt) {
                case VoidFunctionNode fn when this.mode == OverrideMode.ALWAYS -> {
                    if (fnCopies.isCopy().contains(fn.name)) yield null; // filled by pasting from its original
                    List<String> copies = fnCopies.copiesOf().getOrDefault(fn.name, List.of());
                    yield copies.isEmpty() ? generateFunction(fn) : generateFunctionWithCopies(fn, copies);
                }
                case VoidFunctionNode fn -> fillPreserving(fnKey(fn.name), generateFunction(fn), generateFunctionIfEmpty(fn));
                case LoopNode loop when this.mode == OverrideMode.ALWAYS -> generateLoop(loop);
                case LoopNode loop -> fillPreserving(fnKey(loop.name), generateLoop(loop), generateLoopIfEmpty(loop));
                case CommandNode cmd when this.mode == OverrideMode.ALWAYS -> generateCommand(cmd);
                case CommandNode cmd -> fillPreserving(commandKey(cmd.name), generateCommand(cmd), generateCommandIfEmpty(cmd));
                case MenuNode menu when this.mode == OverrideMode.ALWAYS -> generateMenu(menu);
                case MenuNode menu -> fillPreserving(menuKey(menu.name), generateMenu(menu), generateMenuIfEmpty(menu));
                case EventNode event when this.mode == OverrideMode.ALWAYS -> generateEvent(event);
                case EventNode event -> generateEventIfEmpty(event);
                default -> null;
            };
            if (fill != null) parts.add(fill);
        }
        return new GroupAction(parts);
    }

    /** Opens a list (through its menu item when needed), reads its entries and checks {@code names} are among them. */
    private static Action checkNamesExist(HousingList list, List<String> names) {
        ActionBuilder open = new ActionBuilder().closeAll().sendChatAndWait(list.openCommand);
        list.openThrough().ifPresent(open::search);
        return open
                .append(new ReadListNamesAction(list))
                .append(new CheckNamesExistAction(list, names))
                .get();
    }

    /** Every name the script adds to each Housing list, for lists it adds at least one entry to. */
    private static Map<HousingList, List<String>> collectListEntries(MainNode main) {
        Map<HousingList, List<String>> entries = new EnumMap<>(HousingList.class);
        for (MainStatementNode stmt : main.statements) {
            switch (stmt) {
                case VoidFunctionNode fn -> entries.computeIfAbsent(HousingList.FUNCTIONS, k -> new ArrayList<>()).add(fn.name);
                case LoopNode loop -> entries.computeIfAbsent(HousingList.FUNCTIONS, k -> new ArrayList<>()).add(loop.name);
                case CommandNode cmd -> entries.computeIfAbsent(HousingList.COMMANDS, k -> new ArrayList<>()).add(cmd.name);
                case MenuNode menu -> entries.computeIfAbsent(HousingList.MENUS, k -> new ArrayList<>()).add(menu.name);
                default -> { }
            }
        }
        return entries;
    }

    /**
     * Pass 2 for the modes that keep existing content: a fresh shell from Pass 1 is always filled; an
     * entry that already existed is left alone (NEVER) or filled only if it turns out to be empty (IF_EMPTY).
     */
    private Action fillPreserving(String createdKey, Action fill, Action fillIfEmpty) {
        Action otherwise = this.mode == OverrideMode.IF_EMPTY ? fillIfEmpty : null;
        if (fill == null && otherwise == null) return null;
        return new RunIfCreatedAction(createdKey, fill, otherwise);
    }

    /** The Pass 1 step for an existing entry: ALWAYS replaces it, NEVER skips it with a warning, IF_EMPTY defers to Pass 2. */
    private Action existingEntryStep(HousingList list, String name, Action replace) {
        return switch (this.mode) {
            case ALWAYS -> replace;
            case NEVER -> skippedWarning(list.singularName + " '" + list.displayName(name) + "'", "it already exists");
            case IF_EMPTY -> null;
        };
    }

    private static Action skippedWarning(String what, String reason) {
        return new ClientMessageAction("Skipped " + what + ": " + reason + ".", true);
    }

    private Action prepareFunction(String name) {
        Action create = new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.FUNCTIONS)
                .textInput(name, Others.GENERAL_ADD_H6)
                .append(new MarkCreatedAction(fnKey(name)))
                .get();
        Action replace = new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.FUNCTIONS)
                .removeFunctionIfPresent(name)
                .textInput(name, Others.GENERAL_ADD_H6)
                .get();
        return new IfExistsAction(HousingList.FUNCTIONS, name, existingEntryStep(HousingList.FUNCTIONS, name, replace), create);
    }

    private Action prepareCommand(CommandNode cmd) {
        Action create = new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.COMMANDS)
                .append(MovementFactory.createCommand(cmd.name, cmd.targeted, cmd.priority, cmd.listed))
                .append(new MarkCreatedAction(commandKey(cmd.name)))
                .get();
        Action replace = new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.COMMANDS)
                .removeCommandIfPresent(cmd.name)
                .append(MovementFactory.createCommand(cmd.name, cmd.targeted, cmd.priority, cmd.listed))
                .get();
        return new IfExistsAction(HousingList.COMMANDS, cmd.name, existingEntryStep(HousingList.COMMANDS, cmd.name, replace), create);
    }

    private Action prepareMenu(MenuNode menu) {
        Action create = new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.CUSTOM_MENUS)
                .append(MovementFactory.createMenu(menu.name, menu.rows))
                .append(new MarkCreatedAction(menuKey(menu.name)))
                .get();
        Action replace = new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.CUSTOM_MENUS)
                .removeMenuIfPresent(menu.name)
                .append(MovementFactory.createMenu(menu.name, menu.rows))
                .get();
        return new IfExistsAction(HousingList.MENUS, menu.name, existingEntryStep(HousingList.MENUS, menu.name, replace), create);
    }

    /**
     * Applies a function/loop's description and icon via its config menu (right-click), or returns
     * null when there's neither. Called right before the body so stub creation stays a cheap list operation.
     */
    private Action setFunctionMeta(String name, String description, String icon) {
        boolean hasDesc = description != null && !description.trim().isEmpty();
        if (!hasDesc && icon == null) return null;
        return new ActionBuilder()
                .gotoFunction(true, name, false)
                .onlyIf(hasDesc, () -> new ActionBuilder().searchForText(FunctionInputs.DESCRIPTION, description))
                .onlyIf(icon != null, () -> new ActionBuilder().search(FunctionInputs.ICON).customItem(icon))
                .closeAll()
                .get();
    }

    // Registry keys for "created this run" are namespaced because functions, commands and menus live
    // in separate Housing lists and could share a name.
    private static String fnKey(String name)      { return "fn:" + name; }
    private static String commandKey(String name) { return "cmd:" + name; }
    private static String menuKey(String name)    { return "menu:" + name; }

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
     * Detects compiler-created copies ({@code baseName$N} where {@code baseName} is also defined) whose
     * actions are identical to the original's, so they can be filled by pasting. A copy that calls
     * its own copies of other functions differs from the original and is written out normally.
     */
    private static FunctionCopies findFunctionCopies(MainNode main, Map<String, VoidFunctionNode> fnByName) {
        Map<String, List<String>> copiesOf = new LinkedHashMap<>();
        Set<String> isCopy = new HashSet<>();
        for (MainStatementNode s : main.statements) {
            if (!(s instanceof VoidFunctionNode vf)) continue;
            int dollar = vf.name.lastIndexOf('$');
            if (dollar <= 0) continue;

            String tail = vf.name.substring(dollar + 1);
            VoidFunctionNode original = fnByName.get(vf.name.substring(0, dollar));
            if (tail.isEmpty() || !tail.chars().allMatch(Character::isDigit) || original == null) continue;
            if (!vf.statements.toShortString().equals(original.statements.toShortString())) continue;

            copiesOf.computeIfAbsent(original.name, k -> new ArrayList<>()).add(vf.name);
            isCopy.add(vf.name);
        }
        return new FunctionCopies(copiesOf, isCopy);
    }
    /** Fills a function the run created or just emptied. An empty body never opens the function. */
    private Action generateFunction(VoidFunctionNode fn) {
        List<Action> parts = new ArrayList<>();
        addIfPresent(parts, setFunctionMeta(fn.name, fn.description, fn.icon));
        if (!fn.statements.statements.isEmpty()) {
            parts.add(MovementFactory.goToFunction(false, fn.name));
            parts.add(generateStatements(fn.statements, RestrictionAction.NONE, RestrictionCondition.NONE));
            parts.add(MovementFactory.exit(true));
        }
        return parts.isEmpty() ? null : new GroupAction(parts);
    }

    private Action generateFunctionWithCopies(VoidFunctionNode fn, List<String> copyNames) {
        if (fn.statements.statements.isEmpty()) return generateFunction(fn);
        List<Action> parts = new ArrayList<>();
        addIfPresent(parts, setFunctionMeta(fn.name, fn.description, fn.icon));
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

    /** IF_EMPTY for a function that already existed: fill it only if it has no actions yet. */
    private Action generateFunctionIfEmpty(VoidFunctionNode fn) {
        if (fn.statements.statements.isEmpty()) return null;
        List<Action> fill = new ArrayList<>();
        fill.add(generateStatements(fn.statements, RestrictionAction.NONE, RestrictionCondition.NONE));
        fill.add(MovementFactory.exit(true));
        addIfPresent(fill, setFunctionMeta(fn.name, fn.description, fn.icon));
        return new GroupAction(List.of(
                MovementFactory.goToFunction(true, fn.name),
                IfContainerEmptyAction.actionList(new GroupAction(fill), keepExisting("function '" + fn.name + "'"))));
    }

    private Action generateEvent(EventNode event) {
        RestrictionAction ra = RestrictionAction.valueOf("EVENT_" + event.event.name());
        RestrictionCondition rc = ra.conditionContext();
        List<Action> parts = new ArrayList<>();
        parts.add(MovementFactory.goToEvent(event.event));
        parts.add(new ActionBuilder().append(new DeleteAllAction(10)).get());
        parts.add(generateStatements(event.statements, ra, rc));
        parts.add(MovementFactory.exit(true));
        return new GroupAction(parts);
    }

    private Action generateEventIfEmpty(EventNode event) {
        if (event.statements.statements.isEmpty()) return null;
        RestrictionAction ra = RestrictionAction.valueOf("EVENT_" + event.event.name());
        RestrictionCondition rc = ra.conditionContext();
        Action fill = new GroupAction(List.of(generateStatements(event.statements, ra, rc), MovementFactory.exit(true)));
        return new GroupAction(List.of(
                MovementFactory.goToEvent(event.event),
                IfContainerEmptyAction.actionList(fill, keepExisting("event '" + event.event.name() + "'"))));
    }

    /** Closes the editor and warns that an existing entry with content was left as it is. */
    private static Action keepExisting(String what) {
        return new GroupAction(List.of(MovementFactory.exit(true), skippedWarning(what, "it already has content")));
    }

    private Action generateLoop(LoopNode loop) {
        List<Action> parts = new ArrayList<>();
        if (!loop.statements.statements.isEmpty()) {
            parts.add(MovementFactory.goToFunction(false, loop.name));
            parts.add(generateStatements(loop.statements, RestrictionAction.NONE, RestrictionCondition.NONE));
        }
        parts.add(loopConfig(loop));
        return new GroupAction(parts);
    }

    private Action generateLoopIfEmpty(LoopNode loop) {
        if (loop.statements.statements.isEmpty()) return null;
        Action fill = new GroupAction(List.of(
                generateStatements(loop.statements, RestrictionAction.NONE, RestrictionCondition.NONE),
                loopConfig(loop)));
        return new GroupAction(List.of(
                MovementFactory.goToFunction(true, loop.name),
                IfContainerEmptyAction.actionList(fill, keepExisting("loop '" + loop.name + "'"))));
    }

    /** One config-menu visit sets the loop's description, icon and automatic-execution delay. */
    private static Action loopConfig(LoopNode loop) {
        boolean hasDesc = loop.description != null && !loop.description.trim().isEmpty();
        return new ActionBuilder()
                .gotoFunction(true, loop.name, false)
                .onlyIf(hasDesc, () -> new ActionBuilder().searchForText(FunctionInputs.DESCRIPTION, loop.description))
                .onlyIf(loop.icon != null, () -> new ActionBuilder().search(FunctionInputs.ICON).customItem(loop.icon))
                .searchForText(FunctionInputs.AUTOMATIC_EXECUTION, String.valueOf(loop.tickDelay))
                .closeAll()
                .get();
    }

    private Action generateCommand(CommandNode cmd) {
        if (cmd.statements.statements.isEmpty()) return null;
        List<Action> parts = new ArrayList<>();
        parts.add(new ActionBuilder().gotoCommand(true, cmd.name, true).get());
        parts.add(generateStatements(cmd.statements, RestrictionAction.NONE, RestrictionCondition.NONE));
        parts.add(MovementFactory.exit(true));
        return new GroupAction(parts);
    }

    private Action generateCommandIfEmpty(CommandNode cmd) {
        if (cmd.statements.statements.isEmpty()) return null;
        Action fill = new GroupAction(List.of(
                generateStatements(cmd.statements, RestrictionAction.NONE, RestrictionCondition.NONE),
                MovementFactory.exit(true)));
        return new GroupAction(List.of(
                new ActionBuilder().gotoCommand(true, cmd.name, true).get(),
                IfContainerEmptyAction.actionList(fill, keepExisting("command '/" + cmd.name + "'"))));
    }

    private Action generateMenu(MenuNode menu) {
        if (menu.slots.isEmpty()) return null;
        return new GroupAction(List.of(MovementFactory.goToMenuGrid(menu.name), generateSlots(menu)));
    }

    /** IF_EMPTY for a menu that already existed: resize and fill it only if every element is an empty slot. */
    private Action generateMenuIfEmpty(MenuNode menu) {
        if (menu.slots.isEmpty()) return null;
        Action fill = new GroupAction(List.of(
                MovementFactory.setMenuSize(menu.name, menu.rows),
                MovementFactory.goToMenuGrid(menu.name),
                generateSlots(menu)));
        return new GroupAction(List.of(
                MovementFactory.goToMenuGrid(menu.name),
                IfContainerEmptyAction.menuGrid(fill, keepExisting("menu '" + menu.name + "'"))));
    }

    private Action generateSlots(MenuNode menu) {
        List<Action> parts = new ArrayList<>();
        for (SlotNode slot : menu.slots) parts.add(generateSlot(slot));
        parts.add(MovementFactory.exit(true));
        return new GroupAction(parts);
    }

    private static void addIfPresent(List<Action> parts, Action action) {
        if (action != null) parts.add(action);
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
            // A weight of N adds the branch N times. Each copy needs its own Action objects: actions
            // keep their progress, so a shared instance is "already done" after the first copy.
            for (long k = 0; k < weight; k++) {
                branches.add(generateStatements(rand.statements.get(i), innerRa, rc));
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
                        .sound(soundOrNull(getString(args, 0)))
                        .customSound(soundOrNull(getString(args, 0)) == null ? getString(args, 0) : null)
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
            // Bools are stored in Housing as the number 1/0 - must match
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
            case MOD_ASSIGN -> throw new IllegalStateException("'%=' must be lowered before code generation");
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

    /** The Housing sound with this enum name, or null for a custom Minecraft sound id. */
    private static Sound soundOrNull(String name) {
        for (Sound sound : Sound.values()) if (sound.name().equals(name)) return sound;
        return null;
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
