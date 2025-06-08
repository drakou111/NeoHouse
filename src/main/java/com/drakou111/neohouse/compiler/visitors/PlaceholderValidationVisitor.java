package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.ASTVisitor;
import com.drakou111.neohouse.compiler.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.main.ConstantPrimitiveList;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main_statements.*;
import com.drakou111.neohouse.compiler.ast.statement.*;
import com.drakou111.neohouse.compiler.ast.expr.*;
import com.drakou111.neohouse.compiler.ast.expr.call.FunctionCallNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.ParameterListNode;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.constants.Placeholder;
import com.drakou111.neohouse.exceptions.SemanticException;

public class PlaceholderValidationVisitor extends DefaultVisitor {

    @Override
    public ASTNode visitPlaceholder(PlaceholderNode node) {
        // 1) lookup by name
        Placeholder sig;
        try {
            sig = Placeholder.requireByName(node.placeholder);
        } catch (IllegalArgumentException e) {
            throw new SemanticException(node.token, "Unknown placeholder `" + node.placeholder + "`");
        }

        // 2) check arity
        ConstantPrimitiveList args = node.constants;
        int expected = sig.constTypes.size();
        if (args.constants.size() != expected) {
            throw new SemanticException(node.token, "`" + node.placeholder + "` expects " + expected + " constant(s), but got " + args.constants.size());
        }

        // 3) check each constant’s type
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
