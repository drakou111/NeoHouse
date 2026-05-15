package com.drakou111.neohouse.compiler.parser;

import com.drakou111.neohouse.compiler.ast.expr.*;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.*;
import com.drakou111.neohouse.compiler.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.*;
import com.drakou111.neohouse.compiler.ast.main_statements.*;
import com.drakou111.neohouse.compiler.ast.statement.*;
import com.drakou111.neohouse.compiler.enums.*;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.lexer.TokenType;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.AddCondition;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.ReservedCommands;
import com.drakou111.neohouse.exceptions.ParsingException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        throw new ParsingException(peek(), message);
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
            } else if (check(TokenType.COMMAND)) {
                nodes.add(parseCommand());
            } else if (check(TokenType.MENU)) {
                nodes.add(parseMenu());
            } else if (isType(peek())) {
                nodes.add(parseGlobalDeclaration());
            } else {
                throw new ParsingException(peek(), "Unexpected token at top level: " + peek().text);
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
            throw new ParsingException(nameToken, "Invalid event name: '" + eventName + "'");
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
        Token functionNameToken;
        if (isType(peek())) {
            Token typeToken = advance();
            returnType = parseVariableFromToken(typeToken);
            functionNameToken = consume(TokenType.IDENT, "Expected function name");
        } else {
            // No return type: next must be function name
            functionNameToken = consume(TokenType.IDENT, "Expected function name");
        }
        functionName = functionNameToken.text;
        if (AddAction.searchByName(functionName) != null) {
            throw new ParsingException(functionNameToken,
                    "'" + functionName + "' is a built-in action name and cannot be used as a function name");
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

        StatementListNode bodyList = new StatementListNode(body);
        ParameterListNode parameterList = new ParameterListNode(parameters);

        if (returnType == null) {
            // → build a VoidFunctionNode
            return new VoidFunctionNode(functionName, "", parameterList, bodyList, functionToken);
        } else {
            // → build a TypedFunctionNode
            return new TypedFunctionNode(
                    functionName,
                    "",
                    returnType,
                    parameterList,
                    bodyList,
                    functionToken
            );
        }
    }

    public ReturnNode parseReturn() {
        Token returnToken = consume(TokenType.RETURN, "Expected 'return'");

        if (check(TokenType.SEMI)) {
            advance();
            return new VoidReturnNode(returnToken);
        }

        NormalExpressionNode value = parseExpression();
        consume(TokenType.SEMI, "Expected ';' after return expression");
        return new TypedReturnNode(value, returnToken);
    }

    public RepeatNode parseRepeat() {
        Token repeatToken = consume(TokenType.REPEAT, "Expected 'repeat' keyword");
        consume(TokenType.LPAREN, "Expected '(' after 'repeat'");

        // Only allow a long constant
        Token countToken = consume(TokenType.LONG_VALUE, "Expected long constant as repeat count");
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
        ExpressionNode condition = check(TokenType.RPAREN) ? null : parseCondition();
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
        List<Long> weights = new ArrayList<>();

        while (!check(TokenType.RBRACK) && !isAtEnd()) {
            long weight = 1;
            if (check(TokenType.LONG_VALUE) && checkTwo(TokenType.STAR)) {
                Token weightToken = advance();
                try {
                    weight = Long.parseLong(weightToken.text);
                } catch (NumberFormatException e) {
                    throw new ParsingException(weightToken, "Random weight is too large: " + weightToken.text);
                }
                if (weight <= 0)
                    throw new ParsingException(weightToken, "Random weight must be a positive integer, got " + weight);
                advance(); // consume '*'
            }

            consume(TokenType.LBRACE, "Expected '{' to begin a random block");
            List<StatementNode> block = new ArrayList<>();
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                block.add(parseStatement());
            }
            Token rbrace = consume(TokenType.RBRACE, "Expected '}' to close a random block");
            if (block.isEmpty())
                throw new ParsingException(rbrace, "Random block cannot be empty.");

            options.add(new StatementListNode(block));
            weights.add(weight);
        }

        consume(TokenType.RBRACK, "Expected ']' to close random options");

        // Reduce all weights by their GCD
        long divisor = weights.get(0);
        for (long w : weights) divisor = gcd(divisor, w);
        if (divisor > 1) {
            for (int i = 0; i < weights.size(); i++) weights.set(i, weights.get(i) / divisor);
        }

        return new RandomNode(options, weights, randomToken);
    }

    private static long gcd(long a, long b) {
        while (b != 0) { long t = b; b = a % b; a = t; }
        return a;
    }

    public LoopNode parseLoop() {
        Token loopToken = consume(TokenType.LOOP, "Expected 'loop' keyword");

        String name = consume(TokenType.IDENT, "Expected loop name").text;

        consume(TokenType.LPAREN, "Expected '('");

        long delay = 4;
        if (check(TokenType.LONG_VALUE)) {
            String longText = advance().text;
            delay = Long.parseLong(longText);
            if (delay < 4) {
                throw new ParsingException(peek(), "Loops must have a delay of at least 4 ticks (currently set to " + delay + " ticks).");
            }
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

    public CommandNode parseCommand() {
        Token cmdToken = consume(TokenType.COMMAND, "Expected 'command' keyword");
        Token nameToken = consume(TokenType.IDENT, "Expected command name");
        if (nameToken.text.length() < 3)
            throw new ParsingException(nameToken, "Command name must be at least 3 characters long (got '" + nameToken.text + "')");
        if (ReservedCommands.NAMES.contains(nameToken.text.toLowerCase()))
            throw new ParsingException(nameToken, "'" + nameToken.text + "' is a reserved server command and cannot be used as a command name");
        String name = nameToken.text.toLowerCase();

        boolean targeted = false;
        int priority = 0;
        boolean listed = true;
        if (check(TokenType.LPAREN)) {
            advance(); // consume '('
            while (!check(TokenType.RPAREN) && !isAtEnd()) {
                Token nToken = consume(TokenType.IDENT, "Expected parameter name ('priority', 'listed' or 'targeted')");
                consume(TokenType.EQ, "Expected '=' after parameter name '" + nToken.text + "'");
                switch (nToken.text) {
                    case "priority" -> {
                        Token priorityToken = consume(TokenType.LONG_VALUE, "Expected integer (0-20) for 'priority'");
                        priority = (int) Long.parseLong(priorityToken.text);
                        if (priority < 0 || priority > 20)
                            throw new ParsingException(priorityToken, "Priority must be between 0 and 20, got " + priority);
                    }
                    case "listed" -> {
                        if (match(TokenType.TRUE))       listed = true;
                        else if (match(TokenType.FALSE)) listed = false;
                        else throw new ParsingException(peek(), "Expected 'true' or 'false' for 'listed'");
                    }
                    case "targeted" -> {
                        if (match(TokenType.TRUE))       targeted = true;
                        else if (match(TokenType.FALSE)) targeted = false;
                        else throw new ParsingException(peek(), "Expected 'true' or 'false' for 'targeted'");
                    }
                    default -> throw new ParsingException(nToken, "Unknown command parameter '" + nToken.text + "'; expected 'priority', 'listed' or 'targeted'");
                }
                if (!check(TokenType.RPAREN)) consume(TokenType.COMMA, "Expected ',' between command parameters");
            }
            consume(TokenType.RPAREN, "Expected ')' after command options");
        }

        consume(TokenType.LBRACE, "Expected '{' to begin command body");
        List<StatementNode> body = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            body.add(parseStatement());
        }
        consume(TokenType.RBRACE, "Expected '}' after command body");

        return new CommandNode(name, targeted, priority, listed, new StatementListNode(body), cmdToken);
    }

    public MenuNode parseMenu() {
        Token menuToken = consume(TokenType.MENU, "Expected 'menu' keyword");
        Token nameToken = consume(TokenType.IDENT, "Expected menu name");
        String name = nameToken.text;

        consume(TokenType.LPAREN, "Expected '(' after menu name");
        Token rowsToken = consume(TokenType.LONG_VALUE, "Expected number of rows (1-6)");
        int rows = (int) Long.parseLong(rowsToken.text);
        if (rows < 1 || rows > 6)
            throw new ParsingException(rowsToken, "Menu rows must be between 1 and 6, got " + rows);
        consume(TokenType.RPAREN, "Expected ')' after row count");

        consume(TokenType.LBRACE, "Expected '{' to begin menu body");

        List<SlotNode> slots = new ArrayList<>();
        Set<Integer> usedSlots = new HashSet<>();
        int maxSlotId = rows * 9 - 1;

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            Token slotToken = consume(TokenType.IDENT, "Expected 'slot<N>' inside menu body (e.g. slot0, slot4)");
            if (!slotToken.text.matches("slot\\d+"))
                throw new ParsingException(slotToken, "Expected 'slot<N>' (e.g. slot0, slot5), got '" + slotToken.text + "'");

            int slotId = Integer.parseInt(slotToken.text.substring(4));
            if (slotId < 0 || slotId > maxSlotId)
                throw new ParsingException(slotToken, "Slot " + slotId + " out of range for a " + rows + "-row menu (0–" + maxSlotId + ")");
            if (!usedSlots.add(slotId))
                throw new ParsingException(slotToken, "Duplicate slot definition: slot" + slotId);

            consume(TokenType.COLON, "Expected ':' after slot identifier");
            consume(TokenType.LPAREN, "Expected '(' before item literal");
            String item = parseItemLiteral();
            consume(TokenType.RPAREN, "Expected ')' after item literal");

            consume(TokenType.LBRACE, "Expected '{' to begin slot body");
            List<StatementNode> body = new ArrayList<>();
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                body.add(parseStatement());
            }
            consume(TokenType.RBRACE, "Expected '}' after slot body");

            slots.add(new SlotNode(slotId, item, new StatementListNode(body), slotToken));
        }

        consume(TokenType.RBRACE, "Expected '}' after menu body");
        return new MenuNode(name, rows, slots, menuToken);
    }

    public StatementNode parseStatement() {

        if (check(TokenType.RANDOM)) return parseRandom();
        if (check(TokenType.REPEAT)) return parseRepeat();
        if (check(TokenType.RETURN)) return parseReturn();
        if (check(TokenType.IF)) return parseIf();
        if (isType(peek())) return parseDeclaration();

        if (check(TokenType.IDENT) && AddAction.searchByName(peek().text) != null && checkTwo(TokenType.LPAREN)) {
            return parseAction();
        }

        if (check(TokenType.IDENT) && checkTwo(TokenType.LPAREN)) {
            Token nameToken = peek();
            advance();
            advance();
            List<NormalExpressionNode> args = new ArrayList<>();
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

        throw new ParsingException(peek(), "Unknown statement");
    }

    private StatementNode parseAction() {
        Token nameToken = advance();
        String actionName = nameToken.text;
        AddAction sig = AddAction.requireByName(actionName);

        consume(TokenType.LPAREN, "Expected '(' after action name");
        List<NormalExpressionNode> parsedArgs = new ArrayList<>();

        for (int i = 0; i < sig.params.size(); i++) {
            Param param = sig.params.get(i);

            switch (param.kind()) {
                case STRING_LITERAL -> {
                    if (param.hasEnumConstraint()) {
                        Token t = consume(TokenType.STRING_VALUE, "Action " + actionName + " param #" + (i+1) + " must be a STRING literal");
                        String stripped = t.text.substring(1, t.text.length() - 1);
                        if (!param.isValidEnum(stripped)) throw new ParsingException(t,
                            "'" + stripped + "' is not a valid value for '" + actionName + "' param #" + (i+1) + ". Must be one of: " + String.join(", ", param.enumValues()));
                        parsedArgs.add(new ConstantPrimitiveNode(new StringValue(stripped), VariableType.STRING, nameToken));
                    } else {
                        parsedArgs.add(parseExpression());
                    }
                }
                case LONG_LITERAL -> {
                    Token t = consume(TokenType.LONG_VALUE, "Action " + actionName + " param #" + (i+1) + " must be a LONG literal");
                    long val = Long.parseLong(t.text);
                    if (!param.inRange(val)) throw new ParsingException(t,
                        "Value " + val + " is out of range for '" + actionName + "' param #" + (i+1) + ". Must be between " + param.rangeMin() + " and " + param.rangeMax() + ".");
                    parsedArgs.add(new ConstantPrimitiveNode(new LongValue(val), VariableType.STRING, nameToken));
                }
                case DOUBLE_LITERAL -> {
                    Token t = consume(TokenType.DOUBLE_VALUE, "Action " + actionName + " param #" + (i+1) + " must be a DOUBLE literal");
                    double val = Double.parseDouble(t.text);
                    if (!param.inRange(val)) throw new ParsingException(t,
                                                                        "Value " + val + " is out of range for '" + actionName + "' param #" + (i+1) + ". Must be between " + param.decimalMin() + " and " + param.decimalMax() + ".");
                    parsedArgs.add(new ConstantPrimitiveNode(new DoubleValue(Double.parseDouble(t.text)), VariableType.STRING, nameToken));
                }
                case BOOL_LITERAL -> {
                    Token t;
                    if (match(TokenType.TRUE)) t = previous();
                    else if (match(TokenType.FALSE)) t = previous();
                    else throw new ParsingException(peek(), "Action " + actionName + " param #" + (i+1) + " must be a BOOL literal (true or false).");

                    boolean v = t.type == TokenType.TRUE;
                    parsedArgs.add(new ConstantPrimitiveNode(new BoolValue(v), VariableType.BOOL, t));
                }
                case NORMAL_EXPRESSION -> parsedArgs.add(parseExpression());
                case ITEM_LITERAL -> {
                    String searchName = parseItemLiteral();
                    parsedArgs.add(new ConstantPrimitiveNode(new StringValue(searchName), VariableType.STRING, nameToken));
                }
            }

            if (i < sig.params.size() - 1) consume(TokenType.COMMA, "Expected ',' between action parameters");
        }

        consume(TokenType.RPAREN, "Expected ')' after action parameters");
        consume(TokenType.SEMI, "Expected ';' after action call");

        return new ActionCallNode(actionName, new ArgumentListNode(parsedArgs), nameToken);
    }

    private String parseItemLiteral() {
        Token open = consume(TokenType.LBRACE, "Expected '{' to start item data");

        StringBuilder snbt = new StringBuilder("{");
        int braceCount = 1;

        while (braceCount > 0 && !isAtEnd()) {
            Token t = advance();
            snbt.append(t.text);
            if (t.type == TokenType.LBRACE) braceCount++;
            else if (t.type == TokenType.RBRACE) braceCount--;
        }

        String result = snbt.toString();
        if (result.replaceAll("\\s+", "").equals("{}")) {
            throw new ParsingException(open, "A menu slot item cannot be empty — provide at least an id field.");
        }
        return result;
    }

    private NormalExpressionNode parseExpression() {
        return parsePrecedence(Precedence.NONE);
    }

    private NormalExpressionNode parsePrecedence(Precedence prec) {
        // 1) prefix
        Token prefixToken = peek();
        advance();
        NormalExpressionNode left = parsePrefix(prefixToken);

        // 2) postfix (++/--)
        while (!isAtEnd() && getPostfixPrecedence().ordinal() > prec.ordinal()) {
            Token postfixToken = peek();
            if (postfixToken.type == TokenType.PLUS_PLUS || postfixToken.type == TokenType.MINUS_MINUS) {
                advance();
                left = new UnaryExpressionNode(parseUnaryOperatorFromToken(postfixToken), left, null, previous());
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

    private NormalExpressionNode parsePrefix(Token token) {
        switch (token.type) {
            case NOT:
                return new UnaryExpressionNode(parseUnaryOperatorFromToken(token), parsePrecedence(Precedence.UNARY), null, token);
            case MINUS:
                if (check(TokenType.LONG_VALUE)) {
                    Token numTok = advance();
                    return new ConstantPrimitiveNode(new LongValue(-Long.parseLong(numTok.text)), VariableType.LONG, token);
                }
                if (check(TokenType.DOUBLE_VALUE)) {
                    Token numTok = advance();
                    return new ConstantPrimitiveNode(new DoubleValue(-Double.parseDouble(numTok.text.replace("d", ""))), VariableType.DOUBLE, token);
                }
                return new UnaryExpressionNode(parseUnaryOperatorFromToken(token), parsePrecedence(Precedence.UNARY), null, token);
            case LONG_CAST:
            case DOUBLE_CAST:
            case STRING_CAST:
                return new UnaryExpressionNode(parseUnaryOperatorFromToken(token), parsePrecedence(Precedence.UNARY), null, token);
            case CONST_IDENT:
                return switch (token.text) {
                    case "PI" -> new ConstantPrimitiveNode(new DoubleValue(3.14159265358979), VariableType.DOUBLE, token);
                    case "E" -> new ConstantPrimitiveNode(new DoubleValue(2.71828182845904), VariableType.DOUBLE, token);
                    case "HALF_PI" -> new ConstantPrimitiveNode(new DoubleValue(1.57079632679489), VariableType.DOUBLE, token);
                    case "TWO_PI" -> new ConstantPrimitiveNode(new DoubleValue(6.28318530717958), VariableType.DOUBLE, token);
                    case "SQRT_TWO" -> new ConstantPrimitiveNode(new DoubleValue(1.41421356237309), VariableType.DOUBLE, token);
                    case "SQRT_THREE" -> new ConstantPrimitiveNode(new DoubleValue(1.73205080756887), VariableType.DOUBLE, token);
                    case "SQRT_FIVE" -> new ConstantPrimitiveNode(new DoubleValue(2.23606797749978), VariableType.DOUBLE, token);
                    case "PHI" -> new ConstantPrimitiveNode(new DoubleValue(1.61803398874989), VariableType.DOUBLE, token);
                    default -> throw new ParsingException(token, "Unsupported constant: " + token.text);
                };
            case STRING_VALUE:
                return new ConstantPrimitiveNode(new StringValue(token.text.substring(1, token.text.length() - 1)), VariableType.STRING, token);
            case LONG_VALUE:
                return new ConstantPrimitiveNode(new LongValue(Long.parseLong(token.text)), VariableType.LONG, token);
            case DOUBLE_VALUE:
                return new ConstantPrimitiveNode(new DoubleValue(Double.parseDouble(token.text.replace("d",""))), VariableType.DOUBLE, token);
            case TRUE:
                return new ConstantPrimitiveNode(new BoolValue(true), VariableType.BOOL, token);
            case FALSE:
                return new ConstantPrimitiveNode(new BoolValue(false), VariableType.BOOL, token);
            case LPAREN:
                NormalExpressionNode expr = parseExpression();
                consume(TokenType.RPAREN, "Expect ')' after expression.");
                return expr;
            case IDENT: {
                if (check(TokenType.LPAREN)) {
                    advance();
                    List<NormalExpressionNode> args = new ArrayList<>();
                    if (!check(TokenType.RPAREN)) {
                        do {
                            args.add(parseExpression());
                        } while (match(TokenType.COMMA));
                    }
                    consume(TokenType.RPAREN, "Expect ')' after arguments");
                    return new FunctionCallNode(token.text, new ArgumentListNode(args), null, token);
                }

                if (token.text.equals("ph")) return parsePlaceholder();

                stepBack();
                return parseVariable();
            }
            default:
                throw new ParsingException(token, "Expected an expression.");
        }
    }

    private NormalExpressionNode parseInfix(NormalExpressionNode left, Token token) {
        Precedence prec = getOperatorPrecedence(token.type);
        NormalExpressionNode right = parsePrecedence(prec);
        BinaryOperator binOp = parseBinaryOperatorFromToken(token);
        return new BinaryExpressionNode(left, right, binOp, null, token);
    }

    private ExpressionNode parseCondition() {
        Token startToken = peek();
        List<ExpressionNode> clauses = new ArrayList<>();

        // first clause
        clauses.add(parseConditionClause());

        LogicalOperator logicOp = null;
        while (true) {
            if (match(TokenType.AND)) {
                if (logicOp == null) logicOp = LogicalOperator.AND;
                else if (logicOp != LogicalOperator.AND)
                    throw new ParsingException(previous(), "Cannot mix '||' with '&&' in one condition");
            }
            else if (match(TokenType.OR)) {
                if (logicOp == null) logicOp = LogicalOperator.OR;
                else if (logicOp != LogicalOperator.OR)
                    throw new ParsingException(previous(), "Cannot mix '&&' with '||' in one condition");
            }
            else {
                break;
            }

            clauses.add(parseConditionClause());
        }

        return new LogicalExpressionNode(clauses, logicOp == null ? LogicalOperator.AND : logicOp, startToken);
    }

    private ExpressionNode parseConditionClause() {
        // or a condition‐built‐in
        if (check(TokenType.IDENT) && AddCondition.searchByName(peek().text) != null && checkTwo(TokenType.LPAREN)) {
            return parseConditionCall();
        }

        // attempt a normal “a < b” style clause
        NormalExpressionNode left = parseNormalExpression();
        if (isNextBooleanOperator()) {
            Token opToken = advance();
            BooleanOperator op = parseBooleanOperatorFromToken(opToken);
            NormalExpressionNode right = parseNormalExpression();
            return new BooleanExpressionNode(left, right, op, opToken);
        }

        // Otherwise just keep the expression as-is. will be handled in a visitor
        return left;
    }

    private boolean isNextBooleanOperator() {
        TokenType type = peek().type;
        return switch (type) {
            case EQ_EQ, LT, GT, LE, GE -> true;
            default -> false;
        };
    }

    private ConditionExpressionNode parseConditionCall() {
        Token nameToken = advance();
        String condName = nameToken.text;
        AddCondition sig = AddCondition.searchByName(condName);
        if (sig == null) {
            throw new ParsingException(nameToken, "Unknown condition: " + condName);
        }

        consume(TokenType.LPAREN, "Expected '(' after condition name");
        List<NormalExpressionNode> parsedArgs = new ArrayList<>();

        for (int i = 0; i < sig.params.size(); i++) {
            Param param = sig.params.get(i);

            switch (param.kind()) {
                case STRING_LITERAL -> {
                    if (param.hasEnumConstraint()) {
                        Token t = consume(TokenType.STRING_VALUE, "Condition '" + condName + "' was given a " + peek().type + " at parameter #" + (i + 1) + ", but must be a STRING");
                        String stripped = t.text.substring(1, t.text.length() - 1);
                        if (!param.isValidEnum(stripped)) throw new ParsingException(t,
                            "'" + stripped + "' is not a valid value for '" + condName + "' param #" + (i+1) + ". Must be one of: " + String.join(", ", param.enumValues()));
                        parsedArgs.add(new ConstantPrimitiveNode(new StringValue(stripped), VariableType.STRING, t));
                    } else {
                        parsedArgs.add(parseExpression());
                    }
                }
                case LONG_LITERAL -> {
                    Token t = consume(TokenType.LONG_VALUE, "Condition '" + condName + "' was given a " + peek().type + " at parameter #" + (i + 1) + ", but must be a LONG");
                    long val = Long.parseLong(t.text);
                    if (!param.inRange(val)) throw new ParsingException(t,
                        "Value " + val + " is out of range for '" + condName + "' param #" + (i+1) + ". Must be between " + param.rangeMin() + " and " + param.rangeMax() + ".");
                    parsedArgs.add(new ConstantPrimitiveNode(new LongValue(val), VariableType.LONG, t));
                }
                case DOUBLE_LITERAL -> {
                    Token t = consume(TokenType.DOUBLE_VALUE, "Condition '" + condName + "' was given a " + peek().type + " at parameter #" + (i + 1) + ", but must be a DOUBLE");
                    parsedArgs.add(new ConstantPrimitiveNode(new DoubleValue(Double.parseDouble(t.text)), VariableType.DOUBLE, t));
                }
                case BOOL_LITERAL -> {
                    Token t;
                    if (match(TokenType.TRUE)) t = previous();
                    else if (match(TokenType.FALSE)) t = previous();
                    else throw new ParsingException(peek(), "Condition '" + condName + "' was given a " + peek().type + " at parameter #" + (i + 1) + ", but must be a BOOL");

                    boolean v = t.type == TokenType.TRUE;
                    parsedArgs.add(new ConstantPrimitiveNode(new BoolValue(v), VariableType.BOOL, t));
                }
                case NORMAL_EXPRESSION -> parsedArgs.add(parseExpression());
                case ITEM_LITERAL -> {
                    String searchName = parseItemLiteral();
                    parsedArgs.add(new ConstantPrimitiveNode(new StringValue(searchName), VariableType.STRING, nameToken));
                }
            }

            if (i < sig.params.size() - 1) {
                consume(TokenType.COMMA, "Expected ',' between condition parameters");
            }
        }

        consume(TokenType.RPAREN, "Expected ')' after condition parameters");
        return new ConditionCallNode(condName, new ArgumentListNode(parsedArgs), nameToken);
    }

    private NormalExpressionNode parseNormalExpression() {
        return parsePrecedence(Precedence.NONE);
    }

    private Precedence getCurrentPrecedence() {
        if (isAtEnd()) return Precedence.NONE;
        Precedence p = getOperatorPrecedence(peek().type);
        return p != null ? p : Precedence.NONE;
    }

    private Precedence getOperatorPrecedence(TokenType type) {
        return switch (type) {
            case LPAREN -> Precedence.CALL;
            case PLUS, MINUS -> Precedence.TERM;
            case STAR, SLASH -> Precedence.FACTOR;
            case BAR, AMPERSAND, CARET, LEFT_SHIFT, RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> Precedence.BITWISE;
            default -> Precedence.NONE;
        };
    }

    public AssignationNode parseAssignment() {
        VariableNode variableNode = parseVariable();
        Token operatorToken = advance();

        // x++; and x--; as shorthand for x += 1 and x -= 1
        if (operatorToken.type == TokenType.PLUS_PLUS || operatorToken.type == TokenType.MINUS_MINUS) {
            consume(TokenType.SEMI, "Expect ';' after ++/--");
            AssignmentOperator op = operatorToken.type == TokenType.PLUS_PLUS
                    ? AssignmentOperator.ADD_ASSIGN : AssignmentOperator.SUB_ASSIGN;
            NormalExpressionNode one = new ConstantPrimitiveNode(new LongValue(1L), VariableType.LONG, operatorToken);
            return new AssignationNode(variableNode, op, one, operatorToken);
        }

        AssignmentOperator operator = parseAssignmentOperatorFromToken(operatorToken);
        NormalExpressionNode expression = parseExpression();
        consume(TokenType.SEMI, "Expect ';' after assignation");
        return new AssignationNode(variableNode, operator, expression, operatorToken);
    }

    public DeclarationNode parseDeclaration() {
        VariableNode var = parseTypedVariable();
        consume(TokenType.SEMI, "Expect ';' after declaration");
        return new DeclarationNode(var, var.token);
    }

    VariableNode parseVariable() {
        Token firstTok = consume(TokenType.IDENT, "Expect a variable");

        // Bare name (no holder prefix): default to PLAYER holder.
        // A holder prefix is only recognised when it's exactly p/g/t followed by its separator.
        boolean isHolderPrefix = switch (firstTok.text) {
            case "p", "g" -> check(TokenType.DOT);
            case "t" -> check(TokenType.DOT) || check(TokenType.LBRACK);
            default -> false;
        };

        if (!isHolderPrefix) {
            return new VariableNode(null, Holder.PLAYER, firstTok.text, null, firstTok);
        }

        Holder holder = parseHolderFromToken(firstTok);

        String teamName = null;
        if (holder == Holder.TEAM && check(TokenType.LBRACK)) {
            advance();
            if (check(TokenType.STRING_VALUE)) {
                Token strTok = advance();
                teamName = strTok.text.substring(1, strTok.text.length() - 1);
            } else {
                Token nameTok = consume(TokenType.IDENT, "Expected team name inside '[ ]'");
                teamName = nameTok.text;
            }
            consume(TokenType.RBRACK, "Expected ']' after team name");
        }

        consume(TokenType.DOT, "Expect '.' after scope prefix");
        Token varTok = consume(TokenType.IDENT, "Expect variable name identifier");

        return new VariableNode(null, holder, varTok.text, teamName, varTok);
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
        // Accept keywords (e.g. 'random') that appear after 'ph.' as identifier names.
        Token placeholderName = advance();
        if (!placeholderName.text.matches("[a-zA-Z_][a-zA-Z0-9_]*"))
            throw new ParsingException(placeholderName, "Expected placeholder name after 'ph.'");
        consume(TokenType.LPAREN, "Expected '(' after placeholder name");

        List<ConstantPrimitiveNode> args = new ArrayList<>();

        if (!check(TokenType.RPAREN)) {
            do {
                ConstantPrimitive constArg = parseConstantPrimitive();
                args.add(new ConstantPrimitiveNode(constArg, constArg.getType(), previous()));
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RPAREN, "Expected ')' after placeholder parameters");
        return new PlaceholderNode(placeholderName.text, new ConstantPrimitiveList(args), null, placeholderName);
    }

    GlobalDeclarationNode parseGlobalDeclaration() {
        VariableNode var = parseTypedVariable();
        NormalExpressionNode initializer = null;
        if (match(TokenType.EQ)) {
            if (var.holder == Holder.PLAYER) {
                throw new ParsingException(previous(), "Player variables (p.) cannot have initializers; only global (g.) and team (t[...]) variables can.");
            }
            initializer = parseExpression();
        }
        consume(TokenType.SEMI, "Expect ';' after declaration");
        return new GlobalDeclarationNode(var, initializer, var.token);
    }

    private ConstantPrimitive parseConstantPrimitive() {
        Token token = advance();
        return switch (token.type) {
            case STRING_VALUE -> new StringValue(token.text.substring(1, token.text.length() - 1));
            case LONG_VALUE -> new LongValue(Long.parseLong(token.text));
            case DOUBLE_VALUE -> new DoubleValue(Double.parseDouble(token.text.replace("d", "").replace("D", "")));
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
                default -> throw new ParsingException(token, "Unsupported constant: " + token.text);
            };
            default -> throw new ParsingException(token, "Expected a constant value. Did you mean \"" + token.text + "\"?");
        };
    }


    VariableType parseVariableFromToken(Token type) {
        return switch (type.type) {
            case TokenType.LONG -> VariableType.LONG;
            case TokenType.DOUBLE -> VariableType.DOUBLE;
            case TokenType.STRING -> VariableType.STRING;
            case TokenType.BOOL -> VariableType.BOOL;
            case TokenType.VAR -> VariableType.VAR;
            default -> throw new ParsingException(type, "Invalid variable type. Should be 'long', 'double', 'string', 'bool', 'var'");
        };
    }

    Holder parseHolderFromToken(Token holder) {
        return switch (holder.text) {
            case "p" -> Holder.PLAYER;
            case "g" -> Holder.GLOBAL;
            case "t" -> Holder.TEAM;
            default -> throw new ParsingException(holder, "Invalid holder type, expected 'p', 'g' or 't'");
        };
    }

    AssignmentOperator parseAssignmentOperatorFromToken(Token operator) {
        return switch (operator.type) {
            case EQ -> AssignmentOperator.ASSIGN;
            case PLUS_EQ -> AssignmentOperator.ADD_ASSIGN;
            case MINUS_EQ -> AssignmentOperator.SUB_ASSIGN;
            case STAR_EQ -> AssignmentOperator.MUL_ASSIGN;
            case SLASH_EQ -> AssignmentOperator.DIV_ASSIGN;
            case BAR_EQ -> AssignmentOperator.BITWISE_OR_ASSIGN;
            case AMP_EQ -> AssignmentOperator.BITWISE_AND_ASSIGN;
            case CARET_EQ -> AssignmentOperator.BITWISE_XOR_ASSIGN;
            case LEFT_SHIFT_EQ -> AssignmentOperator.LEFT_SHIFT_ASSIGN;
            case RIGHT_SHIFT_EQ -> AssignmentOperator.RIGHT_SHIFT_ASSIGN;
            case UNSIGNED_RIGHT_SHIFT_EQ -> AssignmentOperator.UNSIGNED_RIGHT_SHIFT_ASSIGN;
            default -> throw new ParsingException(operator, "Invalid operator type, expected '=', '+=', '-=', '*=', '/=', '|=', '&=', '^=', '<<=', '>>=' or '>>>='");
        };
    }

    BinaryOperator parseBinaryOperatorFromToken(Token operator) {
        return switch (operator.type) {
            case PLUS -> BinaryOperator.ADD;
            case MINUS -> BinaryOperator.SUBTRACT;
            case STAR -> BinaryOperator.MULTIPLY;
            case SLASH -> BinaryOperator.DIVIDE;
            case BAR -> BinaryOperator.BITWISE_OR;
            case AMPERSAND -> BinaryOperator.BITWISE_AND;
            case CARET -> BinaryOperator.BITWISE_XOR;
            case LEFT_SHIFT -> BinaryOperator.LEFT_SHIFT;
            case RIGHT_SHIFT -> BinaryOperator.RIGHT_SHIFT;
            case UNSIGNED_RIGHT_SHIFT -> BinaryOperator.UNSIGNED_RIGHT_SHIFT;
            default -> throw new ParsingException(operator, "Invalid binary operator, expected '+', '-', '*', '/', '|', '&', '^', '<<', '>>' or '>>>'");
        };
    }

    LogicalOperator parseLogicalOperatorFromToken(Token operator) {
        return switch (operator.type) {
            case AND -> LogicalOperator.AND;
            case OR -> LogicalOperator.OR;
            default -> throw new ParsingException(operator, "Invalid binary operator, expected '&&' or '||'");
        };
    }

    BooleanOperator parseBooleanOperatorFromToken(Token operator) {
        return switch (operator.type) {
            case EQ_EQ -> BooleanOperator.EQUALS;
            case LT -> BooleanOperator.LESS_THAN;
            case LE -> BooleanOperator.LESS_THAN_EQUALS;
            case GT -> BooleanOperator.GREATER_THAN;
            case GE -> BooleanOperator.GREATER_THAN_EQUALS;
            default -> throw new ParsingException(operator, "Invalid boolean operator, expected '==', '<' ,'<=', '>' or '>='. Note that '!=' is not included.");
        };
    }

    UnaryOperator parseUnaryOperatorFromToken(Token operator) {
        return switch (operator.type) {
            case MINUS -> UnaryOperator.NEGATE;
            case NOT -> UnaryOperator.NOT;
            case PLUS_PLUS -> UnaryOperator.INC;
            case MINUS_MINUS -> UnaryOperator.DEC;
            case LONG_CAST -> UnaryOperator.LONG_CAST;
            case DOUBLE_CAST -> UnaryOperator.DOUBLE_CAST;
            case STRING_CAST -> UnaryOperator.STRING_CAST;
            default -> throw new ParsingException(operator, "Invalid unary operator, expected '-', '!', '++', '--', '(long)', '(double)' or '(string)'");
        };
    }
}

