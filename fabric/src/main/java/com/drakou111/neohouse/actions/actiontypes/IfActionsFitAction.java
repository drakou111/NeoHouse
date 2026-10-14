package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.Others;
import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * For the "Append" override mode: with an action editor open (function, command or event), reads
 * every page of its actions and counts them by type. Runs {@code ifFits} when the existing actions
 * plus the {@code planned} ones stay within each type's {@link AddAction#maxCount}; otherwise runs
 * {@code closeEditor} and warns which limit would be exceeded, leaving the entry as it is.
 */
public class IfActionsFitAction extends Action {
    private static final Map<String, AddAction> BY_DISPLAY_NAME = byDisplayName();

    private final String what;
    private final Map<AddAction, Integer> planned;
    private final Action ifFits;
    private final Action closeEditor;
    private final ListPager pager;
    private final Map<AddAction, Integer> existing = new EnumMap<>(AddAction.class);
    private final Set<String> unknownNames = new HashSet<>();
    private boolean decided = false;
    private Action active = null;

    /** {@code what} names the entry in the warning, e.g. "function 'shop'". */
    public IfActionsFitAction(String what, Map<AddAction, Integer> planned, Action ifFits, Action closeEditor) {
        this.what = what;
        this.planned = Map.copyOf(planned);
        this.ifFits = ifFits;
        this.closeEditor = closeEditor;
        this.pager = new ListPager("action list", true,
                menu -> Others.GENERAL_ADD_H6 < menu.slots.size() && menu.slots.get(Others.GENERAL_ADD_H6).hasItem());
    }

    @Override
    public String getDebugName() {
        return "Check that the new actions fit in " + this.what;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!this.decided) {
            if (!this.pager.run(mc, this::countPage)) return false;
            this.decided = true;
            this.active = decide();
        }
        return this.active.run(mc);
    }

    private boolean countPage(List<String> names) {
        for (String name : names) {
            Optional<AddAction> kind = kindOf(name);
            if (kind.isPresent()) {
                this.existing.merge(kind.get(), 1, Integer::sum);
            } else if (this.unknownNames.add(name)) {
                MessageUtils.debugToChat("Unknown action '" + name + "' in " + this.what + ": it isn't counted towards a limit.");
            }
        }
        return true;
    }

    private Action decide() {
        Optional<String> overflow = firstOverflow(this.existing, this.planned);
        Action chosen = overflow.isEmpty() ? this.ifFits
                : new GroupAction(List.of(this.closeEditor, new ClientMessageAction("Skipped " + this.what + ": " + overflow.get() + ".", true)));
        int notTaken = countSteps() - chosen.countSteps();
        if (notTaken > 0) SequenceManager.addSteps(-notTaken);
        return chosen;
    }

    /** The first limit that {@code existing + planned} would exceed, described for a warning. */
    static Optional<String> firstOverflow(Map<AddAction, Integer> existing, Map<AddAction, Integer> planned) {
        for (AddAction kind : AddAction.values()) {
            int adding = planned.getOrDefault(kind, 0);
            if (adding == 0) continue;
            int has = existing.getOrDefault(kind, 0);
            if (has + adding > kind.maxCount)
                return Optional.of("it has %d '%s' actions and the script adds %d, over the limit of %d"
                        .formatted(has, kind.displayName, adding, kind.maxCount));
        }
        return Optional.empty();
    }

    /** The action type an action editor item stands for, by its name. */
    static Optional<AddAction> kindOf(String itemName) {
        return Optional.ofNullable(BY_DISPLAY_NAME.get(itemName.strip().toLowerCase(Locale.ROOT)));
    }

    private static Map<String, AddAction> byDisplayName() {
        Map<String, AddAction> map = new HashMap<>();
        for (AddAction action : AddAction.values()) map.put(action.displayName.toLowerCase(Locale.ROOT), action);
        return map;
    }

    @Override
    public int countSteps() {
        return Math.max(this.ifFits.countSteps(), this.closeEditor.countSteps() + 1);
    }

    @Override
    public boolean requiresCreative() {
        return this.ifFits.requiresCreative() || this.closeEditor.requiresCreative();
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public List<Action> getChildren() {
        return List.of(this.ifFits, this.closeEditor);
    }
}
