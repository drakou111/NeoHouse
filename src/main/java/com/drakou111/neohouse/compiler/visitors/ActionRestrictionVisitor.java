package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.main_statements.*;
import com.drakou111.neohouse.compiler.ast.statement.*;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.RestrictionAction;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayDeque;
import java.util.Deque;

public class ActionRestrictionVisitor extends DefaultVisitor {
    private final Deque<RestrictionAction> ctx = new ArrayDeque<>();

    @Override
    public ASTNode visitVoidFunction(VoidFunctionNode fn) {
        ctx.push(RestrictionAction.NONE);
        super.visitVoidFunction(fn);
        ctx.pop();
        return fn;
    }

    @Override
    public ASTNode visitCommand(CommandNode cmd) {
        ctx.push(RestrictionAction.NONE);
        super.visitCommand(cmd);
        ctx.pop();
        return cmd;
    }

    @Override
    public ASTNode visitLoop(LoopNode fn) {
        ctx.push(RestrictionAction.NONE);
        super.visitLoop(fn);
        ctx.pop();
        return fn;
    }

    @Override
    public ASTNode visitEvent(EventNode event) {
        String key = "EVENT_" + event.event.name();
        RestrictionAction ra = RestrictionAction.valueOf(key);
        ctx.push(ra);
        super.visitEvent(event);
        ctx.pop();
        return event;
    }

    @Override
    public ASTNode visitMenu(MenuNode menu) {
        for (SlotNode slot : menu.slots) {
            slot.accept(this);
        }
        return menu;
    }

    @Override
    public ASTNode visitSlot(SlotNode slot) {
        ctx.push(RestrictionAction.MENU);
        super.visitSlot(slot);
        ctx.pop();
        return slot;
    }

    @Override
    public ASTNode visitConditional(ConditionalNode cond) {
        ensureAllowed(AddAction.CONDITIONAL, cond.token);
        ctx.push(deriveInner(ctx.peek()));
        super.visitConditional(cond);
        ctx.pop();
        return cond;
    }

    @Override
    public ASTNode visitRandom(RandomNode random) {
        ensureAllowed(AddAction.RANDOM, random.token);
        ctx.push(deriveInner(ctx.peek()));
        super.visitRandom(random);
        ctx.pop();
        return random;
    }

    private RestrictionAction deriveInner(RestrictionAction parent) {
        String pname = parent.name();
        if (pname.startsWith("EVENT_") && !pname.startsWith("EVENT_CONDITION_")) {
            return RestrictionAction.valueOf(pname.replace("EVENT_", "EVENT_CONDITION_"));
        }
        if (pname.equals("MENU")) {
            return RestrictionAction.MENU_CONDITIONAL;
        }
        return RestrictionAction.CONDITIONAL;
    }

    @Override
    public ASTNode visitCancel(CancelNode cancel) {
        ensureAllowed(AddAction.CANCEL_EVENT, cancel.token);
        return super.visitCancel(cancel);
    }

    @Override
    public ASTNode visitAssignation(AssignationNode assign) {
        ensureAllowed(AddAction.VARIABLE, assign.token);
        return super.visitAssignation(assign);
    }

    @Override
    public ASTNode visitVoidReturn(VoidReturnNode ret) {
        if (ctx.peek() != RestrictionAction.NONE) {
            ensureAllowed(AddAction.EXIT, ret.token);
        }
        return super.visitVoidReturn(ret);
    }

    @Override
    public ASTNode visitVoidFunctionCall(VoidFunctionCallNode ret) {
        ensureAllowed(AddAction.TRIGGER_FUNCTION, ret.token);
        return super.visitVoidFunctionCall(ret);
    }
    @Override
    public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode ret) {
        ensureAllowed(AddAction.TRIGGER_FUNCTION, ret.token);
        return super.visitGlobalFunctionCall(ret);
    }

    @Override
    public ASTNode visitActionCall(ActionCallNode call) {
        AddAction act = AddAction.searchByName(call.name);
        if (act == null) {
            throw new SemanticException(call.token, "Unknown action “" + call.name + "”");
        }
        ensureAllowed(act, call.token);
        return super.visitActionCall(call);
    }

    private void ensureAllowed(AddAction action, Token t) {
        RestrictionAction restriction = ctx.peek();
        if (!restriction.isAllowed(action)) {
            throw new SemanticException(t, "'" + action.displayName + "' cannot be used in this context");
        }
    }
}