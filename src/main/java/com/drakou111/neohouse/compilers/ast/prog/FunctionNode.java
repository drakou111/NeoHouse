package com.drakou111.neohouse.compilers.ast.prog;

import com.drakou111.neohouse.compilers.ast.BlockNode;
import com.drakou111.neohouse.compilers.ast.ParamNode;
import com.drakou111.neohouse.compilers.ast.ProgramNode;
import com.drakou111.neohouse.compilers.ast.types.TypeNode;

import java.util.List;

public class FunctionNode extends ProgramNode {
    String description;
    String name;
    TypeNode returnType;
    List<ParamNode> params;
    BlockNode body;

    public FunctionNode(String description, String name, TypeNode returnType, List<ParamNode> params, BlockNode body) {
        this.description = description;
        this.name = name;
        this.returnType = returnType;
        this.params = params;
        this.body = body;
    }
}
