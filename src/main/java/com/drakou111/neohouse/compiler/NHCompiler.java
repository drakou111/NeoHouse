package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.lexer.Lexer;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.parser.Parser;
import com.drakou111.neohouse.compiler.visitors.*;
import com.drakou111.neohouse.exceptions.CompilingException;

import java.util.List;

public class NHCompiler implements Compiler {
    @Override
    public Action compile(String source) throws CompilingException {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens);
        MainNode node = parser.parseMain();

        PlaceholderValidationVisitor pv = new PlaceholderValidationVisitor();
        node = pv.analyze(node);

        LinkFunctionVisitor linkFunctionVisitor = new LinkFunctionVisitor(node);
        node = linkFunctionVisitor.analyze(node);

        RepeatUnrollingVisitor repeatUnroller = new RepeatUnrollingVisitor();
        node = repeatUnroller.analyze(node);

        DeclarationCollectorVisitor collector = new DeclarationCollectorVisitor();
        collector.analyze(node);

        ScopeCheckingVisitor checker = new ScopeCheckingVisitor(collector.globalDecls, collector.allLocalNames);
        node = checker.analyze(node);

        ReturnCheckVisitor returnCheck = new ReturnCheckVisitor();
        returnCheck.analyze(node);

        DeadCodeCheckVisitor deadCodeCheck = new DeadCodeCheckVisitor();
        deadCodeCheck.analyze(node);

        ReturnContextChecker returnContextChecker = new ReturnContextChecker();
        returnContextChecker.analyze(node);

        ReturnPlacementVisitor returnPlacementVisitor = new ReturnPlacementVisitor();
        node = returnPlacementVisitor.analyze(node);

        SimplifyingExpressionsVisitor flat = new SimplifyingExpressionsVisitor();
        node = flat.analyze(node);

        FunctionCallLoweringVisitor lower = new FunctionCallLoweringVisitor(node);
        node = lower.analyze(node);

        FunctionParamLoweringVisitor functionParamLoweringVisitor = new FunctionParamLoweringVisitor();
        node = functionParamLoweringVisitor.analyze(node);

        RandomUnfoldingVisitor randomUnfoldingVisitor = new RandomUnfoldingVisitor();
        node = randomUnfoldingVisitor.analyze(node);

        ActionCountSplittingVisitor actionCountSplittingVisitor = new ActionCountSplittingVisitor();
        node = actionCountSplittingVisitor.analyze(node);

        ConditionCountCheckVisitor conditionCountCheckVisitor = new ConditionCountCheckVisitor();
        conditionCountCheckVisitor.analyze(node);

        ActionRestrictionVisitor actionRestrictionVisitor = new ActionRestrictionVisitor();
        node = actionRestrictionVisitor.analyze(node);

        return new CodeGenerator().generate(node);
    }

    // [DONE] UI robustness: GroupAction now runs consecutive synchronous actions in the same tick so
    //  WaitForAction always registers before the server can send a screen-open event.
    //  WaitForAction also has a 200-tick timeout, and ActionBuilder adds a WaitTicksAction(2) buffer
    //  after each screen-wait so the UI has time to settle before the next interaction.

    // [DONE] Typed/void function parameters now support bare names: `function test(long a)` is valid.
    //  Bare identifiers (no holder prefix) default to PLAYER variables. Inside the function body you
    //  can write `a` instead of `p.a`; the FunctionParamLoweringVisitor still renames them to the
    //  internal player variable `functionName_p$<index>` as before.
    //  Note: keep function names short (< ~10 chars) to stay within Hypixel's 16-char variable limit.

    // [DONE] File browser added as /neohouse files. See ui/FileBrowserScreen.java and
    //  ui/CodeEditorScreen.java. Syntax highlighting is a future enhancement.

    // TODO: Add all new note block sounds

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
    }


}
