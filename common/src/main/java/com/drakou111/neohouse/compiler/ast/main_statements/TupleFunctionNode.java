package com.drakou111.neohouse.compiler.ast.main_statements;

import com.drakou111.neohouse.compiler.ast.main.ParameterListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

import java.util.List;
import java.util.stream.Collectors;

public class TupleFunctionNode extends FunctionNode {
    public final List<VariableType> returnTypes;
    public final ParameterListNode parameters;

    public TupleFunctionNode(final String name, final String description, final String icon,
                              final List<VariableType> returnTypes, final ParameterListNode parameters,
                              final StatementListNode statements, final boolean isConst, final Token token) {
        super(name, description, icon, statements, isConst, token);
        this.returnTypes = returnTypes;
        this.parameters = parameters;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitTupleFunction(this);
    }

    @Override
    public String toString() {
        return "TupleFunctionNode[name=" + name + ", returns=(" +
                returnTypes.stream().map(Enum::name).collect(Collectors.joining(", ")) + ")]";
    }

    @Override
    public String toShortString() {
        return "TupleFun[name=" + name + ", ret=(" +
                returnTypes.stream().map(Enum::name).collect(Collectors.joining(",")) + ")]";
    }
}
