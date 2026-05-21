package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.lexer.Lexer;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.parser.Parser;
import com.drakou111.neohouse.compiler.visitors.*;
import com.drakou111.neohouse.exceptions.CompilingException;

import java.nio.file.Path;
import java.util.List;

public class NHCompiler implements Compiler {
    private int lastHelperCount = 0;

    public int getLastHelperCount() { return lastHelperCount; }

    @Override
    public Action compile(String source) throws CompilingException {
        return compile(source, null);
    }

    @Override
    public Action compile(String source, Path scriptsRoot) throws CompilingException {
        source = new Preprocessor(scriptsRoot).process(source);

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

        TypeInferringVisitor typeInferring = new TypeInferringVisitor();
        node = typeInferring.analyze(node);

        StringLengthCheckVisitor stringLengthCheck = new StringLengthCheckVisitor();
        stringLengthCheck.analyze(node);

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
        lastHelperCount = actionCountSplittingVisitor.getHelpersCreated();

        ActionRestrictionVisitor actionRestrictionVisitor = new ActionRestrictionVisitor();
        node = actionRestrictionVisitor.analyze(node);

        return new CodeGenerator().generate(node);
    }
}
