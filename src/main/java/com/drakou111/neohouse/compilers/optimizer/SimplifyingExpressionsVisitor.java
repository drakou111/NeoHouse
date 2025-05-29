package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.enums.AssignmentOperator;
import com.drakou111.neohouse.compilers.ast.enums.Holder;
import com.drakou111.neohouse.compilers.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.UnaryExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.LongValue;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.main.MainNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.ast.statement.AssignationNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Lowers complex expressions into sequences of assignments using temp variables.
 */
public class SimplifyingExpressionsVisitor extends DefaultVisitor<Void> {

    private final String VAR_PREFIX = "t$";
    private List<StatementNode> newStatements = new ArrayList<>();
    private int tempCounter = 0;

    public void analyze(MainNode main) {
        visitMain(main);
    }

    @Override
    public Void visitStatements(StatementListNode statements) {
        List<StatementNode> out = new ArrayList<>();
        for (StatementNode s : statements.statements) {
            newStatements = new ArrayList<>();
            s.accept(this);
            out.addAll(newStatements);
        }
        statements.statements = out;
        return null;
    }

    @Override
    public Void visitAssignation(AssignationNode assignation) {
        // Lower the right‐hand side fully
        LowerResult lr = lowerExpr(assignation.value);

        // First, emit all the intermediate temp assignments
        newStatements.addAll(lr.stmts());
        // Then emit the final: originalVar = lr.simple
        newStatements.add(new AssignationNode(
                assignation.variable,
                AssignmentOperator.ASSIGN,
                lr.simple(),
                assignation.token
        ));
        return null;
    }

    private record LowerResult(ExpressionNode simple, List<StatementNode> stmts) {}

    private LowerResult lowerExpr(ExpressionNode expr) {
        // 1) Simple case: constant or variable
        if (expr instanceof ConstantPrimitiveNode || expr instanceof VariableNode) {
            return new LowerResult(expr, List.of());
        }

        // 2) Binary expression
        if (expr instanceof BinaryExpressionNode bin) {
            // Lower left and right
            LowerResult leftRes  = lowerExpr(bin.left);
            LowerResult rightRes = lowerExpr(bin.right);

            List<StatementNode> stmts = new ArrayList<>();
            stmts.addAll(leftRes.stmts());
            stmts.addAll(rightRes.stmts());

            VariableNode accVar;
            boolean reuseLeft = leftRes.simple() instanceof VariableNode && ((VariableNode) leftRes.simple()).name.startsWith(VAR_PREFIX);

            if (reuseLeft) {
                // Reuse the temp produced by the left subtree
                accVar = (VariableNode) leftRes.simple();
            } else {
                // Create a fresh temp to hold the left value
                accVar = makeTemp(leftRes.simple());
                stmts.add(new AssignationNode(
                        accVar,
                        AssignmentOperator.ASSIGN,
                        leftRes.simple(),
                        bin.token
                ));
            }

            // Now apply the operator with a compound assignment
            AssignmentOperator opAssign = switch (bin.operator) {
                case ADD      -> AssignmentOperator.ADD_ASSIGN;
                case SUBTRACT -> AssignmentOperator.SUB_ASSIGN;
                case MULTIPLY -> AssignmentOperator.MUL_ASSIGN;
                case DIVIDE   -> AssignmentOperator.DIV_ASSIGN;
                default       -> throw new IllegalArgumentException("Unsupported " + bin.operator);
            };
            stmts.add(new AssignationNode(
                    accVar,
                    opAssign,
                    rightRes.simple(),
                    bin.token
            ));

            // The accumulator variable now holds the full result
            return new LowerResult(accVar, stmts);
        }

        if (expr instanceof UnaryExpressionNode un) {
            // First, lower the operand
            LowerResult opRes = lowerExpr(un.operand);

            // Gather any stmts that computed the operand
            List<StatementNode> stmts = new ArrayList<>(opRes.stmts());

            // Decide which temp (or var) will hold the result
            VariableNode accVar;
            boolean reuse = opRes.simple() instanceof VariableNode
                    && ((VariableNode)opRes.simple()).name.startsWith(VAR_PREFIX);
            if (reuse) {
                accVar = (VariableNode)opRes.simple();
            } else {
                accVar = makeTemp(opRes.simple());
                // assign initial value
                stmts.add(new AssignationNode(
                        accVar,
                        AssignmentOperator.ASSIGN,
                        opRes.simple(),
                        un.token
                ));
            }

            // Now apply the unary operation as a compound assignment
            switch (un.operator) {
                case NEGATE, NOT -> {
                    // multiply by -1
                    stmts.add(new AssignationNode(
                            accVar,
                            AssignmentOperator.MUL_ASSIGN,
                            new ConstantPrimitiveNode(new LongValue(-1L), un.token),
                            un.token
                    ));
                }
                case INC -> {
                    stmts.add(new AssignationNode(
                            accVar,
                            AssignmentOperator.ADD_ASSIGN,
                            new ConstantPrimitiveNode(new LongValue(1L), un.token),
                            un.token
                    ));
                }
                case DEC -> {
                    stmts.add(new AssignationNode(
                            accVar,
                            AssignmentOperator.SUB_ASSIGN,
                            new ConstantPrimitiveNode(new LongValue(1L), un.token),
                            un.token
                    ));
                }
                default -> throw new IllegalArgumentException("Unhandled unary op: " + un.operator);
            }

            return new LowerResult(accVar, stmts);
        }

        // 3) Other expr kinds: leave as-is
        return new LowerResult(expr, List.of());
    }

    /**
     * Pick a fresh temp in the same holder as `basedOn`.
     * You’ll need to decide how to choose the holder (here we always use PLAYER for example).
     */
    private VariableNode makeTemp(ExpressionNode basedOn) {
        Holder holder = Holder.PLAYER; // or infer from basedOn
        String name = VAR_PREFIX + (tempCounter++);
        return new VariableNode(null, holder, name, basedOn.token);
    }
}

