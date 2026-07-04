package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.*;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.*;
import com.drakou111.neohouse.compiler.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.*;
import com.drakou111.neohouse.compiler.ast.main_statements.CommandNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compiler.ast.main_statements.SlotNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TupleFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.*;
import com.drakou111.neohouse.compiler.enums.*;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.constants.Placeholder;
import com.drakou111.neohouse.constants.StringLimits;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.List;

public class SimplifyingExpressionsVisitor extends DefaultVisitor {
    private static final String TEMP_MARKER = "t$";
    private int tempCounter = 0;

    private static boolean isTempVar(VariableNode v) {
        return v.name.contains(TEMP_MARKER);
    }

    private record LowerResult(ExpressionNode simple, List<StatementNode> stmts) {}

    private LowerResult lowerExpr(ExpressionNode expr) {
        if (expr instanceof ConstantPrimitiveNode || expr instanceof PlaceholderNode || expr instanceof VariableNode) {
            return new LowerResult(expr, List.of());
        }

        if (expr instanceof BinaryExpressionNode bin) {
            // String concatenation: fold the entire tree to a compile-time template string
            if (bin.type == VariableType.STRING && bin.operator == BinaryOperator.ADD) {
                FoldResult fold = foldStringConcat(bin);
                if (fold.template().length() > StringLimits.CHAT_TEXT) {
                    throw new SemanticException(bin.token,
                            "String is too long (" + fold.template().length() + " characters). Maximum is " + StringLimits.CHAT_TEXT + ".");
                }
                return new LowerResult(
                        new ConstantPrimitiveNode(new StringValue(fold.template()), VariableType.STRING, bin.token),
                        fold.stmts());
            }

            LowerResult leftRes  = lowerExpr(bin.left);
            LowerResult rightRes = lowerExpr(bin.right);

            List<StatementNode> stmts = new ArrayList<>();
            stmts.addAll(leftRes.stmts());
            stmts.addAll(rightRes.stmts());

            VariableType leftType  = leftRes.simple.type;
            VariableType rightType = rightRes.simple.type;

            VariableType varType;
            if (leftType == VariableType.VAR || rightType == VariableType.VAR) {
                varType = VariableType.VAR;
            } else if (leftType == rightType) {
                varType = leftType;
            } else {
                throw new SemanticException(bin.token, "Type mismatch in binary expression: left is " + leftType + ", right is " + rightType);
            }

            VariableNode accVar;
            boolean reuseLeft = leftRes.simple instanceof VariableNode lv0 && isTempVar(lv0);

            if (reuseLeft) {
                accVar = (VariableNode) leftRes.simple;
            } else {
                accVar = makeTemp(leftRes.simple.token, varType);
                stmts.add(new AssignationNode(accVar, AssignmentOperator.ASSIGN, (NormalExpressionNode) leftRes.simple, bin.token));
            }

            AssignmentOperator opAssign = switch (bin.operator) {
                case ADD                -> AssignmentOperator.ADD_ASSIGN;
                case SUBTRACT           -> AssignmentOperator.SUB_ASSIGN;
                case MULTIPLY           -> AssignmentOperator.MUL_ASSIGN;
                case DIVIDE             -> AssignmentOperator.DIV_ASSIGN;
                case BITWISE_OR         -> AssignmentOperator.BITWISE_OR_ASSIGN;
                case BITWISE_AND        -> AssignmentOperator.BITWISE_AND_ASSIGN;
                case BITWISE_XOR        -> AssignmentOperator.BITWISE_XOR_ASSIGN;
                case LEFT_SHIFT         -> AssignmentOperator.LEFT_SHIFT_ASSIGN;
                case RIGHT_SHIFT        -> AssignmentOperator.RIGHT_SHIFT_ASSIGN;
                case UNSIGNED_RIGHT_SHIFT -> AssignmentOperator.UNSIGNED_RIGHT_SHIFT_ASSIGN;
                default -> throw new SemanticException(bin.token, "Unsupported binary operator: " + bin.operator);
            };

            stmts.add(new AssignationNode(accVar, opAssign, (NormalExpressionNode) rightRes.simple, bin.token));

            return new LowerResult(accVar, stmts);
        }

        if (expr instanceof UnaryExpressionNode un) {
            LowerResult opRes = lowerExpr(un.operand);

            List<StatementNode> stmts = new ArrayList<>(opRes.stmts());

            switch (un.operator) {
                case NOT -> {
                    // !expr → expr == false/0  (Housing has no native boolean NOT)
                    ConstantPrimitiveNode falsy = switch (un.operand.type) {
                        case BOOL   -> new ConstantPrimitiveNode(new BoolValue(false), VariableType.BOOL, un.token);
                        case DOUBLE -> new ConstantPrimitiveNode(new DoubleValue(0.0), VariableType.DOUBLE, un.token);
                        default     -> new ConstantPrimitiveNode(new LongValue(0L), VariableType.LONG, un.token);
                    };
                    return new LowerResult(
                            new BooleanExpressionNode((NormalExpressionNode) opRes.simple(), falsy, BooleanOperator.EQUALS, un.token),
                            stmts);
                }
                case LONG_CAST -> {
                    if (un.operand.type == VariableType.STRING) {
                        throw new SemanticException(un.token, "Cannot cast STRING to LONG");
                    }
                    // Apply L suffix to the source so the assignment reads it as a long.
                    // t$ temps are single-use and safe to mutate; user variables get a copy
                    // to avoid polluting other references to the same VariableNode.
                    NormalExpressionNode inner = (NormalExpressionNode) opRes.simple();
                    NormalExpressionNode castSource;
                    if (inner instanceof VariableNode v && isTempVar(v)) {
                        v.castSuffix = "L";
                        castSource = v;
                    } else if (inner instanceof VariableNode v) {
                        VariableNode copy = new VariableNode(v.type, v.holder, v.name, v.teamName, v.token);
                        copy.castSuffix = "L";
                        copy.fallback = v.fallback;
                        castSource = copy;
                    } else {
                        castSource = inner; // constant — no suffix needed
                    }
                    VariableNode result = makeTemp(un.token, VariableType.LONG);
                    stmts.add(new AssignationNode(result, AssignmentOperator.ASSIGN, castSource, un.token));
                    return new LowerResult(result, stmts);
                }
                case DOUBLE_CAST -> {
                    if (un.operand.type == VariableType.STRING) {
                        throw new SemanticException(un.token, "Cannot cast STRING to DOUBLE");
                    }
                    boolean reuse = opRes.simple() instanceof VariableNode rv && isTempVar(rv);
                    VariableNode result;
                    if (reuse) {
                        result = (VariableNode) opRes.simple();
                    } else {
                        result = makeTemp(un.token, VariableType.DOUBLE);
                        stmts.add(new AssignationNode(result, AssignmentOperator.ASSIGN, (NormalExpressionNode) opRes.simple(), un.token));
                    }
                    result.type = VariableType.DOUBLE;
                    if (result.castSuffix == null) result.castSuffix = "D";
                    return new LowerResult(result, stmts);
                }
                case STRING_CAST -> {
                    VariableType fromType = un.operand.type;
                    if (fromType == VariableType.STRING) return opRes;
                    // Constant literal → convert at compile time, no temp variable needed
                    if (opRes.simple() instanceof ConstantPrimitiveNode constNode) {
                        String strVal = switch (constNode.value) {
                            case LongValue l   -> l.toString();
                            case DoubleValue d -> d.toString();
                            case BoolValue b   -> b.toString();
                            default -> throw new SemanticException(un.token, "Cannot cast to string");
                        };
                        return new LowerResult(new ConstantPrimitiveNode(
                                new StringValue(strVal), VariableType.STRING, un.token), opRes.stmts());
                    }
                    // Variable → embed as in-game placeholder reference (avoids broken string +=)
                    if (opRes.simple() instanceof VariableNode v) {
                        String ref = switch (v.holder) {
                            case PLAYER -> "%var.player/" + v.name + v.fallbackToken() + "%";
                            case GLOBAL -> "%var.global/" + v.name + v.fallbackToken() + "%";
                            case TEAM   -> "%var.team/" + v.name + " " + v.teamName + v.fallbackToken() + "%";
                        };
                        return new LowerResult(new ConstantPrimitiveNode(
                                new StringValue(ref), VariableType.STRING, un.token), opRes.stmts());
                    }
                    // Placeholder → embed as in-game placeholder reference
                    if (opRes.simple() instanceof PlaceholderNode p) {
                        com.drakou111.neohouse.constants.Placeholder ph =
                                com.drakou111.neohouse.constants.Placeholder.requireByName(p.placeholder);
                        StringBuilder sb = new StringBuilder("%").append(ph.inGameName);
                        List<ConstantPrimitiveNode> consts = p.constants.constants;
                        if (!consts.isEmpty()) {
                            sb.append("/");
                            for (int i = 0; i < consts.size(); i++) {
                                if (i > 0) sb.append(" ");
                                ConstantPrimitive prim = consts.get(i).value;
                                sb.append(switch (prim) {
                                    case StringValue sv -> sv.value;
                                    case LongValue lv   -> lv.toString();
                                    case DoubleValue dv -> dv.toString();
                                    case BoolValue bv   -> bv.toString();
                                });
                            }
                        }
                        sb.append("%");
                        return new LowerResult(new ConstantPrimitiveNode(
                                new StringValue(sb.toString()), VariableType.STRING, un.token), opRes.stmts());
                    }
                    // Fallback for complex expressions: assign "" then += value
                    VariableNode result = makeTemp(un.token, VariableType.STRING);
                    ConstantPrimitiveNode emptyStr = new ConstantPrimitiveNode(
                            new StringValue(""), VariableType.STRING, un.token);
                    stmts.add(new AssignationNode(result, AssignmentOperator.ASSIGN, emptyStr, un.token));
                    stmts.add(new AssignationNode(result, AssignmentOperator.ADD_ASSIGN,
                            (NormalExpressionNode) opRes.simple(), un.token));
                    return new LowerResult(result, stmts);
                }
            }

            VariableNode accVar;
            boolean reuse = opRes.simple() instanceof VariableNode rv2 && isTempVar(rv2);
            if (reuse) {
                accVar = (VariableNode) opRes.simple();
            } else {
                accVar = makeTemp(opRes.simple().token, un.operand.type);
                stmts.add(new AssignationNode(accVar, AssignmentOperator.ASSIGN, (NormalExpressionNode) opRes.simple(), un.token));
            }

            VariableType varType = un.operand.type;
            ConstantPrimitiveNode constantNode;
            switch (varType) {
                case LONG -> {
                    long value = switch (un.operator) {
                        case NEGATE -> -1L;
                        case INC, DEC -> 1L;
                        default -> throw new SemanticException(un.token, "Unhandled unary op: " + un.operator);
                    };
                    constantNode = new ConstantPrimitiveNode(new LongValue(value), VariableType.LONG, un.token);
                }
                case DOUBLE -> {
                    double value = switch (un.operator) {
                        case NEGATE -> -1.0D;
                        case INC, DEC -> 1.0D;
                        default -> throw new SemanticException(un.token, "Unhandled unary op: " + un.operator);
                    };
                    constantNode = new ConstantPrimitiveNode(new DoubleValue(value), VariableType.DOUBLE, un.token);
                }
                case BOOL, STRING -> throw new SemanticException(un.token, "Cannot apply '" + un.operator + "' to type " + varType);
                default -> throw new SemanticException(un.token, "Unknown VariableType: " + varType);
            }

            AssignmentOperator op = switch (un.operator) {
                case NEGATE -> AssignmentOperator.MUL_ASSIGN;
                case INC -> AssignmentOperator.ADD_ASSIGN;
                case DEC -> AssignmentOperator.SUB_ASSIGN;
                default -> throw new SemanticException(un.token, "Unhandled unary op: " + un.operator
                );
            };

            stmts.add(new AssignationNode(accVar, op, constantNode, un.token));

            return new LowerResult(accVar, stmts);
        }

        if (expr instanceof BooleanExpressionNode be) {
            LowerResult leftRes  = lowerExpr(be.left);
            LowerResult rightRes = lowerExpr(be.right);

            VariableType leftType = leftRes.simple.type;
            VariableType rightType = rightRes.simple.type;

            VariableType varType;
            if (leftType == VariableType.VAR || rightType == VariableType.VAR) {
                varType = VariableType.VAR;
            } else if (leftType == rightType) {
                varType = leftType;
            } else {
                throw new SemanticException(be.token, "Type mismatch in boolean expression: left is " + leftType + ", right is " + rightType);
            }

            List<StatementNode> stmts = new ArrayList<>();
            stmts.addAll(leftRes.stmts());
            stmts.addAll(rightRes.stmts());

            VariableNode leftVar;
            if (leftRes.simple instanceof VariableNode lv && isTempVar(lv)) {
                leftVar = lv;
            } else if (leftRes.simple instanceof VariableNode v2) {
                leftVar = v2;
            } else {
                leftVar = makeTemp(leftRes.simple.token, varType);
                stmts.add(new AssignationNode(leftVar, AssignmentOperator.ASSIGN, (NormalExpressionNode) leftRes.simple, be.token));
            }

            NormalExpressionNode rightSimple = (NormalExpressionNode)rightRes.simple;

            BooleanExpressionNode newBool = new BooleanExpressionNode(leftVar, rightSimple, be.operator, be.token);
            return new LowerResult(newBool, stmts);
        }

        if (expr instanceof LogicalExpressionNode le) {
            List<StatementNode> stmts = new ArrayList<>();

            List<ExpressionNode> newConds = new ArrayList<>();
            for (ExpressionNode condExpr : le.conditions) {
                LowerResult lr = lowerExpr(condExpr);
                stmts.addAll(lr.stmts());

                ExpressionNode simple = lr.simple();

                if (simple instanceof BooleanExpressionNode) {
                    newConds.add(simple);

                } else if (simple instanceof ConditionCallNode) {
                    newConds.add(simple);
                } else if (simple instanceof ConstantPrimitiveNode) {
                    throw new SemanticException(simple.token, "Cannot use a constant directly as a condition");
                } else if (simple instanceof VariableNode || simple instanceof PlaceholderNode) {
                    ConstantPrimitiveNode truthy = switch (simple.type) {
                        case BOOL   -> new ConstantPrimitiveNode(new BoolValue(true), VariableType.BOOL, simple.token);
                        case LONG   -> new ConstantPrimitiveNode(new LongValue(0L), VariableType.LONG, simple.token);
                        case DOUBLE -> new ConstantPrimitiveNode(new DoubleValue(0.0), VariableType.DOUBLE, simple.token);
                        default -> throw new SemanticException(simple.token, "Cannot use a variable of type " + simple.type + " directly in a logical condition");
                    };
                    BooleanOperator op = simple.type == VariableType.BOOL ? BooleanOperator.EQUALS : BooleanOperator.GREATER_THAN;
                    newConds.add(new BooleanExpressionNode((NormalExpressionNode)simple, truthy, op, simple.token));
                }
                else {
                    throw new SemanticException(le.token, "Cannot lower logical condition of type " + simple.getClass().getSimpleName());
                }
            }

            LogicalExpressionNode newLog = new LogicalExpressionNode(newConds, le.operator, le.token);
            return new LowerResult(newLog, stmts);
        }

        if (expr instanceof FunctionCallNode call) {
            List<StatementNode> stmts = new ArrayList<>();
            List<NormalExpressionNode> newArgs = new ArrayList<>();
            for (NormalExpressionNode arg : call.arguments.expressions) {
                LowerResult lr = lowerExpr(arg);
                stmts.addAll(lr.stmts());
                newArgs.add((NormalExpressionNode)lr.simple);
            }
            ArgumentListNode loweredArgs = new ArgumentListNode(newArgs);
            FunctionCallNode newCall = new FunctionCallNode(call.functionName, loweredArgs, call.type, call.token);
            return new LowerResult(newCall, stmts);
        }

        return new LowerResult(expr, List.of());
    }

    private record FoldResult(String template, List<StatementNode> stmts) {}

    private FoldResult foldStringConcat(NormalExpressionNode expr) {
        String simple = tryFoldToTemplate(expr);
        if (simple != null) return new FoldResult(simple, List.of());

        if (expr instanceof BinaryExpressionNode b && b.type == VariableType.STRING && b.operator == BinaryOperator.ADD) {
            FoldResult left  = foldStringConcat(b.left);
            FoldResult right = foldStringConcat(b.right);
            List<StatementNode> stmts = new ArrayList<>();
            stmts.addAll(left.stmts());
            stmts.addAll(right.stmts());
            return new FoldResult(left.template() + right.template(), stmts);
        }

        // Complex sub-expression: lower it, store in a temp var, use var in template
        LowerResult lr = lowerExpr(expr);
        VariableNode tmp = makeTemp(expr.token, VariableType.STRING);
        List<StatementNode> stmts = new ArrayList<>(lr.stmts());
        stmts.add(new AssignationNode(tmp, AssignmentOperator.ASSIGN, (NormalExpressionNode) lr.simple(), expr.token));
        return new FoldResult(tryFoldToTemplate(tmp), stmts);
    }

    private String tryFoldToTemplate(NormalExpressionNode expr) {
        return switch (expr) {
            case ConstantPrimitiveNode c -> switch (c.value) {
                case StringValue s -> s.value;
                case LongValue l   -> l.toString();
                case DoubleValue d -> d.toString();
                case BoolValue b   -> b.toString();
            };
            case VariableNode v -> switch (v.holder) {
                case PLAYER -> "%var.player/" + v.name + v.fallbackToken() + "%";
                case GLOBAL -> "%var.global/" + v.name + v.fallbackToken() + "%";
                case TEAM   -> "%var.team/" + v.name + " " + v.teamName + v.fallbackToken() + "%";
            };
            case PlaceholderNode p -> {
                Placeholder ph = Placeholder.requireByName(p.placeholder);
                StringBuilder sb = new StringBuilder("%").append(ph.inGameName);
                List<ConstantPrimitiveNode> consts = p.constants.constants;
                if (!consts.isEmpty()) {
                    sb.append("/");
                    for (int i = 0; i < consts.size(); i++) {
                        if (i > 0) sb.append(" ");
                        sb.append(tryFoldToTemplate(consts.get(i)));
                    }
                }
                sb.append("%");
                yield sb.toString();
            }
            case UnaryExpressionNode u when u.operator == UnaryOperator.STRING_CAST ->
                tryFoldToTemplate(u.operand);
            case BinaryExpressionNode b when b.type == VariableType.STRING && b.operator == BinaryOperator.ADD -> {
                String left = tryFoldToTemplate(b.left);
                if (left == null) yield null;
                String right = tryFoldToTemplate(b.right);
                if (right == null) yield null;
                yield left + right;
            }
            default -> null;
        };
    }

    private VariableNode makeTemp(Token basedOn, VariableType type) {
        String name = TEMP_MARKER + (tempCounter++);
        return new VariableNode(type, Holder.PLAYER, name, null, basedOn);
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
                throw new SemanticException(main.token, "Expected start of code");
            }
        }
        return new MainNode(result);
    }

    @Override
    public ASTNode visitStatements(StatementListNode statements) {
        List<StatementNode> result = new ArrayList<>();
        for (StatementNode stmt : statements.statements) {
            tempCounter = 0;
            ASTNode lowered = stmt.accept(this);
            if (lowered instanceof StatementListNode slab) {
                result.addAll(slab.statements);
            } else if (lowered instanceof StatementNode single) {
                result.add(single);
            } else {
                throw new SemanticException(statements.token, "Expected statement(s)");
            }
        }
        return new StatementListNode(result);
    }

    @Override
    public ASTNode visitAssignation(AssignationNode assignation) {
        LowerResult lr = lowerExpr(assignation.value);

        List<StatementNode> stmts = new ArrayList<>(lr.stmts());

        VariableNode lhsVar = assignation.variable;
        VariableType lhsType = lhsVar.type;
        VariableType rhsType = lr.simple.type;

        // BooleanExpressionNode result (e.g. from `!boolVar`) — emit if/else to set true or false
        if (lr.simple instanceof BooleanExpressionNode be) {
            LogicalExpressionNode cond = new LogicalExpressionNode(List.of(be), LogicalOperator.AND, be.token);
            ConstantPrimitiveNode trueVal  = new ConstantPrimitiveNode(new BoolValue(true),  VariableType.BOOL, be.token);
            ConstantPrimitiveNode falseVal = new ConstantPrimitiveNode(new BoolValue(false), VariableType.BOOL, be.token);
            stmts.add(new ConditionalNode(cond,
                    new StatementListNode(List.of(new AssignationNode(lhsVar, AssignmentOperator.ASSIGN, trueVal,  be.token))),
                    new StatementListNode(List.of(new AssignationNode(lhsVar, AssignmentOperator.ASSIGN, falseVal, be.token))),
                    be.token));
            return new StatementListNode(stmts);
        }

        if (lhsType != VariableType.VAR && rhsType != VariableType.VAR && lhsType != rhsType) {
            throw new SemanticException(assignation.token, "Cannot assign a " + rhsType + " into a " + lhsType + " variable '" + lhsVar.name + "'");
        }

        stmts.add(new AssignationNode(lhsVar, assignation.operator, (NormalExpressionNode) lr.simple, assignation.token));

        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitActionCall(ActionCallNode actionCall) {
        List<StatementNode> preStmts = new ArrayList<>();
        List<NormalExpressionNode> newArgs = new ArrayList<>();
        for (NormalExpressionNode arg : actionCall.arguments.expressions) {
            LowerResult lr = lowerExpr(arg);
            preStmts.addAll(lr.stmts());
            newArgs.add((NormalExpressionNode) lr.simple());
        }
        preStmts.add(new ActionCallNode(actionCall.name, new ArgumentListNode(newArgs), actionCall.token));
        return new StatementListNode(preStmts);
    }

    @Override
    public ASTNode visitVoidFunctionCall(VoidFunctionCallNode callStmt) {
        List<StatementNode> stmts = new ArrayList<>();
        List<NormalExpressionNode> newArgs = new ArrayList<>();
        for (NormalExpressionNode arg : callStmt.arguments.expressions) {
            LowerResult lr = lowerExpr(arg);
            stmts.addAll(lr.stmts());
            newArgs.add((NormalExpressionNode)lr.simple);
        }
        ArgumentListNode newArgList = new ArgumentListNode(newArgs);
        VoidFunctionCallNode newCall = new VoidFunctionCallNode(callStmt.functionName, newArgList, callStmt.token);
        stmts.add(newCall);
        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode callStmt) {
        List<StatementNode> stmts = new ArrayList<>();
        List<NormalExpressionNode> newArgs = new ArrayList<>();
        for (NormalExpressionNode arg : callStmt.arguments.expressions) {
            LowerResult lr = lowerExpr(arg);
            stmts.addAll(lr.stmts());
            newArgs.add((NormalExpressionNode)lr.simple);
        }
        ArgumentListNode newArgList = new ArgumentListNode(newArgs);
        GlobalFunctionCallNode newCall = new GlobalFunctionCallNode(callStmt.functionName, newArgList, callStmt.token);
        stmts.add(newCall);
        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitTypedReturn(TypedReturnNode returnNode) {
        if (returnNode.expression != null) {
            LowerResult lr = lowerExpr(returnNode.expression);
            List<StatementNode> stmts = new ArrayList<>(lr.stmts());
            TypedReturnNode newRet = new TypedReturnNode((NormalExpressionNode)lr.simple, returnNode.token);
            stmts.add(newRet);
            return new StatementListNode(stmts);
        } else {
            return returnNode;
        }
    }

    @Override
    public ASTNode visitTupleReturn(TupleReturnNode returnNode) {
        List<StatementNode> stmts = new ArrayList<>();
        List<NormalExpressionNode> simpleExprs = new ArrayList<>();
        for (NormalExpressionNode expr : returnNode.expressions) {
            LowerResult lr = lowerExpr(expr);
            stmts.addAll(lr.stmts());
            simpleExprs.add((NormalExpressionNode) lr.simple);
        }
        stmts.add(new TupleReturnNode(simpleExprs, returnNode.token));
        return new StatementListNode(stmts);
    }

    @Override
    public ASTNode visitConditional(ConditionalNode conditional) {
        LowerResult lr = lowerExpr(conditional.condition);
        List<StatementNode> beforeIf = new ArrayList<>(lr.stmts());

        ExpressionNode simpleCond = lr.simple;
        StatementListNode newThen = (StatementListNode) visitStatements(conditional.thenBlock);
        StatementListNode newElse = (conditional.elseBlock == null) ? null : (StatementListNode) visitStatements(conditional.elseBlock);

        ConditionalNode newIf = new ConditionalNode(simpleCond, newThen, newElse, conditional.token);
        beforeIf.add(newIf);
        return new StatementListNode(beforeIf);
    }

    @Override
    public ASTNode visitRepeat(RepeatNode repeat) {
        StatementListNode newBody = (StatementListNode)visitStatements(repeat.statements);
        return new RepeatNode(repeat.count, newBody, repeat.token);
    }

    @Override
    public ASTNode visitLoop(LoopNode loop) {
        tempCounter = 0;
        StatementListNode newBody = (StatementListNode)visitStatements(loop.statements);
        return new LoopNode(loop.name, loop.description, loop.icon, newBody, loop.tickDelay, loop.isConst, loop.token);
    }

    @Override
    public ASTNode visitCommand(CommandNode command) {
        tempCounter = 0;
        StatementListNode newBody = (StatementListNode)visitStatements(command.statements);
        return new CommandNode(command.name, command.targeted, command.priority, command.listed, newBody, command.isConst, command.token);
    }

    @Override
    public ASTNode visitSlot(SlotNode slot) {
        tempCounter = 0;
        StatementListNode newBody = (StatementListNode)visitStatements(slot.statements);
        return new SlotNode(slot.slotId, slot.item, newBody, slot.token);
    }

    @Override
    public ASTNode visitRandom(RandomNode random) {
        List<StatementListNode> newOptions = new ArrayList<>();
        for (StatementListNode opt : random.statements) {
            newOptions.add((StatementListNode)visitStatements(opt));
        }
        return new RandomNode(newOptions, random.weights, random.token);
    }

    @Override
    public ASTNode visitTypedFunction(TypedFunctionNode function) {
        tempCounter = 0;
        StatementListNode newBody = (StatementListNode)visitStatements(function.statements);
        return new TypedFunctionNode(function.name, function.description, function.icon, function.returnType, function.parameters, newBody, function.isConst, function.token);
    }

    @Override
    public ASTNode visitTupleFunction(TupleFunctionNode function) {
        tempCounter = 0;
        StatementListNode newBody = (StatementListNode)visitStatements(function.statements);
        return new TupleFunctionNode(function.name, function.description, function.icon, function.returnTypes, function.parameters, newBody, function.isConst, function.token);
    }

    @Override
    public ASTNode visitVoidFunction(VoidFunctionNode function) {
        tempCounter = 0;
        StatementListNode newBody = (StatementListNode)visitStatements(function.statements);
        return new VoidFunctionNode(function.name, function.description, function.icon, function.parameters, newBody, function.isConst, function.token);
    }

    @Override
    public ASTNode visitEvent(EventNode event) {
        tempCounter = 0;
        StatementListNode newBody = (StatementListNode)visitStatements(event.statements);
        return new EventNode(event.event, newBody, event.isConst, event.token);
    }
}

