package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.HousingList;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;

/**
 * Pages through an already-open Housing list (functions, commands or custom menus) and records every
 * entry name in {@link SequenceManager}, so later steps can decide what to delete, create or skip
 * without searching the list again. Paging also stops on a page that adds no new name.
 */
public class ReadListNamesAction extends Action {
    private final HousingList list;
    private final ListPager pager;

    public ReadListNamesAction(HousingList list) {
        this.list = list;
        this.pager = new ListPager(list.singularName + " list", list.paginated, menu -> true);
    }

    @Override
    public String getDebugName() {
        return "Read " + this.list.name().toLowerCase() + " list";
    }

    @Override
    public boolean run(Minecraft mc) {
        return this.pager.run(mc, names -> {
            boolean addedNew = false;
            for (String name : names) addedNew |= SequenceManager.recordExisting(this.list, name);
            return addedNew;
        });
    }
}
