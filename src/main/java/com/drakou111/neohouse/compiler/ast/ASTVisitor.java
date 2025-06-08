package com.drakou111.neohouse.compiler.ast;

import com.drakou111.neohouse.compiler.ast.expr.*;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantArrayNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.*;
import com.drakou111.neohouse.compiler.ast.main_statements.*;
import com.drakou111.neohouse.compiler.ast.statement.*;

public interface ASTVisitor<T> {
    // Expressions (Constants, Variables, Placeholder, Sub expressions, Function calls)
    T visitConstant(ConstantNode constant);
    T visitConstantPrimitive(ConstantPrimitiveNode constant);
    T visitConstantArray(ConstantArrayNode constant);
    T visitUnaryExpr(UnaryExpressionNode unaryExpression);
    T visitBinaryExpr(BinaryExpressionNode binaryExpression);
    T visitBooleanExpr(BooleanExpressionNode booleanExpression);
    T visitLogicalExpr(LogicalExpressionNode logicalExpression);
    T visitVariable(VariableNode variable);
    T visitPlaceholder(PlaceholderNode placeholder);
    T visitFunctionCall(FunctionCallNode functionCall);

    // Main Statements
    T visitTypedFunction(TypedFunctionNode function);
    T visitVoidFunction(VoidFunctionNode function);
    T visitLoop(LoopNode function);
    T visitEvent(EventNode event);
    T visitGlobalDeclaration(GlobalDeclarationNode declaration);

    // Statements
    T visitTypedReturn(TypedReturnNode returnNode);
    T visitVoidReturn(VoidReturnNode returnNode);
    T visitCancel(CancelNode cancelNode);
    T visitConditional(ConditionalNode conditional);
    T visitDefinition(DefinitionNode definition);
    T visitDeclaration(DeclarationNode declaration);
    T visitAssignation(AssignationNode assignation);
    T visitVoidFunctionCallStatement(VoidFunctionCallNode function);
    T visitGlobalFunctionCall(GlobalFunctionCallNode function);
    T visitRandom(RandomNode random);
    T visitRepeat(RepeatNode random);
    T visitConditionCall(ConditionCallNode condCall);
    T visitActionCall(ActionCallNode condCall);

    // Lists
    T visitMain(MainNode main);
    T visitStatements(StatementListNode statements);
    T visitParameters(ParameterListNode parameters);
    T visitArguments(ArgumentListNode arguments);
    T visitPrimitiveConstants(ConstantPrimitiveList constants);
}
