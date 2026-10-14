package com.drakou111.neohouse.compiler.helpers;

import com.drakou111.neohouse.compiler.ast.ASTNode;

import java.util.List;

public class StringHelper {
    public static String nodeListToString(List<? extends ASTNode> nodes) {
        StringBuilder ret = new StringBuilder("[");
        for (int i = 0; i < nodes.size(); i++) {
            ret.append(nodes.get(i).toShortString());
            if (i < nodes.size() - 1)
                ret.append(",");
        }
        return ret + "]";
    }

}
