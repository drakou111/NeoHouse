package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.parser.Parser;
import com.drakou111.neohouse.compiler.visitors.ActionCountSplittingVisitor;
import com.drakou111.neohouse.compiler.visitors.ActionRestrictionVisitor;
import com.drakou111.neohouse.compiler.visitors.ConditionLimitVisitor;
import com.drakou111.neohouse.compiler.visitors.DeadCodeCheckVisitor;
import com.drakou111.neohouse.compiler.visitors.DeclarationCollectorVisitor;
import com.drakou111.neohouse.compiler.visitors.DuplicateNameCheck;
import com.drakou111.neohouse.compiler.visitors.EnumCheckVisitor;
import com.drakou111.neohouse.compiler.visitors.ExtraVariableCollectorVisitor;
import com.drakou111.neohouse.compiler.visitors.FunctionCallLoweringVisitor;
import com.drakou111.neohouse.compiler.visitors.FunctionDeduplicationVisitor;
import com.drakou111.neohouse.compiler.visitors.FunctionParamLoweringVisitor;
import com.drakou111.neohouse.compiler.visitors.FunctionTempNamingVisitor;
import com.drakou111.neohouse.compiler.visitors.GlobalCallCheckVisitor;
import com.drakou111.neohouse.compiler.visitors.HousingLimitCheck;
import com.drakou111.neohouse.compiler.visitors.ImportVisibilityVisitor;
import com.drakou111.neohouse.compiler.visitors.LinkFunctionVisitor;
import com.drakou111.neohouse.compiler.visitors.NeutralStatementRemovalVisitor;
import com.drakou111.neohouse.compiler.visitors.PlaceholderValidationVisitor;
import com.drakou111.neohouse.compiler.visitors.RandomUnfoldingVisitor;
import com.drakou111.neohouse.compiler.visitors.RecursionCheckVisitor;
import com.drakou111.neohouse.compiler.visitors.RepeatUnrollingVisitor;
import com.drakou111.neohouse.compiler.visitors.ReturnCheckVisitor;
import com.drakou111.neohouse.compiler.visitors.ReturnContextChecker;
import com.drakou111.neohouse.compiler.visitors.ReturnPlacementVisitor;
import com.drakou111.neohouse.compiler.visitors.ScopeCheckingVisitor;
import com.drakou111.neohouse.compiler.visitors.SimplifyingExpressionsVisitor;
import com.drakou111.neohouse.compiler.visitors.StringAppendLoweringVisitor;
import com.drakou111.neohouse.compiler.visitors.StringLengthCheckVisitor;
import com.drakou111.neohouse.compiler.visitors.TempOptimizationVisitor;
import com.drakou111.neohouse.compiler.visitors.TypeInferringVisitor;
import com.drakou111.neohouse.compiler.visitors.UnknownCallCheckVisitor;
import com.drakou111.neohouse.compiler.visitors.UnsetVariableCheckVisitor;
import com.drakou111.neohouse.config.UndeclaredVariableMode;
import com.drakou111.neohouse.exceptions.CompilingException;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NHCompiler implements Compiler {
    private int lastHelperCount = 0;
    private List<String> lastHelperNames = List.of();
    private ScriptSummary lastSummary = ScriptSummary.EMPTY;
    private List<String> lastExtraVariables = List.of();
    private List<CompileWarning> lastWarnings = List.of();
    private ImportGraph lastImportGraph = ImportGraph.singleFile();

    public int getLastHelperCount() { return lastHelperCount; }
    public List<String> getLastHelperNames() { return lastHelperNames; }
    /** What the last script that compiled cleanly holds; {@link ScriptSummary#EMPTY} before any. */
    public ScriptSummary getLastSummary() { return lastSummary; }
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
        MainNode parsed = node;
        if (options.forbidVarType() && !parser.getVarTypeTokens().isEmpty())
            throw new SemanticException(parser.getVarTypeTokens().getFirst(),
                    "The var type is turned off in the settings; use long, double, string or bool.");

        // Enum nominal-type checks run first, on the raw AST where enum tags are still present.
        new EnumCheckVisitor(parser.getFunctionParamEnums(), parser.getFunctionReturnEnums()).check(node);

        PlaceholderValidationVisitor pv = new PlaceholderValidationVisitor();
        node = pv.analyze(node);

        DuplicateNameCheck.check(node);
        new UnknownCallCheckVisitor(options.unknownCallMode(), warnings).analyze(node);

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
        reportUndeclared(checker.getUndeclared(), options, warnings);
        node = new StringAppendLoweringVisitor().analyze(node);

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

        // Calls, return and parameter slots have their final form from here on; temps are still t$N.
        node = new TempOptimizationVisitor().analyze(node);

        NeutralStatementRemovalVisitor neutralRemoval = new NeutralStatementRemovalVisitor();
        node = neutralRemoval.analyze(node);

        // Before random arms and long bodies become helper functions, which keep their function's temps.
        node = new FunctionTempNamingVisitor().analyze(node);

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

        new UnsetVariableCheckVisitor(options.unsetVariableMode(), warnings).analyze(node);

        lastExtraVariables = new ExtraVariableCollectorVisitor().collect(node);
        lastSummary = ScriptSummary.of(parsed, node);
        return node;
    }

    /**
     * Warns about or rejects variables the script uses without declaring them, as the settings ask.
     * With the var type turned off they are always errors, since an undeclared variable is a var.
     */
    private static void reportUndeclared(List<VariableNode> undeclared, CompileOptions options, List<CompileWarning> warnings) {
        UndeclaredVariableMode mode = options.forbidVarType() ? UndeclaredVariableMode.ERROR : options.undeclaredVariableMode();
        if (mode == UndeclaredVariableMode.ALLOW) return;
        for (VariableNode variable : undeclared) {
            String name = variable.sourceName();
            String message = options.forbidVarType()
                    ? "'%s' is not declared, so it would be a var, which is turned off; declare it with a type, e.g. long %s;".formatted(name, name)
                    : "'%s' is not declared; declare it at the top of the file, e.g. long %s;".formatted(name, name);
            if (mode == UndeclaredVariableMode.ERROR) throw new SemanticException(variable.token, message);
            warnings.add(new CompileWarning(variable.token, message));
        }
    }
}
