package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
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

import java.util.ArrayList;
import java.util.List;

public abstract class DefaultVisitor extends EmptyVisitor<ASTNode> {

    public MainNode analyze(MainNode main) {
        return (MainNode) main.accept(this);
    }

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
        NormalExpressionNode operand = (NormalExpressionNode) unaryExpression.operand.accept(this);
        return new UnaryExpressionNode(unaryExpression.operator, operand, unaryExpression.type, unaryExpression.token);
    }

    @Override public ASTNode visitBinaryExpr(BinaryExpressionNode binaryExpression) {
        NormalExpressionNode left = (NormalExpressionNode)binaryExpression.left.accept(this);
        NormalExpressionNode right = (NormalExpressionNode)binaryExpression.right.accept(this);
        return new BinaryExpressionNode(left, right, binaryExpression.operator, binaryExpression.type, binaryExpression.token);
    }

    @Override public ASTNode visitBooleanExpr(BooleanExpressionNode booleanExpression) {
        NormalExpressionNode left = (NormalExpressionNode)booleanExpression.left.accept(this);
        NormalExpressionNode right = (NormalExpressionNode)booleanExpression.right.accept(this);
        return new BooleanExpressionNode(left, right, booleanExpression.operator, booleanExpression.token);
    }

    @Override public ASTNode visitLogicalExpr(LogicalExpressionNode logicalExpression) {
        List<ExpressionNode> expressions = new ArrayList<>();
        for (ExpressionNode expr : logicalExpression.conditions) {
            expressions.add((ExpressionNode)expr.accept(this));
        }
        return new LogicalExpressionNode(expressions, logicalExpression.operator, logicalExpression.token);
    }

    @Override public ASTNode visitVariable(VariableNode variable) {
        return variable;
    }

    @Override public ASTNode visitPlaceholder(PlaceholderNode placeholder) {
        ConstantPrimitiveList constants = (ConstantPrimitiveList)placeholder.constants.accept(this);
        return new PlaceholderNode(placeholder.placeholder, constants, placeholder.type, placeholder.token);
    }

    @Override public ASTNode visitFunctionCall(FunctionCallNode functionCall) {
        ArgumentListNode args = (ArgumentListNode)functionCall.arguments.accept(this);
        return new FunctionCallNode(functionCall.functionName, args, functionCall.type, functionCall.token);
    }

    @Override public ASTNode visitTypedFunction(TypedFunctionNode function) {
        ParameterListNode params = (ParameterListNode)function.parameters.accept(this);
        StatementListNode stmts = (StatementListNode)function.statements.accept(this);
        return new TypedFunctionNode(function.name, function.description, function.returnType, params, stmts, function.token);
    }

    @Override public ASTNode visitVoidFunction(VoidFunctionNode function) {
        ParameterListNode params = (ParameterListNode)function.parameters.accept(this);
        StatementListNode stmts = (StatementListNode)function.statements.accept(this);
        return new VoidFunctionNode(function.name, function.description, params, stmts, function.token);
    }

    @Override public ASTNode visitLoop(LoopNode loop) {
        StatementListNode stmts = (StatementListNode)loop.statements.accept(this);
        return new LoopNode(loop.name, loop.description, stmts, loop.tickDelay, loop.token);
    }

    @Override public ASTNode visitEvent(EventNode event) {
        StatementListNode stmts = (StatementListNode)event.statements.accept(this);
        return new EventNode(event.event, stmts, event.token);
    }

    @Override public ASTNode visitCommand(CommandNode command) {
        StatementListNode stmts = (StatementListNode)command.statements.accept(this);
        return new CommandNode(command.name, command.targeted, command.priority, command.listed, stmts, command.token);
    }

    @Override public ASTNode visitMenu(MenuNode menu) {
        List<SlotNode> slots = new ArrayList<>();
        for (SlotNode slot : menu.slots) {
            slots.add((SlotNode) slot.accept(this));
        }
        return new MenuNode(menu.name, menu.rows, slots, menu.token);
    }

    @Override public ASTNode visitSlot(SlotNode slot) {
        StatementListNode stmts = (StatementListNode) slot.statements.accept(this);
        return new SlotNode(slot.slotId, slot.item, stmts, slot.token);
    }

    @Override public ASTNode visitGlobalDeclaration(GlobalDeclarationNode declaration) {
        VariableNode var = (VariableNode)declaration.variable.accept(this);
        NormalExpressionNode init = declaration.initializer != null
                ? (NormalExpressionNode) declaration.initializer.accept(this)
                : null;
        return new GlobalDeclarationNode(var, init, declaration.token);
    }

    @Override public ASTNode visitTypedReturn(TypedReturnNode returnNode) {
        NormalExpressionNode expr = (NormalExpressionNode)returnNode.expression.accept(this);
        return new TypedReturnNode(expr, returnNode.token);
    }

    @Override public ASTNode visitVoidReturn(VoidReturnNode returnNode) {
        return returnNode;
    }

    @Override public ASTNode visitCancel(CancelNode cancelNode) {
        return cancelNode;
    }

    @Override public ASTNode visitConditional(ConditionalNode conditional) {
        ExpressionNode cond = null;
        if (conditional.condition != null)
            cond = (ExpressionNode)conditional.condition.accept(this);
        StatementListNode thenBlock = (StatementListNode)conditional.thenBlock.accept(this);
        StatementListNode elseBlock = null;
        if (conditional.elseBlock != null)
            elseBlock = (StatementListNode)conditional.elseBlock.accept(this);
        return new ConditionalNode(cond, thenBlock, elseBlock, conditional.token);
    }

    @Override public ASTNode visitDefinition(DefinitionNode definition) {
        return definition;
    }

    @Override public ASTNode visitDeclaration(DeclarationNode declaration) {
        VariableNode var = (VariableNode)declaration.variable.accept(this);
        return new DeclarationNode(var, declaration.token);
    }

    @Override public ASTNode visitAssignation(AssignationNode assignation) {
        VariableNode var = (VariableNode)assignation.variable.accept(this);
        NormalExpressionNode expr = (NormalExpressionNode)assignation.value.accept(this);
        return new AssignationNode(var, assignation.operator, expr, assignation.token);
    }

    @Override public ASTNode visitVoidFunctionCall(VoidFunctionCallNode function) {
        ArgumentListNode args = (ArgumentListNode)function.arguments.accept(this);
        return new VoidFunctionCallNode(function.functionName, args, function.token);
    }

    @Override public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode function) {
        ArgumentListNode args = (ArgumentListNode)function.arguments.accept(this);
        return new GlobalFunctionCallNode(function.functionName, args, function.token);
    }

    @Override public ASTNode visitRandom(RandomNode random) {
        List<StatementListNode> stmts = new ArrayList<>();
        for (StatementListNode list : random.statements) {
            stmts.add((StatementListNode)list.accept(this));
        }
        return new RandomNode(stmts, random.weights, random.token);
    }

    @Override public ASTNode visitRepeat(RepeatNode repeat) {
        StatementListNode stmts = (StatementListNode)repeat.statements.accept(this);
        return new RepeatNode(repeat.count, stmts, repeat.token);
    }

    @Override public ASTNode visitConditionCall(ConditionCallNode condCall) {
        ArgumentListNode argList = (ArgumentListNode)condCall.arguments.accept(this);
        return new ConditionCallNode(condCall.name, argList, condCall.token);
    }

    @Override public ASTNode visitActionCall(ActionCallNode actionCall) {
        ArgumentListNode argList = (ArgumentListNode)actionCall.arguments.accept(this);
        return new ActionCallNode(actionCall.name, argList, actionCall.token);
    }

    @Override public ASTNode visitMain(MainNode main) {
        List<MainStatementNode> stmts = new ArrayList<>();
        for (MainStatementNode stmt : main.statements) {
            stmts.add((MainStatementNode)stmt.accept(this));
        }
        return new MainNode(stmts);
    }

    @Override public ASTNode visitStatements(StatementListNode statements) {
        List<StatementNode> stmts = new ArrayList<>();
        for (StatementNode stmt : statements.statements) {
            stmts.add((StatementNode)stmt.accept(this));
        }
        return new StatementListNode(stmts);
    }

    @Override public ASTNode visitParameters(ParameterListNode parameters) {
        List<VariableNode> stmts = new ArrayList<>();
        for (VariableNode stmt : parameters.parameters) {
            stmts.add((VariableNode)stmt.accept(this));
        }
        return new ParameterListNode(stmts);
    }

    @Override public ASTNode visitArguments(ArgumentListNode arguments) {
        List<NormalExpressionNode> stmts = new ArrayList<>();
        for (NormalExpressionNode stmt : arguments.expressions) {
            stmts.add((NormalExpressionNode)stmt.accept(this));
        }
        return new ArgumentListNode(stmts);
    }

    @Override public ASTNode visitPrimitiveConstants(ConstantPrimitiveList constants) {
        return constants;
    }
}
