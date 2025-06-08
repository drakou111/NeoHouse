package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.ChatAction;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.helpers.StringHelper;
import com.drakou111.neohouse.compiler.lexer.Lexer;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.*;
import com.drakou111.neohouse.compiler.parser.Parser;
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
                    pause(2);
                    return 1;
                }
                
                """;

        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens);
        MainNode node = parser.parseMain();

        PlaceholderValidationVisitor pv = new PlaceholderValidationVisitor();
        node = pv.analyze(node);

        // Sets types to function calls from the definition of the functions.
        LinkFunctionVisitor linkFunctionVisitor = new LinkFunctionVisitor(node);
        node = linkFunctionVisitor.analyze(node);

        // Unrolls repeat blocks.
        RepeatUnrollingVisitor repeatUnroller = new RepeatUnrollingVisitor();
        node = repeatUnroller.analyze(node);

        // Links declarations with variables
        DeclarationCollectorVisitor collector = new DeclarationCollectorVisitor();
        collector.analyze(node);

        // ...
        ScopeCheckingVisitor checker = new ScopeCheckingVisitor(collector.globalDecls, collector.allLocalNames);
        node = checker.analyze(node);

        // Simplifies expressions to only use assignments
        SimplifyingExpressionsVisitor flat = new SimplifyingExpressionsVisitor();
        node = flat.analyze(node);

        // Simplify function parameters to work for hypixel
        FunctionCallLoweringVisitor lower = new FunctionCallLoweringVisitor(node);
        node = lower.analyze(node);

        // Makes sure that all paths of a function ends with a return (only for TypedFunction)
        ReturnCheckVisitor returnCheck = new ReturnCheckVisitor();
        returnCheck.analyze(node);

        // Makes sure 'return e;' is only used in a typedFunction and nothing else, and can't use 'return;'.
        ReturnContextChecker returnContextChecker = new ReturnContextChecker();
        returnContextChecker.analyze(node);

        // Makes all functions void with 0 parameters.
        FunctionParamLoweringVisitor functionParamLoweringVisitor = new FunctionParamLoweringVisitor();
        node = functionParamLoweringVisitor.analyze(node);

        // TODO: SOMEHOW handle actions and conditions :sob:                                                  [DONE]
        // TODO: Avoid illegal calls in events or other stuff like that...
        // TODO: Avoid restrictions... Ex: too many of the same statement, or conditional stack, or random
        // TODO: Rename all variables with small names...
        // TODO: Rename all function names with small names...
        // TODO: Add built-in functions...

        System.out.println(StringHelper.formatLikeJson(node.toString(), "  "));
    }
}
