package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.constants.*;
import com.drakou111.neohouse.constants.actions.*;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import com.drakou111.neohouse.factories.params.DropItemParams;
import com.drakou111.neohouse.factories.params.PlaySoundParams;
import com.drakou111.neohouse.factories.params.TitleParams;
import com.drakou111.neohouse.factories.params.VariableParams;
import com.drakou111.neohouse.utils.ActionBuilder;

public class ActionFactory {

    private static Action buildSimpleAction(RestrictionAction state, AddAction action) {
        if (!state.isAllowed(action))
            throw new InterruptActionException("Cannot add action '" + action.displayName + "' here.");
        return new ActionBuilder().addAction(action).get();
    }

    private static Action buildAction(RestrictionAction state, AddAction action, InputParam... params) {
        return buildAction(state, action, Others.GENERAL_GO_BACK_H4, params);
    }

    private static Action buildAction(RestrictionAction state, AddAction action, int backId, InputParam... params) {
        if (!state.isAllowed(action))
            throw new InterruptActionException("Cannot add action '" + action.displayName + "' here.");
        ActionBuilder b = new ActionBuilder().addAction(action);
        for (InputParam p : params) p.apply(b);
        return b.clickSlotAndWait(backId, true).get();
    }


    public static Action addActionConditional(RestrictionAction state, boolean matchAny, Action conditions, Action ifs, Action elses, String note) {
        if (!state.isAllowed(AddAction.CONDITIONAL)) throw new InterruptActionException("Cannot add action 'Conditional' here.");
        return new ActionBuilder()
                .addAction(AddAction.CONDITIONAL)
                .onlyIf(matchAny, () -> new ActionBuilder()
                        .search(ConditionInputs.MATCH_ANY))
                .onlyIf(conditions != null, () -> new ActionBuilder()
                        .search(ConditionInputs.CONDITIONS)
                        .append(conditions)
                        .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true))
                .onlyIf(ifs != null, () -> new ActionBuilder()
                        .search(ConditionInputs.IF)
                        .append(ifs)
                        .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true))
                .onlyIf(elses != null, () -> new ActionBuilder()
                        .search(ConditionInputs.ELSE)
                        .append(elses)
                        .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true))
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4))
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionRandom(RestrictionAction state, Action randoms, String note) {
        if (!state.isAllowed(AddAction.RANDOM)) throw new InterruptActionException("Cannot add action 'Random' here.");
        return new ActionBuilder()
                .addAction(AddAction.RANDOM)
                .onlyIf(randoms != null, () -> new ActionBuilder()
                        .search(RandomInputs.ACTIONS)
                        .append(randoms)
                        .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true))
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4))
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionVariable(RestrictionAction state, VariableParams p) {
        String teamName = p.holder == Holder.TEAM && p.teamName != null
                       && !p.teamName.trim().isEmpty() && !p.teamName.equals("None") ? p.teamName : null;
        InputParam operatorParam = b -> {
            if (p.operator != Operator.INCREMENT) {
                b.search(SetVariableInputs.OPERATION);
                b.search(p.operator.displayName, null, true, null,
                        p.operator.requiresToggle ? new ActionBuilder()
                                .search(Operator.ADVANCED_OPERATIONS.displayName)
                                .search(p.operator.displayName) : null);
            }
        };
        return buildAction(state, AddAction.VARIABLE,
                InputParam.choice(SetVariableInputs.HOLDER, p.holder == Holder.GLOBAL ? 1 : p.holder == Holder.TEAM ? 2 : 0),
                InputParam.listSearch(SetVariableInputs.TEAM, teamName),
                InputParam.chat(SetVariableInputs.VARIABLE, p.variable, "Kills"),
                operatorParam,
                InputParam.anvil(SetVariableInputs.VALUE, p.value, "1"),
                InputParam.toggle(SetVariableInputs.AUTOMATIC_UNSET, p.automaticUnset),
                InputParam.note(p.note));
    }

    public static Action addActionKillPlayer(RestrictionAction state)        { return buildSimpleAction(state, AddAction.KILL);               }
    public static Action addActionFullHeal(RestrictionAction state)          { return buildSimpleAction(state, AddAction.FULL_HEAL);          }
    public static Action addActionResetInventory(RestrictionAction state)    { return buildSimpleAction(state, AddAction.CLEAR_INVENTORY);    }
    public static Action addActionParkourCheckpoint(RestrictionAction state) { return buildSimpleAction(state, AddAction.PARKOUR_CHECKPOINT); }
    public static Action addActionClearPotion(RestrictionAction state)       { return buildSimpleAction(state, AddAction.CLEAR_POTION);       }
    public static Action addActionExit(RestrictionAction state)              { return buildSimpleAction(state, AddAction.EXIT);               }
    public static Action addActionCancelEvent(RestrictionAction state)       { return buildSimpleAction(state, AddAction.CANCEL_EVENT);       }
    public static Action addActionCloseMenu(RestrictionAction state)         { return buildSimpleAction(state, AddAction.CLOSE_MENU);         }

    public static Action addActionChangeGroup(RestrictionAction state, String groupName, boolean demotionProtection, String note) {
        return buildAction(state, AddAction.CHANGE_GROUP,
                InputParam.listSearch(PlayerGroupInputs.GROUP, groupName),
                InputParam.toggle(PlayerGroupInputs.DEMOTION_PROTECTION, !demotionProtection),
                InputParam.note(note));
    }

    public static Action addActionTitle(RestrictionAction state, TitleParams p) {
        String finalTitle    = p.title    == null || p.title.trim().isEmpty()    ? "&l" : p.title;
        String finalSubTitle = p.subtitle == null || p.subtitle.trim().isEmpty() ? "&l" : p.subtitle;
        return buildAction(state, AddAction.TITLE,
                InputParam.chat(TitleInputs.TITLE,    finalTitle,    "Hello World!"),
                InputParam.chat(TitleInputs.SUBTITLE, finalSubTitle, "&l"),
                InputParam.anvil(TitleInputs.FADEIN,  String.valueOf(p.fadeIn),  "1"),
                InputParam.anvil(TitleInputs.STAY,    String.valueOf(p.stay),    "5"),
                InputParam.anvil(TitleInputs.FADEOUT, String.valueOf(p.fadeOut), "1"),
                InputParam.note(p.note));
    }

    public static Action addActionActionBar(RestrictionAction state, String text, String note) {
        String finalText = text == null || text.trim().isEmpty() ? "&l" : text;
        return buildAction(state, AddAction.ACTION_BAR,
                InputParam.chat(ActionBarInputs.MESSAGE, finalText, "Hello World!"),
                InputParam.note(note));
    }

    public static Action addActionMaxHealth(RestrictionAction state, String maxHealth, RestrictedOperator operator, boolean healOnChange, String note) {
        return buildAction(state, AddAction.MAX_HEALTH,
                InputParam.anvil(MaxHealthInputs.MAX_HEALTH, maxHealth, "20"),
                InputParam.listSearch(MaxHealthInputs.MODE, operator != RestrictedOperator.SET ? operator.displayName : null),
                InputParam.toggle(MaxHealthInputs.HEAL_ON_CHANGE, !healOnChange),
                InputParam.note(note));
    }

    public static Action addActionGiveItem(RestrictionAction state, String snbtData, boolean allowMultiple, InventorySlots inventorySlot, boolean replaceExistingItem, String note) {
        return buildAction(state, AddAction.GIVE_ITEM,
                InputParam.item(GiveItemInputs.ITEM, snbtData),
                InputParam.toggle(GiveItemInputs.ALLOW_MULTIPLE, allowMultiple),
                InputParam.listSearch(GiveItemInputs.INVENTORY_SLOT, inventorySlot != InventorySlots.FIRST_AVAILABLE_SLOT ? inventorySlot.displayName : null),
                InputParam.toggle(GiveItemInputs.REPLACE_ITEM, replaceExistingItem),
                InputParam.note(note));
    }

    public static Action addActionRemoveItem(RestrictionAction state, String snbtData, String note) {
        return buildAction(state, AddAction.REMOVE_ITEM,
                InputParam.item(RemoveItemInputs.ITEM, snbtData),
                InputParam.note(note));
    }

    public static Action addActionSendMessage(RestrictionAction state, String text, String note) {
        String finalText = text == null || text.trim().isEmpty() ? "&l" : text;
        return buildAction(state, AddAction.CHAT_MESSAGE,
                InputParam.chat(ChatMessageInputs.MESSAGE, finalText, "Hello!"),
                InputParam.note(note));
    }

    public static Action addActionApplyPotion(RestrictionAction state, Potion potion, int duration, int level, boolean override, boolean showIcon, String note) {
        return buildAction(state, AddAction.APPLY_POTION,
                InputParam.listSearch(ApplyPotionInputs.EFFECT, potion != null ? potion.displayName : null),
                InputParam.anvil(ApplyPotionInputs.DURATION, String.valueOf(duration), "60"),
                InputParam.anvil(ApplyPotionInputs.LEVEL,    String.valueOf(level),    "1"),
                InputParam.toggle(ApplyPotionInputs.OVERRIDE,  override),
                InputParam.toggle(ApplyPotionInputs.SHOW_ICON, !showIcon),
                InputParam.note(note));
    }

    public static Action addActionGiveXP(RestrictionAction state, int xp, String note) {
        return buildAction(state, AddAction.GIVE_XP,
                InputParam.anvil(GiveExperienceInputs.LEVELS, String.valueOf(xp), "1"),
                InputParam.note(note));
    }

    public static Action addActionSendLobby(RestrictionAction state, Lobby lobby, String note) {
        return buildAction(state, AddAction.SEND_LOBBY,
                InputParam.listSearch(SendLobbyInputs.LOCATION, lobby != null ? lobby.displayName : null),
                InputParam.note(note));
    }

    public static Action addActionTeleport(RestrictionAction state, Teleport teleport, String customLocation, boolean preventInsideBlocks, String note) {
        return buildAction(state, AddAction.TELEPORT,
                InputParam.predefined(TeleportInputs.LOCATION, teleport != null && teleport != Teleport.CUSTOM, teleport != null ? teleport.displayName : null),
                InputParam.withCustom(TeleportInputs.LOCATION, teleport == Teleport.CUSTOM, Teleport.CUSTOM.displayName, customLocation),
                InputParam.toggle(TeleportInputs.PREVENT_TELEPORT_INSIDE_BLOCKS, preventInsideBlocks),
                InputParam.note(note));
    }

    public static Action addActionFailParkour(RestrictionAction state, String text, String note) {
        String finalText = text == null || text.trim().isEmpty() ? "&l" : text;
        return buildAction(state, AddAction.FAIL_PARKOUR,
                InputParam.chat(FailParkourInputs.REASON, finalText, "Failed!"),
                InputParam.note(note));
    }

    public static Action addActionPlaySound(RestrictionAction state, PlaySoundParams p) {
        return buildAction(state, AddAction.PLAY_SOUND,
                InputParam.listSearch(PlaySoundInputs.SOUND, p.sound != null ? p.sound.displayName : null),
                InputParam.anvil(PlaySoundInputs.VOLUME, String.valueOf(p.volume), "0.7"),
                InputParam.anvil(PlaySoundInputs.PITCH,  String.valueOf(p.pitch),  "1.0"),
                InputParam.predefined(PlaySoundInputs.LOCATION, p.teleport != null && p.teleport != Teleport.CUSTOM, p.teleport != null ? p.teleport.displayName : null),
                InputParam.withCustom(PlaySoundInputs.LOCATION, p.teleport == Teleport.CUSTOM, Teleport.CUSTOM.displayName, p.customLocation),
                InputParam.note(p.note));
    }

    public static Action addActionCompassTarget(RestrictionAction state, Teleport teleport, String customLocation, String note) {
        return buildAction(state, AddAction.COMPASS_TARGET,
                InputParam.predefined(CompassTargetInputs.LOCATION, teleport != null && teleport != Teleport.CUSTOM, teleport != null ? teleport.displayName : null),
                InputParam.withCustom(CompassTargetInputs.LOCATION, teleport == Teleport.CUSTOM, Teleport.CUSTOM.displayName, customLocation),
                InputParam.note(note));
    }

    public static Action addActionGamemode(RestrictionAction state, Gamemode gamemode, String note) {
        int clicks = gamemode == Gamemode.ADVENTURE ? 1 : gamemode == Gamemode.SURVIVAL ? 2 : gamemode == Gamemode.CREATIVE ? 3 : 0;
        return buildAction(state, AddAction.SET_GAME_MODE,
                InputParam.choice(GamemodeInputs.GAMEMODE, clicks),
                InputParam.note(note));
    }

    public static Action addActionChangeHealth(RestrictionAction state, String health, RestrictedOperator operator, String note) {
        return buildAction(state, AddAction.CHANGE_HEALTH,
                InputParam.anvil(HealthInputs.HEALTH, health, "20"),
                InputParam.listSearch(HealthInputs.MODE, operator != RestrictedOperator.SET ? operator.displayName : null),
                InputParam.note(note));
    }

    public static Action addActionChangeHunger(RestrictionAction state, String hunger, RestrictedOperator operator, String note) {
        return buildAction(state, AddAction.CHANGE_HUNGER,
                InputParam.anvil(HungerInputs.LEVEL, hunger, "20"),
                InputParam.listSearch(HungerInputs.MODE, operator != RestrictedOperator.SET ? operator.displayName : null),
                InputParam.note(note));
    }

    public static Action addActionTriggerFunction(RestrictionAction state, String name, boolean forAll, String note) {
        return buildAction(state, AddAction.TRIGGER_FUNCTION,
                InputParam.listSearch(TriggerFunctionInputs.FUNCTION, name),
                InputParam.toggle(TriggerFunctionInputs.TRIGGER_FOR_ALL, forAll),
                InputParam.note(note));
    }

    public static Action addActionApplyLayout(RestrictionAction state, String name, String note) {
        return buildAction(state, AddAction.APPLY_LAYOUT,
                InputParam.listSearch(LayoutInputs.LAYOUT, name),
                InputParam.note(note));
    }

    public static Action addActionEnchantHeldItem(RestrictionAction state, Enchant enchant, int level, String note) {
        return buildAction(state, AddAction.ENCHANT_HELD_ITEM,
                InputParam.listSearch(EnchantHeldInputs.ENCHANTMENT, enchant != null ? enchant.displayName : null),
                InputParam.anvil(EnchantHeldInputs.LEVEL, String.valueOf(level), "1"),
                InputParam.note(note));
    }

    public static Action addActionPause(RestrictionAction state, int timeToWait, String note) {
        return buildAction(state, AddAction.PAUSE,
                InputParam.anvil(PauseInputs.TICKS, String.valueOf(timeToWait), "20"),
                InputParam.note(note));
    }

    public static Action addActionTeam(RestrictionAction state, String team, String note) {
        return buildAction(state, AddAction.SET_TEAM,
                InputParam.listSearch(TeamInputs.TEAM, team != null && !team.equals("None") ? team : null),
                InputParam.note(note));
    }

    public static Action addActionDisplayMenu(RestrictionAction state, String menu, String note) {
        return buildAction(state, AddAction.DISPLAY_MENU,
                InputParam.listSearch(DisplayMenuInputs.MENU, menu),
                InputParam.note(note));
    }

    public static Action addActionDropItem(RestrictionAction state, DropItemParams p) {
        return buildAction(state, AddAction.DROP_ITEM, Others.GENERAL_GO_BACK_H5,
                InputParam.item(DropItemInputs.ITEM, p.snbtData),
                InputParam.predefined(DropItemInputs.LOCATION, p.teleport != null && p.teleport != Teleport.CUSTOM, p.teleport != null ? p.teleport.displayName : null),
                InputParam.withCustom(DropItemInputs.LOCATION, p.teleport == Teleport.CUSTOM, Teleport.CUSTOM.displayName, p.customLocation),
                InputParam.toggle(DropItemInputs.DROP_NATURALLY,    !p.dropNaturally),
                InputParam.toggle(DropItemInputs.PREVENT_MERGE,     p.disableItemMerging),
                InputParam.anvil(DropItemInputs.DESPAWN_DURATION,   String.valueOf(p.despawnDuration), "6000"),
                InputParam.anvil(DropItemInputs.PICKUP_DELAY,       String.valueOf(p.pickupDelay),     "10"),
                InputParam.toggle(DropItemInputs.PRIORITIZE_PLAYER, p.prioritizePlayer),
                InputParam.toggle(DropItemInputs.FALLBACK_INVENTORY, p.fallbackToInventory),
                InputParam.note(p.note));
    }

    public static Action addActionChangeVelocity(RestrictionAction state, String velocityX, String velocityY, String velocityZ, String note) {
        return buildAction(state, AddAction.CHANGE_VELOCITY,
                InputParam.anvil(ChangeVelocityInputs.X_DIRECTION, velocityX, "10"),
                InputParam.anvil(ChangeVelocityInputs.Y_DIRECTION, velocityY, "10"),
                InputParam.anvil(ChangeVelocityInputs.Z_DIRECTION, velocityZ, "10"),
                InputParam.note(note));
    }

    public static Action addActionLaunchTarget(RestrictionAction state, Teleport teleport, String customLocation, String strength, String note) {
        return buildAction(state, AddAction.LAUNCH_TO_TARGET,
                InputParam.predefined(LaunchTargetInputs.TARGET_LOCATION, teleport != null && teleport != Teleport.CUSTOM, teleport != null ? teleport.displayName : null),
                InputParam.withCustom(LaunchTargetInputs.TARGET_LOCATION, teleport == Teleport.CUSTOM, Teleport.CUSTOM.displayName, customLocation),
                InputParam.anvil(LaunchTargetInputs.STRENGTH, strength, "2"),
                InputParam.note(note));
    }

    public static Action addActionWeather(RestrictionAction state, Weather weather, String note) {
        int clicks = weather == Weather.SUNNY ? 1 : weather == Weather.RAINING ? 2 : 0;
        return buildAction(state, AddAction.SET_WEATHER,
                InputParam.choice(SetWeatherInputs.WEATHER, clicks),
                InputParam.note(note));
    }

    public static Action addActionTime(RestrictionAction state, Time time, String customTime, String note) {
        return buildAction(state, AddAction.SET_TIME,
                InputParam.predefined(SetTimeInputs.TIME, time != null && time != Time.CUSTOM, time != null ? time.displayName : null),
                InputParam.withCustom(SetTimeInputs.TIME, time == Time.CUSTOM, Time.CUSTOM.displayName, customTime),
                InputParam.note(note));
    }

    public static Action addActionNametag(RestrictionAction state, boolean displayNametag, String note) {
        return buildAction(state, AddAction.NAME_TAG_DISPLAY,
                InputParam.toggle(NameTagInputs.DISPLAY, !displayNametag),
                InputParam.note(note));
    }
}
