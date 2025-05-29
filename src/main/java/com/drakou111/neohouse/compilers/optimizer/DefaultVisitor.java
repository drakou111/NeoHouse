package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.UnaryExpressionNode;
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

public abstract class DefaultVisitor<T> implements ASTVisitor<T> {
    // Expressions (Constants, Variables, Placeholder, Sub expressions, Function calls)
    @Override public T visitConstant(ConstantNode constant) {
        return null;
    }

    @Override public T visitConstantPrimitive(ConstantPrimitiveNode constant)  {
        return null;
    }

    @Override public T visitConstantArray(ConstantArrayNode constant) {
        return null;
    }

    @Override public T visitUnaryExpr(UnaryExpressionNode unaryExpression) {
        unaryExpression.operand.accept(this); return null;
    }

    @Override public T visitBinaryExpr(BinaryExpressionNode binaryExpression) {
        binaryExpression.left.accept(this);
        binaryExpression.right.accept(this);
        return null;
    }

    @Override public T visitVariable(VariableNode variable) {
        return null;
    }

    @Override public T visitPlaceholder(PlaceholderNode placeholder) {
        placeholder.constants.accept(this);
        return null;
    }

    @Override public T visitFunctionCall(FunctionCallNode functionCall) {
        functionCall.arguments.accept(this);
        return null;
    }

    // Main Statements
    @Override public T visitFunction(FunctionNode function) {
        function.parameters.accept(this);
        function.statements.accept(this);
        return null;
    }

    @Override public T visitLoop(LoopNode loop) {
        loop.statements.accept(this);
        return null;
    }

    @Override public T visitEvent(EventNode event) {
        event.statements.accept(this);
        return null;
    }

    @Override public T visitGlobalDeclaration(GlobalDeclarationNode declaration) {
        declaration.variable.accept(this);
        return null;
    }

    // Statements
    @Override public T visitReturn(ReturnNode returnNode) {
        returnNode.expression.accept(this);
        return null;
    }

    @Override public T visitCancel(CancelNode cancelNode) {
        return null;
    }

    @Override public T visitConditional(ConditionalNode conditional) {
        conditional.condition.accept(this);
        conditional.thenBlock.accept(this);
        conditional.elseBlock.accept(this);
        return null;
    }

    @Override public T visitDefinition(DefinitionNode definition) {
        return null;
    }

    @Override public T visitDeclaration(DeclarationNode declaration) {
        declaration.variable.accept(this);
        return null;
    }

    @Override public T visitAssignation(AssignationNode assignation) {
        assignation.variable.accept(this);
        assignation.value.accept(this);
        return null;
    }

    @Override public T visitVoidFunctionCallStatement(VoidFunctionCallNode function) {
        function.arguments.accept(this);
        return null;
    }

    @Override public T visitGlobalFunctionCall(GlobalFunctionCallNode function) {
        function.arguments.accept(this);
        return null;
    }

    @Override public T visitRandom(RandomNode random) {
        for (StatementListNode list : random.statements) {
            list.accept(this);
        }
        return null;
    }

    @Override public T visitRepeat(RepeatNode repeat) {
        repeat.statements.accept(this);
        return null;
    }

    // Lists
    @Override public T visitMain(MainNode main) {
        for (MainStatementNode stmt : main.statements) {
            stmt.accept(this);
        }
        return null;
    }

    @Override public T visitStatements(StatementListNode statements) {
        for (StatementNode stmt : statements.statements) {
            stmt.accept(this);
        }
        return null;
    }

    @Override public T visitParameters(ParameterListNode parameters) {
        for (VariableNode stmt : parameters.parameters) {
            stmt.accept(this);
        }
        return null;
    }

    @Override public T visitArguments(ArgumentListNode arguments) {
        for (ExpressionNode stmt : arguments.expressions) {
            stmt.accept(this);
        } return null;
    }

    @Override public T visitPrimitiveConstants(ConstantPrimitiveList constants) {
        return null;
    }
}
