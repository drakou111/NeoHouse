package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.enums.AssignmentOperator;
import com.drakou111.neohouse.compilers.ast.enums.Holder;
import com.drakou111.neohouse.compilers.ast.expr.*;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.LongValue;
import com.drakou111.neohouse.compilers.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.main.*;
import com.drakou111.neohouse.compilers.ast.main_statements.EventNode;
import com.drakou111.neohouse.compilers.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compilers.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compilers.ast.statement.*;
import com.drakou111.neohouse.compilers.lexer.Token;

import java.util.ArrayList;
import java.util.List;

public class SimplifyingExpressionsVisitor extends DefaultVisitor {
    private final String VAR_PREFIX = "t$";
    private int tempCounter = 0;

    public MainNode analyze(MainNode main) {
        return (MainNode)main.accept(this);
    }

    private record LowerResult(ExpressionNode simple, List<StatementNode> stmts) {}

    private LowerResult lowerExpr(ExpressionNode expr) {
        if (expr instanceof ConstantPrimitiveNode || expr instanceof VariableNode || expr instanceof PlaceholderNode) {
            return new LowerResult(expr, List.of());
        }

        if (expr instanceof BinaryExpressionNode bin) {
            LowerResult leftRes  = lowerExpr(bin.left);
            LowerResult rightRes = lowerExpr(bin.right);

            List<StatementNode> stmts = new ArrayList<>();
            stmts.addAll(leftRes.stmts());
            stmts.addAll(rightRes.stmts());

            VariableNode accVar;
            boolean reuseLeft = leftRes.simple() instanceof VariableNode && ((VariableNode) leftRes.simple()).name.startsWith(VAR_PREFIX);

            if (reuseLeft) {
                accVar = (VariableNode) leftRes.simple();
            } else {
                accVar = makeTemp(leftRes.simple().token);
                stmts.add(new AssignationNode(accVar, AssignmentOperator.ASSIGN, leftRes.simple(), bin.token));
            }

            AssignmentOperator opAssign = switch (bin.operator) {
                case ADD      -> AssignmentOperator.ADD_ASSIGN;
                case SUBTRACT -> AssignmentOperator.SUB_ASSIGN;
                case MULTIPLY -> AssignmentOperator.MUL_ASSIGN;
                case DIVIDE   -> AssignmentOperator.DIV_ASSIGN;
            };
            stmts.add(new AssignationNode(accVar, opAssign, rightRes.simple(), bin.token));

            return new LowerResult(accVar, stmts);
        }

        if (expr instanceof UnaryExpressionNode un) {
            LowerResult opRes = lowerExpr(un.operand);

            List<StatementNode> stmts = new ArrayList<>(opRes.stmts());

            VariableNode accVar;
            boolean reuse = opRes.simple() instanceof VariableNode && ((VariableNode)opRes.simple()).name.startsWith(VAR_PREFIX);
            if (reuse) {
                accVar = (VariableNode)opRes.simple();
            } else {
                accVar = makeTemp(opRes.simple().token);
                stmts.add(new AssignationNode(accVar, AssignmentOperator.ASSIGN, opRes.simple(), un.token));
            }

            switch (un.operator) {
                case NEGATE, NOT -> stmts.add(new AssignationNode(accVar, AssignmentOperator.MUL_ASSIGN, new ConstantPrimitiveNode(new LongValue(-1L), null, un.token), un.token));
                case INC -> stmts.add(new AssignationNode(accVar, AssignmentOperator.ADD_ASSIGN, new ConstantPrimitiveNode(new LongValue(1L), null, un.token), un.token));
                case DEC -> stmts.add(new AssignationNode(accVar, AssignmentOperator.SUB_ASSIGN, new ConstantPrimitiveNode(new LongValue(1L), null, un.token), un.token));
                default -> throw new IllegalArgumentException("Unhandled unary op: " + un.operator);
            }

            return new LowerResult(accVar, stmts);
        }

        if (expr instanceof BooleanExpressionNode be) {
            LowerResult leftRes  = lowerExpr(be.left);
            LowerResult rightRes = lowerExpr(be.right);

            List<StatementNode> stmts = new ArrayList<>();
            stmts.addAll(leftRes.stmts());
            stmts.addAll(rightRes.stmts());

            VariableNode leftVar;
            if (leftRes.simple instanceof VariableNode lv && lv.name.startsWith(VAR_PREFIX)) {
                leftVar = lv;
            } else if (leftRes.simple instanceof VariableNode v2) {
                leftVar = v2;
            } else {
                leftVar = makeTemp(leftRes.simple.token);
                stmts.add(new AssignationNode(leftVar, AssignmentOperator.ASSIGN, leftRes.simple, be.token));
            }

            NormalExpressionNode rightSimple = (NormalExpressionNode)rightRes.simple;

            BooleanExpressionNode newBool = new BooleanExpressionNode(leftVar, rightSimple, be.operator, be.token);
            return new LowerResult(newBool, stmts);
        }

        if (expr instanceof LogicalExpressionNode le) {
            List<StatementNode> stmts = new ArrayList<>();

            List<BooleanExpressionNode> newConds = new ArrayList<>();
            for (BooleanExpressionNode cond : le.conditions) {
                LowerResult lr = lowerExpr(cond);
                stmts.addAll(lr.stmts());
                newConds.add((BooleanExpressionNode) lr.simple());
            }

            LogicalExpressionNode newLog = new LogicalExpressionNode(newConds, le.operator, le.token);

            return new LowerResult(newLog, stmts);
        }

        return new LowerResult(expr, List.of());
    }

    /**
     * Pick a fresh temp in the same holder as `basedOn`.
     * You’ll need to decide how to choose the holder (here we always use PLAYER for example).
     */
    private VariableNode makeTemp(Token basedOn) {
        Holder holder = Holder.PLAYER; // or infer from basedOn
        String name = VAR_PREFIX + (tempCounter++);
        return new VariableNode(null, holder, name, basedOn);
    }

    @Override
    public ASTNode visitMain(MainNode main) {
        List<MainStatementNode> result = new ArrayList<>();
        for (MainStatementNode stmt : main.statements) {
            ASTNode lowered = stmt.accept(this);
            if (lowered instanceof MainNode slab) {
                result.addAll(slab.statements);
            } else if (lowered instanceof MainStatementNode single) {
                result.add(single);
            } else {
                throw new IllegalStateException("Expected MainStatementNode or MainNode but got: " + lowered.getClass().getSimpleName());
            }
        }
        return new MainNode(result);
    }

    @Override
    public ASTNode visitStatements(StatementListNode statements) {
        List<StatementNode> result = new ArrayList<>();
        for (StatementNode stmt : statements.statements) {
            ASTNode lowered = stmt.accept(this);
            if (lowered instanceof StatementListNode slab) {
                result.addAll(slab.statements);
            } else if (lowered instanceof StatementNode single) {
                result.add(single);
            } else {
                // Shouldn’t happen if every visitXYZ returns statements.
                throw new IllegalStateException("Expected StatementNode or StatementListNode but got: " + lowered.getClass().getSimpleName());
            }
        }
        return new StatementListNode(result);
    }

    @Override
    public ASTNode visitAssignation(AssignationNode assignation) {
        // Lower the RHS expression fully:
        LowerResult lr = lowerExpr(assignation.value);
        List<StatementNode> stmts = new ArrayList<>(lr.stmts());
        // The final assignment: target = lr.simple
        stmts.add(new AssignationNode(assignation.variable, AssignmentOperator.ASSIGN, lr.simple, assignation.token));
        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitVoidFunctionCallStatement(VoidFunctionCallNode callStmt) {
        // Lower each argument, collecting temporary pre‐stmts:
        List<StatementNode> stmts = new ArrayList<>();
        List<ExpressionNode> newArgs = new ArrayList<>();
        for (ExpressionNode arg : callStmt.arguments.expressions) {
            LowerResult lr = lowerExpr(arg);
            stmts.addAll(lr.stmts());
            newArgs.add(lr.simple);
        }
        // Recreate the FunctionCallNode with lowered args:
        ArgumentListNode newArgList = new ArgumentListNode(newArgs);
        VoidFunctionCallNode newCall = new VoidFunctionCallNode(callStmt.functionName, newArgList, callStmt.token);
        stmts.add(newCall);
        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode callStmt) {
        List<StatementNode> stmts = new ArrayList<>();
        List<ExpressionNode> newArgs = new ArrayList<>();
        for (ExpressionNode arg : callStmt.arguments.expressions) {
            LowerResult lr = lowerExpr(arg);
            stmts.addAll(lr.stmts());
            newArgs.add(lr.simple);
        }
        ArgumentListNode newArgList = new ArgumentListNode(newArgs);
        GlobalFunctionCallNode newCall = new GlobalFunctionCallNode(callStmt.functionName, newArgList, callStmt.token);
        stmts.add(newCall);
        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitReturn(ReturnNode returnNode) {
        // Lower the returned expression (if any):
        if (returnNode.expression != null) {
            LowerResult lr = lowerExpr(returnNode.expression);
            List<StatementNode> stmts = new ArrayList<>(lr.stmts());
            ReturnNode newRet = new ReturnNode(lr.simple, returnNode.token);
            stmts.add(newRet);
            return new StatementListNode(stmts);
        } else {
            return returnNode;
        }
    }

    @Override
    public ASTNode visitConditional(ConditionalNode conditional) {
        LowerResult lr = lowerExpr(conditional.condition);
        List<StatementNode> beforeIf = new ArrayList<>(lr.stmts());

        ConditionExpressionNode simpleCond = (ConditionExpressionNode) lr.simple;
        StatementListNode newThen = (StatementListNode) visitStatements(conditional.thenBlock);
        StatementListNode newElse = (conditional.elseBlock == null)
                ? null
                : (StatementListNode) visitStatements(conditional.elseBlock);

        ConditionalNode newIf = new ConditionalNode(simpleCond, newThen, newElse, conditional.token);
        beforeIf.add(newIf);
        return new StatementListNode(beforeIf);
    }

    @Override
    public ASTNode visitRepeat(RepeatNode repeat) {
        // Lower the body of the repeat:
        StatementListNode newBody = (StatementListNode)visitStatements(repeat.statements);
        return new RepeatNode(repeat.count, newBody, repeat.token);
    }

    @Override
    public ASTNode visitLoop(LoopNode loop) {
        // Lower the body of the loop:
        StatementListNode newBody = (StatementListNode)visitStatements(loop.statements);
        return new LoopNode(loop.name, loop.description, newBody, loop.tickDelay, loop.token);
    }

    @Override
    public ASTNode visitRandom(RandomNode random) {
        // Lower each alternative block:
        List<StatementListNode> newOptions = new ArrayList<>();
        for (StatementListNode opt : random.statements) {
            newOptions.add((StatementListNode)visitStatements(opt));
        }
        return new RandomNode(newOptions, random.token);
    }

    @Override
    public ASTNode visitFunction(FunctionNode function) {
        // Lower the function‐body statements, but leave signature intact:
        StatementListNode newBody = (StatementListNode)visitStatements(function.statements);
        return new FunctionNode(function.name, function.description, function.returnType, function.parameters, newBody, function.token);
    }

    @Override
    public ASTNode visitEvent(EventNode event) {
        StatementListNode newBody = (StatementListNode)visitStatements(event.statements);
        return new EventNode(event.event, newBody, event.token);
    }
}

