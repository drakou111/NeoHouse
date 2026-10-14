package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.config.MultiCallMode;

import com.drakou111.neohouse.compiler.ast.expr.*;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.main.*;
import com.drakou111.neohouse.compiler.ast.main_statements.*;
import com.drakou111.neohouse.compiler.ast.statement.*;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.*;

/**
 * Detects and optionally fixes functions called more than once in the same
 * execution context (directly or transitively).
 *
 * WARN mode: throws a SemanticException on the first detected multi-call.
 * FIX  mode: creates duplicate function copies ($0, $1, …) to avoid the problem.
 */
public class FunctionDeduplicationVisitor {

    private final MultiCallMode mode;

    // All function definitions found in the main node
    private final Map<String, VoidFunctionNode>  voidDefs  = new LinkedHashMap<>();
    private final Map<String, TypedFunctionNode> typedDefs = new LinkedHashMap<>();

    // For FIX mode: extra copies to append after rewriting
    private final List<VoidFunctionNode>  extraVoidCopies  = new ArrayList<>();
    private final List<TypedFunctionNode> extraTypedCopies = new ArrayList<>();

    public FunctionDeduplicationVisitor(MultiCallMode mode) {
        this.mode = mode;
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public MainNode analyze(MainNode main) {
        if (mode == MultiCallMode.OFF) return main;

        // Collect all function definitions
        for (MainStatementNode stmt : main.statements) {
            if (stmt instanceof VoidFunctionNode vf)  voidDefs.put(vf.name, vf);
            else if (stmt instanceof TypedFunctionNode tf) typedDefs.put(tf.name, tf);
        }

        // Build transitive call profiles for each function
        // profile(F) = Map<callee, total times called when F runs>
        Map<String, Map<String, Integer>> profiles = buildTransitiveProfiles();

        if (mode == MultiCallMode.WARN) {
            warnPass(main, profiles);
            return main;
        }

        // FIX mode
        return fixPass(main, profiles);
    }

    // -------------------------------------------------------------------------
    // Transitive profile computation
    // -------------------------------------------------------------------------

    /**
     * Computes for every defined function F a Map<calleeName, count> representing
     * how many times each callee (direct or transitive) is invoked when F runs once.
     *
     * We compute this iteratively until stable (handles transitive dependencies).
     */
    private Map<String, Map<String, Integer>> buildTransitiveProfiles() {
        // Direct profiles are never mutated during expansion
        Map<String, Map<String, Integer>> directProfiles = new HashMap<>();
        for (VoidFunctionNode vf : voidDefs.values()) directProfiles.put(vf.name, directCallProfile(vf.statements));
        for (TypedFunctionNode tf : typedDefs.values()) directProfiles.put(tf.name, directCallProfile(tf.statements));

        // Initialize transitive profiles as copies of direct
        Map<String, Map<String, Integer>> transitiveProfiles = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> e : directProfiles.entrySet())
            transitiveProfiles.put(e.getKey(), new HashMap<>(e.getValue()));

        // transitive[F] = direct[F] + sum_{C in direct[F]} direct[F][C] * transitive[C]
        // Iterate until stable. Always expand from direct (not from current transitive)
        // so counts never grow unboundedly across iterations.
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String fn : directProfiles.keySet()) {
                Map<String, Integer> direct = directProfiles.get(fn);
                Map<String, Integer> expanded = new HashMap<>(direct);
                for (Map.Entry<String, Integer> e : direct.entrySet()) {
                    String callee = e.getKey();
                    int callCount = e.getValue();
                    Map<String, Integer> calleeTransitive = transitiveProfiles.get(callee);
                    if (calleeTransitive == null) continue;
                    for (Map.Entry<String, Integer> ce : calleeTransitive.entrySet()) {
                        if (!ce.getKey().equals(fn))
                            expanded.merge(ce.getKey(), ce.getValue() * callCount, Integer::sum);
                    }
                }
                expanded.remove(fn);
                if (!expanded.equals(transitiveProfiles.get(fn))) {
                    transitiveProfiles.put(fn, expanded);
                    changed = true;
                }
            }
        }

        return transitiveProfiles;
    }

    /**
     * Computes the direct (non-transitive) call profile of a statement list.
     * For sequential statements, counts are summed.
     * For conditional/random branches, both branches are summed (conservative).
     * For repeat, the body is multiplied by the count.
     */
    private Map<String, Integer> directCallProfile(StatementListNode statements) {
        Map<String, Integer> result = new HashMap<>();
        for (StatementNode stmt : statements.statements) {
            addProfileFromStatement(stmt, result);
        }
        return result;
    }

    private void addProfileFromStatement(StatementNode stmt, Map<String, Integer> result) {
        if (stmt instanceof VoidFunctionCallNode call) {
            result.merge(call.functionName, 1, Integer::sum);
        } else if (stmt instanceof GlobalFunctionCallNode call) {
            result.merge(call.functionName, 1, Integer::sum);
        } else if (stmt instanceof ConditionalNode cond) {
            // Sum both branches conservatively
            Map<String, Integer> thenP = directCallProfile(cond.thenBlock);
            Map<String, Integer> elseP = cond.elseBlock != null
                    ? directCallProfile(cond.elseBlock)
                    : new HashMap<>();
            for (String k : thenP.keySet()) result.merge(k, thenP.get(k), Integer::sum);
            for (String k : elseP.keySet())  result.merge(k, elseP.get(k), Integer::sum);
        } else if (stmt instanceof RandomNode rand) {
            // Sum all branches conservatively
            for (StatementListNode branch : rand.statements) {
                Map<String, Integer> bp = directCallProfile(branch);
                for (String k : bp.keySet()) result.merge(k, bp.get(k), Integer::sum);
            }
        } else if (stmt instanceof RepeatNode repeat) {
            Map<String, Integer> inner = directCallProfile(repeat.statements);
            for (String k : inner.keySet()) result.merge(k, (int)(inner.get(k) * repeat.count), Integer::sum);
        } else if (stmt instanceof AssignationNode assign) {
            addCallsFromExpr(assign.value, result);
        } else if (stmt instanceof TypedReturnNode ret && ret.expression != null) {
            addCallsFromExpr(ret.expression, result);
        } else if (stmt instanceof ActionCallNode action) {
            for (var arg : action.arguments.expressions) addCallsFromExpr(arg, result);
        }
    }

    /** Recursively collects FunctionCallNode names from an expression. */
    private void addCallsFromExpr(ExpressionNode expr, Map<String, Integer> result) {
        if (expr instanceof FunctionCallNode call) {
            result.merge(call.functionName, 1, Integer::sum);
            for (var arg : call.arguments.expressions) addCallsFromExpr(arg, result);
        } else if (expr instanceof BinaryExpressionNode bin) {
            addCallsFromExpr(bin.left, result);
            addCallsFromExpr(bin.right, result);
        } else if (expr instanceof UnaryExpressionNode un) {
            addCallsFromExpr(un.operand, result);
        }
    }

    // -------------------------------------------------------------------------
    // WARN pass
    // -------------------------------------------------------------------------

    private void warnPass(MainNode main, Map<String, Map<String, Integer>> profiles) {
        // Check each root body and function body
        for (MainStatementNode stmt : main.statements) {
            Map<String, Integer> bodyProfile;
            com.drakou111.neohouse.compiler.lexer.Token tok = stmt.token;

            if (stmt instanceof EventNode ev) {
                bodyProfile = computeBodyProfile(ev.statements, profiles);
            } else if (stmt instanceof CommandNode cmd) {
                bodyProfile = computeBodyProfile(cmd.statements, profiles);
            } else if (stmt instanceof LoopNode loop) {
                bodyProfile = computeBodyProfile(loop.statements, profiles);
            } else if (stmt instanceof VoidFunctionNode vf) {
                bodyProfile = computeBodyProfile(vf.statements, profiles);
                tok = vf.token;
            } else if (stmt instanceof TypedFunctionNode tf) {
                bodyProfile = computeBodyProfile(tf.statements, profiles);
                tok = tf.token;
            } else if (stmt instanceof MenuNode menu) {
                for (SlotNode slot : menu.slots) {
                    Map<String, Integer> slotProfile = computeBodyProfile(slot.statements, profiles);
                    for (Map.Entry<String, Integer> e : slotProfile.entrySet()) {
                        if (e.getValue() > 1) {
                            throw new SemanticException(slot.token,
                                    "Function '" + e.getKey() + "' is called " + e.getValue() +
                                    " times in the same execution context (multi-call). " +
                                    "Set multiCallMode to FIX to auto-resolve, or restructure your code.");
                        }
                    }
                }
                continue;
            } else {
                continue;
            }

            for (Map.Entry<String, Integer> e : bodyProfile.entrySet()) {
                if (e.getValue() > 1) {
                    throw new SemanticException(tok,
                            "Function '" + e.getKey() + "' is called " + e.getValue() +
                            " times in the same execution context (multi-call). " +
                            "Set multiCallMode to FIX to auto-resolve, or restructure your code.");
                }
            }
        }
    }

    /**
     * Computes the total call profile of a body (direct + transitive expansion).
     * For branching structures (conditional/random) we take the max of branches
     * to get the worst case. This is used for WARN-mode analysis.
     */
    private Map<String, Integer> computeBodyProfile(StatementListNode stmts,
                                                     Map<String, Map<String, Integer>> profiles) {
        Map<String, Integer> result = new HashMap<>();
        for (StatementNode stmt : stmts.statements) {
            addTransitiveFromStatement(stmt, profiles, result);
        }
        return result;
    }

    private void addTransitiveFromStatement(StatementNode stmt,
                                             Map<String, Map<String, Integer>> profiles,
                                             Map<String, Integer> counters) {
        if (stmt instanceof VoidFunctionCallNode call) {
            counters.merge(call.functionName, 1, Integer::sum);
            // Add transitive calls
            Map<String, Integer> fp = profiles.get(call.functionName);
            if (fp != null) {
                for (Map.Entry<String, Integer> e : fp.entrySet()) {
                    counters.merge(e.getKey(), e.getValue(), Integer::sum);
                }
            }
        } else if (stmt instanceof GlobalFunctionCallNode call) {
            counters.merge(call.functionName, 1, Integer::sum);
            Map<String, Integer> fp = profiles.get(call.functionName);
            if (fp != null) {
                for (Map.Entry<String, Integer> e : fp.entrySet()) {
                    counters.merge(e.getKey(), e.getValue(), Integer::sum);
                }
            }
        } else if (stmt instanceof ConditionalNode cond) {
            Map<String, Integer> thenC = new HashMap<>(counters);
            for (StatementNode s : cond.thenBlock.statements) addTransitiveFromStatement(s, profiles, thenC);
            Map<String, Integer> elseC = new HashMap<>(counters);
            if (cond.elseBlock != null) {
                for (StatementNode s : cond.elseBlock.statements) addTransitiveFromStatement(s, profiles, elseC);
            }
            // Take max of both branches for each key
            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(thenC.keySet());
            allKeys.addAll(elseC.keySet());
            for (String k : allKeys) {
                int tv = thenC.getOrDefault(k, counters.getOrDefault(k, 0));
                int ev = elseC.getOrDefault(k, counters.getOrDefault(k, 0));
                counters.put(k, Math.max(tv, ev));
            }
        } else if (stmt instanceof RandomNode rand) {
            List<Map<String, Integer>> branchResults = new ArrayList<>();
            for (StatementListNode branch : rand.statements) {
                Map<String, Integer> bc = new HashMap<>(counters);
                for (StatementNode s : branch.statements) addTransitiveFromStatement(s, profiles, bc);
                branchResults.add(bc);
            }
            Set<String> allKeys = new HashSet<>();
            for (Map<String, Integer> bc : branchResults) allKeys.addAll(bc.keySet());
            for (String k : allKeys) {
                int maxV = 0;
                for (Map<String, Integer> bc : branchResults) maxV = Math.max(maxV, bc.getOrDefault(k, counters.getOrDefault(k, 0)));
                counters.put(k, maxV);
            }
        } else if (stmt instanceof RepeatNode repeat) {
            Map<String, Integer> iterCounters = new HashMap<>(counters);
            for (StatementNode s : repeat.statements.statements) {
                addTransitiveFromStatement(s, profiles, iterCounters);
            }
            for (Map.Entry<String, Integer> e : iterCounters.entrySet()) {
                long scaled = (long) e.getValue() * repeat.count;
                counters.put(e.getKey(), (int) Math.min(scaled, Integer.MAX_VALUE));
            }
        } else if (stmt instanceof AssignationNode assign) {
            addTransitiveFromExpr(assign.value, profiles, counters);
        } else if (stmt instanceof TypedReturnNode ret && ret.expression != null) {
            addTransitiveFromExpr(ret.expression, profiles, counters);
        } else if (stmt instanceof ActionCallNode action) {
            for (var arg : action.arguments.expressions) addTransitiveFromExpr(arg, profiles, counters);
        }
    }

    private void addTransitiveFromExpr(ExpressionNode expr,
                                        Map<String, Map<String, Integer>> profiles,
                                        Map<String, Integer> counters) {
        if (expr instanceof FunctionCallNode call) {
            counters.merge(call.functionName, 1, Integer::sum);
            Map<String, Integer> fp = profiles.get(call.functionName);
            if (fp != null) {
                for (Map.Entry<String, Integer> e : fp.entrySet()) {
                    counters.merge(e.getKey(), e.getValue(), Integer::sum);
                }
            }
            for (var arg : call.arguments.expressions) addTransitiveFromExpr(arg, profiles, counters);
        } else if (expr instanceof BinaryExpressionNode bin) {
            addTransitiveFromExpr(bin.left, profiles, counters);
            addTransitiveFromExpr(bin.right, profiles, counters);
        } else if (expr instanceof UnaryExpressionNode un) {
            addTransitiveFromExpr(un.operand, profiles, counters);
        }
    }

    // -------------------------------------------------------------------------
    // FIX pass
    // -------------------------------------------------------------------------

    private MainNode fixPass(MainNode main, Map<String, Map<String, Integer>> profiles) {
        // Step 1: determine how many copies of each function are needed globally
        // For each root body (and each function body), compute how many extra copies are needed
        Map<String, Integer> maxExtraCopies = new HashMap<>();

        for (MainStatementNode stmt : main.statements) {
            if (stmt instanceof MenuNode menu) {
                for (SlotNode slot : menu.slots) {
                    collectExtraCopiesNeeded(slot.statements, profiles, maxExtraCopies);
                }
            } else if (stmt instanceof EventNode ev) {
                collectExtraCopiesNeeded(ev.statements, profiles, maxExtraCopies);
            } else if (stmt instanceof CommandNode cmd) {
                collectExtraCopiesNeeded(cmd.statements, profiles, maxExtraCopies);
            } else if (stmt instanceof LoopNode loop) {
                collectExtraCopiesNeeded(loop.statements, profiles, maxExtraCopies);
            } else if (stmt instanceof VoidFunctionNode vf) {
                collectExtraCopiesNeeded(vf.statements, profiles, maxExtraCopies);
            } else if (stmt instanceof TypedFunctionNode tf) {
                collectExtraCopiesNeeded(tf.statements, profiles, maxExtraCopies);
            }
        }

        // Remove functions that don't need extra copies
        maxExtraCopies.entrySet().removeIf(e -> e.getValue() <= 0);

        if (maxExtraCopies.isEmpty()) return main;

        // Step 2: create copies for void functions
        // (typed functions can't be global-called so they're less likely to need copies,
        //  but we handle them too)
        Map<String, List<String>> copyNames = new HashMap<>(); // original -> [copy0, copy1, ...]

        for (Map.Entry<String, Integer> e : maxExtraCopies.entrySet()) {
            String baseName = e.getKey();
            int numCopies   = e.getValue();
            List<String> names = new ArrayList<>();
            for (int i = 0; i < numCopies; i++) {
                String copyBase = baseName.length() > 47 ? baseName.substring(0, 47) : baseName;
                String copyName = copyBase + "$" + i;
                names.add(copyName);
            }
            copyNames.put(baseName, names);

            // Create the actual copy nodes
            if (voidDefs.containsKey(baseName)) {
                VoidFunctionNode orig = voidDefs.get(baseName);
                for (String copyName : names) {
                    VoidFunctionNode copy = new VoidFunctionNode(
                            copyName, orig.description, orig.icon, orig.parameters, orig.statements, orig.token);
                    extraVoidCopies.add(copy);
                }
            } else if (typedDefs.containsKey(baseName)) {
                TypedFunctionNode orig = typedDefs.get(baseName);
                for (String copyName : names) {
                    TypedFunctionNode copy = new TypedFunctionNode(
                            copyName, orig.description, orig.icon, orig.returnType, orig.parameters,
                            orig.statements, orig.token);
                    extraTypedCopies.add(copy);
                }
            }
        }

        // Step 3: rewrite each body using copy names
        List<MainStatementNode> rewritten = new ArrayList<>();
        for (MainStatementNode stmt : main.statements) {
            rewritten.add(rewriteMainStatement(stmt, copyNames, profiles));
        }

        // Append extra copies to the main statement list
        rewritten.addAll(extraVoidCopies);
        rewritten.addAll(extraTypedCopies);

        return new MainNode(rewritten);
    }

    private void collectExtraCopiesNeeded(StatementListNode stmts,
                                           Map<String, Map<String, Integer>> profiles,
                                           Map<String, Integer> maxExtraCopies) {
        // Simulate the counter-based walk to find max call count for each function
        Map<String, Integer> counters = new HashMap<>();
        simulateWalk(stmts, profiles, counters, maxExtraCopies);
    }

    /**
     * Walks statements tracking call counts per function (including transitive).
     * Records how many extra copies (beyond the first) are needed.
     */
    private void simulateWalk(StatementListNode stmts,
                               Map<String, Map<String, Integer>> profiles,
                               Map<String, Integer> counters,
                               Map<String, Integer> maxExtraCopies) {
        for (StatementNode stmt : stmts.statements) {
            simulateStatement(stmt, profiles, counters, maxExtraCopies);
        }
    }

    private void simulateStatement(StatementNode stmt,
                                    Map<String, Map<String, Integer>> profiles,
                                    Map<String, Integer> counters,
                                    Map<String, Integer> maxExtraCopies) {
        if (stmt instanceof VoidFunctionCallNode call) {
            int cnt = counters.merge(call.functionName, 1, Integer::sum);
            if (cnt > 1) {
                maxExtraCopies.merge(call.functionName, cnt - 1, Integer::max);
            }
            // Commit transitive calls
            Map<String, Integer> fp = profiles.get(call.functionName);
            if (fp != null) {
                for (Map.Entry<String, Integer> e : fp.entrySet()) {
                    int tc = counters.merge(e.getKey(), e.getValue(), Integer::sum);
                    if (tc > 1) {
                        maxExtraCopies.merge(e.getKey(), tc - 1, Integer::max);
                    }
                }
            }
        } else if (stmt instanceof GlobalFunctionCallNode call) {
            int cnt = counters.merge(call.functionName, 1, Integer::sum);
            if (cnt > 1) {
                maxExtraCopies.merge(call.functionName, cnt - 1, Integer::max);
            }
            Map<String, Integer> fp = profiles.get(call.functionName);
            if (fp != null) {
                for (Map.Entry<String, Integer> e : fp.entrySet()) {
                    int tc = counters.merge(e.getKey(), e.getValue(), Integer::sum);
                    if (tc > 1) {
                        maxExtraCopies.merge(e.getKey(), tc - 1, Integer::max);
                    }
                }
            }
        } else if (stmt instanceof ConditionalNode cond) {
            // Process both branches from same snapshot, take max after
            Map<String, Integer> thenC = new HashMap<>(counters);
            simulateWalk(cond.thenBlock, profiles, thenC, maxExtraCopies);
            Map<String, Integer> elseC = new HashMap<>(counters);
            if (cond.elseBlock != null) simulateWalk(cond.elseBlock, profiles, elseC, maxExtraCopies);
            // Take max of both branches for each key
            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(thenC.keySet());
            allKeys.addAll(elseC.keySet());
            for (String k : allKeys) {
                counters.put(k, Math.max(
                        thenC.getOrDefault(k, 0),
                        elseC.getOrDefault(k, 0)));
            }
        } else if (stmt instanceof RandomNode rand) {
            List<Map<String, Integer>> branchCounters = new ArrayList<>();
            for (StatementListNode branch : rand.statements) {
                Map<String, Integer> bc = new HashMap<>(counters);
                simulateWalk(branch, profiles, bc, maxExtraCopies);
                branchCounters.add(bc);
            }
            Set<String> allKeys = new HashSet<>();
            for (Map<String, Integer> bc : branchCounters) allKeys.addAll(bc.keySet());
            for (String k : allKeys) {
                int maxV = 0;
                for (Map<String, Integer> bc : branchCounters) maxV = Math.max(maxV, bc.getOrDefault(k, 0));
                counters.put(k, maxV);
            }
        } else if (stmt instanceof RepeatNode repeat) {
            // Simulate body once and scale counts — avoids O(count) loop for large repeats
            Map<String, Integer> iterCounters = new HashMap<>(counters);
            simulateWalk(repeat.statements, profiles, iterCounters, maxExtraCopies);
            for (Map.Entry<String, Integer> e : iterCounters.entrySet()) {
                long scaled = (long) e.getValue() * repeat.count;
                counters.put(e.getKey(), (int) Math.min(scaled, Integer.MAX_VALUE));
            }
        } else if (stmt instanceof AssignationNode assign) {
            simulateExpr(assign.value, profiles, counters, maxExtraCopies);
        } else if (stmt instanceof TypedReturnNode ret && ret.expression != null) {
            simulateExpr(ret.expression, profiles, counters, maxExtraCopies);
        } else if (stmt instanceof ActionCallNode action) {
            for (var arg : action.arguments.expressions) simulateExpr(arg, profiles, counters, maxExtraCopies);
        }
    }

    private void simulateExpr(ExpressionNode expr,
                               Map<String, Map<String, Integer>> profiles,
                               Map<String, Integer> counters,
                               Map<String, Integer> maxExtraCopies) {
        if (expr instanceof FunctionCallNode call) {
            int cnt = counters.merge(call.functionName, 1, Integer::sum);
            if (cnt > 1) maxExtraCopies.merge(call.functionName, cnt - 1, Integer::max);
            Map<String, Integer> fp = profiles.get(call.functionName);
            if (fp != null) {
                for (Map.Entry<String, Integer> e : fp.entrySet()) {
                    int tc = counters.merge(e.getKey(), e.getValue(), Integer::sum);
                    if (tc > 1) maxExtraCopies.merge(e.getKey(), tc - 1, Integer::max);
                }
            }
            for (var arg : call.arguments.expressions) simulateExpr(arg, profiles, counters, maxExtraCopies);
        } else if (expr instanceof BinaryExpressionNode bin) {
            simulateExpr(bin.left, profiles, counters, maxExtraCopies);
            simulateExpr(bin.right, profiles, counters, maxExtraCopies);
        } else if (expr instanceof UnaryExpressionNode un) {
            simulateExpr(un.operand, profiles, counters, maxExtraCopies);
        }
    }

    // -------------------------------------------------------------------------
    // Rewriting (replacing 2nd, 3rd, ... calls with copies)
    // -------------------------------------------------------------------------

    private MainStatementNode rewriteMainStatement(MainStatementNode stmt,
                                                    Map<String, List<String>> copyNames,
                                                    Map<String, Map<String, Integer>> profiles) {
        if (stmt instanceof EventNode ev) {
            StatementListNode body = rewriteBody(ev.statements, copyNames, profiles);
            return new EventNode(ev.event, body, ev.token);
        } else if (stmt instanceof CommandNode cmd) {
            StatementListNode body = rewriteBody(cmd.statements, copyNames, profiles);
            return new CommandNode(cmd.name, cmd.targeted, cmd.priority, cmd.listed, body, cmd.token);
        } else if (stmt instanceof LoopNode loop) {
            StatementListNode body = rewriteBody(loop.statements, copyNames, profiles);
            return new LoopNode(loop.name, loop.description, loop.icon, body, loop.tickDelay, loop.token);
        } else if (stmt instanceof MenuNode menu) {
            List<SlotNode> slots = new ArrayList<>();
            for (SlotNode slot : menu.slots) {
                StatementListNode body = rewriteBody(slot.statements, copyNames, profiles);
                slots.add(new SlotNode(slot.slotId, slot.item, body, slot.token));
            }
            return new MenuNode(menu.name, menu.rows, slots, menu.token);
        } else if (stmt instanceof VoidFunctionNode vf) {
            StatementListNode body = rewriteBody(vf.statements, copyNames, profiles);
            return new VoidFunctionNode(vf.name, vf.description, vf.icon, vf.parameters, body, vf.token);
        } else if (stmt instanceof TypedFunctionNode tf) {
            StatementListNode body = rewriteBody(tf.statements, copyNames, profiles);
            return new TypedFunctionNode(tf.name, tf.description, tf.icon, tf.returnType, tf.parameters, body, tf.token);
        }
        return stmt;
    }

    private StatementListNode rewriteBody(StatementListNode stmts,
                                           Map<String, List<String>> copyNames,
                                           Map<String, Map<String, Integer>> profiles) {
        // Pre-scan: compute total calls (direct + transitive) vs direct-only counts.
        // If a function F has transitive callers that push its total count above 1,
        // we can't rename the transitive calls (they're inside other functions), so we
        // must rename all DIRECT calls to copies, leaving the original for transitive use.
        Map<String, Integer> preScanTotals = new HashMap<>();
        Map<String, Integer> directCounts  = new HashMap<>();
        for (StatementNode stmt : stmts.statements) {
            addTransitiveFromStatement(stmt, profiles, preScanTotals);
            addProfileFromStatement(stmt, directCounts);
        }
        // useCopyFromFirstCall: functions whose copies start at callIdx=0 (not callIdx=1)
        Set<String> useCopyFromFirstCall = new HashSet<>();
        for (String fname : preScanTotals.keySet()) {
            if (preScanTotals.get(fname) > directCounts.getOrDefault(fname, 0)) {
                useCopyFromFirstCall.add(fname);
            }
        }
        Map<String, Integer> counters = new HashMap<>();
        return rewriteStatements(stmts, copyNames, profiles, counters, useCopyFromFirstCall);
    }

    /**
     * Resolves which function name to actually call, given how many times it has already
     * been called directly in this body (callIdx) and whether transitive conflicts mean
     * we must use copies starting from the very first call.
     */
    private String resolveCallTarget(String fname, int callIdx,
                                      Map<String, List<String>> copyNames,
                                      Set<String> useCopyFromFirstCall) {
        List<String> copies = copyNames.get(fname);
        if (copies == null) return fname;
        if (useCopyFromFirstCall.contains(fname)) {
            // Transitive conflict: direct calls use copies ($0, $1, ...), original left for transitive
            if (callIdx < copies.size()) return copies.get(callIdx);
        } else if (callIdx > 0) {
            // Multiple direct calls: original for first, copies ($0, $1, ...) for subsequent
            if (callIdx - 1 < copies.size()) return copies.get(callIdx - 1);
        }
        return fname;
    }

    private StatementListNode rewriteStatements(StatementListNode stmts,
                                                 Map<String, List<String>> copyNames,
                                                 Map<String, Map<String, Integer>> profiles,
                                                 Map<String, Integer> counters,
                                                 Set<String> useCopyFromFirstCall) {
        List<StatementNode> result = new ArrayList<>();
        for (StatementNode stmt : stmts.statements) {
            result.add(rewriteStatement(stmt, copyNames, profiles, counters, useCopyFromFirstCall));
        }
        return new StatementListNode(result);
    }

    private StatementNode rewriteStatement(StatementNode stmt,
                                            Map<String, List<String>> copyNames,
                                            Map<String, Map<String, Integer>> profiles,
                                            Map<String, Integer> counters,
                                            Set<String> useCopyFromFirstCall) {
        if (stmt instanceof VoidFunctionCallNode call) {
            String fname = call.functionName;
            int callIdx = counters.getOrDefault(fname, 0);
            counters.put(fname, callIdx + 1);
            Map<String, Integer> fp = profiles.get(fname);
            if (fp != null) {
                for (Map.Entry<String, Integer> e : fp.entrySet()) {
                    counters.merge(e.getKey(), e.getValue(), Integer::sum);
                }
            }
            String target = resolveCallTarget(fname, callIdx, copyNames, useCopyFromFirstCall);
            if (!target.equals(fname)) return new VoidFunctionCallNode(target, call.arguments, call.token);
            return call;

        } else if (stmt instanceof GlobalFunctionCallNode call) {
            String fname = call.functionName;
            int callIdx = counters.getOrDefault(fname, 0);
            counters.put(fname, callIdx + 1);
            Map<String, Integer> fp = profiles.get(fname);
            if (fp != null) {
                for (Map.Entry<String, Integer> e : fp.entrySet()) {
                    counters.merge(e.getKey(), e.getValue(), Integer::sum);
                }
            }
            String target = resolveCallTarget(fname, callIdx, copyNames, useCopyFromFirstCall);
            if (!target.equals(fname)) return new GlobalFunctionCallNode(target, call.arguments, call.token);
            return call;

        } else if (stmt instanceof ConditionalNode cond) {
            Map<String, Integer> thenC = new HashMap<>(counters);
            StatementListNode thenBlock = rewriteStatements(cond.thenBlock, copyNames, profiles, thenC, useCopyFromFirstCall);
            Map<String, Integer> elseC = new HashMap<>(counters);
            StatementListNode elseBlock = null;
            if (cond.elseBlock != null) {
                elseBlock = rewriteStatements(cond.elseBlock, copyNames, profiles, elseC, useCopyFromFirstCall);
            }
            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(thenC.keySet());
            allKeys.addAll(elseC.keySet());
            for (String k : allKeys) {
                counters.put(k, Math.max(thenC.getOrDefault(k, 0), elseC.getOrDefault(k, 0)));
            }
            return new ConditionalNode(cond.condition, thenBlock, elseBlock, cond.token);

        } else if (stmt instanceof RandomNode rand) {
            List<Map<String, Integer>> branchCounters = new ArrayList<>();
            List<StatementListNode> rewrittenBranches = new ArrayList<>();
            for (StatementListNode branch : rand.statements) {
                Map<String, Integer> bc = new HashMap<>(counters);
                rewrittenBranches.add(rewriteStatements(branch, copyNames, profiles, bc, useCopyFromFirstCall));
                branchCounters.add(bc);
            }
            Set<String> allKeys = new HashSet<>();
            for (Map<String, Integer> bc : branchCounters) allKeys.addAll(bc.keySet());
            for (String k : allKeys) {
                int maxV = 0;
                for (Map<String, Integer> bc : branchCounters) maxV = Math.max(maxV, bc.getOrDefault(k, 0));
                counters.put(k, maxV);
            }
            return new RandomNode(rewrittenBranches, rand.weights, rand.token);

        } else if (stmt instanceof RepeatNode repeat) {
            StatementListNode body = rewriteStatements(repeat.statements, copyNames, profiles, counters, useCopyFromFirstCall);
            return new RepeatNode(repeat.count, body, repeat.token);

        } else if (stmt instanceof AssignationNode assign) {
            NormalExpressionNode newVal = rewriteExpr(assign.value, copyNames, profiles, counters, useCopyFromFirstCall);
            if (newVal != assign.value) return new AssignationNode(assign.variable, assign.operator, newVal, assign.token);
            return assign;

        } else if (stmt instanceof TypedReturnNode ret && ret.expression != null) {
            NormalExpressionNode newExpr = rewriteExpr(ret.expression, copyNames, profiles, counters, useCopyFromFirstCall);
            if (newExpr != ret.expression) return new TypedReturnNode(newExpr, ret.token);
            return ret;

        } else if (stmt instanceof ActionCallNode action) {
            List<NormalExpressionNode> newArgs = new ArrayList<>(action.arguments.expressions);
            boolean changed = false;
            for (int i = 0; i < newArgs.size(); i++) {
                NormalExpressionNode rewritten = rewriteExpr(newArgs.get(i), copyNames, profiles, counters, useCopyFromFirstCall);
                if (rewritten != newArgs.get(i)) { newArgs.set(i, rewritten); changed = true; }
            }
            if (changed) return new ActionCallNode(action.name, new ArgumentListNode(newArgs), action.token);
            return action;
        }

        return stmt;
    }

    private NormalExpressionNode rewriteExpr(NormalExpressionNode expr,
                                              Map<String, List<String>> copyNames,
                                              Map<String, Map<String, Integer>> profiles,
                                              Map<String, Integer> counters,
                                              Set<String> useCopyFromFirstCall) {
        if (expr instanceof FunctionCallNode call) {
            String fname = call.functionName;
            int callIdx = counters.getOrDefault(fname, 0);
            counters.put(fname, callIdx + 1);
            Map<String, Integer> fp = profiles.get(fname);
            if (fp != null) {
                for (Map.Entry<String, Integer> e : fp.entrySet()) {
                    counters.merge(e.getKey(), e.getValue(), Integer::sum);
                }
            }
            List<NormalExpressionNode> newArgs = new ArrayList<>(call.arguments.expressions);
            boolean argsChanged = false;
            for (int i = 0; i < newArgs.size(); i++) {
                NormalExpressionNode rw = rewriteExpr(newArgs.get(i), copyNames, profiles, counters, useCopyFromFirstCall);
                if (rw != newArgs.get(i)) { newArgs.set(i, rw); argsChanged = true; }
            }
            String target = resolveCallTarget(fname, callIdx, copyNames, useCopyFromFirstCall);
            if (!target.equals(fname)) {
                return new FunctionCallNode(target, new ArgumentListNode(newArgs), call.type, call.token);
            }
            if (argsChanged) return new FunctionCallNode(fname, new ArgumentListNode(newArgs), call.type, call.token);
            return call;

        } else if (expr instanceof BinaryExpressionNode bin) {
            NormalExpressionNode left  = rewriteExpr(bin.left,  copyNames, profiles, counters, useCopyFromFirstCall);
            NormalExpressionNode right = rewriteExpr(bin.right, copyNames, profiles, counters, useCopyFromFirstCall);
            if (left != bin.left || right != bin.right)
                return new BinaryExpressionNode(left, right, bin.operator, bin.type, bin.token);
            return bin;

        } else if (expr instanceof UnaryExpressionNode un) {
            NormalExpressionNode inner = rewriteExpr(un.operand, copyNames, profiles, counters, useCopyFromFirstCall);
            if (inner != un.operand)
                return new UnaryExpressionNode(un.operator, inner, un.type, un.token);
            return un;
        }

        return expr;
    }
}
