package com.drakou111.neohouse.actions;

public class ActionResult {
    public final boolean finished;
    public final String error;
    public boolean abort;

    public ActionResult(boolean finished) {
        this.finished = finished;
        this.error = null;
        this.abort = false;
    }

    public ActionResult(boolean finished, String error, boolean abort) {
        this.finished = finished;
        this.error = error;
        this.abort = abort;
    }

    public boolean hasError() {
        return error != null;
    }

    public boolean hasAbort() {
        return abort;
    }
}
