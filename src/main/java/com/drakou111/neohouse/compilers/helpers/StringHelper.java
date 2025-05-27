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

    public static String formatLikeJson(String input, String gap) {
        StringBuilder output = new StringBuilder();
        int indent = 0;
        boolean inKey = true;
        boolean escape = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            // Handle escape characters inside quoted strings
            if (c == '\\' && i + 1 < input.length() && input.charAt(i + 1) == '"') {
                output.append("\\\"");
                i++;
                continue;
            }

            switch (c) {
                case '[':
                    output.append("[\n");
                    indent++;
                    output.append(gap.repeat(indent));
                    break;
                case ']':
                    output.append("\n");
                    indent--;
                    output.append(gap.repeat(indent)).append("]");
                    break;
                case ',':
                    output.append(",");
                    break;
                case '=':
                    output.append(" : ");
                    break;
                case ' ':
                    if (i > 0 && input.charAt(i - 1) != '=' && input.charAt(i - 1) != '[') {
                        output.append("\n").append(gap.repeat(indent));
                    }
                    break;
                default:
                    output.append(c);
            }
        }

        return output.toString();
    }
}
