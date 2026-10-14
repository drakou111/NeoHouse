package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.HousingList;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stops the run before anything is created when the house's existing entries plus the script's new
 * ones would exceed what Housing allows for that list. Must run after {@link ReadListNamesAction}.
 */
public class CheckListLimitAction extends Action {
    private final HousingList list;
    private final List<String> scriptNames;

    public CheckListLimitAction(HousingList list, List<String> scriptNames) {
        this.list = list;
        this.scriptNames = List.copyOf(scriptNames);
    }

    @Override
    public boolean run(Minecraft mc) {
        Set<String> afterRun = new HashSet<>(SequenceManager.existingDisplayNames(this.list));
        int existing = afterRun.size();
        for (String name : this.scriptNames) afterRun.add(this.list.displayName(name));
        if (afterRun.size() <= this.list.maxEntries) return true;

        int added = afterRun.size() - existing;
        throw new InterruptActionException("This house already has %d %ss and the script would add %d more, but Housing allows at most %d."
                .formatted(existing, this.list.singularName, added, this.list.maxEntries));
    }

    @Override
    public int countSteps() {
        return 0;
    }
}
