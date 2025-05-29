package com.drakou111.neohouse.compilers;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.ChatAction;
import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.main.MainNode;
import com.drakou111.neohouse.compilers.helpers.StringHelper;
import com.drakou111.neohouse.compilers.lexer.Lexer;
import com.drakou111.neohouse.compilers.lexer.Token;
import com.drakou111.neohouse.compilers.optimizer.SimplifyingExpressionsVisitor;
import com.drakou111.neohouse.compilers.parser.Parser;
import com.drakou111.neohouse.exceptions.CompilingException;

import java.util.ArrayList;
import java.util.List;

public class NHCompiler implements Compiler {
    @Override
    public Action compile(String source) throws CompilingException {
        return new ChatAction("Executing NH script:\n" + source);
    }


    public static void main(String[] args) {
        String code = """
                function test (long p.test) {
                    p.test = (-p.test);
                }
                """;

        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();

        /*for (Token token : tokens) {
            System.out.println(token);
        }*/

        Parser parser = new Parser(tokens);
        MainNode node = parser.parseMain();
        //System.out.println(StringHelper.formatLikeJson(node.toString(), "  "));

        SimplifyingExpressionsVisitor flat = new SimplifyingExpressionsVisitor();
        flat.analyze(node);


        System.out.println(StringHelper.formatLikeJson(node.toString(), "  "));
    }
}
