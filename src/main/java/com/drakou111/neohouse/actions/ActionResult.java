package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.State;

public class ActionResult {
    public final State state;
    public final boolean finished;

    public ActionResult(State state, boolean finished) {
        this.state = state;
        this.finished = finished;
    }
}
