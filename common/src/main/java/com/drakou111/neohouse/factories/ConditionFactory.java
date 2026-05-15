package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.constants.*;
import com.drakou111.neohouse.constants.conditions.*;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import com.drakou111.neohouse.utils.ActionBuilder;

public class ConditionFactory {
    public static Action addConditionGroup(RestrictionCondition state, boolean invert, String group, boolean includeHigherGroups, String note) {
        if (!state.isAllowed(AddCondition.GROUP)) throw new InterruptActionException("Cannot add condition 'Group' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.GROUP, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(GroupInputs.INVERT)
                )
                .onlyIf(group != null, () -> new ActionBuilder()
                        .search(GroupInputs.REQUIRED_GROUP)
                        .search(group)
                )
                .onlyIf(includeHigherGroups, () -> new ActionBuilder()
                        .search(GroupInputs.INCLUDE_HIGHER)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionVariable(RestrictionCondition state, boolean invert, Holder holder, String teamName, String variable, Comparator comparator, String value, String fallback, String note) {
        if (!state.isAllowed(AddCondition.VARIABLE)) throw new InterruptActionException("Cannot add condition 'Variable' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.VARIABLE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(VariableInputs.INVERT)
                )
                .onlyIf(holder == Holder.GLOBAL, () -> new ActionBuilder()
                        .search(VariableInputs.HOLDER)
                )
                .onlyIf(holder == Holder.TEAM, () -> new ActionBuilder()
                        .search(VariableInputs.HOLDER)
                        .search(VariableInputs.HOLDER)
                        .onlyIf(teamName != null && !teamName.equals("None"), () -> new ActionBuilder()
                                .search(VariableInputs.TEAM)
                                .search(teamName)
                        )
                )
                .onlyIf(variable != null && !variable.trim().isEmpty() && !variable.equals("Kills"), () -> new ActionBuilder()
                        .searchForChat(VariableInputs.VARIABLE, variable)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .search(VariableInputs.COMPARATOR)
                        .search(comparator.displayName)
                )
                .onlyIf(value != null && !value.trim().isEmpty(), () -> new ActionBuilder()
                        .searchForAnvil(VariableInputs.COMPARE_VALUE, value)
                )
                .onlyIf(fallback != null && !fallback.trim().isEmpty(), () -> new ActionBuilder()
                        .searchForAnvil(VariableInputs.FALLBACK_VALUE, fallback)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionPermission(RestrictionCondition state, boolean invert, Permission permission, String note) {
        if (!state.isAllowed(AddCondition.PERMISSION)) throw new InterruptActionException("Cannot add condition 'Permission' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.PERMISSION, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(PermissionInputs.INVERT)
                )
                .onlyIf(permission != null, () -> new ActionBuilder()
                        .search(PermissionInputs.PERMISSION)
                        .search(permission.displayName)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionRegion(RestrictionCondition state, boolean invert, String region, String note) {
        if (!state.isAllowed(AddCondition.REGION)) throw new InterruptActionException("Cannot add condition 'Region' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.REGION, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(RegionInputs.INVERT)
                )
                .onlyIf(region != null && !region.trim().isEmpty(), () -> new ActionBuilder()
                        .search(RegionInputs.REGION)
                        .search(region)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionHasItem(RestrictionCondition state, boolean invert, String snbtData, WhatCheck whatCheck, WhereCheck whereCheck, AmountCheck amountCheck, String note) {
        if (!state.isAllowed(AddCondition.HAS_ITEM)) throw new InterruptActionException("Cannot add condition 'Has Item' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.HAS_ITEM, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(HasItemInputs.INVERT)
                )
                .onlyIf(!snbtData.isEmpty(), () -> new ActionBuilder()
                        .search(HasItemInputs.ITEM)
                        .customItem(snbtData)
                )
                .onlyIf(whatCheck != null && whatCheck != WhatCheck.METADATA, () -> new ActionBuilder()
                        .search(HasItemInputs.WHAT_TO_CHECK)
                )
                .onlyIf(whereCheck != null && whereCheck != WhereCheck.ANYWHERE, () -> new ActionBuilder()
                        .search(HasItemInputs.WHERE_TO_CHECK)
                        .search(whereCheck.displayName)
                )
                .onlyIf(amountCheck != null && amountCheck != AmountCheck.ANY, () -> new ActionBuilder()
                        .search(HasItemInputs.REQUIRED_AMOUNT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionDoingParkour(RestrictionCondition state, boolean invert, String note) {
        if (!state.isAllowed(AddCondition.DOING_PARKOUR)) throw new InterruptActionException("Cannot add condition 'Doing Parkour' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.DOING_PARKOUR, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(DoingParkourInputs.INVERT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionPotion(RestrictionCondition state, boolean invert, Potion potion, String note) {
        if (!state.isAllowed(AddCondition.POTION)) throw new InterruptActionException("Cannot add condition 'Potion' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.POTION, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(EffectInputs.INVERT)
                )
                .onlyIf(potion != null, () -> new ActionBuilder()
                        .search(EffectInputs.EFFECT)
                        .search(potion.displayName)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionSneaking(RestrictionCondition state, boolean invert, String note) {
        if (!state.isAllowed(AddCondition.SNEAKING)) throw new InterruptActionException("Cannot add condition 'Sneaking' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.SNEAKING, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(SneakingInputs.INVERT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionFlying(RestrictionCondition state, boolean invert, String note) {
        if (!state.isAllowed(AddCondition.FLYING)) throw new InterruptActionException("Cannot add condition 'Flying' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.FLYING, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(FlyingInputs.INVERT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionHealth(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        if (!state.isAllowed(AddCondition.HEALTH)) throw new InterruptActionException("Cannot add condition 'Health' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.HEALTH, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(HealthInputs.INVERT)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .search(HealthInputs.COMPARATOR)
                        .search(comparator.displayName)
                )
                .onlyIf(value != null, () -> new ActionBuilder()
                        .searchForAnvil(HealthInputs.COMPARE_VALUE, value)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionMaxHealth(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        if (!state.isAllowed(AddCondition.MAX_HEALTH)) throw new InterruptActionException("Cannot add condition 'Max Health' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.MAX_HEALTH, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(MaxHealthInputs.INVERT)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .search(MaxHealthInputs.COMPARATOR)
                        .search(comparator.displayName)
                )
                .onlyIf(value != null, () -> new ActionBuilder()
                        .searchForAnvil(HealthInputs.COMPARE_VALUE, value)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionHunger(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        if (!state.isAllowed(AddCondition.HUNGER)) throw new InterruptActionException("Cannot add condition 'Hunger' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.HUNGER, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(HungerInputs.INVERT)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .search(HungerInputs.COMPARATOR)
                        .search(comparator.displayName)
                )
                .onlyIf(value != null, () -> new ActionBuilder()
                        .searchForAnvil(HungerInputs.COMPARE_VALUE, value)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionGamemode(RestrictionCondition state, boolean invert, Gamemode gamemode, String note) {
        if (!state.isAllowed(AddCondition.GAME_MODE)) throw new InterruptActionException("Cannot add condition 'Game Mode' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.GAME_MODE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(GamemodeInputs.INVERT)
                )
                .onlyIf(gamemode == Gamemode.ADVENTURE, () -> new ActionBuilder()
                        .search(GamemodeInputs.GAMEMODE)
                )
                .onlyIf(gamemode == Gamemode.SURVIVAL, () -> new ActionBuilder()
                        .search(GamemodeInputs.GAMEMODE)
                        .search(GamemodeInputs.GAMEMODE)
                )
                .onlyIf(gamemode == Gamemode.CREATIVE, () -> new ActionBuilder()
                        .search(GamemodeInputs.GAMEMODE)
                        .search(GamemodeInputs.GAMEMODE)
                        .search(GamemodeInputs.GAMEMODE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionPlaceholder(RestrictionCondition state, boolean invert, String placeholder, Comparator comparator, String value, String note) {
        if (!state.isAllowed(AddCondition.PLACEHOLDER)) throw new InterruptActionException("Cannot add condition 'Placeholder' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.PLACEHOLDER, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(PlaceholderInputs.INVERT)
                )
                .onlyIf(placeholder != null && !placeholder.trim().isEmpty(), () -> new ActionBuilder()
                        .searchForAnvil(PlaceholderInputs.PLACEHOLDER, placeholder)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .search(PlaceholderInputs.COMPARATOR)
                        .search(comparator.displayName)
                )
                .onlyIf(value != null && !value.trim().isEmpty(), () -> new ActionBuilder()
                        .searchForAnvil(PlaceholderInputs.COMPARE_VALUE, value)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionTeam(RestrictionCondition state, boolean invert, String team, String note) {
        if (!state.isAllowed(AddCondition.TEAM)) throw new InterruptActionException("Cannot add condition 'Team' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.TEAM, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(TeamInputs.INVERT)
                )
                .onlyIf(team != null && !team.equals("None") && !team.trim().isEmpty(), () -> new ActionBuilder()
                        .search(TeamInputs.REQUIRED_TEAM)
                        .search(team)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionPortalType(RestrictionCondition state, boolean invert, PortalType portalType, String note) {
        if (!state.isAllowed(AddCondition.PORTAL_TYPE)) throw new InterruptActionException("Cannot add condition 'Portal Type' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.PORTAL_TYPE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(PortalTypeInputs.INVERT)
                )
                .onlyIf(portalType != null && portalType != PortalType.NETHER_PORTAL, () -> new ActionBuilder()
                        .search(PortalTypeInputs.PORTAL_TYPE)
                        .search(portalType.displayName)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionPVPEnabled(RestrictionCondition state, boolean invert, String note) {
        if (!state.isAllowed(AddCondition.PVP_ENABLED)) throw new InterruptActionException("Cannot add condition 'PVP Enabled' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.PVP_ENABLED, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(PvpEnabledInputs.INVERT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionDamageCause(RestrictionCondition state, boolean invert, DamageCause damageCause, String note) {
        if (!state.isAllowed(AddCondition.DAMAGE_CAUSE)) throw new InterruptActionException("Cannot add condition 'Damage Cause' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.DAMAGE_CAUSE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(DamageCauseInputs.INVERT)
                )
                .onlyIf(damageCause != null, () -> new ActionBuilder()
                        .search(DamageCauseInputs.CAUSE)
                        .search(damageCause.displayName)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionDamageAmount(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        if (!state.isAllowed(AddCondition.DAMAGE_AMOUNT)) throw new InterruptActionException("Cannot add condition 'Damage Amount' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.DAMAGE_AMOUNT, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(DamageAmountInputs.INVERT)
                )
                .onlyIf(comparator != null && comparator != Comparator.EQUAL, () -> new ActionBuilder()
                        .search(DamageAmountInputs.COMPARATOR)
                        .search(comparator.displayName)
                )
                .onlyIf(value != null, () -> new ActionBuilder()
                        .searchForAnvil(DamageAmountInputs.COMPARE_VALUE, value)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionFishingEnvironment(RestrictionCondition state, boolean invert, FishingEnvironment fishingEnvironment, String note) {
        if (!state.isAllowed(AddCondition.BLOCK_TYPE)) throw new InterruptActionException("Cannot add condition 'Fishing Environment' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.BLOCK_TYPE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(FishingEnvironmentInputs.INVERT)
                )
                .onlyIf(fishingEnvironment != FishingEnvironment.WATER, () -> new ActionBuilder()
                        .search(FishingEnvironmentInputs.ENVIRONMENT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionBlockType(RestrictionCondition state, boolean invert, String snbtData, boolean matchTypeOnly, String note) {
        if (!state.isAllowed(AddCondition.BLOCK_TYPE)) throw new InterruptActionException("Cannot add condition 'Block Type' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.BLOCK_TYPE, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(BlockTypeInputs.INVERT)
                )
                .onlyIf(!snbtData.isEmpty(), () -> new ActionBuilder()
                        .search(BlockTypeInputs.ITEM)
                        .customItem(snbtData)
                )
                .onlyIf(matchTypeOnly, () -> new ActionBuilder()
                        .search(BlockTypeInputs.MATCH_TYPE_ONLY)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }

    public static Action addConditionIsItem(RestrictionCondition state, boolean invert, String snbtData, WhatCheck whatCheck, WhereCheck whereCheck, AmountCheck amountCheck, String note) {
        if (!state.isAllowed(AddCondition.IS_ITEM)) throw new InterruptActionException("Cannot add condition 'Is Item' here.");

        return new ActionBuilder()
                .addCondition(AddCondition.IS_ITEM, state)
                .onlyIf(invert, () -> new ActionBuilder()
                        .search(IsItemInputs.INVERT)
                )
                .onlyIf(!snbtData.isEmpty(), () -> new ActionBuilder()
                        .search(IsItemInputs.ITEM)
                        .customItem(snbtData)
                )
                .onlyIf(whatCheck != null && whatCheck != WhatCheck.METADATA, () -> new ActionBuilder()
                        .search(IsItemInputs.WHAT_TO_CHECK)
                )
                .onlyIf(whereCheck != null && whereCheck != WhereCheck.ANYWHERE, () -> new ActionBuilder()
                        .search(IsItemInputs.WHERE_TO_CHECK)
                        .search(whereCheck.displayName)
                )
                .onlyIf(amountCheck != null && amountCheck != AmountCheck.ANY, () -> new ActionBuilder()
                        .search(IsItemInputs.REQUIRED_AMOUNT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                .get();
    }
}
