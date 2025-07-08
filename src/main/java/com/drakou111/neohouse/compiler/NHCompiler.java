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
                function test() {
                    if (p.va == 1) {
                        p.va = 0;
                        p.va = 1;
                        p.va = 2;
                        p.va = 3;
                        p.va = 4;
                        p.va = 5;
                        p.va = 6;
                        p.va = 7;
                        p.va = 8;
                        p.va = 9;
                        p.va = 9;
                        p.va = 9;
                        p.va = 9;
                        p.va = 9;
                        p.va = 9;
                        p.va = 9;
                        p.va = 9;
                        p.va = 0;
                        p.va = 1;
                        p.va = 2;
                        p.va = 3;
                        p.va = 4;
                        p.va = 5;
                        p.va = 6;
                        p.va = 7;
                        p.va = 8;
                        p.va = 9;
                    }
                }
                
                """;

        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens);
        MainNode node = parser.parseMain();

        // TODO: - 21 -> -21

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

        // TODO: can only use return in conditionals/randoms, or as the very last statement. IF at very end of functions, simply remove it, its literally useless, especially since exits can't exit in functions directly.

        // Makes all functions void with 0 parameters by using global variables to share the parameters and outputs.
        FunctionParamLoweringVisitor functionParamLoweringVisitor = new FunctionParamLoweringVisitor();
        node = functionParamLoweringVisitor.analyze(node);

        // Unfold randoms with multiple statements into function calls
        RandomUnfoldingVisitor randomUnfoldingVisitor = new RandomUnfoldingVisitor();
        node = randomUnfoldingVisitor.analyze(node);

        // unfold statements with too many of the same action to be put into a new function. For now, don't check if its valid.
        ActionCountSplittingVisitor actionCountSplittingVisitor = new ActionCountSplittingVisitor();
        node = actionCountSplittingVisitor.analyze(node);

        // TODO: ^ same but for conditions, but just make it check.

        // Prevent actions from being in the wrong areas. Like an IF inside an IF.
        ActionRestrictionVisitor actionRestrictionVisitor = new ActionRestrictionVisitor();
        node = actionRestrictionVisitor.analyze(node);

        // TODO: Add built-in functions...

        System.out.println(StringHelper.formatLikeJson(node.toString(), "  "));
    }


}
