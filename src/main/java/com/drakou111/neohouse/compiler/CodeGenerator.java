package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.NeoHouseConfig;
import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.CheckSlotEmptyAction;
import com.drakou111.neohouse.actions.actiontypes.ClientMessageAction;
import com.drakou111.neohouse.actions.actiontypes.DeleteAllAction;
import com.drakou111.neohouse.actions.actiontypes.GroupAction;
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
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.enums.BooleanOperator;
import com.drakou111.neohouse.compiler.enums.LogicalOperator;
import com.drakou111.neohouse.constants.*;
import com.drakou111.neohouse.factories.ActionFactory;
import com.drakou111.neohouse.factories.ConditionFactory;
import com.drakou111.neohouse.factories.MovementFactory;
import com.drakou111.neohouse.utils.ActionBuilder;

import java.util.ArrayList;
import java.util.List;

public class CodeGenerator {

    public Action generate(MainNode main) {
        List<Action> parts = new ArrayList<>();
        boolean override = NeoHouseConfig.get().overrideExisting;

        if (override) {
            // Pass 1: create empty shells for forward-reference support
            for (MainStatementNode stmt : main.statements) {
                if (stmt instanceof VoidFunctionNode fn) {
                    parts.add(MovementFactory.goAndRecreateFunction(fn.name, fn.description, null, false, 0));
                    parts.add(MovementFactory.exit(true));
                } else if (stmt instanceof LoopNode loop) {
                    parts.add(MovementFactory.goAndRecreateFunction(loop.name, loop.description, null, true, (int) loop.tickDelay));
                    parts.add(MovementFactory.exit(true));
                } else if (stmt instanceof CommandNode cmd) {
                    parts.add(MovementFactory.goAndRecreateCommand(cmd.name, cmd.targeted, cmd.priority, cmd.listed));
                    parts.add(MovementFactory.exit(true));
                } else if (stmt instanceof MenuNode menu) {
                    parts.add(MovementFactory.goAndRecreateMenu(menu.name, menu.rows));
                    parts.add(MovementFactory.exit(true));
                }
            }
        }

        // Pass 2: fill in bodies
        for (MainStatementNode stmt : main.statements) {
            if (stmt instanceof VoidFunctionNode fn) {
                parts.add(override ? generateFunction(fn) : generateFunctionIfAbsent(fn));
            } else if (stmt instanceof EventNode event) {
                parts.add(override ? generateEvent(event) : generateEventIfEmpty(event));
            } else if (stmt instanceof LoopNode loop) {
                parts.add(override ? generateLoop(loop) : generateLoopIfAbsent(loop));
            } else if (stmt instanceof CommandNode cmd) {
                parts.add(override ? generateCommand(cmd) : generateCommandIfAbsent(cmd));
            } else if (stmt instanceof MenuNode menu) {
                parts.add(override ? generateMenu(menu) : generateMenuIfAbsent(menu));
            }
        }
        return new GroupAction(parts);
    }

    private Action generateFunction(VoidFunctionNode fn) {
        List<Action> parts = new ArrayList<>();
        parts.add(MovementFactory.goToFunction(false, fn.name));
        parts.add(generateStatements(fn.statements, RestrictionAction.NONE, RestrictionCondition.NONE));
        parts.add(MovementFactory.exit(true));
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
        List<Action> parts = new ArrayList<>();
        parts.add(MovementFactory.goToFunction(false, loop.name));
        parts.add(generateStatements(loop.statements, RestrictionAction.NONE, RestrictionCondition.NONE));
        parts.add(MovementFactory.exit(true));
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

    private Action generateFunctionIfAbsent(VoidFunctionNode fn) {
        Action warn = new GroupAction(List.of(
            new ActionBuilder().closeAll().get(),
            new ClientMessageAction("Skipped function '" + fn.name + "' — already exists.", true)
        ));
        Action createAndFill = new ActionBuilder()
                .anvilInput(fn.name, Others.GENERAL_ADD_H6)
                .append(MovementFactory.goToFunction(true, fn.name))
                .append(generateStatements(fn.statements, RestrictionAction.NONE, RestrictionCondition.NONE))
                .closeAll()
                .get();
        return new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.FUNCTIONS)
                .search(fn.name, null, false, new ActionBuilder().append(warn), new ActionBuilder().append(createAndFill))
                .get();
    }

    private Action generateLoopIfAbsent(LoopNode loop) {
        Action warn = new GroupAction(List.of(
            new ActionBuilder().closeAll().get(),
            new ClientMessageAction("Skipped loop '" + loop.name + "' — already exists.", true)
        ));
        Action createAndFill = new ActionBuilder()
                .anvilInput(loop.name, Others.GENERAL_ADD_H6)
                .append(MovementFactory.goToFunction(true, loop.name))
                .append(generateStatements(loop.statements, RestrictionAction.NONE, RestrictionCondition.NONE))
                .closeAll()
                .get();
        return new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.FUNCTIONS)
                .search(loop.name, null, false, new ActionBuilder().append(warn), new ActionBuilder().append(createAndFill))
                .get();
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

    private Action generateMenuIfAbsent(MenuNode menu) {
        Action warn = new GroupAction(List.of(
            new ActionBuilder().closeAll().get(),
            new ClientMessageAction("Skipped menu '" + menu.name + "' — already exists.", true)
        ));
        List<Action> slotActions = new ArrayList<>();
        for (SlotNode slot : menu.slots) slotActions.add(generateSlot(slot));
        Action createAndFill = new ActionBuilder()
                .anvilInput(menu.name, Others.GENERAL_ADD_H6)
                .append(MovementFactory.goToMenuGrid(menu.name))
                .append(new GroupAction(slotActions))
                .closeAll()
                .get();
        return new ActionBuilder()
                .closeAll()
                .sendChatAndWait(Commands.CUSTOM_MENUS)
                .search(menu.name, null, false, new ActionBuilder().append(warn), new ActionBuilder().append(createAndFill))
                .get();
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

    private Action generateStatements(StatementListNode stmts, RestrictionAction ra, RestrictionCondition rc) {
        List<Action> actions = new ArrayList<>();
        for (StatementNode stmt : stmts.statements) {
            actions.add(generateStatement(stmt, ra, rc));
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
            case GlobalFunctionCallNode call -> ActionFactory.addActionTriggerFunction(ra, call.functionName, false, null);
            case ActionCallNode call -> generateActionCall(call, ra);
            default -> throw new IllegalStateException("Unhandled statement in code generation: " + stmt.getClass().getSimpleName());
        };
    }

    private Action generateAssignation(AssignationNode assign, RestrictionAction ra) {
        Holder holder = toHolder(assign.variable.holder);
        String variable = assign.variable.name;
        String teamName = assign.variable.teamName;
        Operator operator = toOperator(assign.operator);
        String value = exprToValue(assign.value);
        return ActionFactory.addActionVariable(ra, holder, variable, teamName, operator, value, false, null);
    }

    private Action generateConditional(ConditionalNode cond, RestrictionAction ra, RestrictionCondition rc) {
        if (cond.condition == null) {
            return generateStatements(cond.thenBlock, ra, rc);
        }
        boolean matchAny = cond.condition instanceof LogicalExpressionNode log
                && log.operator == LogicalOperator.OR;
        Action conditions = generateConditionExpr(cond.condition, rc);
        RestrictionAction innerRa = ra.conditionalVariant();
        Action thenAction = generateStatements(cond.thenBlock, innerRa, rc);
        Action elseAction = cond.elseBlock != null && !cond.elseBlock.statements.isEmpty() ? generateStatements(cond.elseBlock, innerRa, rc) : null;
        return ActionFactory.addActionConditional(ra, matchAny, conditions, thenAction, elseAction, null);
    }

    private Action generateRandom(RandomNode rand, RestrictionAction ra, RestrictionCondition rc) {
        RestrictionAction innerRa = ra.conditionalVariant();
        List<Action> branches = new ArrayList<>();
        for (StatementListNode branch : rand.statements) {
            branches.add(generateStatements(branch, innerRa, rc));
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
                    ActionFactory.addActionTitle(ra, getString(args, 0), getString(args, 1), (int) getLong(args, 2), (int) getLong(args, 3), (int) getLong(args, 4), null);
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
                yield ActionFactory.addActionPlaySound(ra, Sound.valueOf(getString(args, 0)), getDouble(args, 1), getDouble(args, 2), tp, tp == Teleport.CUSTOM ? loc : null, null);
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
            case DROP_ITEM -> {
                String data = getString(args, 0);
                String loc = getString(args, 1);
                Teleport tp = parseTeleport(loc);
                yield ActionFactory.addActionDropItem(ra, data, tp, tp == Teleport.CUSTOM ? loc : null, getBool(args, 2), getBool(args, 3), getLong(args, 4), getLong(args, 5), getBool(args, 6), getBool(args, 7), null);
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
            case BoolValue b -> b.toString();
        };
    }

    private String variableToString(VariableNode v) {
        return switch (v.holder) {
            case PLAYER -> "%var.player/" + v.name + "%";
            case GLOBAL -> "%var.global/" + v.name + "%";
            case TEAM -> "%var.team/" + v.name + " " + v.teamName + "%";
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
        return ((StringValue) ((ConstantPrimitiveNode) args.get(idx)).value).value;
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
