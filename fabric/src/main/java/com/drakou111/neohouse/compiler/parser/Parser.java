package com.drakou111.neohouse.compiler.parser;

import com.drakou111.neohouse.compiler.ImportGraph;
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
import com.drakou111.neohouse.constants.Teleport;
import com.drakou111.neohouse.exceptions.ParsingException;

import java.util.*;

public class Parser {
    private final List<Token> tokens;
    private final ImportGraph imports;
    private int pos = 0;
    private final HashMap<String, ConstantPrimitiveNode> constTable = new HashMap<>();
    /** enum name -> its members in declaration order (member index = ordinal value). */
    private final Map<String, List<String>> enumDefs = new HashMap<>();
    /** enum name -> the token naming it in its declaration (used for import visibility). */
    private final Map<String, Token> enumDeclTokens = new HashMap<>();
    /** full variable name (holder + "." + name) -> enum type name, for enum-typed variables. */
    private final Map<String, String> varEnumType = new HashMap<>();
    /** function name -> per-parameter enum type name (null entry = non-enum parameter). */
    private final Map<String, List<String>> functionParamEnums = new HashMap<>();
    /** function name -> its enum return type name (absent/null = non-enum return). */
    private final Map<String, String> functionReturnEnums = new HashMap<>();

    public Map<String, List<String>> getFunctionParamEnums() { return functionParamEnums; }
    public Map<String, String> getFunctionReturnEnums() { return functionReturnEnums; }
    public Map<String, List<String>> getEnumDefs() { return enumDefs; }

    // Built-in math constants. Single source of truth for the values; the names are
    // also listed in the CONST_IDENT lexer pattern.
    private static final Map<String, Double> MATH_CONSTANTS = Map.of(
            "PI",         3.14159265358979,
            "E",          2.71828182845904,
            "HALF_PI",    1.57079632679489,
            "TWO_PI",     6.28318530717958,
            "SQRT_TWO",   1.41421356237309,
            "SQRT_THREE", 1.73205080756887,
            "SQRT_FIVE",  2.23606797749978,
            "PHI",        1.61803398874989
    );

    /** Optional leading named parameters (`icon: {..}`, `description: "..."`) shared by functions and loops. */
    private record IconDescription(String icon, String description) {}

    public Parser(List<Token> tokens) {
        this(tokens, ImportGraph.singleFile());
    }

    public Parser(List<Token> tokens, ImportGraph imports) {
        this.tokens = tokens;
        this.imports = imports;
    }

    /** Rejects a reference to a constant or enum declared in a file the referencing file doesn't import. */
    private void requireVisible(Token use, Token declaration, String what) {
        if (imports.canSee(use.file, declaration.file)) return;
        throw new ParsingException(use, "%s is declared in '%s', which '%s' does not import".formatted(what, declaration.file, use.file));
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
        if (pos + 1 >= tokens.size()) return false;
        return tokens.get(pos + 1).type == type;
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
            } else if (check(TokenType.ENUM)) {
                parseEnumDeclaration();
            } else if (check(TokenType.CONST)) {
                parseConstDeclaration();
            } else if (isType(peek()) || isEnumType(peek())) {
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
        List<VariableType> tupleReturnTypes = null;
        String returnEnumName = null;
        String functionName;

        Token functionNameToken;
        if (check(TokenType.LPAREN)) {
            // Tuple return type: function (T, T, ...) name
            advance(); // consume '('
            tupleReturnTypes = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    Token typeToken = advance();
                    tupleReturnTypes.add(parseVariableFromToken(typeToken));
                } while (match(TokenType.COMMA));
            }
            if (tupleReturnTypes.size() < 2)
                throw new ParsingException(functionToken, "Tuple return type must have at least 2 types");
            consume(TokenType.RPAREN, "Expected ')' after tuple return types");
            functionNameToken = consume(TokenType.IDENT, "Expected function name");
        } else if (isType(peek()) || isEnumType(peek())) {
            // Single return type: function T name  (T may be an enum name)
            Token typeToken = advance();
            if (isEnumType(typeToken)) {
                returnType = VariableType.LONG;
                returnEnumName = typeToken.text;
            } else {
                returnType = parseVariableFromToken(typeToken);
            }
            functionNameToken = consume(TokenType.IDENT, "Expected function name");
        } else {
            // No return type: void function
            functionNameToken = consume(TokenType.IDENT, "Expected function name");
        }
        functionName = functionNameToken.text;
        if (AddAction.searchByName(functionName) != null) {
            throw new ParsingException(functionNameToken,
                    "'" + functionName + "' is a built-in action name and cannot be used as a function name");
        }

        consume(TokenType.LPAREN, "Expected '(' after function name");

        IconDescription meta = parseIconDescription();
        String description = meta.description();
        String icon = meta.icon();

        List<VariableNode> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                parameters.add(parseTypedVariable());
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RPAREN, "Expected ')' after parameters");

        // Record the enum signature so call sites can be checked later (functions may be forward-referenced).
        List<String> paramEnumNames = new ArrayList<>();
        for (VariableNode param : parameters) paramEnumNames.add(param.enumName);
        functionParamEnums.put(functionName, paramEnumNames);
        functionReturnEnums.put(functionName, returnEnumName);

        consume(TokenType.LBRACE, "Expected '{' to begin function body");

        // Enum-typed params are only in scope inside this body; register them, then restore afterwards.
        Map<String, String> savedEnumTypes = new HashMap<>();
        for (VariableNode param : parameters) {
            String fullname = param.holder + "." + param.name;
            savedEnumTypes.put(fullname, varEnumType.get(fullname));
            if (param.enumName != null) varEnumType.put(fullname, param.enumName);
            else varEnumType.remove(fullname);
        }

        List<StatementNode> body = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            body.add(parseStatement());
        }

        for (Map.Entry<String, String> e : savedEnumTypes.entrySet()) {
            if (e.getValue() == null) varEnumType.remove(e.getKey());
            else varEnumType.put(e.getKey(), e.getValue());
        }

        consume(TokenType.RBRACE, "Expected '}' after function body");

        StatementListNode bodyList = new StatementListNode(body);
        ParameterListNode parameterList = new ParameterListNode(parameters);

        if (tupleReturnTypes != null) {
            return new TupleFunctionNode(functionName, description, icon, tupleReturnTypes, parameterList, bodyList, functionToken);
        } else if (returnType == null) {
            return new VoidFunctionNode(functionName, description, icon, parameterList, bodyList, functionToken);
        } else {
            return new TypedFunctionNode(functionName, description, icon, returnType, parameterList, bodyList, functionToken);
        }
    }

    /** Parses optional leading `icon: {..}` / `description: "..."` named params inside a `(...)` header. */
    private IconDescription parseIconDescription() {
        String description = "";
        String icon = null;
        while (check(TokenType.IDENT) && checkTwo(TokenType.COLON)
                && (peek().text.equals("icon") || peek().text.equals("description"))) {
            Token nToken = advance();
            consume(TokenType.COLON, "Expected ':' after '" + nToken.text + "'");
            if (nToken.text.equals("icon")) {
                if (icon != null) throw new ParsingException(nToken, "Duplicate 'icon' parameter");
                icon = parseItemLiteral();
            } else {
                if (!description.isEmpty()) throw new ParsingException(nToken, "Duplicate 'description' parameter");
                Token descToken = consume(TokenType.STRING_VALUE, "Expected string literal for 'description'");
                description = descToken.text.substring(1, descToken.text.length() - 1);
            }
            if (!check(TokenType.RPAREN)) consume(TokenType.COMMA, "Expected ',' after named parameter");
        }
        return new IconDescription(icon, description);
    }

    /**
     * Parses a single action/condition argument according to its declared {@link Param} kind.
     * {@code ctx} is a label like {@code "Action 'sendMessage'"} used in error messages.
     */
    /**
     * If the next token is a {@code const} identifier whose type matches {@code expected},
     * consumes it and returns its folded value node; otherwise returns {@code null} without consuming.
     * Lets constants be used wherever a same-typed literal is required.
     */
    private ConstantPrimitiveNode tryConstArg(VariableType expected) {
        if (check(TokenType.IDENT)) {
            ConstantPrimitiveNode c = constTable.get(peek().text);
            if (c != null && c.type == expected) {
                requireVisible(peek(), c.token, "Constant '" + peek().text + "'");
                Token t = advance();
                return new ConstantPrimitiveNode(c.value, c.type, t);
            }
        }
        return null;
    }

    private NormalExpressionNode parseLiteralArg(Param param, int paramNumber, String ctx) {
        return switch (param.kind()) {
            case STRING_LITERAL -> {
                if (param.hasEnumConstraint()) {
                    Token t = consume(TokenType.STRING_VALUE, ctx + " param #" + paramNumber + " must be a STRING literal");
                    String stripped = t.text.substring(1, t.text.length() - 1);
                    if (!param.isValidEnum(stripped)) throw new ParsingException(t,
                            "'" + stripped + "' is not a valid value for " + ctx + " param #" + paramNumber + ".");
                    yield new ConstantPrimitiveNode(new StringValue(stripped), VariableType.STRING, t);
                }
                yield parseExpression();
            }
            case LONG_LITERAL -> {
                ConstantPrimitiveNode c = tryConstArg(VariableType.LONG);
                Token t = c != null ? c.token : consume(TokenType.LONG_VALUE, ctx + " param #" + paramNumber + " must be a LONG literal");
                long val = c != null ? ((LongValue) c.value).value : NumberLiterals.parseLong(t);
                if (!param.inRange(val)) throw new ParsingException(t,
                        "Value " + val + " is out of range for " + ctx + " param #" + paramNumber + ". Must be between " + param.rangeMin() + " and " + param.rangeMax() + ".");
                yield new ConstantPrimitiveNode(new LongValue(val), VariableType.LONG, t);
            }
            case DOUBLE_LITERAL -> {
                ConstantPrimitiveNode c = tryConstArg(VariableType.DOUBLE);
                Token t = c != null ? c.token : consume(TokenType.DOUBLE_VALUE, ctx + " param #" + paramNumber + " must be a DOUBLE literal");
                double val = c != null ? ((DoubleValue) c.value).value : NumberLiterals.parseDouble(t);
                if (!param.inRange(val)) throw new ParsingException(t,
                        "Value " + val + " is out of range for " + ctx + " param #" + paramNumber + ". Must be between " + param.decimalMin() + " and " + param.decimalMax() + ".");
                yield new ConstantPrimitiveNode(new DoubleValue(val), VariableType.DOUBLE, t);
            }
            case BOOL_LITERAL -> {
                ConstantPrimitiveNode c = tryConstArg(VariableType.BOOL);
                if (c != null) yield new ConstantPrimitiveNode(c.value, VariableType.BOOL, c.token);
                Token t;
                if (match(TokenType.TRUE) || match(TokenType.FALSE)) t = previous();
                else throw new ParsingException(peek(), ctx + " param #" + paramNumber + " must be a BOOL literal (true or false).");
                yield new ConstantPrimitiveNode(new BoolValue(t.type == TokenType.TRUE), VariableType.BOOL, t);
            }
            case ITEM_LITERAL -> {
                Token itemStart = peek();
                String searchName = parseItemLiteral();
                yield new ConstantPrimitiveNode(new StringValue(searchName), VariableType.STRING, itemStart);
            }
            case NORMAL_EXPRESSION -> parseExpression();
            case LOCATION -> {
                // Bare preset identifier (HOUSE_SPAWN / INVOKER / CURRENT), or any string expression for custom coords.
                if (check(TokenType.IDENT)) {
                    Token idTok = peek();
                    Teleport preset = null;
                    try { preset = Teleport.valueOf(idTok.text); } catch (IllegalArgumentException ignored) {}
                    if (preset != null && preset != Teleport.CUSTOM) {
                        advance();
                        yield new ConstantPrimitiveNode(new StringValue(idTok.text), VariableType.STRING, idTok);
                    }
                }
                yield parseExpression();
            }
        };
    }

    public ReturnNode parseReturn() {
        Token returnToken = consume(TokenType.RETURN, "Expected 'return'");

        if (check(TokenType.SEMI)) {
            advance();
            return new VoidReturnNode(returnToken);
        }

        // Tuple return: return (e0, e1, ...);
        if (check(TokenType.LPAREN)) {
            advance(); // consume '('
            List<NormalExpressionNode> exprs = new ArrayList<>();
            exprs.add(parseExpression());
            if (check(TokenType.COMMA)) {
                while (match(TokenType.COMMA)) {
                    exprs.add(parseExpression());
                }
                consume(TokenType.RPAREN, "Expected ')' after tuple return expressions");
                consume(TokenType.SEMI, "Expected ';' after return");
                return new TupleReturnNode(exprs, returnToken);
            }
            // Single expression in parens - treat as normal return
            consume(TokenType.RPAREN, "Expected ')'");
            consume(TokenType.SEMI, "Expected ';' after return expression");
            return new TypedReturnNode(exprs.get(0), returnToken);
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
        long count = NumberLiterals.parseLong(countToken);

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
                weight = NumberLiterals.parseLong(weightToken);
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

        Token rbrack = consume(TokenType.RBRACK, "Expected ']' to close random options");

        if (weights.isEmpty())
            throw new ParsingException(rbrack, "Random must have at least one block.");

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

        IconDescription meta = parseIconDescription();
        String description = meta.description();
        String icon = meta.icon();

        long delay = 4;
        if (check(TokenType.LONG_VALUE)) {
            Token delayToken = advance();
            delay = NumberLiterals.parseLong(delayToken);
            if (delay < 4) {
                throw new ParsingException(delayToken, "Loops must have a delay of at least 4 ticks (currently set to " + delay + " ticks).");
            }
        }

        consume(TokenType.RPAREN, "Expected ')'");

        consume(TokenType.LBRACE, "Expected '{' to begin loop body");

        List<StatementNode> body = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            body.add(parseStatement());
        }

        consume(TokenType.RBRACE, "Expected '}' after loop body");

        return new LoopNode(name, description, icon, new StatementListNode(body), delay, loopToken);
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
                consume(TokenType.COLON, "Expected ':' after parameter name '" + nToken.text + "'");
                switch (nToken.text) {
                    case "priority" -> {
                        Token priorityToken = consume(TokenType.LONG_VALUE, "Expected integer (0-20) for 'priority'");
                        long priorityValue = NumberLiterals.parseLong(priorityToken);
                        if (priorityValue < 0 || priorityValue > 20)
                            throw new ParsingException(priorityToken, "Priority must be between 0 and 20, got " + priorityValue);
                        priority = (int) priorityValue;
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
        long rowsValue = NumberLiterals.parseLong(rowsToken);
        if (rowsValue < 1 || rowsValue > 6)
            throw new ParsingException(rowsToken, "Menu rows must be between 1 and 6, got " + rowsValue);
        int rows = (int) rowsValue;
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
                throw new ParsingException(slotToken, "Slot " + slotId + " out of range for a " + rows + "-row menu (0-" + maxSlotId + ")");
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
        if (check(TokenType.LPAREN)) return parseTupleDestructure();
        if (isType(peek()) || isEnumType(peek())) return parseDeclaration();

        if (check(TokenType.IDENT) && AddAction.searchByName(peek().text) != null && checkTwo(TokenType.LPAREN)) {
            return parseAction();
        }

        if (check(TokenType.GLOBAL)) {
            Token globalToken = advance();
            Token nameToken = consume(TokenType.IDENT, "Expected function name after 'global'");
            consume(TokenType.LPAREN, "Expected '(' after function name");
            List<NormalExpressionNode> args = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    args.add(parseExpression());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RPAREN, "Expected ')' after arguments");
            consume(TokenType.SEMI, "Expected ';' after global call");
            return new GlobalFunctionCallNode(nameToken.text, new ArgumentListNode(args), globalToken);
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
            consume(TokenType.RPAREN, "Expected ')' after arguments");
            consume(TokenType.SEMI, "Expected ';' after call");
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
            parsedArgs.add(parseLiteralArg(sig.params.get(i), i + 1, "Action '" + actionName + "'"));
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
            throw new ParsingException(open, "An item cannot be empty - provide at least an id field.");
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
                left = makeUnary(parseUnaryOperatorFromToken(postfixToken), left, previous());
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
                return makeUnary(parseUnaryOperatorFromToken(token), parsePrecedence(Precedence.UNARY), token);
            case MINUS:
                if (check(TokenType.LONG_VALUE)) {
                    Token numTok = advance();
                    return new ConstantPrimitiveNode(new LongValue(NumberLiterals.parseNegatedLong(numTok)), VariableType.LONG, token);
                }
                if (check(TokenType.DOUBLE_VALUE)) {
                    Token numTok = advance();
                    return new ConstantPrimitiveNode(new DoubleValue(-NumberLiterals.parseDouble(numTok)), VariableType.DOUBLE, token);
                }
                return makeUnary(parseUnaryOperatorFromToken(token), parsePrecedence(Precedence.UNARY), token);
            case LONG_CAST:
            case DOUBLE_CAST:
            case STRING_CAST:
                return makeUnary(parseUnaryOperatorFromToken(token), parsePrecedence(Precedence.UNARY), token);
            case CONST_IDENT: {
                Double constVal = MATH_CONSTANTS.get(token.text);
                if (constVal == null) throw new ParsingException(token, "Unsupported constant: " + token.text);
                return new ConstantPrimitiveNode(new DoubleValue(constVal), VariableType.DOUBLE, token);
            }
            case STRING_VALUE:
                return new ConstantPrimitiveNode(new StringValue(token.text.substring(1, token.text.length() - 1)), VariableType.STRING, token);
            case LONG_VALUE:
                return new ConstantPrimitiveNode(new LongValue(NumberLiterals.parseLong(token)), VariableType.LONG, token);
            case DOUBLE_VALUE:
                return new ConstantPrimitiveNode(new DoubleValue(NumberLiterals.parseDouble(token)), VariableType.DOUBLE, token);
            case TRUE:
                return new ConstantPrimitiveNode(new BoolValue(true), VariableType.BOOL, token);
            case FALSE:
                return new ConstantPrimitiveNode(new BoolValue(false), VariableType.BOOL, token);
            case LPAREN:
                NormalExpressionNode expr = parseExpression();
                consume(TokenType.RPAREN, "Expected ')' after expression.");
                return expr;
            case IDENT: {
                // Qualified enum access: Color.GREEN -> its ordinal, tagged with the enum name.
                if (enumDefs.containsKey(token.text)) {
                    requireVisible(token, enumDeclTokens.get(token.text), "Enum '" + token.text + "'");
                    if (!check(TokenType.DOT))
                        throw new ParsingException(token, "Enum '" + token.text + "' must be used as '" + token.text + ".MEMBER'");
                    advance(); // consume '.'
                    Token memberTok = consume(TokenType.IDENT, "Expected an enum member after '" + token.text + ".'");
                    int idx = enumDefs.get(token.text).indexOf(memberTok.text);
                    if (idx < 0)
                        throw new ParsingException(memberTok, "'" + memberTok.text + "' is not a member of enum '" + token.text + "'");
                    ConstantPrimitiveNode node = new ConstantPrimitiveNode(new LongValue(idx), VariableType.LONG, memberTok);
                    node.enumName = token.text;
                    return node;
                }

                ConstantPrimitiveNode constNode = constTable.get(token.text);
                if (constNode != null) {
                    requireVisible(token, constNode.token, "Constant '" + token.text + "'");
                    ConstantPrimitiveNode copy = new ConstantPrimitiveNode(constNode.value, constNode.type, token);
                    copy.enumName = constNode.enumName; // preserve enum identity for bare member refs
                    return copy;
                }

                if (check(TokenType.LPAREN)) {
                    advance();
                    List<NormalExpressionNode> args = new ArrayList<>();
                    if (!check(TokenType.RPAREN)) {
                        do {
                            args.add(parseExpression());
                        } while (match(TokenType.COMMA));
                    }
                    consume(TokenType.RPAREN, "Expected ')' after arguments");
                    return new FunctionCallNode(token.text, new ArgumentListNode(args), null, token);
                }

                if (token.text.equals("ph")) return parsePlaceholder();

                stepBack();
                VariableNode readVar = parseVariable();
                attachFallback(readVar);
                return readVar;
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
        // or a condition-built-in
        if (check(TokenType.IDENT) && AddCondition.searchByName(peek().text) != null && checkTwo(TokenType.LPAREN)) {
            return parseConditionCall();
        }

        // attempt a normal "a < b" style clause
        NormalExpressionNode left = parseExpression();
        if (isNextBooleanOperator()) {
            Token opToken = advance();
            BooleanOperator op = parseBooleanOperatorFromToken(opToken);
            NormalExpressionNode right = parseExpression();
            return new BooleanExpressionNode(left, right, op, opToken);
        }

        // Otherwise just keep the expression as-is. will be handled in a visitor
        return left;
    }

    private boolean isNextBooleanOperator() {
        TokenType type = peek().type;
        return switch (type) {
            case EQ_EQ, BANG_EQ, LT, GT, LE, GE -> true;
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
            parsedArgs.add(parseLiteralArg(sig.params.get(i), i + 1, "Condition '" + condName + "'"));
            if (i < sig.params.size() - 1) {
                consume(TokenType.COMMA, "Expected ',' between condition parameters");
            }
        }

        consume(TokenType.RPAREN, "Expected ')' after condition parameters");
        return new ConditionCallNode(condName, new ArgumentListNode(parsedArgs), nameToken);
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
            case STAR, SLASH, PERCENT -> Precedence.FACTOR;
            case BAR, AMPERSAND, CARET, LEFT_SHIFT, RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> Precedence.BITWISE;
            default -> Precedence.NONE;
        };
    }

    public AssignationNode parseAssignment() {
        if (check(TokenType.IDENT) && constTable.containsKey(peek().text))
            throw new ParsingException(peek(), "Cannot assign to constant '" + peek().text + "'");
        VariableNode variableNode = parseVariable();
        Token operatorToken = advance();

        // x++; and x--; as shorthand for x += 1 and x -= 1
        if (operatorToken.type == TokenType.PLUS_PLUS || operatorToken.type == TokenType.MINUS_MINUS) {
            consume(TokenType.SEMI, "Expected ';' after ++/--");
            AssignmentOperator op = operatorToken.type == TokenType.PLUS_PLUS
                    ? AssignmentOperator.ADD_ASSIGN : AssignmentOperator.SUB_ASSIGN;
            NormalExpressionNode one = new ConstantPrimitiveNode(new LongValue(1L), VariableType.LONG, operatorToken);
            return new AssignationNode(variableNode, op, one, operatorToken);
        }

        AssignmentOperator operator = parseAssignmentOperatorFromToken(operatorToken);
        NormalExpressionNode expression = parseExpression();
        consume(TokenType.SEMI, "Expected ';' after assignation");
        return new AssignationNode(variableNode, operator, expression, operatorToken);
    }

    public TupleDestructureNode parseTupleDestructure() {
        Token openParen = consume(TokenType.LPAREN, "Expected '('");
        List<VariableNode> targets = new ArrayList<>();
        do {
            targets.add(parseVariable());
        } while (match(TokenType.COMMA));

        if (targets.size() < 2)
            throw new ParsingException(openParen, "Tuple destructure must have at least 2 targets");

        consume(TokenType.RPAREN, "Expected ')' after targets");
        Token opToken = advance();
        AssignmentOperator operator = parseAssignmentOperatorFromToken(opToken);

        Token nameToken = consume(TokenType.IDENT, "Expected function name");
        consume(TokenType.LPAREN, "Expected '(' after function name");
        List<NormalExpressionNode> argExprs = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                argExprs.add(parseExpression());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "Expected ')' after arguments");
        consume(TokenType.SEMI, "Expected ';' after tuple destructure");

        return new TupleDestructureNode(targets, operator, nameToken.text,
                new ArgumentListNode(argExprs), openParen);
    }

    public DeclarationNode parseDeclaration() {
        VariableNode var = parseTypedVariable();
        if (var.enumName != null) varEnumType.put(var.holder + "." + var.name, var.enumName);
        consume(TokenType.SEMI, "Expected ';' after declaration");
        return new DeclarationNode(var, var.token);
    }

    /** After parsing a variable read, consume an optional {@code ?fallback} suffix.
     *  The fallback must be a constant literal whose type is checked later against the variable. */
    private void attachFallback(VariableNode v) {
        if (!match(TokenType.QUESTION)) return;
        Token qTok = previous();
        NormalExpressionNode fb = parsePrecedence(Precedence.UNARY);
        ConstantPrimitiveNode folded = foldFallbackConstant(fb);
        if (folded == null)
            throw new ParsingException(qTok, "Fallback after '?' must be a constant literal (e.g. p.name?0)");
        v.fallback = folded;
    }

    /** Folds a fallback expression to a constant, accepting a literal or a negated numeric literal. */
    private ConstantPrimitiveNode foldFallbackConstant(NormalExpressionNode e) {
        if (e instanceof ConstantPrimitiveNode c) return c;
        if (e instanceof UnaryExpressionNode u && u.operator == UnaryOperator.NEGATE
                && u.operand instanceof ConstantPrimitiveNode c) {
            return switch (c.value) {
                case LongValue l   -> new ConstantPrimitiveNode(new LongValue(-l.value), VariableType.LONG, c.token);
                case DoubleValue d -> new ConstantPrimitiveNode(new DoubleValue(-d.value), VariableType.DOUBLE, c.token);
                default -> null;
            };
        }
        return null;
    }

    VariableNode parseVariable() {
        Token firstTok = consume(TokenType.IDENT, "Expected a variable");

        // Bare name (no holder prefix): default to PLAYER holder.
        // A holder prefix is only recognised when it's exactly p/g/t followed by its separator.
        boolean isHolderPrefix = switch (firstTok.text) {
            case "p", "g" -> check(TokenType.DOT);
            case "t" -> check(TokenType.DOT) || check(TokenType.LBRACK);
            default -> false;
        };

        if (!isHolderPrefix) {
            VariableNode bare = new VariableNode(null, Holder.PLAYER, firstTok.text, null, firstTok);
            bare.wasBare = true;
            bare.enumName = varEnumType.get(Holder.PLAYER + "." + firstTok.text);
            return bare;
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

        consume(TokenType.DOT, "Expected '.' after scope prefix");
        Token varTok = consume(TokenType.IDENT, "Expected a variable name");

        VariableNode vn = new VariableNode(null, holder, varTok.text, teamName, varTok);
        vn.enumName = varEnumType.get(holder + "." + varTok.text);
        return vn;
    }

    VariableNode parseTypedVariable() {
        Token type = advance();
        String enumType = null;
        VariableType variableType;
        if (isEnumType(type)) {
            variableType = VariableType.LONG; // enums are longs under the hood
            enumType = type.text;
        } else {
            variableType = parseVariableFromToken(type);
        }
        VariableNode variableNode = parseVariable();
        variableNode.type = variableType;
        variableNode.enumName = enumType;
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

    /**
     * Parses an enum declaration: {@code enum Name { A, B, C }}. Each member is registered as a
     * {@code long} constant with its ordinal value (0, 1, 2, ...), i.e. enums are just named longs.
     */
    private void parseEnumDeclaration() {
        consume(TokenType.ENUM, "Expected 'enum'");
        Token nameTok = consume(TokenType.IDENT, "Expected enum name after 'enum'");
        String enumName = nameTok.text;
        if (enumDefs.containsKey(enumName))
            throw new ParsingException(nameTok, "Enum '" + enumName + "' is already declared");
        consume(TokenType.LBRACE, "Expected '{' to begin enum body");

        List<String> members = new ArrayList<>();
        long index = 0;
        if (!check(TokenType.RBRACE)) {
            do {
                if (check(TokenType.RBRACE)) break; // allow a trailing comma
                Token memberTok = consume(TokenType.IDENT, "Expected enum member name");
                String member = memberTok.text;
                if (constTable.containsKey(member))
                    throw new ParsingException(memberTok, "Enum member '" + member + "' clashes with an existing constant or enum member");
                // Members are long constants (their ordinal), tagged with the enum name for type checks.
                ConstantPrimitiveNode memberNode = new ConstantPrimitiveNode(new LongValue(index), VariableType.LONG, memberTok);
                memberNode.enumName = enumName;
                constTable.put(member, memberNode);
                members.add(member);
                index++;
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RBRACE, "Expected '}' after enum body");
        if (members.isEmpty())
            throw new ParsingException(nameTok, "Enum '" + enumName + "' must declare at least one member");
        enumDefs.put(enumName, members);
        enumDeclTokens.put(enumName, nameTok);
    }

    /** True if the token is an identifier naming a declared enum, usable as a type. */
    private boolean isEnumType(Token t) {
        if (t.type != TokenType.IDENT || !enumDefs.containsKey(t.text)) return false;
        requireVisible(t, enumDeclTokens.get(t.text), "Enum '" + t.text + "'");
        return true;
    }

    private UnaryExpressionNode makeUnary(UnaryOperator op, NormalExpressionNode operand, Token token) {
        return new UnaryExpressionNode(op, operand, null, token);
    }

    private void parseConstDeclaration() {
        consume(TokenType.CONST, "Expected 'const'");
        if (peek().type == TokenType.VAR)
            throw new ParsingException(peek(), "Constants cannot use type 'var'; use an explicit type instead");
        if (!isType(peek()))
            throw new ParsingException(peek(), "Expected a type (long, double, string, bool) after 'const'");
        Token typeToken = advance();
        VariableType declaredType = parseVariableFromToken(typeToken);
        Token nameTok = consume(TokenType.IDENT, "Expected constant name after type");
        String name = nameTok.text;
        if (constTable.containsKey(name))
            throw new ParsingException(nameTok, "Constant '" + name + "' is already declared");
        consume(TokenType.EQ, "Expected '=' in constant declaration");
        NormalExpressionNode valueExpr = parseExpression();
        if (!(valueExpr instanceof ConstantPrimitiveNode constNode))
            throw new ParsingException(nameTok, "Constant '" + name + "' value must be a literal or a reference to another constant");
        if (constNode.type != declaredType)
            throw new ParsingException(nameTok, "Constant '" + name + "' declared as " + declaredType + " but value is " + constNode.type);
        consume(TokenType.SEMI, "Expected ';' after constant declaration");
        constTable.put(name, new ConstantPrimitiveNode(constNode.value, declaredType, nameTok));
    }

    GlobalDeclarationNode parseGlobalDeclaration() {
        VariableNode var = parseTypedVariable();
        if (var.enumName != null) varEnumType.put(var.holder + "." + var.name, var.enumName);
        NormalExpressionNode initializer = null;
        if (match(TokenType.EQ)) {
            if (var.holder == Holder.PLAYER) {
                throw new ParsingException(previous(), "Player variables (p.) cannot have initializers; only global (g.) and team (t[...]) variables can.");
            }
            initializer = parseExpression();
        }
        consume(TokenType.SEMI, "Expected ';' after declaration");
        return new GlobalDeclarationNode(var, initializer, var.token);
    }

    private ConstantPrimitive parseConstantPrimitive() {
        Token token = advance();
        return switch (token.type) {
            case STRING_VALUE -> new StringValue(token.text.substring(1, token.text.length() - 1));
            case LONG_VALUE -> new LongValue(NumberLiterals.parseLong(token));
            case DOUBLE_VALUE -> new DoubleValue(NumberLiterals.parseDouble(token));
            case TRUE -> new BoolValue(true);
            case FALSE -> new BoolValue(false);
            case CONST_IDENT -> {
                Double constVal = MATH_CONSTANTS.get(token.text);
                if (constVal == null) throw new ParsingException(token, "Unsupported constant: " + token.text);
                yield new DoubleValue(constVal);
            }
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
            case PERCENT_EQ -> AssignmentOperator.MOD_ASSIGN;
            case BAR_EQ -> AssignmentOperator.BITWISE_OR_ASSIGN;
            case AMP_EQ -> AssignmentOperator.BITWISE_AND_ASSIGN;
            case CARET_EQ -> AssignmentOperator.BITWISE_XOR_ASSIGN;
            case LEFT_SHIFT_EQ -> AssignmentOperator.LEFT_SHIFT_ASSIGN;
            case RIGHT_SHIFT_EQ -> AssignmentOperator.RIGHT_SHIFT_ASSIGN;
            case UNSIGNED_RIGHT_SHIFT_EQ -> AssignmentOperator.UNSIGNED_RIGHT_SHIFT_ASSIGN;
            default -> throw new ParsingException(operator, "Invalid operator type, expected '=', '+=', '-=', '*=', '/=', '%=', '|=', '&=', '^=', '<<=', '>>=' or '>>>='");
        };
    }

    BinaryOperator parseBinaryOperatorFromToken(Token operator) {
        return switch (operator.type) {
            case PLUS -> BinaryOperator.ADD;
            case MINUS -> BinaryOperator.SUBTRACT;
            case STAR -> BinaryOperator.MULTIPLY;
            case SLASH -> BinaryOperator.DIVIDE;
            case PERCENT -> BinaryOperator.MODULO;
            case BAR -> BinaryOperator.BITWISE_OR;
            case AMPERSAND -> BinaryOperator.BITWISE_AND;
            case CARET -> BinaryOperator.BITWISE_XOR;
            case LEFT_SHIFT -> BinaryOperator.LEFT_SHIFT;
            case RIGHT_SHIFT -> BinaryOperator.RIGHT_SHIFT;
            case UNSIGNED_RIGHT_SHIFT -> BinaryOperator.UNSIGNED_RIGHT_SHIFT;
            default -> throw new ParsingException(operator, "Invalid binary operator, expected '+', '-', '*', '/', '%', '|', '&', '^', '<<', '>>' or '>>>'");
        };
    }

    BooleanOperator parseBooleanOperatorFromToken(Token operator) {
        return switch (operator.type) {
            case EQ_EQ -> BooleanOperator.EQUALS;
            // Housing has no "not equal": it becomes an equality check with the condition inverted.
            case BANG_EQ -> BooleanOperator.NOT_EQUALS;
            case LT -> BooleanOperator.LESS_THAN;
            case LE -> BooleanOperator.LESS_THAN_EQUALS;
            case GT -> BooleanOperator.GREATER_THAN;
            case GE -> BooleanOperator.GREATER_THAN_EQUALS;
            default -> throw new ParsingException(operator, "Invalid comparison, expected '==', '!=', '<', '<=', '>' or '>='.");
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

