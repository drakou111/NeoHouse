package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.HousingList;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Stops the run before anything is changed when the script uses teams, layouts or groups that don't
 * exist in the house (Housing can't create them for us). Must run after {@link ReadListNamesAction}.
 */
public class CheckNamesExistAction extends Action {
    private final HousingList list;
    private final List<String> names;

    public CheckNamesExistAction(HousingList list, List<String> names) {
        this.list = list;
        this.names = List.copyOf(names);
    }

    @Override
    public String getDebugName() {
        return "Check " + this.list.singularName + "s exist: " + String.join(", ", this.names);
    }

    @Override
    public boolean run(Minecraft mc) {
        List<String> missing = new ArrayList<>();
        for (String name : this.names) if (!SequenceManager.exists(this.list, name)) missing.add(name);
        if (missing.isEmpty()) return true;

        throw new InterruptActionException("This script uses %s%s that %s exist in this house: %s. Create %s first."
                .formatted(this.list.singularName, missing.size() == 1 ? "" : "s", missing.size() == 1 ? "doesn't" : "don't",
                        String.join(", ", missing), missing.size() == 1 ? "it" : "them"));
    }

    @Override
    public int countSteps() {
        return 0;
    }
}
