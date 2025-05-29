package com.drakou111.neohouse.compilers.ast;

import com.drakou111.neohouse.compilers.ast.main_statements.EventNode;
import com.drakou111.neohouse.compilers.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.UnaryExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantArrayNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compilers.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compilers.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compilers.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compilers.ast.main.*;
import com.drakou111.neohouse.compilers.ast.statement.*;

public interface ASTVisitor<T> {
    // Expressions (Constants, Variables, Placeholder, Sub expressions, Function calls)
    T visitConstant(ConstantNode constant);
    T visitConstantPrimitive(ConstantPrimitiveNode constant);
    T visitConstantArray(ConstantArrayNode constant);
    T visitUnaryExpr(UnaryExpressionNode unaryExpression);
    T visitBinaryExpr(BinaryExpressionNode binaryExpression);
    T visitVariable(VariableNode variable);
    T visitPlaceholder(PlaceholderNode placeholder);
    T visitFunctionCall(FunctionCallNode functionCall);

    // Main Statements
    T visitFunction(FunctionNode function);
    T visitLoop(LoopNode function);
    T visitEvent(EventNode event);
    T visitGlobalDeclaration(GlobalDeclarationNode declaration);

    // Statements
    T visitReturn(ReturnNode returnNode);
    T visitCancel(CancelNode cancelNode);
    T visitConditional(ConditionalNode conditional);
    T visitDefinition(DefinitionNode definition);
    T visitDeclaration(DeclarationNode declaration);
    T visitAssignation(AssignationNode assignation);
    T visitVoidFunctionCallStatement(VoidFunctionCallNode function);
    T visitGlobalFunctionCall(GlobalFunctionCallNode function);
    T visitRandom(RandomNode random);
    T visitRepeat(RepeatNode random);

    // Lists
    T visitMain(MainNode main);
    T visitStatements(StatementListNode statements);
    T visitParameters(ParameterListNode parameters);
    T visitArguments(ArgumentListNode arguments);
    T visitPrimitiveConstants(ConstantPrimitiveList constants);
}
