package com.drakou111.neohouse.compilers.parser;

import com.drakou111.neohouse.compilers.ast.enums.*;
import com.drakou111.neohouse.compilers.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.UnaryExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.*;
import com.drakou111.neohouse.compilers.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.main.*;
import com.drakou111.neohouse.compilers.ast.main_statements.EventNode;
import com.drakou111.neohouse.compilers.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compilers.ast.main_statements.LoopNode;
import com.drakou111.neohouse.compilers.ast.statement.*;
import com.drakou111.neohouse.compilers.ast.main_statements.GlobalDeclarationNode;
import com.drakou111.neohouse.compilers.lexer.Token;
import com.drakou111.neohouse.compilers.lexer.TokenType;
import com.drakou111.neohouse.exceptions.ParsingException;

import java.util.ArrayList;
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

    private boolean checkTwo(TokenType type) {
        Token next = tokens.get(pos + 1);
        return next.type != TokenType.EOF && next.type == type;
    }

    private Token advance() {
        if (!isAtEnd()) pos++;
        return previous();
    }

    private Token stepBack() {
        if (pos > 0) pos--;
        return peek();
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

    private ParsingException error(Token token, String message) {
        System.err.println("[line " + token.line + "] Error at '" + token.text + "': " + message);
        return new ParsingException(message);
    }


    private boolean isType(Token token) {
        return switch (token.type) {
            case LONG, DOUBLE, STRING, BOOL, VAR -> true;
            default -> false;
        };
    }

    public MainNode parseMain() {
        List<MainStatementNode> nodes = new ArrayList<>();

        while (!isAtEnd()) {
            if (check(TokenType.FUNCTION)) {
                nodes.add(parseFunction());
            } else if (check(TokenType.LOOP)) {
                nodes.add(parseLoop());
            } else if (check(TokenType.EVENT)) {
                nodes.add(parseEvent());
            } else if (isType(peek())) {
                nodes.add(parseGlobalDeclaration());
            } else {
                throw error(peek(), "Unexpected token at top level: " + peek().text);
            }
        }

        return new MainNode(nodes);
    }

    public EventNode parseEvent() {
        Token eventToken = consume(TokenType.EVENT, "Expected 'event' keyword");

        Token nameToken = consume(TokenType.IDENT, "Expected event name");
        String eventName = nameToken.text;

        Event event = Event.fromName(eventName);
        if (event == null) {
            throw error(nameToken, "Invalid event name: '" + eventName + "'");
        }

        consume(TokenType.LBRACE, "Expected '{' to begin event body");

        List<StatementNode> body = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            body.add(parseStatement());
        }

        consume(TokenType.RBRACE, "Expected '}' after event body");

        return new EventNode(event, new StatementListNode(body), eventToken);
    }

    public FunctionNode parseFunction() {
        Token functionToken = consume(TokenType.FUNCTION, "Expected 'function' keyword");

        VariableType returnType = null;
        String functionName;

        // Peek ahead: if it's a type, it's a typed function
        if (isType(peek())) {
            Token typeToken = advance();
            returnType = parseVariableFromToken(typeToken);
            functionName = consume(TokenType.IDENT, "Expected function name").text;
        } else {
            // No return type: next must be function name
            functionName = consume(TokenType.IDENT, "Expected function name").text;
        }

        consume(TokenType.LPAREN, "Expected '(' after function name");

        List<VariableNode> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                parameters.add(parseTypedVariable());
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RPAREN, "Expected ')' after parameters");

        consume(TokenType.LBRACE, "Expected '{' to begin function body");

        List<StatementNode> body = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            body.add(parseStatement());
        }

        consume(TokenType.RBRACE, "Expected '}' after function body");

        return new FunctionNode(functionName, "", returnType, new ParameterListNode(parameters), new StatementListNode(body), functionToken);
    }

    public CancelNode parseCancel() {
        Token cancelToken = consume(TokenType.CANCEL, "Expected 'cancel' keyword");
        consume(TokenType.SEMI, "Expected ';' after cancel statement");
        return new CancelNode(cancelToken);
    }

    public ReturnNode parseReturn() {
        Token returnToken = consume(TokenType.RETURN, "Expected 'return'");

        // Case 1: return;
        if (check(TokenType.SEMI)) {
            advance(); // consume ;
            return new ReturnNode(null, returnToken);
        }

        // Case 2: return EXPRESSION;
        ExpressionNode value = parseExpression();
        consume(TokenType.SEMI, "Expected ';' after return expression");
        return new ReturnNode(value, returnToken);
    }

    public RepeatNode parseRepeat() {
        Token repeatToken = consume(TokenType.REPEAT, "Expected 'repeat' keyword");
        consume(TokenType.LPAREN, "Expected '(' after 'repeat'");

        // Only allow a long constant
        Token countToken = consume(TokenType.LONG_CONST, "Expected long constant as repeat count");
        long count = Long.parseLong(countToken.text);

        consume(TokenType.RPAREN, "Expected ')' after repeat count");
        consume(TokenType.LBRACE, "Expected '{' to begin repeat block");

        List<StatementNode> body = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            body.add(parseStatement());
        }

        consume(TokenType.RBRACE, "Expected '}' after repeat block");

        return new RepeatNode(count, new StatementListNode(body), repeatToken);
    }

    public ConditionalNode parseIf() {
        Token token = consume(TokenType.IF, "Expected 'if' keyword");
        consume(TokenType.LPAREN, "Expected '(' after 'if'");
        ExpressionNode condition = parseExpression();
        consume(TokenType.RPAREN, "Expected ')' after if condition");

        consume(TokenType.LBRACE, "Expected '{' to begin 'if' block");
        List<StatementNode> thenBranch = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            thenBranch.add(parseStatement());
        }
        consume(TokenType.RBRACE, "Expected '}' after 'if' block");

        List<StatementNode> elseBranch = null;
        if (match(TokenType.ELSE)) {
            consume(TokenType.LBRACE, "Expected '{' to begin 'else' block");
            elseBranch = new ArrayList<>();
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                elseBranch.add(parseStatement());
            }
            consume(TokenType.RBRACE, "Expected '}' after 'else' block");
        }

        return new ConditionalNode(condition, new StatementListNode(thenBranch), new StatementListNode(elseBranch), token);
    }



    public RandomNode parseRandom() {
        Token randomToken = consume(TokenType.RANDOM, "Expected 'random' keyword");
        consume(TokenType.LBRACK, "Expected '[' after 'random'");

        List<StatementListNode> options = new ArrayList<>();

        while (!check(TokenType.RBRACK) && !isAtEnd()) {
            consume(TokenType.LBRACE, "Expected '{' to begin a random block");

            List<StatementNode> block = new ArrayList<>();
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                block.add(parseStatement());
            }

            consume(TokenType.RBRACE, "Expected '}' to close a random block");
            options.add(new StatementListNode(block));
        }

        consume(TokenType.RBRACK, "Expected ']' to close random options");

        return new RandomNode(options, randomToken);
    }

    public LoopNode parseLoop() {
        Token loopToken = consume(TokenType.LOOP, "Expected 'loop' keyword");

        String name = consume(TokenType.IDENT, "Expected loop name").text;

        consume(TokenType.LPAREN, "Expected '('");

        long delay = 4;
        if (check(TokenType.LONG_CONST)) {
            String longText = advance().text;
            delay = Long.parseLong(longText);
        }

        consume(TokenType.RPAREN, "Expected ')'");

        consume(TokenType.LBRACE, "Expected '{' to begin loop body");

        List<StatementNode> body = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            body.add(parseStatement());
        }

        consume(TokenType.RBRACE, "Expected '}' after loop body");

        return new LoopNode(name, "", new StatementListNode(body), delay, loopToken);
    }

    public StatementNode parseStatement() {

        if (check(TokenType.RANDOM)) return parseRandom();
        if (check(TokenType.REPEAT)) return parseRepeat();
        if (check(TokenType.CANCEL)) return parseCancel();
        if (check(TokenType.RETURN)) return parseReturn();
        if (check(TokenType.IF)) return parseIf();

        if (isType(peek())) return parseDeclaration();

        if (match(TokenType.GLOBAL)) {
            Token nameToken = consume(TokenType.IDENT, "Expect function name after 'global'");
            consume(TokenType.LPAREN, "Expect '(' after function name");
            List<ExpressionNode> args = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    args.add(parseExpression());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RPAREN, "Expect ')' after arguments");
            consume(TokenType.SEMI, "Expect ';' after call");
            return new GlobalFunctionCallNode(nameToken.text, new ArgumentListNode(args), nameToken);
        }

        if (check(TokenType.IDENT) && checkTwo(TokenType.LPAREN)) {
            Token nameToken = peek();
            advance();
            advance();
            List<ExpressionNode> args = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    args.add(parseExpression());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RPAREN, "Expect ')' after arguments");
            consume(TokenType.SEMI, "Expect ';' after call");
            return new VoidFunctionCallNode(nameToken.text, new ArgumentListNode(args), nameToken);
        }

        if (check(TokenType.IDENT)) return parseAssignment();

        throw error(peek(), "Unknown statement");
    }

    private ExpressionNode parseExpression() {
        return parsePrecedence(Precedence.NONE);
    }

    private ExpressionNode parsePrecedence(Precedence prec) {
        // 1) prefix
        Token prefixToken = peek();
        advance();
        ExpressionNode left = parsePrefix(prefixToken);

        // 2) postfix (++/--)
        while (!isAtEnd() && getPostfixPrecedence().ordinal() > prec.ordinal()) {
            Token postfixToken = peek();
            if (postfixToken.type == TokenType.PLUS_PLUS || postfixToken.type == TokenType.MINUS_MINUS) {
                advance();
                left = new UnaryExpressionNode(parseUnaryOperatorFromToken(postfixToken), left, previous());
            } else {
                break;
            }
        }

        // 3) infix (+, -, *, /, ==, etc.)
        while (!isAtEnd() && prec.ordinal() < getCurrentPrecedence().ordinal()) {
            Token infixToken = peek();
            advance();
            left = parseInfix(left, infixToken);
        }

        return left;
    }

    private Precedence getPostfixPrecedence() {
        if (isAtEnd()) return Precedence.NONE;
        return switch (peek().type) {
            case PLUS_PLUS, MINUS_MINUS -> Precedence.POSTFIX;
            default -> Precedence.NONE;
        };
    }

    private ExpressionNode parsePrefix(Token token) {
        switch (token.type) {
            case BANG:
            case MINUS:
                // Unary operators
                ExpressionNode right = parsePrecedence(Precedence.UNARY);
                return new UnaryExpressionNode(parseUnaryOperatorFromToken(token), right, token);
            case CONST_IDENT:
                return switch (token.text) {
                    case "PI" -> new ConstantPrimitiveNode(new DoubleValue(3.14159265358979), token);
                    case "E" -> new ConstantPrimitiveNode(new DoubleValue(2.71828182845904), token);
                    case "HALF_PI" -> new ConstantPrimitiveNode(new DoubleValue(1.57079632679489), token);
                    case "TWO_PI" -> new ConstantPrimitiveNode(new DoubleValue(6.28318530717958), token);
                    case "SQRT_TWO" -> new ConstantPrimitiveNode(new DoubleValue(1.41421356237309), token);
                    case "SQRT_THREE" -> new ConstantPrimitiveNode(new DoubleValue(1.73205080756887), token);
                    case "SQRT_FIVE" -> new ConstantPrimitiveNode(new DoubleValue(2.23606797749978), token);
                    case "PHI" -> new ConstantPrimitiveNode(new DoubleValue(1.61803398874989), token);
                    default -> throw error(token, "Unsupported constant: " + token.text);
                };
            case STRING_CONST:
                return new ConstantPrimitiveNode(new StringValue(token.text.substring(1, token.text.length() - 1)), token);
            case LONG_CONST:
                return new ConstantPrimitiveNode(new LongValue(Long.parseLong(token.text)), token);
            case DOUBLE_CONST:
                return new ConstantPrimitiveNode(new DoubleValue(Double.parseDouble(token.text.replace("d",""))), token);
            case TRUE:
                return new ConstantPrimitiveNode(new BoolValue(true), token);
            case FALSE:
                return new ConstantPrimitiveNode(new BoolValue(false), token);
            case LPAREN:
                ExpressionNode expr = parseExpression();
                consume(TokenType.RPAREN, "Expect ')' after expression.");
                return expr;
            case IDENT: {
                if (check(TokenType.LPAREN)) {
                    advance();
                    List<ExpressionNode> args = new ArrayList<>();
                    if (!check(TokenType.RPAREN)) {
                        do {
                            args.add(parseExpression());
                        } while (match(TokenType.COMMA));
                    }
                    consume(TokenType.RPAREN, "Expect ')' after arguments");
                    return new FunctionCallNode(token.text, new ArgumentListNode(args), token);
                }

                if (token.text.equals("ph")) return parsePlaceholder();

                stepBack();
                return parseVariable();
            }
            default:
                throw error(token, "Expected expression.");
        }
    }

    private ExpressionNode parseInfix(ExpressionNode left, Token token) {
        Precedence prec = getOperatorPrecedence(token.type);
        ExpressionNode right = parsePrecedence(prec);
        return new BinaryExpressionNode(left, right, parseBinaryOperatorFromToken(token), token);
    }

    private Precedence getCurrentPrecedence() {
        if (isAtEnd()) return Precedence.NONE;
        Precedence p = getOperatorPrecedence(peek().type);
        return p != null ? p : Precedence.NONE;
    }

    private Precedence getOperatorPrecedence(TokenType type) {
        return switch (type) {
            case LPAREN -> Precedence.CALL;
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
        consume(TokenType.SEMI, "Expect ';' after assignation");

        return new AssignationNode(variableNode, operator, expression, operatorToken);
    }

    public DeclarationNode parseDeclaration() {
        VariableNode var = parseTypedVariable();
        consume(TokenType.SEMI, "Expect ';' after declaration");
        return new DeclarationNode(var, var.token);
    }

    VariableNode parseVariable() {
        Token holder = consume(TokenType.IDENT, "Expect a holder identifier ('p', 'g' or 't')");
        Holder holderType = parseHolderFromToken(holder);
        consume(TokenType.DOT, "Expect '.' after scope prefix");
        Token name = consume(TokenType.IDENT, "Expect variable name identifier");
        return new VariableNode(null, holderType, name.text, name);
    }

    VariableNode parseTypedVariable() {
        Token type = advance();
        VariableType variableType = parseVariableFromToken(type);
        VariableNode variableNode = parseVariable();
        variableNode.type = variableType;
        return variableNode;
    }

    PlaceholderNode parsePlaceholder() {
        consume(TokenType.DOT, "Expected '.' after 'ph'");
        Token placeholderName = consume(TokenType.IDENT, "Expected placeholder name after 'ph.'");
        consume(TokenType.LPAREN, "Expected '(' after placeholder name");

        List<ConstantPrimitive> args = new ArrayList<>();

        if (!check(TokenType.RPAREN)) {
            do {
                ConstantPrimitive constArg = parseConstantPrimitive();
                args.add(constArg);
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RPAREN, "Expected ')' after placeholder parameters");
        return new PlaceholderNode(placeholderName.text, new ConstantPrimitiveList(args), placeholderName);
    }

    GlobalDeclarationNode parseGlobalDeclaration() {
        DeclarationNode declarationNode = parseDeclaration();
        return new GlobalDeclarationNode(declarationNode.variable, declarationNode.token);
    }

    private ConstantPrimitive parseConstantPrimitive() {
        Token token = advance();
        return switch (token.type) {
            case STRING_CONST -> new StringValue(token.text.substring(1, token.text.length() - 1));
            case LONG_CONST -> new LongValue(Long.parseLong(token.text));
            case DOUBLE_CONST -> new DoubleValue(Double.parseDouble(token.text.replace("d", "").replace("D", "")));
            case TRUE -> new BoolValue(true);
            case FALSE -> new BoolValue(false);
            case CONST_IDENT -> switch (token.text) {
                case "PI" -> new DoubleValue(3.14159265358979);
                case "E" -> new DoubleValue(2.71828182845904);
                case "HALF_PI" -> new DoubleValue(1.57079632679489);
                case "TWO_PI" -> new DoubleValue(6.28318530717958);
                case "SQRT_TWO" -> new DoubleValue(1.41421356237309);
                case "SQRT_THREE" -> new DoubleValue(1.73205080756887);
                case "SQRT_FIVE" -> new DoubleValue(2.23606797749978);
                case "PHI" -> new DoubleValue(1.61803398874989);
                default -> throw error(token, "Unsupported constant: " + token.text);
            };
            default -> throw error(token, "Expected a constant value.");
        };
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

