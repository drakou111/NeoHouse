package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compiler.ast.main.ConstantPrimitiveList;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.constants.Placeholder;
import com.drakou111.neohouse.exceptions.SemanticException;

public class PlaceholderValidationVisitor extends DefaultVisitor {

    @Override
    public ASTNode visitPlaceholder(PlaceholderNode node) {
        Placeholder sig;
        try {
            sig = Placeholder.requireByName(node.placeholder);
        } catch (IllegalArgumentException e) {
            throw new SemanticException(node.token, "Unknown placeholder `" + node.placeholder + "`");
        }

        ConstantPrimitiveList args = node.constants;
        int expected = sig.constTypes.size();
        if (args.constants.size() != expected) {
            throw new SemanticException(node.token, "`" + node.placeholder + "` expects " + expected + " constant(s), but got " + args.constants.size());
        }

        for (int i = 0; i < expected; i++) {
            ConstantPrimitiveNode c = args.constants.get(i);
            VariableType want = sig.constTypes.get(i);
            if (c.type != want) {
                throw new SemanticException(c.token, "Placeholder `" + node.placeholder + "` parameter #" + (i+1) + " must be " + want + " but found " + c.type);
            }
        }
        return new PlaceholderNode(node.placeholder, node.constants, sig.type, node.token);
    }
}
