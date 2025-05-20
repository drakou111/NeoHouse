package com.drakou111.neohouse.compilers.parser;

import com.drakou111.neohouse.compilers.ast.BlockNode;
import com.drakou111.neohouse.compilers.ast.enums.BaseType;
import com.drakou111.neohouse.compilers.ast.expr.ConstExprNode;
import com.drakou111.neohouse.compilers.ast.main.MainNode;
import com.drakou111.neohouse.compilers.ast.prog.EventNode;
import com.drakou111.neohouse.compilers.ast.prog.FunctionNode;
import com.drakou111.neohouse.compilers.ast.ParamNode;
import com.drakou111.neohouse.compilers.ast.ProgramNode;
import com.drakou111.neohouse.compilers.ast.stmt.StmtNode;
import com.drakou111.neohouse.compilers.ast.types.ArrayTypeNode;
import com.drakou111.neohouse.compilers.ast.types.BaseTypeNode;
import com.drakou111.neohouse.compilers.ast.types.TypeNode;
import com.drakou111.neohouse.compilers.lexer.Token;
import com.drakou111.neohouse.compilers.lexer.TokenType;
import com.drakou111.neohouse.exceptions.ParseException;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private boolean isAtEnd() {
        return pos >= tokens.size();
    }

    private int currentPosition() {
        return pos;
    }

    // Helpers
    private Token peek() {
        return pos < tokens.size() ? tokens.get(pos) : null;
    }
    private Token consume() {
        return tokens.get(pos++);
    }
    private boolean match(TokenType... types) {
        Token t = peek();
        if (t != null) {
            for (TokenType type : types) {
                if (t.type == type) {
                    pos++;
                    return true;
                }
            }
        }
        return false;
    }
    private Token expect(TokenType type) throws ParseException {
        Token t = peek();
        if (t == null || t.type != type) {
            throw new ParseException("Expected " + type + " at position " + currentPosition() + " but got " + (t == null ? "EOF" : t.type));
        }
        pos++;
        return t;
    }
    private Token previous() {
        return tokens.get(pos - 1);
    }
    private boolean check(TokenType type) {
        Token t = peek();
        return t != null && t.type == type;
    }

    MainNode parseMain() {
        List<ProgramNode> nodes = new ArrayList<>();

        while (!isAtEnd()) {
            if (match(TokenType.FUNCTION)) {
                nodes.add(parseFunction());
            } else if (match(TokenType.EVENT)) {
                nodes.add(parseEvent());
            } else {
                throw new ParseException(getErrorString("'function' or 'event'"));
            }
        }

        return new MainNode(nodes);
    }

    FunctionNode parseFunction() throws ParseException {
        String note = null;

        // Check if previous token is a note (/// ...)
        if (pos > 0) {
            Token prev = tokens.get(pos - 1);
            if (prev.type == TokenType.NOTE) {
                note = prev.text.substring(3).trim();
                pos--;
                consume();
            }
        }

        expect(TokenType.FUNCTION);

        // Check if next token is a type keyword
        TypeNode returnType = null;
        Token next = peek();

        if (next != null && isTypeToken(next.type)) {
            returnType = parseType();
        }

        Token nameToken = expect(TokenType.IDENT);

        List<ParamNode> params = parseParamList();

        BlockNode body = parseBlock();

        return new FunctionNode(note, nameToken.text, returnType, params, body);
    }

    private boolean isTypeToken(TokenType type) {
        return type == TokenType.LONG || type == TokenType.DOUBLE
                || type == TokenType.STRING || type == TokenType.VAR;
    }

    private TypeNode parseType() throws ParseException {
        BaseTypeNode base;

        // 1) Consume one of the base‐type tokens
        if (match(TokenType.LONG)) {
            base = new BaseTypeNode(BaseType.LONG);
        }
        else if (match(TokenType.DOUBLE)) {
            base = new BaseTypeNode(BaseType.DOUBLE);
        }
        else if (match(TokenType.STRING)) {
            base = new BaseTypeNode(BaseType.STRING);
        }
        else if (match(TokenType.VAR)) {
            base = new BaseTypeNode(BaseType.VAR);
        }
        else {
            throw new ParseException(getErrorString("a variable type (long, double, string, var)"));
        }

        // 2) Check for array “[size]”
        if (match(TokenType.LBRACK)) {
            ConstExprNode size = parseConstExpr();
            expect(TokenType.RBRACK);

            return new ArrayTypeNode(base, size);
        }

        // 3) Otherwise it’s just a simple BaseTypeNode
        return base;
    }

    private List<ParamNode> parseParamList() throws ParseException {

    }

    private BlockNode parseBlock() throws ParseException {
        expect(TokenType.LBRACE);
        List<StmtNode> stmts = parseStmtList();
        expect(TokenType.RBRACE);
        return new BlockNode(stmts);
    }

    EventNode parseEvent() {

    }

    private List<StmtNode> parseStmtList() throws ParseException {
        List<StmtNode> stmts = new ArrayList<>();
        // keep parsing statements until we hit a '}' (end of block) or EOF
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            stmts.add(parseStmt());
        }
        return stmts;
    }

    private StmtNode parseStmt() throws ParseException {

    }

    private ConstExprNode parseConstExpr() throws ParseException {

    }

    private ConstExprNode parseConstAddSub() throws ParseException {
        ConstExprNode left = parseConstMulDivMod();
        while (true) {
            if (match(TokenType.PLUS)) {
                ConstExprNode right = parseConstMulDivMod();
                left = new BinaryOpConstNode(left, "+", right);
            }
            else if (match(TokenType.MINUS)) {
                ConstExprNode right = parseConstMulDivMod();
                left = new BinaryOpConstNode(left, "-", right);
            }
            else {
                break;
            }
        }
        return left;
    }

    private ConstExprNode parseConstMulDivMod() throws ParseException {
        ConstExprNode left = parseConstUnary();
        while (true) {
            if (match(TokenType.STAR)) {
                ConstExprNode right = parseConstUnary();
                left = new BinaryOpConstNode(left, "*", right);
            }
            else if (match(TokenType.SLASH)) {
                ConstExprNode right = parseConstUnary();
                left = new BinaryOpConstNode(left, "/", right);
            }
            else if (match(TokenType.PERCENT)) {
                ConstExprNode right = parseConstUnary();
                left = new BinaryOpConstNode(left, "%", right);
            }
            else {
                break;
            }
        }
        return left;
    }

    private ConstExprNode parseConstUnary() throws ParseException {
        if (match(TokenType.MINUS)) {
            ConstExprNode operand = parseConstUnary();
            return new UnaryOpConstNode("-", operand);
        }
        if (match(TokenType.BAR)) {           // '|'
            ConstExprNode inside = parseConstExpr();
            expect(TokenType.BAR);
            return new UnaryOpConstNode("abs", inside);
        }
        if (match(TokenType.IDENT)) {
            String fn = previous().text;
            switch (fn) {
                case "sin": case "cos": case "tan":
                case "asin": case "acos": case "atan":
                case "sqrt":
                    expect(TokenType.LPAREN);
                    ConstExprNode arg = parseConstExpr();
                    expect(TokenType.RPAREN);
                    return new FuncConstNode(fn, List.of(arg));
                case "nroot":
                case "pow":
                    expect(TokenType.LPAREN);
                    ConstExprNode a = parseConstExpr();
                    expect(TokenType.COMMA);
                    ConstExprNode b = parseConstExpr();
                    expect(TokenType.RPAREN);
                    return new FuncConstNode(fn, List.of(a,b));
                default:
                    // not a function name → backtrack
                    pos--;  // undo the IDENT match
            }
        }
        return parseConstPostfix();
    }

    private ConstExprNode parseConstPostfix() throws ParseException {
        ConstExprNode node = parseConstPrimary();
        if (match(TokenType.PLUS_PLUS)) {
            return new UnaryOpConstNode("++", node);
        }
        if (match(TokenType.MINUS_MINUS)) {
            return new UnaryOpConstNode("--", node);
        }
        return node;
    }

    private ConstExprNode parseConstPrimary() throws ParseException {
        Token t = peek();

        // Parentheses
        if (match(TokenType.LPAREN)) {
            ConstExprNode inside = parseConstExpr();
            expect(TokenType.RPAREN);
            return inside;
        }

        // Numeric/string/bool literals
        if (match(TokenType.LONG_CONST,
                TokenType.DOUBLE_CONST,
                TokenType.STRING_CONST,
                TokenType.TRUE,
                TokenType.FALSE)) {
            return new ConstLiteralNode(previous().text);
        }

        // Named constants
        if (match(TokenType.CONST_IDENT)) {
            return new ConstNamedNode(previous().text);
        }

        throw new ParseException("Expected constant expression but got " + t);
    }

    private String getErrorString(String expected) {
        Token peek = peek();
        return "Expected " + expected + " at " + currentPosition() + " but got " + (peek == null ? "EOF" : peek.type);
    }
}

