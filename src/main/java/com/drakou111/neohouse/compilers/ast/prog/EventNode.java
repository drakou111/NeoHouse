package com.drakou111.neohouse.compilers.ast.prog;

import com.drakou111.neohouse.compilers.ast.BlockNode;
import com.drakou111.neohouse.compilers.ast.ProgramNode;

public class EventNode extends ProgramNode {
    String eventName;
    BlockNode body;
}
