package com.drakou111.neohouse.compiler.diagnostics;

import com.drakou111.neohouse.compiler.lexer.Token;

/** A non-fatal compiler diagnostic. Unlike a CompilingException, a warning does not
 *  stop compilation — it is surfaced in the IDE's diagnostics bar and in chat on run. */
public record CompileWarning(Token token, String message) {}
