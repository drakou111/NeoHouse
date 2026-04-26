package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
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
    public ASTNode visitConditional(ConditionalNode cond) {
        ensureAllowed(AddAction.CONDITIONAL, cond.token);
        RestrictionAction parent = ctx.peek();
        String pname = parent.name();
        RestrictionAction here = pname.startsWith("EVENT_") && !pname.startsWith("EVENT_CONDITION_")
                ? RestrictionAction.valueOf(pname.replace("EVENT_", "EVENT_CONDITION_"))
                : RestrictionAction.CONDITIONAL;
        ctx.push(here);
        super.visitConditional(cond);
        ctx.pop();
        return cond;
    }

    @Override
    public ASTNode visitRandom(RandomNode random) {
        ensureAllowed(AddAction.RANDOM, random.token);
        RestrictionAction parent = ctx.peek();
        String pname = parent.name();
        RestrictionAction here = pname.startsWith("EVENT_") && !pname.startsWith("EVENT_CONDITION_")
                ? RestrictionAction.valueOf(pname.replace("EVENT_", "EVENT_CONDITION_"))
                : RestrictionAction.CONDITIONAL;
        ctx.push(here);
        super.visitRandom(random);
        ctx.pop();
        return random;
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
        var allowed = ctx.peek();
        if (!allowed.getAllowedActions().contains(action)) {
            throw new SemanticException(t, "Action '" + action.name() + "' is not allowed in " + allowed.name().toLowerCase());
        }
    }
}