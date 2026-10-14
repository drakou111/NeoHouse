package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.BooleanExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.ConditionCallNode;
import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.LogicalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.UnaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantArrayNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.ConstantPrimitiveList;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.ParameterListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.CommandNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compiler.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compiler.ast.main_statements.MenuNode;
import com.drakou111.neohouse.compiler.ast.main_statements.SlotNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TupleFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.ActionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.CancelNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compiler.ast.statement.DeclarationNode;
import com.drakou111.neohouse.compiler.ast.statement.DefinitionNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.RandomNode;
import com.drakou111.neohouse.compiler.ast.statement.RepeatNode;
import com.drakou111.neohouse.compiler.ast.statement.TypedReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.TupleDestructureNode;
import com.drakou111.neohouse.compiler.ast.statement.TupleReturnNode;

public abstract class EmptyVisitor<T> implements ASTVisitor<T> {

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
        unaryExpression.operand.accept(this);
        return null;
    }

    @Override public T visitBinaryExpr(BinaryExpressionNode binaryExpression) {
        binaryExpression.left.accept(this);
        binaryExpression.right.accept(this);
        return null;
    }

    @Override public T visitBooleanExpr(BooleanExpressionNode booleanExpression) {
        booleanExpression.left.accept(this);
        booleanExpression.right.accept(this);
        return null;
    }

    @Override public T visitLogicalExpr(LogicalExpressionNode logicalExpression) {
        for (ExpressionNode expr : logicalExpression.conditions) {
            expr.accept(this);
        }
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

    @Override public T visitTypedFunction(TypedFunctionNode function) {
        function.parameters.accept(this);
        function.statements.accept(this);
        return null;
    }

    @Override public T visitTupleFunction(TupleFunctionNode function) {
        function.parameters.accept(this);
        function.statements.accept(this);
        return null;
    }

    @Override public T visitVoidFunction(VoidFunctionNode function) {
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

    @Override public T visitCommand(CommandNode command) {
        command.statements.accept(this);
        return null;
    }

    @Override public T visitMenu(MenuNode menu) {
        for (SlotNode slot : menu.slots) {
            slot.accept(this);
        }
        return null;
    }

    @Override public T visitSlot(SlotNode slot) {
        slot.statements.accept(this);
        return null;
    }

    @Override public T visitGlobalDeclaration(GlobalDeclarationNode declaration) {
        declaration.variable.accept(this);
        if (declaration.initializer != null) declaration.initializer.accept(this);
        return null;
    }

    @Override public T visitTypedReturn(TypedReturnNode returnNode) {
        returnNode.expression.accept(this);
        return null;
    }

    @Override public T visitTupleReturn(TupleReturnNode returnNode) {
        for (var expr : returnNode.expressions) expr.accept(this);
        return null;
    }

    @Override public T visitVoidReturn(VoidReturnNode returnNode) {
        return null;
    }

    @Override public T visitCancel(CancelNode cancelNode) {
        return null;
    }

    @Override public T visitConditional(ConditionalNode conditional) {
        if (conditional.condition != null)
            conditional.condition.accept(this);
        conditional.thenBlock.accept(this);
        if (conditional.elseBlock != null)
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

    @Override public T visitVoidFunctionCall(VoidFunctionCallNode function) {
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

    @Override public T visitConditionCall(ConditionCallNode condCall) {
      condCall.arguments.accept(this);
      return null;
    }

    @Override public T visitActionCall(ActionCallNode actionCall) {
      actionCall.arguments.accept(this);
      return null;
    }

    @Override public T visitTupleDestructure(TupleDestructureNode node) {
        node.arguments.accept(this);
        return null;
    }

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
