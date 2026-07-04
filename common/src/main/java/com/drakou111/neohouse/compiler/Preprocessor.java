package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            String replacement;
            if (pathStr.endsWith("/*")) {
                replacement = resolveWildcard(pathStr, visiting);
            } else {
                replacement = resolveSingle(pathStr, visiting);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String resolveSingle(String pathStr, Set<Path> visiting) throws CompilingException {
        Path importPath = scriptsRoot.resolve(pathStr).toAbsolutePath().normalize();

        if (!pathStr.endsWith(".nh")) {
            throw new CompilingException(null, "Import path must end with '.nh' or '/*': \"" + pathStr + "\"");
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
        return processInternal(importedSource, childVisiting);
    }

    private String resolveWildcard(String pathStr, Set<Path> visiting) throws CompilingException {
        // "some/dir/*" → resolve "some/dir/" and import all .nh files inside
        String dirStr = pathStr.substring(0, pathStr.length() - 1); // strip the '*'
        Path dir = scriptsRoot.resolve(dirStr).toAbsolutePath().normalize();

        if (!Files.isDirectory(dir)) {
            throw new CompilingException(null, "Wildcard import directory not found: \"" + dirStr + "\"");
        }

        List<Path> files;
        try (var stream = Files.list(dir)) {
            files = stream
                    .filter(p -> p.getFileName().toString().endsWith(".nh"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new CompilingException(null, "Cannot read directory for wildcard import \"" + pathStr + "\": " + e.getMessage());
        }

        if (files.isEmpty()) return "";

        List<String> parts = new ArrayList<>();
        for (Path file : files) {
            String rel = scriptsRoot.relativize(file).toString().replace('\\', '/');
            parts.add(resolveSingle(rel, visiting));
        }
        return String.join("\n", parts);
    }
}
