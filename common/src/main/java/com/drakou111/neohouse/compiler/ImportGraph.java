package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.lexer.Token;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which script file imports which. A file can use the definitions of every file it imports,
 * directly or transitively, but never those of a file that merely imports it.
 */
public final class ImportGraph {
    private final String rootFile;
    private final Map<String, Set<String>> directImports = new HashMap<>();
    private final Map<String, Token> rootImportTokens = new HashMap<>();
    private final Map<String, Set<String>> visibleCache = new HashMap<>();

    public ImportGraph(String rootFile) {
        this.rootFile = rootFile;
    }

    /** A graph for a script with no imports, where every token belongs to {@link Token#UNNAMED_FILE}. */
    public static ImportGraph singleFile() {
        return new ImportGraph(Token.UNNAMED_FILE);
    }

    public String getRootFile() {
        return this.rootFile;
    }

    public boolean isRoot(String file) {
        return this.rootFile.equals(file);
    }

    void addImport(String importer, String imported) {
        this.directImports.computeIfAbsent(importer, k -> new LinkedHashSet<>()).add(imported);
        this.visibleCache.clear();
    }

    void setRootImportToken(String file, Token importToken) {
        this.rootImportTokens.putIfAbsent(file, importToken);
    }

    /**
     * The {@code import} statement in the root file through which {@code file} was pulled in,
     * so problems inside imported files can be pointed at from the file being edited.
     */
    public Optional<Token> getRootImportToken(String file) {
        return Optional.ofNullable(this.rootImportTokens.get(file));
    }

    public boolean canSee(String fromFile, String declaringFile) {
        if (fromFile.equals(declaringFile)) return true;
        return this.visibleCache.computeIfAbsent(fromFile, this::collectReachable).contains(declaringFile);
    }

    private Set<String> collectReachable(String start) {
        Set<String> reachable = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.push(start);
        while (!pending.isEmpty()) {
            String file = pending.pop();
            for (String imported : this.directImports.getOrDefault(file, Set.of()))
                if (reachable.add(imported)) pending.push(imported);
        }
        return reachable;
    }
}
