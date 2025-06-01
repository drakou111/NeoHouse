package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.*;
import com.drakou111.neohouse.compilers.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantArrayNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compilers.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.main.*;
import com.drakou111.neohouse.compilers.ast.main_statements.EventNode;
import com.drakou111.neohouse.compilers.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compilers.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compilers.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compilers.ast.statement.*;

public abstract class DefaultVisitor extends EmptyVisitor<ASTNode> {

    // Expressions (Constants, Variables, Placeholder, Sub expressions, Function calls)
    @Override public ASTNode visitConstant(ConstantNode constant) {
        return constant;
    }

    @Override public ASTNode visitConstantPrimitive(ConstantPrimitiveNode constant)  {
        return constant;
    }

    @Override public ASTNode visitConstantArray(ConstantArrayNode constant) {
        return constant;
    }

    @Override public ASTNode visitUnaryExpr(UnaryExpressionNode unaryExpression) {
        unaryExpression.operand.accept(this);
        return unaryExpression;
    }

    @Override public ASTNode visitBinaryExpr(BinaryExpressionNode binaryExpression) {
        binaryExpression.left.accept(this);
        binaryExpression.right.accept(this);
        return binaryExpression;
    }

    @Override public ASTNode visitBooleanExpr(BooleanExpressionNode booleanExpression) {
        booleanExpression.left.accept(this);
        booleanExpression.right.accept(this);
        return booleanExpression;
    }

    @Override public ASTNode visitLogicalExpr(LogicalExpressionNode logicalExpression) {
        for (BooleanExpressionNode expr : logicalExpression.conditions) {
            expr.accept(this);
        }
        return logicalExpression;
    }

    @Override public ASTNode visitVariable(VariableNode variable) {
        return variable;
    }

    @Override public ASTNode visitPlaceholder(PlaceholderNode placeholder) {
        placeholder.constants.accept(this);
        return placeholder;
    }

    @Override public ASTNode visitFunctionCall(FunctionCallNode functionCall) {
        functionCall.arguments.accept(this);
        return functionCall;
    }

    // Main Statements
    @Override public ASTNode visitFunction(FunctionNode function) {
        function.parameters.accept(this);
        function.statements.accept(this);
        return function;
    }

    @Override public ASTNode visitLoop(LoopNode loop) {
        loop.statements.accept(this);
        return loop;
    }

    @Override public ASTNode visitEvent(EventNode event) {
        event.statements.accept(this);
        return event;
    }

    @Override public ASTNode visitGlobalDeclaration(GlobalDeclarationNode declaration) {
        declaration.variable.accept(this);
        return declaration;
    }

    // Statements
    @Override public ASTNode visitReturn(ReturnNode returnNode) {
        if (returnNode.expression != null)
            returnNode.expression.accept(this);
        return returnNode;
    }

    @Override public ASTNode visitCancel(CancelNode cancelNode) {
        return cancelNode;
    }

    @Override public ASTNode visitConditional(ConditionalNode conditional) {
        conditional.condition.accept(this);
        conditional.thenBlock.accept(this);
        if (conditional.elseBlock != null)
            conditional.elseBlock.accept(this);
        return conditional;
    }

    @Override public ASTNode visitDefinition(DefinitionNode definition) {
        return definition;
    }

    @Override public ASTNode visitDeclaration(DeclarationNode declaration) {
        declaration.variable.accept(this);
        return declaration;
    }

    @Override public ASTNode visitAssignation(AssignationNode assignation) {
        assignation.variable.accept(this);
        assignation.value.accept(this);
        return assignation;
    }

    @Override public ASTNode visitVoidFunctionCallStatement(VoidFunctionCallNode function) {
        function.arguments.accept(this);
        return function;
    }

    @Override public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode function) {
        function.arguments.accept(this);
        return function;
    }

    @Override public ASTNode visitRandom(RandomNode random) {
        for (StatementListNode list : random.statements) {
            list.accept(this);
        }
        return random;
    }

    @Override public ASTNode visitRepeat(RepeatNode repeat) {
        repeat.statements.accept(this);
        return repeat;
    }

    // Lists
    @Override public ASTNode visitMain(MainNode main) {
        for (MainStatementNode stmt : main.statements) {
            stmt.accept(this);
        }
        return main;
    }

    @Override public ASTNode visitStatements(StatementListNode statements) {
        for (StatementNode stmt : statements.statements) {
            stmt.accept(this);
        }
        return statements;
    }

    @Override public ASTNode visitParameters(ParameterListNode parameters) {
        for (VariableNode stmt : parameters.parameters) {
            stmt.accept(this);
        }
        return parameters;
    }

    @Override public ASTNode visitArguments(ArgumentListNode arguments) {
        for (ExpressionNode stmt : arguments.expressions) {
            stmt.accept(this);
        } return arguments;
    }

    @Override public ASTNode visitPrimitiveConstants(ConstantPrimitiveList constants) {
        return constants;
    }
}
