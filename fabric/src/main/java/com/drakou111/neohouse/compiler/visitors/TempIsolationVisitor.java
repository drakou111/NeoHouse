package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps a function's expression temporaries ({@code t$N}) apart from those of every function that
 * calls it. Temps are numbered from 0 in each statement, and a call inside an expression runs while
 * the caller still holds partial results in its temps ({@code p.r = p.a * 2 + sq(p.b)} keeps
 * {@code p.a * 2} in {@code t$0} across the call), so a callee using the same names overwrote them.
 *
 * <p>Each function's temps are shifted to start above every temp its callers (directly or through
 * other calls) can be holding: if {@code f} uses {@code t$0..t$1} and calls {@code g}, {@code g}'s
 * temps become {@code t$2...}. Events, commands, loops and menus are never called, so theirs keep
 * starting at 0. Only compiler temps are renamed. Must run after calls are lowered to
 * {@link VoidFunctionCallNode}s and before bodies are split into helper functions.</p>
 */
public class TempIsolationVisitor extends DefaultVisitor {
    private static final Pattern TEMP = Pattern.compile("t\\$(\\d+)");
    private static final String TEMP_PREFIX = "t$";

    private record DfsFrame(String function, Iterator<String> callees) {}

    private int offset = 0;

    @Override
    public MainNode analyze(MainNode main) {
        Map<String, BodyScan> functions = new HashMap<>();
        List<BodyScan> bodies = new ArrayList<>();
        for (MainStatementNode statement : main.statements) {
            BodyScan scan = new BodyScan();
            statement.accept(scan);
            bodies.add(scan);
            if (statement instanceof FunctionNode function) functions.put(function.name, scan);
        }

        Map<String, Integer> base = computeBases(bodies, functions);

        List<MainStatementNode> result = new ArrayList<>(main.statements.size());
        for (MainStatementNode statement : main.statements) {
            int shift = statement instanceof FunctionNode function ? base.getOrDefault(function.name, 0) : 0;
            if (shift == 0) {
                result.add(statement);
                continue;
            }
            this.offset = shift;
            result.add((MainStatementNode) statement.accept(this));
        }
        return new MainNode(result);
    }

    /** Temp {@code t$N} becomes {@code t$(N + offset)}; every other variable is left alone. */
    @Override
    public ASTNode visitVariable(VariableNode variable) {
        Matcher m = TEMP.matcher(variable.name);
        if (!m.matches()) return variable;
        VariableNode renamed = new VariableNode(variable.type, variable.holder,
                TEMP_PREFIX + (Integer.parseInt(m.group(1)) + this.offset), variable.teamName, variable.token);
        renamed.fallback = variable.fallback;
        renamed.castSuffix = variable.castSuffix;
        return renamed;
    }

    /**
     * The lowest temp number each function may use: at least {@code base(caller) + temps(caller)}
     * for every caller. A longest-path pass over the call graph in topological order; in a call cycle
     * (recursion, already reported) the edge closing the cycle is ignored.
     */
    private static Map<String, Integer> computeBases(List<BodyScan> bodies, Map<String, BodyScan> functions) {
        Map<String, Integer> base = new HashMap<>();
        for (BodyScan body : bodies) raiseCallees(body, 0, functions, base);
        for (String name : topologicalOrder(functions)) raiseCallees(functions.get(name), base.getOrDefault(name, 0), functions, base);
        return base;
    }

    private static void raiseCallees(BodyScan caller, int callerBase, Map<String, BodyScan> functions, Map<String, Integer> base) {
        int needed = callerBase + caller.tempCount;
        for (String callee : caller.callees)
            if (functions.containsKey(callee)) base.merge(callee, needed, Math::max);
    }

    /** Functions with every caller before its callees (reverse DFS post-order; back edges skipped). */
    private static List<String> topologicalOrder(Map<String, BodyScan> functions) {
        List<String> postOrder = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String start : functions.keySet()) {
            if (!visited.add(start)) continue;
            Deque<DfsFrame> stack = new ArrayDeque<>();
            stack.push(new DfsFrame(start, functions.get(start).callees.iterator()));
            while (!stack.isEmpty()) {
                DfsFrame frame = stack.peek();
                if (!frame.callees().hasNext()) {
                    postOrder.add(frame.function());
                    stack.pop();
                    continue;
                }
                String callee = frame.callees().next();
                if (functions.containsKey(callee) && visited.add(callee))
                    stack.push(new DfsFrame(callee, functions.get(callee).callees.iterator()));
            }
        }
        Collections.reverse(postOrder);
        return postOrder;
    }

    /** How many temps a body uses (highest {@code t$N} + 1) and which functions it calls. */
    private static final class BodyScan extends DefaultVisitor {
        private int tempCount = 0;
        private final Set<String> callees = new LinkedHashSet<>();

        @Override
        public ASTNode visitVariable(VariableNode variable) {
            Matcher m = TEMP.matcher(variable.name);
            if (m.matches()) this.tempCount = Math.max(this.tempCount, Integer.parseInt(m.group(1)) + 1);
            return variable;
        }

        @Override
        public ASTNode visitVoidFunctionCall(VoidFunctionCallNode call) {
            this.callees.add(call.functionName);
            return super.visitVoidFunctionCall(call);
        }

        @Override
        public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode call) {
            this.callees.add(call.functionName);
            return super.visitGlobalFunctionCall(call);
        }
    }
}
