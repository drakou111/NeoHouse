package com.drakou111.neohouse.compilers.ast;

import com.drakou111.neohouse.compilers.ast.event.EventNode;
import com.drakou111.neohouse.compilers.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.UnaryExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.call.GlobalFunctionCallNode;
import com.drakou111.neohouse.compilers.ast.expr.call.SingleFunctionCallNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantArrayNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compilers.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.function.AutomaticFunctionNode;
import com.drakou111.neohouse.compilers.ast.function.FunctionNode;
import com.drakou111.neohouse.compilers.ast.statement.MainNode;

public interface ASTVisitor<T> {
    T visitConstant(ConstantNode constant);
    T visitConstantPrimitive(ConstantPrimitiveNode constant);
    T visitConstantArray(ConstantArrayNode constant);

    T visitUnaryExpr(UnaryExpressionNode unaryExpression);
    T visitBinaryExpr(BinaryExpressionNode binaryExpression);
    T visitVariable(VariableNode variable);
    T visitPlaceholder(PlaceholderNode placeholder);
    T visitSingleFunctionCall(SingleFunctionCallNode functionCall);
    T visitGlobalFunctionCall(GlobalFunctionCallNode functionCall);

    T visitFunction(FunctionNode function);
    T visitAutomaticFunction(AutomaticFunctionNode function);
    T visitEvent(EventNode event);

    T visitMain(MainNode main);
}
