package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.BooleanExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.UnaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TupleFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.TupleDestructureNode;
import com.drakou111.neohouse.compiler.ast.statement.TypedReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.List;
import java.util.Map;

/**
 * Nominal type checking for enums. Enums are represented as {@code long}s but must behave like their
 * own type: an enum value may only be compared against / assigned from the same enum, may not appear
 * in arithmetic or other expressions, and function parameters / return types are checked at call sites.
 *
 * <p>Runs on the raw parser AST (before any rewriting visitor) so that the parser's {@code enumName}
 * tags on variables and member references are still present. Each expression visit returns the enum
 * name it evaluates to, or {@code null} if it is not an enum value.
 */
public class EnumCheckVisitor extends EmptyVisitor<String> {
    private final Map<String, List<String>> paramEnums;   // function name -> per-parameter enum name (null = non-enum)
    private final Map<String, String> returnEnums;        // function name -> enum return name (null = non-enum)
    private String currentReturnEnum;                     // enum return of the function currently being checked

    public EnumCheckVisitor(Map<String, List<String>> paramEnums, Map<String, String> returnEnums) {
        this.paramEnums = paramEnums;
        this.returnEnums = returnEnums;
    }

    public void check(MainNode main) {
        main.accept(this);
    }

    // ---------- expressions: return the enum identity of the value, or null ----------

    @Override public String visitConstantPrimitive(ConstantPrimitiveNode n) { return n.enumName; }

    @Override public String visitVariable(VariableNode v) { return v.enumName; }

    @Override public String visitFunctionCall(FunctionCallNode call) {
        checkCallArgs(call.functionName, call.arguments, call.token);
        return returnEnums.get(call.functionName);
    }

    @Override public String visitBinaryExpr(BinaryExpressionNode n) {
        rejectEnum(n.left.accept(this), n.token);
        rejectEnum(n.right.accept(this), n.token);
        return null;
    }

    @Override public String visitUnaryExpr(UnaryExpressionNode n) {
        rejectEnum(n.operand.accept(this), n.token);
        return null;
    }

    @Override public String visitBooleanExpr(BooleanExpressionNode n) {
        String l = n.left.accept(this);
        String r = n.right.accept(this);
        if (l == null && r == null) return null;
        if (l != null && r != null) {
            if (!l.equals(r))
                throw new SemanticException(n.token, "Cannot compare enum '" + l + "' with enum '" + r + "'; both sides must be the same enum");
        } else {
            throw new SemanticException(n.token, "Cannot compare enum '" + (l != null ? l : r) + "' with a non-enum value");
        }
        return null;
    }

    // ---------- statements ----------

    @Override public String visitAssignation(AssignationNode a) {
        String target = a.variable.enumName;
        String value = a.value.accept(this);
        if (target != null && a.operator != AssignmentOperator.ASSIGN)
            throw new SemanticException(a.token, "Cannot use compound assignment on '" + a.variable.name +
                    "' of enum type '" + target + "'; enums only support '='");
        checkAssign(target, value, a.variable.name, a.token);
        return null;
    }

    @Override public String visitGlobalDeclaration(GlobalDeclarationNode d) {
        if (d.initializer != null)
            checkAssign(d.variable.enumName, d.initializer.accept(this), d.variable.name, d.token);
        return null;
    }

    @Override public String visitTypedReturn(TypedReturnNode r) {
        String value = r.expression.accept(this);
        if (currentReturnEnum != null) {
            if (value == null)
                throw new SemanticException(r.token, "Function must return enum '" + currentReturnEnum + "', but the returned value is not an enum");
            if (!currentReturnEnum.equals(value))
                throw new SemanticException(r.token, "Function returns enum '" + currentReturnEnum + "' but this returns enum '" + value + "'");
        } else if (value != null) {
            throw new SemanticException(r.token, "Cannot return enum '" + value + "' from a function whose return type is not '" + value + "'");
        }
        return null;
    }

    @Override public String visitTypedFunction(TypedFunctionNode fn) {
        String saved = currentReturnEnum;
        currentReturnEnum = returnEnums.get(fn.name);
        fn.statements.accept(this);
        currentReturnEnum = saved;
        return null;
    }

    @Override public String visitVoidFunction(VoidFunctionNode fn) {
        String saved = currentReturnEnum;
        currentReturnEnum = null;
        fn.statements.accept(this);
        currentReturnEnum = saved;
        return null;
    }

    @Override public String visitTupleFunction(TupleFunctionNode fn) {
        String saved = currentReturnEnum;
        currentReturnEnum = null;
        fn.statements.accept(this);
        currentReturnEnum = saved;
        return null;
    }

    @Override public String visitVoidFunctionCall(VoidFunctionCallNode c) {
        checkCallArgs(c.functionName, c.arguments, c.token);
        return null;
    }

    @Override public String visitGlobalFunctionCall(GlobalFunctionCallNode c) {
        checkCallArgs(c.functionName, c.arguments, c.token);
        return null;
    }

    @Override public String visitTupleDestructure(TupleDestructureNode n) {
        checkCallArgs(n.functionName, n.arguments, n.token);
        return null;
    }

    // ---------- helpers ----------

    /** Visits each argument (checking nested expressions) and verifies its enum identity matches the parameter. */
    private void checkCallArgs(String fname, ArgumentListNode args, Token token) {
        List<String> pe = paramEnums.get(fname);
        List<NormalExpressionNode> exprs = args.expressions;
        for (int i = 0; i < exprs.size(); i++) {
            String argEnum = exprs.get(i).accept(this);
            if (pe == null) continue;
            String want = i < pe.size() ? pe.get(i) : null;
            if (want != null) {
                if (argEnum == null)
                    throw new SemanticException(token, "Argument " + (i + 1) + " of '" + fname + "' must be enum '" + want + "'");
                if (!want.equals(argEnum))
                    throw new SemanticException(token, "Argument " + (i + 1) + " of '" + fname + "' is enum '" + argEnum + "' but '" + want + "' is expected");
            } else if (argEnum != null) {
                throw new SemanticException(token, "Argument " + (i + 1) + " of '" + fname + "' is enum '" + argEnum + "' but a non-enum value is expected");
            }
        }
    }

    private void checkAssign(String target, String value, String name, Token token) {
        if (target != null) {
            if (value == null)
                throw new SemanticException(token, "Cannot assign a non-enum value to '" + name + "' of enum type '" + target + "'");
            if (!target.equals(value))
                throw new SemanticException(token, "Cannot assign enum '" + value + "' to '" + name + "' of enum type '" + target + "'");
        } else if (value != null) {
            throw new SemanticException(token, "Cannot assign enum '" + value + "' to non-enum variable '" + name + "'");
        }
    }

    private void rejectEnum(String enumName, Token token) {
        if (enumName != null)
            throw new SemanticException(token, "Enum '" + enumName + "' cannot be used in an expression; enums only support assignment and comparison");
    }
}
