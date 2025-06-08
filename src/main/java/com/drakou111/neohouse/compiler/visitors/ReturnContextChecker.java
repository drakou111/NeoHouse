package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.TypedReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidReturnNode;
import com.drakou111.neohouse.exceptions.SemanticException;

/**
 * Ensures that:
 *  - TypedReturnNode appears only inside a TypedFunctionNode.
 *  - VoidReturnNode appears only inside a VoidFunctionNode.
 *  - No return is allowed outside of any function.
 */
public class ReturnContextChecker extends EmptyVisitor<Void> {
    private boolean inTypedFunction = false;
    private VariableType functionType = null;

    public Void analyze(ASTNode root) {
        root.accept(this);
        return null;
    }

    @Override
    public Void visitVoidFunction(VoidFunctionNode fn) {
        inTypedFunction = false;
        fn.parameters.accept(this);
        fn.statements.accept(this);
        return null;
    }

    @Override
    public Void visitTypedFunction(TypedFunctionNode fn) {
        inTypedFunction = true;
        functionType = fn.returnType;
        fn.parameters.accept(this);
        fn.statements.accept(this);
        inTypedFunction = false;
        return null;
    }

    @Override
    public Void visitTypedReturn(TypedReturnNode ret) {
        if (!inTypedFunction) {
            throw new SemanticException(ret.token, "Cannot return a value here, not in a typed function");
        }
        ret.expression.accept(this);
        if (functionType != ret.expression.type && functionType != VariableType.VAR)
            throw new SemanticException(ret.token, "Return type mismatch. Expected to return " + functionType + " but returning " + ret.expression.type);

        return null;
    }

    @Override
    public Void visitVoidReturn(VoidReturnNode ret) {
        if (inTypedFunction) {
            throw new SemanticException(ret.token, "Must return a value in a typed function");
        }

        return null;
    }
}
