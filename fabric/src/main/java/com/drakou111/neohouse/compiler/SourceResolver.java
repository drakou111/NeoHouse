package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.compiler.lexer.Lexer;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.lexer.TokenType;
import com.drakou111.neohouse.exceptions.ParsingException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Lexes the root script and every file it imports into one token stream. Each file is included
 * once, after the files it imports, and its tokens remember which file they came from. Import
 * statements are recognised on tokens, so commented-out imports are ignored.
 */
public final class SourceResolver {
    private static final String SCRIPT_EXTENSION = ".nh";
    private static final String WILDCARD_SUFFIX = "/*";

    private final Path scriptsRoot;
    private final ImportGraph graph;
    private final List<CompileWarning> warnings;
    private final List<Token> output = new ArrayList<>();
    private final Set<String> included = new HashSet<>();
    private final Set<String> inProgress = new LinkedHashSet<>();

    /**
     * @param sourceFile the file being compiled, or {@code null} for an unsaved script (imports are then rejected)
     */
    public SourceResolver(Path sourceFile, List<CompileWarning> warnings) {
        this.warnings = Objects.requireNonNull(warnings);
        if (sourceFile == null) {
            this.scriptsRoot = null;
            this.graph = ImportGraph.singleFile();
        } else {
            Path absolute = sourceFile.toAbsolutePath().normalize();
            this.scriptsRoot = absolute.getParent();
            this.graph = new ImportGraph(absolute.getFileName().toString());
        }
    }

    public ImportGraph getGraph() {
        return this.graph;
    }

    public List<Token> resolve(String rootSource) {
        String root = this.graph.getRootFile();
        this.included.add(root);
        include(root, rootSource, null);

        int eofLine = this.output.isEmpty() ? 1 : this.output.getLast().line;
        this.output.add(new Token(TokenType.EOF, "", "", eofLine, 1, root));
        return this.output;
    }

    private void include(String file, String source, Token rootImportToken) {
        this.inProgress.add(file);
        List<Token> tokens = new Lexer(source, file).tokenize();
        List<Token> body = new ArrayList<>(tokens.size());
        Set<String> importedHere = new HashSet<>();
        int braceDepth = 0;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.type == TokenType.EOF) break;
            if (token.type == TokenType.LBRACE) braceDepth++;
            else if (token.type == TokenType.RBRACE) braceDepth--;

            if (token.type != TokenType.IMPORT) {
                body.add(token);
                continue;
            }
            if (braceDepth > 0) throw new ParsingException(token, "Imports must be at the top level of a file");

            Token pathToken = expect(tokens, i + 1, TokenType.STRING_VALUE, "Expected a quoted file path after 'import'");
            expect(tokens, i + 2, TokenType.SEMI, "Expected ';' after the import path");
            i += 2;

            Token originForChildren = this.graph.isRoot(file) ? token : rootImportToken;
            String pathText = pathToken.text.substring(1, pathToken.text.length() - 1).trim();
            for (String target : expandImport(pathToken, pathText)) {
                if (!importedHere.add(target)) {
                    this.warnings.add(new CompileWarning(pathToken, "'" + target + "' is imported more than once in this file"));
                    continue;
                }
                if (this.inProgress.contains(target) || this.graph.isRoot(target)) {
                    String chain = String.join(" -> ", this.inProgress) + " -> " + target;
                    throw new ParsingException(pathToken, "Circular import: " + chain);
                }
                this.graph.addImport(file, target);
                if (!this.included.add(target)) continue;

                this.graph.setRootImportToken(target, originForChildren);
                include(target, readScript(pathToken, target), originForChildren);
            }
        }

        this.output.addAll(body);
        this.inProgress.remove(file);
    }

    private List<String> expandImport(Token pathToken, String pathText) {
        if (this.scriptsRoot == null)
            throw new ParsingException(pathToken, "Imports only work in a script saved in the scripts folder");

        if (!pathText.endsWith(WILDCARD_SUFFIX)) {
            if (!pathText.endsWith(SCRIPT_EXTENSION))
                throw new ParsingException(pathToken, "Import path must end with '" + SCRIPT_EXTENSION + "' or '" + WILDCARD_SUFFIX + "': \"" + pathText + "\"");
            Path target = this.scriptsRoot.resolve(pathText).normalize();
            if (!Files.isRegularFile(target)) throw new ParsingException(pathToken, "Import not found: \"" + pathText + "\"");
            return List.of(relativeName(target));
        }

        String directoryText = pathText.substring(0, pathText.length() - 1);
        Path directory = this.scriptsRoot.resolve(directoryText).normalize();
        if (!Files.isDirectory(directory))
            throw new ParsingException(pathToken, "Wildcard import directory not found: \"" + directoryText + "\"");

        List<String> targets = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path candidate : files.sorted().toList()) {
                if (!candidate.getFileName().toString().endsWith(SCRIPT_EXTENSION)) continue;
                String name = relativeName(candidate);
                if (!this.graph.isRoot(name)) targets.add(name);
            }
        } catch (IOException e) {
            throw new ParsingException(pathToken, "Cannot read directory for wildcard import \"" + pathText + "\": " + e.getMessage());
        }
        return targets;
    }

    private String readScript(Token pathToken, String relativeName) {
        try {
            return Files.readString(this.scriptsRoot.resolve(relativeName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ParsingException(pathToken, "Cannot read import \"" + relativeName + "\": " + e.getMessage());
        }
    }

    private String relativeName(Path file) {
        return this.scriptsRoot.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static Token expect(List<Token> tokens, int index, TokenType type, String message) {
        Token token = tokens.get(Math.min(index, tokens.size() - 1));
        if (token.type != type) throw new ParsingException(token, message);
        return token;
    }
}
