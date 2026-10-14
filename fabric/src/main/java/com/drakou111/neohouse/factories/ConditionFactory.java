package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.constants.*;
import com.drakou111.neohouse.constants.conditions.*;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import com.drakou111.neohouse.utils.ActionBuilder;

public class ConditionFactory {

    private static Action buildCondition(RestrictionCondition state, AddCondition condition, InputParam... params) {
        if (!state.isAllowed(condition))
            throw new InterruptActionException("Cannot add condition '" + condition.displayName + "' here.");
        ActionBuilder b = new ActionBuilder().addCondition(condition);
        for (InputParam p : params) p.apply(b);
        return b.clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addConditionGroup(RestrictionCondition state, boolean invert, String group, boolean includeHigherGroups, String note) {
        return buildCondition(state, AddCondition.GROUP,
                InputParam.toggle(GroupInputs.INVERT, invert),
                InputParam.listSearch(GroupInputs.REQUIRED_GROUP, group),
                InputParam.toggle(GroupInputs.INCLUDE_HIGHER, includeHigherGroups),
                InputParam.note(note));
    }

    public static Action addConditionVariable(RestrictionCondition state, boolean invert, Holder holder, String teamName, String variable, Comparator comparator, String value, String fallback, String note) {
        String teamNameVal = holder == Holder.TEAM && teamName != null && !teamName.equals("None") ? teamName : null;
        return buildCondition(state, AddCondition.VARIABLE,
                InputParam.toggle(VariableInputs.INVERT, invert),
                InputParam.choice(VariableInputs.HOLDER, holder == Holder.GLOBAL ? 1 : holder == Holder.TEAM ? 2 : 0),
                InputParam.listSearch(VariableInputs.TEAM, teamNameVal),
                b -> { if (variable != null && !variable.trim().isEmpty() && !variable.equals("Kills")) b.searchForText(VariableInputs.VARIABLE, variable); },
                InputParam.listSearch(VariableInputs.COMPARATOR, comparator != null && comparator != Comparator.EQUAL ? comparator.displayName : null),
                InputParam.text(VariableInputs.COMPARE_VALUE, value, null),
                b -> { if (fallback != null && !fallback.trim().isEmpty()) b.searchForText(VariableInputs.FALLBACK_VALUE, fallback); },
                InputParam.note(note));
    }

    public static Action addConditionPermission(RestrictionCondition state, boolean invert, Permission permission, String note) {
        return buildCondition(state, AddCondition.PERMISSION,
                InputParam.toggle(PermissionInputs.INVERT, invert),
                InputParam.listSearch(PermissionInputs.PERMISSION, permission != null ? permission.displayName : null),
                InputParam.note(note));
    }

    public static Action addConditionRegion(RestrictionCondition state, boolean invert, String region, String note) {
        return buildCondition(state, AddCondition.REGION,
                InputParam.toggle(RegionInputs.INVERT, invert),
                InputParam.listSearch(RegionInputs.REGION, region),
                InputParam.note(note));
    }

    public static Action addConditionHasItem(RestrictionCondition state, boolean invert, String snbtData, WhatCheck whatCheck, WhereCheck whereCheck, AmountCheck amountCheck, String note) {
        return buildCondition(state, AddCondition.HAS_ITEM,
                InputParam.toggle(HasItemInputs.INVERT, invert),
                InputParam.item(HasItemInputs.ITEM, snbtData),
                InputParam.toggle(HasItemInputs.WHAT_TO_CHECK, whatCheck != null && whatCheck != WhatCheck.METADATA),
                InputParam.listSearch(HasItemInputs.WHERE_TO_CHECK, whereCheck != null && whereCheck != WhereCheck.ANYWHERE ? whereCheck.displayName : null),
                InputParam.toggle(HasItemInputs.REQUIRED_AMOUNT, amountCheck != null && amountCheck != AmountCheck.ANY),
                InputParam.note(note));
    }

    public static Action addConditionDoingParkour(RestrictionCondition state, boolean invert, String note) {
        return buildCondition(state, AddCondition.DOING_PARKOUR,
                InputParam.toggle(DoingParkourInputs.INVERT, invert),
                InputParam.note(note));
    }

    public static Action addConditionPotion(RestrictionCondition state, boolean invert, Potion potion, String note) {
        return buildCondition(state, AddCondition.POTION,
                InputParam.toggle(EffectInputs.INVERT, invert),
                InputParam.listSearch(EffectInputs.EFFECT, potion != null ? potion.displayName : null),
                InputParam.note(note));
    }

    public static Action addConditionSneaking(RestrictionCondition state, boolean invert, String note) {
        return buildCondition(state, AddCondition.SNEAKING,
                InputParam.toggle(SneakingInputs.INVERT, invert),
                InputParam.note(note));
    }

    public static Action addConditionFlying(RestrictionCondition state, boolean invert, String note) {
        return buildCondition(state, AddCondition.FLYING,
                InputParam.toggle(FlyingInputs.INVERT, invert),
                InputParam.note(note));
    }

    public static Action addConditionHealth(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        return buildCondition(state, AddCondition.HEALTH,
                InputParam.toggle(HealthInputs.INVERT, invert),
                InputParam.listSearch(HealthInputs.COMPARATOR, comparator != null && comparator != Comparator.EQUAL ? comparator.displayName : null),
                InputParam.text(HealthInputs.COMPARE_VALUE, value, null),
                InputParam.note(note));
    }

    public static Action addConditionMaxHealth(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        return buildCondition(state, AddCondition.MAX_HEALTH,
                InputParam.toggle(MaxHealthInputs.INVERT, invert),
                InputParam.listSearch(MaxHealthInputs.COMPARATOR, comparator != null && comparator != Comparator.EQUAL ? comparator.displayName : null),
                InputParam.text(HealthInputs.COMPARE_VALUE, value, null),
                InputParam.note(note));
    }

    public static Action addConditionHunger(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        return buildCondition(state, AddCondition.HUNGER,
                InputParam.toggle(HungerInputs.INVERT, invert),
                InputParam.listSearch(HungerInputs.COMPARATOR, comparator != null && comparator != Comparator.EQUAL ? comparator.displayName : null),
                InputParam.text(HungerInputs.COMPARE_VALUE, value, null),
                InputParam.note(note));
    }

    public static Action addConditionGamemode(RestrictionCondition state, boolean invert, Gamemode gamemode, String note) {
        int clicks = gamemode == Gamemode.ADVENTURE ? 1 : gamemode == Gamemode.SURVIVAL ? 2 : gamemode == Gamemode.CREATIVE ? 3 : 0;
        return buildCondition(state, AddCondition.GAME_MODE,
                InputParam.toggle(GamemodeInputs.INVERT, invert),
                InputParam.choice(GamemodeInputs.GAMEMODE, clicks),
                InputParam.note(note));
    }

    public static Action addConditionPlaceholder(RestrictionCondition state, boolean invert, String placeholder, Comparator comparator, String value, String note) {
        return buildCondition(state, AddCondition.PLACEHOLDER,
                InputParam.toggle(PlaceholderInputs.INVERT, invert),
                InputParam.textIfSet(PlaceholderInputs.PLACEHOLDER, placeholder),
                InputParam.listSearch(PlaceholderInputs.COMPARATOR, comparator != null && comparator != Comparator.EQUAL ? comparator.displayName : null),
                InputParam.textIfSet(PlaceholderInputs.COMPARE_VALUE, value),
                InputParam.note(note));
    }

    public static Action addConditionTeam(RestrictionCondition state, boolean invert, String team, String note) {
        String teamVal = team != null && !team.equals("None") && !team.trim().isEmpty() ? team : null;
        return buildCondition(state, AddCondition.TEAM,
                InputParam.toggle(TeamInputs.INVERT, invert),
                InputParam.listSearch(TeamInputs.REQUIRED_TEAM, teamVal),
                InputParam.note(note));
    }

    public static Action addConditionPortalType(RestrictionCondition state, boolean invert, PortalType portalType, String note) {
        return buildCondition(state, AddCondition.PORTAL_TYPE,
                InputParam.toggle(PortalTypeInputs.INVERT, invert),
                InputParam.listSearch(PortalTypeInputs.PORTAL_TYPE, portalType != null && portalType != PortalType.NETHER_PORTAL ? portalType.displayName : null),
                InputParam.note(note));
    }

    public static Action addConditionPVPEnabled(RestrictionCondition state, boolean invert, String note) {
        return buildCondition(state, AddCondition.PVP_ENABLED,
                InputParam.toggle(PvpEnabledInputs.INVERT, invert),
                InputParam.note(note));
    }

    public static Action addConditionDamageCause(RestrictionCondition state, boolean invert, DamageCause damageCause, String note) {
        return buildCondition(state, AddCondition.DAMAGE_CAUSE,
                InputParam.toggle(DamageCauseInputs.INVERT, invert),
                InputParam.listSearch(DamageCauseInputs.CAUSE, damageCause != null ? damageCause.displayName : null),
                InputParam.note(note));
    }

    public static Action addConditionDamageAmount(RestrictionCondition state, boolean invert, Comparator comparator, String value, String note) {
        return buildCondition(state, AddCondition.DAMAGE_AMOUNT,
                InputParam.toggle(DamageAmountInputs.INVERT, invert),
                InputParam.listSearch(DamageAmountInputs.COMPARATOR, comparator != null && comparator != Comparator.EQUAL ? comparator.displayName : null),
                InputParam.text(DamageAmountInputs.COMPARE_VALUE, value, null),
                InputParam.note(note));
    }

    public static Action addConditionFishingEnvironment(RestrictionCondition state, boolean invert, FishingEnvironment fishingEnvironment, String note) {
        return buildCondition(state, AddCondition.BLOCK_TYPE,
                InputParam.toggle(FishingEnvironmentInputs.INVERT, invert),
                InputParam.toggle(FishingEnvironmentInputs.ENVIRONMENT, fishingEnvironment != FishingEnvironment.WATER),
                InputParam.note(note));
    }

    public static Action addConditionBlockType(RestrictionCondition state, boolean invert, String snbtData, boolean matchTypeOnly, String note) {
        return buildCondition(state, AddCondition.BLOCK_TYPE,
                InputParam.toggle(BlockTypeInputs.INVERT, invert),
                InputParam.item(BlockTypeInputs.ITEM, snbtData),
                InputParam.toggle(BlockTypeInputs.MATCH_TYPE_ONLY, matchTypeOnly),
                InputParam.note(note));
    }

    public static Action addConditionIsItem(RestrictionCondition state, boolean invert, String snbtData, WhatCheck whatCheck, WhereCheck whereCheck, AmountCheck amountCheck, String note) {
        return buildCondition(state, AddCondition.IS_ITEM,
                InputParam.toggle(IsItemInputs.INVERT, invert),
                InputParam.item(IsItemInputs.ITEM, snbtData),
                InputParam.toggle(IsItemInputs.WHAT_TO_CHECK, whatCheck != null && whatCheck != WhatCheck.METADATA),
                InputParam.listSearch(IsItemInputs.WHERE_TO_CHECK, whereCheck != null && whereCheck != WhereCheck.ANYWHERE ? whereCheck.displayName : null),
                InputParam.toggle(IsItemInputs.REQUIRED_AMOUNT, amountCheck != null && amountCheck != AmountCheck.ANY),
                InputParam.note(note));
    }
}
