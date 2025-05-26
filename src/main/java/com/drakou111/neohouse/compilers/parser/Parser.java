package com.drakou111.neohouse.compilers.parser;

import com.drakou111.neohouse.compilers.ast.enums.*;
import com.drakou111.neohouse.compilers.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.UnaryExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.*;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.statement.AssignationNode;
import com.drakou111.neohouse.compilers.ast.statement.DeclarationNode;
import com.drakou111.neohouse.compilers.ast.statement.GlobalDeclarationNode;
import com.drakou111.neohouse.compilers.lexer.Token;
import com.drakou111.neohouse.compilers.lexer.TokenType;
import com.drakou111.neohouse.exceptions.ParseException;

import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) pos++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token previous() {
        return tokens.get(pos - 1);
    }

    private ParseException error(Token token, String message) {
        System.err.println("[line " + token.line + "] Error at '" + token.text + "': " + message);
        return new ParseException(message);
    }

    private ExpressionNode parseExpression() {
        return parsePrecedence(Precedence.NONE);
    }

    private ExpressionNode parsePrecedence(Precedence prec) {
        // Parse prefix
        Token prefixToken = peek();
        advance();
        ExpressionNode left = parsePrefix(prefixToken);

        // Parse infix as long as the next operator has higher precedence
        while (!isAtEnd() && prec.ordinal() < getCurrentPrecedence().ordinal()) {
            Token infixToken = peek();
            advance();
            left = parseInfix(left, infixToken);
        }

        return left;
    }

    private ExpressionNode parsePrefix(Token token) {
        switch (token.type) {
            case BANG:
            case MINUS:
                // Unary operators
                ExpressionNode right = parsePrecedence(Precedence.UNARY);
                return new UnaryExpressionNode(parseUnaryOperatorFromToken(token), right);
            case CONST_IDENT:
                return switch (token.text) {
                    case "PI" -> new ConstantPrimitiveNode(new DoubleValue(3.14159265358979));
                    case "E" -> new ConstantPrimitiveNode(new DoubleValue(2.71828182845904));
                    case "HALF_PI" -> new ConstantPrimitiveNode(new DoubleValue(1.57079632679489));
                    case "TWO_PI" -> new ConstantPrimitiveNode(new DoubleValue(6.28318530717958));
                    case "SQRT_TWO" -> new ConstantPrimitiveNode(new DoubleValue(1.41421356237309));
                    case "SQRT_THREE" -> new ConstantPrimitiveNode(new DoubleValue(1.73205080756887));
                    case "SQRT_FIVE" -> new ConstantPrimitiveNode(new DoubleValue(2.23606797749978));
                    case "PHI" -> new ConstantPrimitiveNode(new DoubleValue(1.61803398874989));
                    default -> throw error(token, "Unsupported constant: " + token.text);
                };
            case STRING_CONST:
                return new ConstantPrimitiveNode(new StringValue(token.text.substring(1, token.text.length() - 1)));
            case LONG_CONST:
                return new ConstantPrimitiveNode(new LongValue(Long.parseLong(token.text)));
            case DOUBLE_CONST:
                return new ConstantPrimitiveNode(new DoubleValue(Double.parseDouble(token.text.replace("d",""))));
            case TRUE:
                return new ConstantPrimitiveNode(new BoolValue(true));
            case FALSE:
                return new ConstantPrimitiveNode(new BoolValue(false));
            case LPAREN:
                ExpressionNode expr = parseExpression();
                consume(TokenType.RPAREN, "Expect ')' after expression.");
                return expr;
            case IDENT: {
                Holder holder = parseHolderFromToken(token);
                consume(TokenType.DOT, "Expected '.' after holder (p/g/t).");
                Token nameToken = consume(TokenType.IDENT, "Expected variable name after '.'.");
                return new VariableNode(null, holder, nameToken.text);
            }
            default:
                throw error(token, "Expected expression.");
        }
    }

    private ExpressionNode parseInfix(ExpressionNode left, Token token) {
        Precedence prec = getOperatorPrecedence(token.type);
        ExpressionNode right = parsePrecedence(prec);
        return new BinaryExpressionNode(left, right, parseBinaryOperatorFromToken(token));
    }

    private Precedence getCurrentPrecedence() {
        if (isAtEnd()) return Precedence.NONE;
        Precedence p = getOperatorPrecedence(peek().type);
        return p != null ? p : Precedence.NONE;
    }

    private Precedence getOperatorPrecedence(TokenType type) {
        return switch (type) {
            case EQ_EQ, NOT_EQ -> Precedence.EQUALITY;
            case LT, LE, GT, GE -> Precedence.COMPARISON;
            case PLUS, MINUS -> Precedence.TERM;
            case STAR, SLASH -> Precedence.FACTOR;
            default -> Precedence.NONE;
        };
    }


    public AssignationNode parseAssignment() {

        VariableNode variableNode = parseVariable();

        Token operatorToken = advance();
        AssignmentOperator operator = parseAssignmentOperatorFromToken(operatorToken);

        ExpressionNode expression = parseExpression();
        consume(TokenType.SEMI, "Expect ';' after declaration");

        return new AssignationNode(variableNode, operator, expression);
    }

    public DeclarationNode parseDeclaration() {
        Token type = advance();
        VariableType variableType = parseVariableFromToken(type);

        VariableNode variableNode = parseVariable();

        consume(TokenType.SEMI, "Expect ';' after declaration");
        return new DeclarationNode(new VariableNode(variableType, variableNode.holder, variableNode.name));
    }

    VariableNode parseVariable() {
        Token holder = consume(TokenType.IDENT, "Expect a holder identifier ('p', 'g' or 't')");
        Holder holderType = parseHolderFromToken(holder);
        consume(TokenType.DOT, "Expect '.' after scope prefix");
        Token name = consume(TokenType.IDENT, "Expect variable name identifier");
        return new VariableNode(null, holderType, name.text);
    }

    GlobalDeclarationNode parseGlobalDeclaration() {
        return new GlobalDeclarationNode(parseDeclaration().variable);
    }

    VariableType parseVariableFromToken(Token type) {
        return switch (type.type) {
            case TokenType.LONG -> VariableType.LONG;
            case TokenType.DOUBLE -> VariableType.DOUBLE;
            case TokenType.STRING -> VariableType.STRING;
            case TokenType.BOOL -> VariableType.BOOL;
            case TokenType.VAR -> VariableType.VAR;
            default -> throw error(type, "Invalid variable type. Should be 'long', 'double', 'string', 'bool', 'var'");
        };
    }

    Holder parseHolderFromToken(Token holder) {
        return switch (holder.text) {
            case "p" -> Holder.PLAYER;
            case "g" -> Holder.GLOBAL;
            case "t" -> Holder.TEAM;
            default -> throw error(holder, "Invalid holder type, expected 'p', 'g' or 't'");
        };
    }

    AssignmentOperator parseAssignmentOperatorFromToken(Token operator) {
        return switch (operator.type) {
            case EQ -> AssignmentOperator.ASSIGN;
            case PLUS_EQ -> AssignmentOperator.ADD_ASSIGN;
            case MINUS_EQ -> AssignmentOperator.SUB_ASSIGN;
            case STAR_EQ -> AssignmentOperator.MUL_ASSIGN;
            case SLASH_EQ -> AssignmentOperator.DIV_ASSIGN;
            default -> throw error(operator, "Invalid operator type, expected '=', '+=', '-=', '*=' or '/='");
        };
    }

    BinaryOperator parseBinaryOperatorFromToken(Token operator) {
        return switch (operator.type) {
            case PLUS -> BinaryOperator.ADD;
            case MINUS -> BinaryOperator.SUBTRACT;
            case STAR -> BinaryOperator.MULTIPLY;
            case SLASH -> BinaryOperator.DIVIDE;
            case EQ_EQ -> BinaryOperator.EQUALS;
            case NOT_EQ -> BinaryOperator.NOT_EQUALS;
            case LT -> BinaryOperator.LESS_THAN;
            case LE -> BinaryOperator.LESS_THAN_EQUALS;
            case GT -> BinaryOperator.GREATER_THAN;
            case GE -> BinaryOperator.GREATER_THAN_EQUALS;
            case AND -> BinaryOperator.AND;
            case OR -> BinaryOperator.OR;
            default -> throw error(operator, "Invalid binary operator, expected '+', '-', '*', '/', '==', '!=', '<' ,'<=', '>', '>=', '&&' or '||'");
        };
    }

    UnaryOperator parseUnaryOperatorFromToken(Token operator) {
        return switch (operator.type) {
            case MINUS -> UnaryOperator.NEGATE;
            case BANG -> UnaryOperator.NOT;
            case PLUS_PLUS -> UnaryOperator.INC;
            case MINUS_MINUS -> UnaryOperator.DEC;
            default -> throw error(operator, "Invalid unary operator, expected '-', '!', '++' or '--'");
        };
    }
}

