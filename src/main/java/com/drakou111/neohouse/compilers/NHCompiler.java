package com.drakou111.neohouse.compilers;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.ChatAction;
import com.drakou111.neohouse.compilers.ast.main.MainNode;
import com.drakou111.neohouse.compilers.helpers.StringHelper;
import com.drakou111.neohouse.compilers.lexer.Lexer;
import com.drakou111.neohouse.compilers.lexer.Token;
import com.drakou111.neohouse.compilers.optimizer.*;
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
                function long b (long p.something) {
                    return 1;
                }
                function long a (long p.something) {
                    return 1;
                }
                function long somefunc (long p.somevar) {
                    p.somevar2 = a(p.somevar);
                    return 1;
                }
                
                """;

        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens);
        MainNode node = parser.parseMain();

        // ...
        ReturnContextChecker returnContextChecker = new ReturnContextChecker();
        returnContextChecker.analyze(node);

        // ...
        LinkFunctionVisitor linkFunctionVisitor = new LinkFunctionVisitor(node);
        node = linkFunctionVisitor.analyze(node);

        // ...
        RepeatUnrollingVisitor repeatUnroller = new RepeatUnrollingVisitor();
        node = repeatUnroller.analyze(node);

        // ...
        DeclarationCollectorVisitor collector = new DeclarationCollectorVisitor();
        collector.analyze(node);

        // ...
        ScopeCheckingVisitor checker = new ScopeCheckingVisitor(collector.globalDecls, collector.allLocalNames);
        node = checker.analyze(node);

        // ...
        SimplifyingExpressionsVisitor flat = new SimplifyingExpressionsVisitor();
        node = flat.analyze(node);

        // ...
        FunctionCallLoweringVisitor lower = new FunctionCallLoweringVisitor(node);
        node = lower.analyze(node);

        // ...
        ReturnCheckVisitor returnCheck = new ReturnCheckVisitor();
        returnCheck.analyze(node);

        FunctionParamLoweringVisitor functionParamLoweringVisitor = new FunctionParamLoweringVisitor();
        node = functionParamLoweringVisitor.analyze(node);

        System.out.println(StringHelper.formatLikeJson(node.toString(), "  "));
    }
}
