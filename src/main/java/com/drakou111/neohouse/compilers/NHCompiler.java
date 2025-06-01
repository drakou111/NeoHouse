package com.drakou111.neohouse.compilers;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.ChatAction;
import com.drakou111.neohouse.compilers.ast.main.MainNode;
import com.drakou111.neohouse.compilers.helpers.StringHelper;
import com.drakou111.neohouse.compilers.lexer.Lexer;
import com.drakou111.neohouse.compilers.lexer.Token;
import com.drakou111.neohouse.compilers.optimizer.DeclarationCollectorVisitor;
import com.drakou111.neohouse.compilers.optimizer.ScopeCheckingVisitor;
import com.drakou111.neohouse.compilers.optimizer.SimplifyingExpressionsVisitor;
import com.drakou111.neohouse.compilers.optimizer.TypeInferringVisitor;
import com.drakou111.neohouse.compilers.parser.Parser;
import com.drakou111.neohouse.exceptions.CompilingException;

import java.util.List;

public class NHCompiler implements Compiler {
    @Override
    public Action compile(String source) throws CompilingException {
        return new ChatAction("Executing NH script:\n" + source);
    }


    public static void main(String[] args) {
        String code = """
                function somefunc (long p.somevar) {
                    p.hello = 2 * p.somevar + somecall("tesT");
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

        DeclarationCollectorVisitor collector = new DeclarationCollectorVisitor();
        collector.analyze(node);

        // 2) Run the actual scope/symbol‐table check:
        ScopeCheckingVisitor checker = new ScopeCheckingVisitor(collector.globalDecls, collector.allLocalNames);
        node = checker.analyze(node);

        //TODO: function call typing (somecall() = return type of that function)

        SimplifyingExpressionsVisitor flat = new SimplifyingExpressionsVisitor();
        node = flat.analyze(node);

        System.out.println(StringHelper.formatLikeJson(node.toString(), "  "));
    }
}
