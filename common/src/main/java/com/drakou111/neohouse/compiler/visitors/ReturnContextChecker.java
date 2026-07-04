package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TupleFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.TupleReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.TypedReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidReturnNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.exceptions.SemanticException;

public class ReturnContextChecker extends EmptyVisitor<Void> {
    private boolean inTypedFunction = false;
    private boolean inTupleFunction = false;
    private VariableType functionType = null;
    private java.util.List<VariableType> tupleFunctionTypes = null;

    public Void analyze(ASTNode root) {
        root.accept(this);
        return null;
    }

    @Override
    public Void visitVoidFunction(VoidFunctionNode fn) {
        inTypedFunction = false;
        inTupleFunction = false;
        fn.parameters.accept(this);
        fn.statements.accept(this);
        return null;
    }

    @Override
    public Void visitTypedFunction(TypedFunctionNode fn) {
        inTypedFunction = true;
        inTupleFunction = false;
        functionType = fn.returnType;
        fn.parameters.accept(this);
        fn.statements.accept(this);
        inTypedFunction = false;
        return null;
    }

    @Override
    public Void visitTupleFunction(TupleFunctionNode fn) {
        inTypedFunction = false;
        inTupleFunction = true;
        tupleFunctionTypes = fn.returnTypes;
        fn.parameters.accept(this);
        fn.statements.accept(this);
        inTupleFunction = false;
        tupleFunctionTypes = null;
        return null;
    }

    @Override
    public Void visitTypedReturn(TypedReturnNode ret) {
        if (!inTypedFunction) {
            throw new SemanticException(ret.token, "Cannot return a single value here — use 'return (e0, e1, ...);' in a tuple function");
        }
        ret.expression.accept(this);
        if (functionType != ret.expression.type && functionType != VariableType.VAR)
            throw new SemanticException(ret.token, "Return type mismatch. Expected to return " + functionType + " but returning " + ret.expression.type);
        return null;
    }

    @Override
    public Void visitTupleReturn(TupleReturnNode ret) {
        if (!inTupleFunction) {
            throw new SemanticException(ret.token, "Tuple return 'return (e0, e1, ...);' is only valid inside a tuple function");
        }
        if (ret.expressions.size() != tupleFunctionTypes.size())
            throw new SemanticException(ret.token, "Tuple return has " + ret.expressions.size() +
                    " values but function returns " + tupleFunctionTypes.size());
        for (int i = 0; i < ret.expressions.size(); i++) {
            ret.expressions.get(i).accept(this);
            VariableType expected = tupleFunctionTypes.get(i);
            VariableType actual = ret.expressions.get(i).type;
            if (expected != actual && expected != VariableType.VAR)
                throw new SemanticException(ret.token, "Tuple return type mismatch at position " + i +
                        ": expected " + expected + " but got " + actual);
        }
        return null;
    }

    @Override
    public Void visitVoidReturn(VoidReturnNode ret) {
        if (inTypedFunction || inTupleFunction) {
            throw new SemanticException(ret.token, "Must return a value in a typed/tuple function");
        }
        return null;
    }
}
