package com.drakou111.neohouse.compilers.helpers;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantValue;

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

    public static String constListToString(List<? extends ConstantValue> consts) {
        StringBuilder ret = new StringBuilder("[");
        for (int i = 0; i < consts.size(); i++) {
            ret.append(consts.get(i).toShortString());
            if (i < consts.size() - 1)
                ret.append(",");
        }
        return ret + "]";
    }
}
