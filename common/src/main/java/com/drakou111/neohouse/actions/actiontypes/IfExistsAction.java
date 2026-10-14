package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.HousingList;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Runs one of two branches depending on whether an entry existed in a Housing list when this run
 * started (see {@link ReadListNamesAction}). Either branch may be null (a no-op). Steps for the
 * branch not taken are subtracted so the progress total stays accurate.
 */
public class IfExistsAction extends Action {

    private final HousingList list;
    private final String name;
    private final Action ifPresent;
    private final Action ifAbsent;
    private boolean decided = false;
    private Action active = null;

    public IfExistsAction(HousingList list, String name, Action ifPresent, Action ifAbsent) {
        this.list = list;
        this.name = name;
        this.ifPresent = ifPresent;
        this.ifAbsent = ifAbsent;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!this.decided) {
            this.decided = true;
            this.active = SequenceManager.exists(this.list, this.name) ? this.ifPresent : this.ifAbsent;
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
        int present = this.ifPresent != null ? this.ifPresent.countSteps() : 0;
        int absent = this.ifAbsent != null ? this.ifAbsent.countSteps() : 0;
        return Math.max(present, absent);
    }

    @Override
    public boolean requiresCreative() {
        return (this.ifPresent != null && this.ifPresent.requiresCreative())
                || (this.ifAbsent != null && this.ifAbsent.requiresCreative());
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public List<Action> getChildren() {
        return Stream.of(this.ifPresent, this.ifAbsent).filter(Objects::nonNull).toList();
    }
}
