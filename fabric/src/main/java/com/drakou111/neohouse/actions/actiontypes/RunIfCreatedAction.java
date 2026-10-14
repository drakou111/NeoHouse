package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Runs {@code ifCreated} when the named entry was created as a fresh shell earlier this run (see
 * {@link MarkCreatedAction}), otherwise {@code otherwise}. Either may be null (a no-op).
 *
 * <p>Progress steps are counted up-front (before the decision is known); the steps of the branch
 * not taken are subtracted so the progress total stays accurate.</p>
 */
public class RunIfCreatedAction extends Action {
    private final String key;
    private final Action ifCreated;
    private final Action otherwise;
    private boolean decided = false;
    private Action active = null;

    public RunIfCreatedAction(String key, Action ifCreated) {
        this(key, ifCreated, null);
    }

    public RunIfCreatedAction(String key, Action ifCreated, Action otherwise) {
        this.key = key;
        this.ifCreated = ifCreated;
        this.otherwise = otherwise;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!this.decided) {
            this.decided = true;
            this.active = SequenceManager.wasCreated(this.key) ? this.ifCreated : this.otherwise;
            int taken = this.active != null ? this.active.countSteps() : 0;
            int total = countSteps();
            if (total > taken) SequenceManager.addSteps(-(total - taken));
        }
        if (this.active == null) return true;
        if (this.active.run(mc)) {
            this.active = null;
            return true;
        }
        return false;
    }

    @Override
    public int countSteps() {
        int created = this.ifCreated != null ? this.ifCreated.countSteps() : 0;
        int other = this.otherwise != null ? this.otherwise.countSteps() : 0;
        return Math.max(created, other);
    }

    @Override
    public boolean requiresCreative() {
        return (this.ifCreated != null && this.ifCreated.requiresCreative())
                || (this.otherwise != null && this.otherwise.requiresCreative());
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public List<Action> getChildren() {
        return Stream.of(this.ifCreated, this.otherwise).filter(Objects::nonNull).toList();
    }
}
