package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.ActionBuilder;
import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.*;
import com.drakou111.neohouse.exceptions.InteruptActionException;

public class ConditionFactory {
    public static Action addConditionGroup(RestrictionCondition state, boolean invert, String group, boolean includeHigherGroups, String note) {
        if (state.pageSlotFor(AddCondition.GROUP) == null) throw new InteruptActionException("Cannot add condition 'Group' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.GROUP, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_GROUP_INVERT)
                )
                .onlyIf(group != null, () -> new ActionBuilder()
                        .searchAndWait(group, null)
                )
                .onlyIf(includeHigherGroups, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_GROUP_INCLUDE_HIGHER)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionVariable(RestrictionCondition state, boolean invert, Holder holder, String variable, Comparator comparator, String value, String fallback, String note) {
        if (state.pageSlotFor(AddCondition.VARIABLE) == null) throw new InteruptActionException("Cannot add condition 'Variable' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.VARIABLE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_VARIABLE_INVERT)
                )
                .onlyIf(holder != null && holder != Holder.PLAYER, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_VARIABLE_HOLDER)
                        .clickAndSelectSlot(holder.getPage(), holder.getSlotID())
                )
                .onlyIf(variable != null && !variable.trim().isEmpty() && !variable.equals("Kills"), () -> new ActionBuilder()
                        .chatInput(variable, Slots.CONDITION_VARIABLE_VAR)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_VARIABLE_COMPARATOR)
                        .clickAndSelectSlot(comparator.getPage(), comparator.getSlotID())
                )
                .onlyIf(value != null && !value.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(value, Slots.CONDITION_VARIABLE_VALUE)
                )
                .onlyIf(fallback != null && !fallback.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(fallback, Slots.CONDITION_VARIABLE_FALLBACK_VALUE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionPermission(RestrictionCondition state, boolean invert, Permission permission, String note) {
        if (state.pageSlotFor(AddCondition.PERMISSION) == null) throw new InteruptActionException("Cannot add condition 'Permission' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.PERMISSION, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_PERMISSION_INVERT)
                )
                .onlyIf(permission != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_PERMISSION_TYPE)
                        .clickAndSelectSlot(permission.getPage(), permission.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionRegion(RestrictionCondition state, boolean invert, String region, String note) {
        if (state.pageSlotFor(AddCondition.REGION) == null) throw new InteruptActionException("Cannot add condition 'Region' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.REGION, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_REGION_INVERT)
                )
                .onlyIf(region != null && !region.trim().isEmpty(), () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_REGION_TYPE)
                        .searchAndWait(region, null)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionHasItem(RestrictionCondition state, boolean invert, Integer inventoryItemSlotID, WhatCheck whatCheck, WhereCheck whereCheck, AmountCheck amountCheck, String note) {
        if (state.pageSlotFor(AddCondition.HAS_ITEM) == null) throw new InteruptActionException("Cannot add condition 'Has Item' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.HAS_ITEM, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_FLYING_INVERT)
                )
                .onlyIf(inventoryItemSlotID != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_ITEM_ITEM)
                        .clickSlotAndWait(inventoryItemSlotID)
                )
                .onlyIf(whatCheck != null && whatCheck != WhatCheck.METADATA, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_ITEM_WHAT_TO_CHECK)
                        .clickAndSelectSlot(whatCheck.getPage(), whatCheck.getSlotID())
                )
                .onlyIf(whereCheck != null && whereCheck != WhereCheck.ANYWHERE, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_ITEM_WHERE_TO_CHECK)
                        .clickAndSelectSlot(whereCheck.getPage(), whereCheck.getSlotID())
                )
                .onlyIf(amountCheck != null && amountCheck != AmountCheck.ANY, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_ITEM_AMOUNT)
                        .clickAndSelectSlot(amountCheck.getPage(), amountCheck.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionDoingParkour(RestrictionCondition state, boolean invert, String note) {
        if (state.pageSlotFor(AddCondition.DOING_PARKOUR) == null) throw new InteruptActionException("Cannot add condition 'Doing Parkour' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.DOING_PARKOUR, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_DOING_PARKOUR_INVERT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionPotion(RestrictionCondition state, boolean invert, Potion potion, String note) {
        if (state.pageSlotFor(AddCondition.POTION) == null) throw new InteruptActionException("Cannot add condition 'Potion' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.POTION, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_EFFECT_INVERT)
                )
                .onlyIf(potion != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_EFFECT_TYPE)
                        .clickAndSelectSlot(potion.getPage(), potion.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionSneaking(RestrictionCondition state, boolean invert, String note) {
        if (state.pageSlotFor(AddCondition.SNEAKING) == null) throw new InteruptActionException("Cannot add condition 'Sneaking' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.SNEAKING, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_SNEAKING_INVERT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionFlying(RestrictionCondition state, boolean invert, String note) {
        if (state.pageSlotFor(AddCondition.FLYING) == null) throw new InteruptActionException("Cannot add condition 'Flying' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.FLYING, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_FLYING_INVERT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionHealth(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        if (state.pageSlotFor(AddCondition.HEALTH) == null) throw new InteruptActionException("Cannot add condition 'Health' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.HEALTH, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_HEALTH_INVERT)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_HEALTH_COMPARATOR)
                        .clickAndSelectSlot(comparator.getPage(), comparator.getSlotID())
                )
                .onlyIf(value != null, () -> new ActionBuilder()
                        .anvilInput(value, Slots.CONDITION_HEALTH_VALUE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionMaxHealth(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        if (state.pageSlotFor(AddCondition.MAX_HEALTH) == null) throw new InteruptActionException("Cannot add condition 'Max Health' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.MAX_HEALTH, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_MAX_HEALTH_INVERT)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_MAX_HEALTH_COMPARATOR)
                        .clickAndSelectSlot(comparator.getPage(), comparator.getSlotID())
                )
                .onlyIf(value != null, () -> new ActionBuilder()
                        .anvilInput(value, Slots.CONDITION_MAX_HEALTH_VALUE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionHunger(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        if (state.pageSlotFor(AddCondition.HUNGER) == null) throw new InteruptActionException("Cannot add condition 'Hunger' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.HUNGER, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_HUNGER_INVERT)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_HUNGER_COMPARATOR)
                        .clickAndSelectSlot(comparator.getPage(), comparator.getSlotID())
                )
                .onlyIf(value != null, () -> new ActionBuilder()
                        .anvilInput(value, Slots.CONDITION_HUNGER_VALUE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionGamemode(RestrictionCondition state, boolean invert, Gamemode gamemode, String note) {
        if (state.pageSlotFor(AddCondition.GAME_MODE) == null) throw new InteruptActionException("Cannot add condition 'Game Mode' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.GAME_MODE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_GAME_MODE_INVERT)
                )
                .onlyIf(gamemode != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_GAME_MODE_TYPE)
                        .clickAndSelectSlot(gamemode.getPage(), gamemode.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionPlaceholder(RestrictionCondition state, boolean invert, String placeholder, Comparator comparator, String value, String note) {
        if (state.pageSlotFor(AddCondition.PLACEHOLDER) == null) throw new InteruptActionException("Cannot add condition 'Placeholder' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.PLACEHOLDER, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_PLACEHOLDER_INVERT)
                )
                .onlyIf(placeholder != null && !placeholder.trim().isEmpty(), () -> new ActionBuilder()
                        .anvilInput(placeholder, Slots.CONDITION_PLACEHOLDER_VAR)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_PLACEHOLDER_COMPARATOR)
                        .clickAndSelectSlot(comparator.getPage(), comparator.getSlotID())
                )
                .onlyIf(value != null && !value.trim().isEmpty(), () -> new ActionBuilder()
                        .anvilInput(value, Slots.CONDITION_PLACEHOLDER_VALUE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addConditionTeam(RestrictionCondition state, boolean invert, String team, String note) {
        if (state.pageSlotFor(AddCondition.TEAM) == null) throw new InteruptActionException("Cannot add condition 'Team' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.TEAM, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_TEAM_INVERT)
                )
                .onlyIf(team != null && !team.equals("None") && !team.trim().isEmpty(), () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_TEAM_TYPE)
                        .search(team, null)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }
}
