package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.ConditionCallNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.StringValue;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.MenuNode;
import com.drakou111.neohouse.compiler.ast.statement.ActionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.GlobalFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.AddCondition;
import com.drakou111.neohouse.constants.HousingList;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects the teams, layouts and groups a script refers to by name (setTeam, hasTeam, team
 * variables, applyLayout, setGroup, isGroup), and the functions it calls and menus it displays without
 * defining them, so a run can first check that they exist in the house. Only literal names are
 * collected; names built from placeholders are skipped.
 */
public class HousingNameCollector extends DefaultVisitor {
    private final Map<HousingList, Set<String>> names = new EnumMap<>(HousingList.class);
    private final Set<String> definedFunctions = new HashSet<>();
    private final Set<String> definedMenus = new HashSet<>();

    /** Names per list; lists the script doesn't need anything from are absent. */
    public Map<HousingList, Set<String>> collect(MainNode main) {
        for (MainStatementNode statement : main.statements) {
            if (statement instanceof FunctionNode function) this.definedFunctions.add(function.name);
            else if (statement instanceof MenuNode menu) this.definedMenus.add(menu.name);
        }
        main.accept(this);
        return this.names;
    }

    @Override
    public MainNode analyze(MainNode main) {
        main.accept(this);
        return main;
    }

    @Override
    public ASTNode visitActionCall(ActionCallNode call) {
        AddAction action = AddAction.searchByName(call.name);
        if (action != null) {
            List<NormalExpressionNode> args = call.arguments.expressions;
            switch (action) {
                case SET_TEAM -> add(HousingList.TEAMS, args, 0);
                case APPLY_LAYOUT -> add(HousingList.LAYOUTS, args, 0);
                case CHANGE_GROUP -> add(HousingList.GROUPS, args, 0);
                case DISPLAY_MENU -> {
                    if (!args.isEmpty() && args.getFirst() instanceof ConstantPrimitiveNode constant
                            && constant.value instanceof StringValue text && !this.definedMenus.contains(text.value))
                        addName(HousingList.MENUS, text.value);
                }
                default -> { }
            }
        }
        return super.visitActionCall(call);
    }

    @Override
    public ASTNode visitVoidFunctionCall(VoidFunctionCallNode call) {
        if (!this.definedFunctions.contains(call.functionName)) addName(HousingList.FUNCTIONS, call.functionName);
        return super.visitVoidFunctionCall(call);
    }

    @Override
    public ASTNode visitGlobalFunctionCall(GlobalFunctionCallNode call) {
        if (!this.definedFunctions.contains(call.functionName)) addName(HousingList.FUNCTIONS, call.functionName);
        return super.visitGlobalFunctionCall(call);
    }

    @Override
    public ASTNode visitConditionCall(ConditionCallNode call) {
        AddCondition condition = AddCondition.searchByName(call.name);
        if (condition != null) {
            List<NormalExpressionNode> args = call.arguments.expressions;
            switch (condition) {
                case TEAM -> add(HousingList.TEAMS, args, 1);
                case GROUP -> add(HousingList.GROUPS, args, 1);
                default -> { }
            }
        }
        return super.visitConditionCall(call);
    }

    @Override
    public ASTNode visitVariable(VariableNode variable) {
        if (variable.teamName != null) addName(HousingList.TEAMS, variable.teamName);
        return super.visitVariable(variable);
    }

    private void add(HousingList list, List<NormalExpressionNode> args, int index) {
        if (index < args.size() && args.get(index) instanceof ConstantPrimitiveNode constant && constant.value instanceof StringValue text)
            addName(list, text.value);
    }

    private void addName(HousingList list, String name) {
        if (name.isBlank() || name.contains("%")) return;
        this.names.computeIfAbsent(list, k -> new LinkedHashSet<>()).add(name);
    }
}
