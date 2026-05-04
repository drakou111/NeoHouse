package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Preprocessor {
    private static final Pattern IMPORT_PAT = Pattern.compile(
        "import\\s+\"([^\"]+)\"\\s*;"
    );

    private final Path scriptsRoot; // null if not running from a file (imports disabled)

    public Preprocessor() {
        this.scriptsRoot = null;
    }

    public Preprocessor(Path scriptsRoot) {
        this.scriptsRoot = scriptsRoot;
    }

    public String process(String source) throws CompilingException {
        return processInternal(source, new LinkedHashSet<>());
    }

    private String processInternal(String source, Set<Path> visiting) throws CompilingException {
        if (scriptsRoot != null) {
            source = resolveImports(source, visiting);
        }
        return source;
    }

    private String resolveImports(String source, Set<Path> visiting) throws CompilingException {
        Matcher m = IMPORT_PAT.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String pathStr = m.group(1).trim();
            Path importPath = scriptsRoot.resolve(pathStr).toAbsolutePath().normalize();

            if (!importPath.toString().endsWith(".nh")) {
                throw new CompilingException(null, "Import path must end with '.nh': \"" + pathStr + "\"");
            }
            if (!Files.exists(importPath)) {
                throw new CompilingException(null, "Import not found: \"" + pathStr + "\"");
            }
            if (visiting.contains(importPath)) {
                String chain = visiting.stream()
                        .map(p -> scriptsRoot.relativize(p).toString())
                        .collect(Collectors.joining(" → "));
                throw new CompilingException(null, "Circular import: " + chain + " → " + pathStr);
            }

            String importedSource;
            try {
                importedSource = Files.readString(importPath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new CompilingException(null, "Cannot read import \"" + pathStr + "\": " + e.getMessage());
            }

            Set<Path> childVisiting = new LinkedHashSet<>(visiting);
            childVisiting.add(importPath);
            String processed = processInternal(importedSource, childVisiting);

            // Replace the import statement with the processed content, preserving surrounding newlines
            m.appendReplacement(sb, Matcher.quoteReplacement(processed));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
