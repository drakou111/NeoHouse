package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.ActionBuilder;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.RestrictionAction;
import com.drakou111.neohouse.constants.Slots;

public class ConditionFactory {
    public static Action addActionConditional(RestrictionAction state, boolean matchAny, Action conditions, Action ifs, Action elses, String note) {
        if (state.equals(RestrictionAction.CONDITIONAL_OR_RANDOM)) return null;

        return new ActionBuilder()
                .addAction(AddAction.CONDITIONAL, state)
                .onlyIf(matchAny, new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_CONDITIONAL_MATCH_ANY)
                )
                .onlyIf(conditions != null, new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_CONDITIONAL_CONDITIONALS)
                        .append(conditions)
                        .clickSlotAndWait(Slots.GENERAL_GO_BACK_H6)
                )
                .onlyIf(ifs != null, new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_CONDITIONAL_IF)
                        .append(ifs)
                        .clickSlotAndWait(Slots.GENERAL_GO_BACK_H6)
                )
                .onlyIf(elses != null, new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_CONDITIONAL_ELSE)
                        .append(elses)
                        .clickSlotAndWait(Slots.GENERAL_GO_BACK_H6)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }
}
