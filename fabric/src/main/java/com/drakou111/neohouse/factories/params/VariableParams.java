package com.drakou111.neohouse.factories.params;

import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.constants.Operator;

public class VariableParams {
    public Holder holder = Holder.PLAYER;
    public String variable = "Kills";
    public String teamName = null;
    public Operator operator = Operator.INCREMENT;
    public String value = "1";
    public boolean automaticUnset = false;
    public String note = null;

    public VariableParams holder(Holder v)        { this.holder = v; return this; }
    public VariableParams variable(String v)      { this.variable = v; return this; }
    public VariableParams teamName(String v)      { this.teamName = v; return this; }
    public VariableParams operator(Operator v)    { this.operator = v; return this; }
    public VariableParams value(String v)         { this.value = v; return this; }
    public VariableParams automaticUnset(boolean v){ this.automaticUnset = v; return this; }
    public VariableParams note(String v)          { this.note = v; return this; }
}
