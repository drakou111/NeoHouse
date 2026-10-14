package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.parser.Parser;
import com.drakou111.neohouse.compiler.visitors.*;
import com.drakou111.neohouse.exceptions.CompilingException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NHCompiler implements Compiler {
    private int lastHelperCount = 0;
    private List<String> lastHelperNames = List.of();
    private List<String> lastExtraVariables = List.of();
    private List<CompileWarning> lastWarnings = List.of();
    private ImportGraph lastImportGraph = ImportGraph.singleFile();

    public int getLastHelperCount() { return lastHelperCount; }
    public List<String> getLastHelperNames() { return lastHelperNames; }
    public List<String> getLastExtraVariables() { return lastExtraVariables; }
    public List<CompileWarning> getLastWarnings() { return lastWarnings; }
    /** Import graph of the last compile, also set when that compile failed (as far as it got). */
    public ImportGraph getLastImportGraph() { return lastImportGraph; }

    @Override
    public Action compile(String source) throws CompilingException {
        return compile(source, null);
    }

    @Override
    public Action compile(String source, Path sourceFile) throws CompilingException {
        CompileOptions options = CompileOptions.fromConfig();
        return new CodeGenerator(options.overrideMode()).generate(analyze(source, sourceFile, options));
    }

    /** Runs every front-end pass and returns the final, lowered AST that the code generator consumes. */
    public MainNode analyze(String source, Path sourceFile, CompileOptions options) throws CompilingException {
        List<CompileWarning> warnings = new ArrayList<>();
        lastWarnings = warnings;

        SourceResolver resolver = new SourceResolver(sourceFile, warnings);
        lastImportGraph = resolver.getGraph();
        List<Token> tokens = resolver.resolve(source);

        Parser parser = new Parser(tokens, lastImportGraph);
        MainNode node = parser.parseMain();

        // Enum nominal-type checks run first, on the raw AST where enum tags are still present.
        new EnumCheckVisitor(parser.getFunctionParamEnums(), parser.getFunctionReturnEnums()).check(node);

        PlaceholderValidationVisitor pv = new PlaceholderValidationVisitor();
        node = pv.analyze(node);

        LinkFunctionVisitor linkFunctionVisitor = new LinkFunctionVisitor(node);
        node = linkFunctionVisitor.analyze(node);

        new ImportVisibilityVisitor(lastImportGraph, node).analyze(node);

        new RecursionCheckVisitor(options.recursionMode(), warnings).analyze(node);

        new GlobalCallCheckVisitor(options.globalCallMode(), warnings).analyze(node);

        RepeatUnrollingVisitor repeatUnroller = new RepeatUnrollingVisitor();
        node = repeatUnroller.analyze(node);

        // After unrolling, so each unrolled call site can be given its own copy.
        node = new FunctionDeduplicationVisitor(options.multiCallMode(), warnings).analyze(node);

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

        NeutralStatementRemovalVisitor neutralRemoval = new NeutralStatementRemovalVisitor();
        node = neutralRemoval.analyze(node);

        // Calls are plain void calls from here on; before random arms and long bodies become helpers.
        node = new TempIsolationVisitor().analyze(node);

        RandomUnfoldingVisitor randomUnfoldingVisitor = new RandomUnfoldingVisitor();
        node = randomUnfoldingVisitor.analyze(node);

        ActionCountSplittingVisitor actionCountSplittingVisitor = new ActionCountSplittingVisitor();
        node = actionCountSplittingVisitor.analyze(node);
        lastHelperCount = actionCountSplittingVisitor.getHelpersCreated();
        lastHelperNames = actionCountSplittingVisitor.getHelperNames();

        ActionRestrictionVisitor actionRestrictionVisitor = new ActionRestrictionVisitor();
        node = actionRestrictionVisitor.analyze(node);

        HousingLimitCheck.check(node);
        new ConditionLimitVisitor().analyze(node);

        lastExtraVariables = new ExtraVariableCollectorVisitor().collect(node);
        return node;
    }
}
