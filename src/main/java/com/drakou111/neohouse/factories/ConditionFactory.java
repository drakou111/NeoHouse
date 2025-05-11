package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.*;
import com.drakou111.neohouse.exceptions.InteruptActionException;
import com.drakou111.neohouse.utils.ActionBuilder;

public class ConditionFactory {
    public static Action addConditionGroup(RestrictionCondition state, boolean invert, String group, boolean includeHigherGroups, String note) {
        if (state.pageSlotFor(AddCondition.GROUP) == null) throw new InteruptActionException("Cannot add condition 'Group' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.GROUP, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_GROUP_INVERT, true)
                )
                .onlyIf(group != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_GROUP_TYPE, true)
                        .search(group, null, true, null, null)
                )
                .onlyIf(includeHigherGroups, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_GROUP_INCLUDE_HIGHER, true)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionVariable(RestrictionCondition state, boolean invert, Holder holder, String variable, Comparator comparator, String value, String fallback, String note) {
        if (state.pageSlotFor(AddCondition.VARIABLE) == null) throw new InteruptActionException("Cannot add condition 'Variable' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.VARIABLE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_VARIABLE_INVERT, true)
                )
                .onlyIf(holder != null && holder != Holder.PLAYER, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_VARIABLE_HOLDER, true)
                        .clickAndSelectSlot(holder.getPage(), holder.getSlotID())
                )
                .onlyIf(variable != null && !variable.trim().isEmpty() && !variable.equals("Kills"), () -> new ActionBuilder()
                        .chatInput(variable, Slots.CONDITION_VARIABLE_VAR)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_VARIABLE_COMPARATOR, true)
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
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionPermission(RestrictionCondition state, boolean invert, Permission permission, String note) {
        if (state.pageSlotFor(AddCondition.PERMISSION) == null) throw new InteruptActionException("Cannot add condition 'Permission' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.PERMISSION, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_PERMISSION_INVERT, true)
                )
                .onlyIf(permission != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_PERMISSION_TYPE, true)
                        .clickAndSelectSlot(permission.getPage(), permission.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionRegion(RestrictionCondition state, boolean invert, String region, String note) {
        if (state.pageSlotFor(AddCondition.REGION) == null) throw new InteruptActionException("Cannot add condition 'Region' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.REGION, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_REGION_INVERT, true)
                )
                .onlyIf(region != null && !region.trim().isEmpty(), () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_REGION_TYPE, true)
                        .search(region, null, true, null, null)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionHasItem(RestrictionCondition state, boolean invert, Integer inventoryItemSlotID, WhatCheck whatCheck, WhereCheck whereCheck, AmountCheck amountCheck, String note) {
        if (state.pageSlotFor(AddCondition.HAS_ITEM) == null) throw new InteruptActionException("Cannot add condition 'Has Item' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.HAS_ITEM, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_ITEM_INVERT, true)
                )
                .onlyIf(inventoryItemSlotID != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_ITEM_ITEM, true)
                        .clickSlotAndWait(inventoryItemSlotID, true)
                )
                .onlyIf(whatCheck != null && whatCheck != WhatCheck.METADATA, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_ITEM_WHAT_TO_CHECK, true)
                        .clickAndSelectSlot(whatCheck.getPage(), whatCheck.getSlotID())
                )
                .onlyIf(whereCheck != null && whereCheck != WhereCheck.ANYWHERE, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_ITEM_WHERE_TO_CHECK, true)
                        .clickAndSelectSlot(whereCheck.getPage(), whereCheck.getSlotID())
                )
                .onlyIf(amountCheck != null && amountCheck != AmountCheck.ANY, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_ITEM_AMOUNT, true)
                        .clickAndSelectSlot(amountCheck.getPage(), amountCheck.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionDoingParkour(RestrictionCondition state, boolean invert, String note) {
        if (state.pageSlotFor(AddCondition.DOING_PARKOUR) == null) throw new InteruptActionException("Cannot add condition 'Doing Parkour' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.DOING_PARKOUR, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_DOING_PARKOUR_INVERT, true)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionPotion(RestrictionCondition state, boolean invert, Potion potion, String note) {
        if (state.pageSlotFor(AddCondition.POTION) == null) throw new InteruptActionException("Cannot add condition 'Potion' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.POTION, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_EFFECT_INVERT, true)
                )
                .onlyIf(potion != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_EFFECT_TYPE, true)
                        .clickAndSelectSlot(potion.getPage(), potion.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionSneaking(RestrictionCondition state, boolean invert, String note) {
        if (state.pageSlotFor(AddCondition.SNEAKING) == null) throw new InteruptActionException("Cannot add condition 'Sneaking' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.SNEAKING, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_SNEAKING_INVERT, true)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionFlying(RestrictionCondition state, boolean invert, String note) {
        if (state.pageSlotFor(AddCondition.FLYING) == null) throw new InteruptActionException("Cannot add condition 'Flying' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.FLYING, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_FLYING_INVERT, true)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionHealth(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        if (state.pageSlotFor(AddCondition.HEALTH) == null) throw new InteruptActionException("Cannot add condition 'Health' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.HEALTH, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_HEALTH_INVERT, true)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_HEALTH_COMPARATOR, true)
                        .clickAndSelectSlot(comparator.getPage(), comparator.getSlotID())
                )
                .onlyIf(value != null, () -> new ActionBuilder()
                        .anvilInput(value, Slots.CONDITION_HEALTH_VALUE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionMaxHealth(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        if (state.pageSlotFor(AddCondition.MAX_HEALTH) == null) throw new InteruptActionException("Cannot add condition 'Max Health' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.MAX_HEALTH, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_MAX_HEALTH_INVERT, true)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_MAX_HEALTH_COMPARATOR, true)
                        .clickAndSelectSlot(comparator.getPage(), comparator.getSlotID())
                )
                .onlyIf(value != null, () -> new ActionBuilder()
                        .anvilInput(value, Slots.CONDITION_MAX_HEALTH_VALUE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionHunger(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        if (state.pageSlotFor(AddCondition.HUNGER) == null) throw new InteruptActionException("Cannot add condition 'Hunger' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.HUNGER, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_HUNGER_INVERT, true)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_HUNGER_COMPARATOR, true)
                        .clickAndSelectSlot(comparator.getPage(), comparator.getSlotID())
                )
                .onlyIf(value != null, () -> new ActionBuilder()
                        .anvilInput(value, Slots.CONDITION_HUNGER_VALUE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionGamemode(RestrictionCondition state, boolean invert, Gamemode gamemode, String note) {
        if (state.pageSlotFor(AddCondition.GAME_MODE) == null) throw new InteruptActionException("Cannot add condition 'Game Mode' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.GAME_MODE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_GAME_MODE_INVERT, true)
                )
                .onlyIf(gamemode != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_GAME_MODE_TYPE, true)
                        .clickAndSelectSlot(gamemode.getPage(), gamemode.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionPlaceholder(RestrictionCondition state, boolean invert, String placeholder, Comparator comparator, String value, String note) {
        if (state.pageSlotFor(AddCondition.PLACEHOLDER) == null) throw new InteruptActionException("Cannot add condition 'Placeholder' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.PLACEHOLDER, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_PLACEHOLDER_INVERT, true)
                )
                .onlyIf(placeholder != null && !placeholder.trim().isEmpty(), () -> new ActionBuilder()
                        .anvilInput(placeholder, Slots.CONDITION_PLACEHOLDER_VAR)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_PLACEHOLDER_COMPARATOR, true)
                        .clickAndSelectSlot(comparator.getPage(), comparator.getSlotID())
                )
                .onlyIf(value != null && !value.trim().isEmpty(), () -> new ActionBuilder()
                        .anvilInput(value, Slots.CONDITION_PLACEHOLDER_VALUE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionTeam(RestrictionCondition state, boolean invert, String team, String note) {
        if (state.pageSlotFor(AddCondition.TEAM) == null) throw new InteruptActionException("Cannot add condition 'Team' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.TEAM, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_TEAM_INVERT, true)
                )
                .onlyIf(team != null && !team.equals("None") && !team.trim().isEmpty(), () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_TEAM_TYPE, true)
                        .search(team, null, true, null, null)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionPortalType(RestrictionCondition state, boolean invert, PortalType portalType, String note) {
        if (state.pageSlotFor(AddCondition.PORTAL_TYPE) == null) throw new InteruptActionException("Cannot add condition 'Portal Type' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.PORTAL_TYPE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_PORTAL_TYPE_INVERT, true)
                )
                .onlyIf(portalType != null && portalType != PortalType.NETHER_PORTAL, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_PORTAL_TYPE_TYPE, true)
                        .clickAndSelectSlot(portalType.getPage(), portalType.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionPVPEnabled(RestrictionCondition state, boolean invert, String note) {
        if (state.pageSlotFor(AddCondition.PVP_ENABLED) == null) throw new InteruptActionException("Cannot add condition 'PVP Enabled' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.PVP_ENABLED, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_PVP_ENABLED_INVERT, true)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionDamageCause(RestrictionCondition state, boolean invert, DamageCause damageCause, String note) {
        if (state.pageSlotFor(AddCondition.DAMAGE_CAUSE) == null) throw new InteruptActionException("Cannot add condition 'Damage Cause' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.DAMAGE_CAUSE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_DAMAGE_CAUSE_INVERT, true)
                )
                .onlyIf(damageCause != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_DAMAGE_CAUSE_TYPE, true)
                        .clickAndSelectSlot(damageCause.getPage(), damageCause.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionDamageAmount(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        if (state.pageSlotFor(AddCondition.DAMAGE_AMOUNT) == null) throw new InteruptActionException("Cannot add condition 'Damage Amount' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.DAMAGE_AMOUNT, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_DAMAGE_AMOUNT_INVERT, true)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_DAMAGE_AMOUNT_COMPARATOR, true)
                        .clickAndSelectSlot(comparator.getPage(), comparator.getSlotID())
                )
                .onlyIf(value != null, () -> new ActionBuilder()
                        .anvilInput(value, Slots.CONDITION_DAMAGE_AMOUNT_VALUE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionBlockType(RestrictionCondition state, boolean invert, Integer inventoryItemSlotID, boolean matchTypeOnly, String note) {
        if (state.pageSlotFor(AddCondition.BLOCK_TYPE) == null) throw new InteruptActionException("Cannot add condition 'Block Type' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.BLOCK_TYPE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_BLOCK_TYPE_INVERT, true)
                )
                .onlyIf(inventoryItemSlotID != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_BLOCK_TYPE_ITEM, true)
                        .clickSlotAndWait(inventoryItemSlotID, true)
                )
                .onlyIf(matchTypeOnly, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_BLOCK_TYPE_MATCH_TYPE_ONLY, true)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionIsItem(RestrictionCondition state, boolean invert, Integer inventoryItemSlotID, WhatCheck whatCheck, WhereCheck whereCheck, AmountCheck amountCheck, String note) {
        if (state.pageSlotFor(AddCondition.IS_ITEM) == null) throw new InteruptActionException("Cannot add condition 'Is Item' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.IS_ITEM, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_IS_ITEM_INVERT, true)
                )
                .onlyIf(inventoryItemSlotID != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_IS_ITEM_ITEM, true)
                        .clickSlotAndWait(inventoryItemSlotID, true)
                )
                .onlyIf(whatCheck != null && whatCheck != WhatCheck.METADATA, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_IS_ITEM_WHAT_CHECK, true)
                        .clickAndSelectSlot(whatCheck.getPage(), whatCheck.getSlotID())
                )
                .onlyIf(whereCheck != null && whereCheck != WhereCheck.ANYWHERE, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_IS_ITEM_WHERE_CHECK, true)
                        .clickAndSelectSlot(whereCheck.getPage(), whereCheck.getSlotID())
                )
                .onlyIf(amountCheck != null && amountCheck != AmountCheck.ANY, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.CONDITION_IS_ITEM_AMOUNT, true)
                        .clickAndSelectSlot(amountCheck.getPage(), amountCheck.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4, true)
                .get();
    }
}
