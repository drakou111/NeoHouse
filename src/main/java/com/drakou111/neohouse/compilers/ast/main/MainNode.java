package com.drakou111.neohouse.compilers.ast.main;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ProgramNode;

import java.util.ArrayList;
import java.util.List;

public class MainNode extends ASTNode {
    List<ProgramNode> programs;

    public MainNode() {
        programs = new ArrayList<>();
    }

    public MainNode(List<ProgramNode> programs) {
        this.programs = programs;
    }
}
