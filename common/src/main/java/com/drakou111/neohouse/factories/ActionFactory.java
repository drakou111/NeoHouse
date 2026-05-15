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

    public static Action addActionConditional(RestrictionAction state, boolean matchAny, Action conditions, Action ifs, Action elses, String note) {
        if (!state.isAllowed(AddAction.CONDITIONAL)) throw new InterruptActionException("Cannot add action 'Conditional' here.");

        return new ActionBuilder()
                .addAction(AddAction.CONDITIONAL, state)
                .onlyIf(matchAny, () ->  new ActionBuilder()
                        .search(ConditionInputs.MATCH_ANY)
                )
                .onlyIf(conditions != null, () ->  new ActionBuilder()
                        .search(ConditionInputs.CONDITIONS)
                        .append(conditions)
                        .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                )
                .onlyIf(ifs != null, () ->  new ActionBuilder()
                        .search(ConditionInputs.IF)
                        .append(ifs)
                        .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                )
                .onlyIf(elses != null, () -> new ActionBuilder()
                        .search(ConditionInputs.ELSE)
                        .append(elses)
                        .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionChangeGroup(RestrictionAction state, String groupName, boolean demotionProtection, String note) {
        if (!state.isAllowed(AddAction.CHANGE_GROUP)) throw new InterruptActionException("Cannot add action 'Change Group' here.");

        return new ActionBuilder()
                .addAction(AddAction.CHANGE_GROUP, state)
                .onlyIf(groupName != null && !groupName.trim().isEmpty(), () -> new ActionBuilder()
                        .search(PlayerGroupInputs.GROUP)
                        .search(groupName)
                )
                .onlyIf(!demotionProtection, () -> new ActionBuilder()
                        .search(PlayerGroupInputs.DEMOTION_PROTECTION)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();

    }

    public static Action addActionKillPlayer(RestrictionAction state) {
        if (!state.isAllowed(AddAction.KILL)) throw new InterruptActionException("Cannot add action 'Kill' here.");

        return new ActionBuilder()
                .addAction(AddAction.KILL, state)
                .get();
    }

    public static Action addActionFullHeal(RestrictionAction state) {
        if (!state.isAllowed(AddAction.FULL_HEAL)) throw new InterruptActionException("Cannot add action 'Full Heal' here.");

        return new ActionBuilder()
                .addAction(AddAction.FULL_HEAL, state)
                .get();
    }

    public static Action addActionTitle(RestrictionAction state, TitleParams p) {
        if (!state.isAllowed(AddAction.TITLE)) throw new InterruptActionException("Cannot add action 'Title' here.");

        String finalTitle;
        if (p.title == null || p.title.trim().isEmpty()) finalTitle = "&l";
        else finalTitle = p.title;

        String finalSubTitle;
        if (p.subtitle == null || p.subtitle.trim().isEmpty()) finalSubTitle = "&l";
        else finalSubTitle = p.subtitle;

        return new ActionBuilder()
                .addAction(AddAction.TITLE, state)
                .onlyIf(!finalTitle.equals("Hello World!"), () -> new ActionBuilder()
                        .searchForChat(TitleInputs.TITLE, finalTitle)
                )
                .onlyIf(!finalSubTitle.equals("&l"), () -> new ActionBuilder()
                        .searchForChat(TitleInputs.SUBTITLE, finalSubTitle)
                )
                .onlyIf(p.fadeIn != 1, () -> new ActionBuilder()
                        .searchForAnvil(TitleInputs.FADEIN, String.valueOf(p.fadeIn))
                )
                .onlyIf(p.stay != 5, () -> new ActionBuilder()
                        .searchForAnvil(TitleInputs.STAY, String.valueOf(p.stay))
                )
                .onlyIf(p.fadeOut != 1, () -> new ActionBuilder()
                        .searchForAnvil(TitleInputs.FADEOUT, String.valueOf(p.fadeOut))
                )
                .onlyIf(p.note != null && !p.note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(p.note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionActionBar(RestrictionAction state, String text, String note) {
        if (!state.isAllowed(AddAction.ACTION_BAR)) throw new InterruptActionException("Cannot add action 'Action Bar' here.");

        String finalText;
        if (text == null || text.trim().isEmpty()) finalText = "&l";
        else finalText = text;

        return new ActionBuilder()
                .addAction(AddAction.ACTION_BAR, state)
                .onlyIf(!finalText.equals("Hello World!"), () -> new ActionBuilder()
                        .searchForChat(ActionBarInputs.MESSAGE, finalText)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionResetInventory(RestrictionAction state) {
        if (!state.isAllowed(AddAction.CLEAR_INVENTORY)) throw new InterruptActionException("Cannot add action 'Clear Inventory' here.");

        return new ActionBuilder()
                .addAction(AddAction.CLEAR_INVENTORY, state)
                .get();
    }

    public static Action addActionMaxHealth(RestrictionAction state, String maxHealth, RestrictedOperator operator, boolean healOnChange, String note) {
        if (!state.isAllowed(AddAction.MAX_HEALTH)) throw new InterruptActionException("Cannot add action 'Max Health' here.");

        return new ActionBuilder()
                .addAction(AddAction.MAX_HEALTH, state)
                .onlyIf(maxHealth != null && !maxHealth.equals("20"), () -> new ActionBuilder()
                        .searchForAnvil(MaxHealthInputs.MAX_HEALTH, String.valueOf(maxHealth))
                )
                .onlyIf(operator != RestrictedOperator.SET, () -> new ActionBuilder()
                        .search(MaxHealthInputs.MODE)
                        .search(operator.displayName)
                )
                .onlyIf(!healOnChange, () -> new ActionBuilder()
                        .search(MaxHealthInputs.HEAL_ON_CHANGE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionParkourCheckpoint(RestrictionAction state) {
        if (!state.isAllowed(AddAction.PARKOUR_CHECKPOINT)) throw new InterruptActionException("Cannot add action 'Parkour Checkpoint' here.");

        return new ActionBuilder()
                .addAction(AddAction.PARKOUR_CHECKPOINT, state)
                .get();
    }

    public static Action addActionGiveItem(RestrictionAction state, String snbtData, boolean allowMultiple, InventorySlots inventorySlot, boolean replaceExistingItem, String note) {
        if (!state.isAllowed(AddAction.GIVE_ITEM)) throw new InterruptActionException("Cannot add action 'Give Item' here.");

        return new ActionBuilder()
                .addAction(AddAction.GIVE_ITEM, state)
                .onlyIf(!snbtData.isEmpty(), () -> new ActionBuilder()
                        .search(GiveItemInputs.ITEM)
                        .customItem(snbtData)
                )
                .onlyIf(allowMultiple, () -> new ActionBuilder()
                        .search(GiveItemInputs.ALLOW_MULTIPLE)
                )
                .onlyIf(inventorySlot != InventorySlots.FIRST_AVAILABLE_SLOT, () -> new ActionBuilder()
                        .search(GiveItemInputs.INVENTORY_SLOT)
                        .search(inventorySlot.displayName)
                )
                .onlyIf(replaceExistingItem, () -> new ActionBuilder()
                        .search(GiveItemInputs.REPLACE_ITEM)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionRemoveItem(RestrictionAction state, String snbtData, String note) {
        if (!state.isAllowed(AddAction.REMOVE_ITEM)) throw new InterruptActionException("Cannot add action 'Remove Item' here.");

        return new ActionBuilder()
                .addAction(AddAction.REMOVE_ITEM, state)
                .onlyIf(!snbtData.isEmpty(), () -> new ActionBuilder()
                        .search(RemoveItemInputs.ITEM)
                        .customItem(snbtData)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionSendMessage(RestrictionAction state, String text, String note) {
        if (!state.isAllowed(AddAction.CHAT_MESSAGE)) throw new InterruptActionException("Cannot add action 'Send Chat Message' here.");

        String finalText;
        if (text == null || text.trim().isEmpty()) finalText = "&l";
        else finalText = text;

        return new ActionBuilder()
                .addAction(AddAction.CHAT_MESSAGE, state)
                .onlyIf(!finalText.equals("Hello!"), () -> new ActionBuilder()
                        .searchForChat(ChatMessageInputs.MESSAGE, finalText)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionApplyPotion(RestrictionAction state, Potion potion, int duration, int level, boolean override, boolean showIcon, String note) {
        if (!state.isAllowed(AddAction.APPLY_POTION)) throw new InterruptActionException("Cannot add action 'Apply Potion' here.");

        return new ActionBuilder()
                .addAction(AddAction.APPLY_POTION, state)
                .onlyIf(potion != null, () -> new ActionBuilder()
                        .search(ApplyPotionInputs.EFFECT)
                        .search(potion.displayName)
                )
                .onlyIf(duration != 60, () -> new ActionBuilder()
                        .searchForAnvil(ApplyPotionInputs.DURATION, String.valueOf(duration))
                )
                .onlyIf(level != 1, () -> new ActionBuilder()
                        .searchForAnvil(ApplyPotionInputs.LEVEL, String.valueOf(level))
                )
                .onlyIf(override, () -> new ActionBuilder()
                        .search(ApplyPotionInputs.OVERRIDE)
                )
                .onlyIf(!showIcon, () -> new ActionBuilder()
                        .search(ApplyPotionInputs.SHOW_ICON)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionClearPotion(RestrictionAction state) {
        if (!state.isAllowed(AddAction.CLEAR_POTION)) throw new InterruptActionException("Cannot add action 'Clear Potion' here.");

        return new ActionBuilder()
                .addAction(AddAction.CLEAR_POTION, state)
                .get();
    }

    public static Action addActionGiveXP(RestrictionAction state, int xp, String note) {
        if (!state.isAllowed(AddAction.GIVE_XP)) throw new InterruptActionException("Cannot add action 'Give XP' here.");

        return new ActionBuilder()
                .addAction(AddAction.GIVE_XP, state)
                .onlyIf(xp != 1, () -> new ActionBuilder()
                        .searchForAnvil(GiveExperienceInputs.LEVELS, String.valueOf(xp))
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionSendLobby(RestrictionAction state, Lobby lobby, String note) {
        if (!state.isAllowed(AddAction.SEND_LOBBY)) throw new InterruptActionException("Cannot add action 'Send to Lobby' here.");

        return new ActionBuilder()
                .addAction(AddAction.SEND_LOBBY, state)
                .onlyIf(lobby != null, () -> new ActionBuilder()
                        .search(SendLobbyInputs.LOCATION)
                        .search(lobby.displayName)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionVariable(RestrictionAction state, VariableParams p) {
        if (!state.isAllowed(AddAction.VARIABLE)) throw new InterruptActionException("Cannot add action 'Variable' here.");

        return new ActionBuilder()
            .addAction(AddAction.VARIABLE, state)
            .onlyIf(p.holder == Holder.GLOBAL, () -> new ActionBuilder()
                .search(SetVariableInputs.HOLDER)
            )
            .onlyIf(p.holder == Holder.TEAM, () -> new ActionBuilder()
                    .search(SetVariableInputs.HOLDER)
                    .search(SetVariableInputs.HOLDER)
            )
            .onlyIf(p.holder == Holder.TEAM && p.teamName != null && !p.teamName.trim().isEmpty() && !p.teamName.equals("None"), () -> new ActionBuilder()
                    .search(SetVariableInputs.TEAM)
                    .search(p.teamName)
            )
            .onlyIf(!p.variable.equals("Kills"), () -> new ActionBuilder()
                    .searchForChat(SetVariableInputs.VARIABLE, p.variable)
            )
            .onlyIf(p.operator != Operator.INCREMENT, () -> new ActionBuilder()
                    .search(SetVariableInputs.OPERATION)
                    .search(p.operator.displayName, null, true, null,
                            p.operator.requiresToggle ? new ActionBuilder()
                                    .search(Operator.ADVANCED_OPERATIONS.displayName)
                                    .search(p.operator.displayName) : null)
            )
            .onlyIf(!p.value.equals("1"), () -> new ActionBuilder()
                    .searchForAnvil(SetVariableInputs.VALUE, p.value)
            )
            .onlyIf(p.automaticUnset, () -> new ActionBuilder()
                    .search(SetVariableInputs.AUTOMATIC_UNSET)
            )
            .onlyIf(p.note != null && !p.note.trim().isEmpty(), () -> new ActionBuilder()
                    .chatInput(p.note, Others.GENERAL_NOTE_H4)
            )
            .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
            .get();
    }

    public static Action addActionTeleport(RestrictionAction state, Teleport teleport, String customLocation, boolean preventInsideBlocks, String note) {
        if (!state.isAllowed(AddAction.TELEPORT)) throw new InterruptActionException("Cannot add action 'Teleport' here.");

        return new ActionBuilder()
                .addAction(AddAction.TELEPORT, state)
                .onlyIf(teleport != null && teleport != Teleport.CUSTOM, () -> new ActionBuilder()
                        .search(TeleportInputs.LOCATION)
                        .search(teleport.displayName)
                )
                .onlyIf(teleport == Teleport.CUSTOM, () -> new ActionBuilder()
                        .search(TeleportInputs.LOCATION)
                        .searchForAnvil(Teleport.CUSTOM.displayName, customLocation)
                )
                .onlyIf(preventInsideBlocks, () -> new ActionBuilder()
                        .search(TeleportInputs.PREVENT_TELEPORT_INSIDE_BLOCKS)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionFailParkour(RestrictionAction state, String text, String note) {
        if (!state.isAllowed(AddAction.FAIL_PARKOUR)) throw new InterruptActionException("Cannot add action 'Fail Parkour' here.");

        String finalText;
        if (text == null || text.trim().isEmpty()) finalText = "&l";
        else finalText = text;

        return new ActionBuilder()
                .addAction(AddAction.FAIL_PARKOUR, state)
                .onlyIf(!finalText.equals("Failed!"), () -> new ActionBuilder()
                        .searchForChat(FailParkourInputs.REASON, finalText)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionPlaySound(RestrictionAction state, PlaySoundParams p) {
        if (!state.isAllowed(AddAction.PLAY_SOUND)) throw new InterruptActionException("Cannot add action 'Play Sound' here.");

        return new ActionBuilder()
                .addAction(AddAction.PLAY_SOUND, state)
                .onlyIf(p.sound != null, () -> new ActionBuilder()
                        .search(PlaySoundInputs.SOUND)
                        .search(p.sound.displayName)
                )
                .onlyIf(p.volume != 0.7D, () -> new ActionBuilder()
                        .searchForAnvil(PlaySoundInputs.VOLUME, String.valueOf(p.volume))
                )
                .onlyIf(p.pitch != 1.0D, () -> new ActionBuilder()
                        .searchForAnvil(PlaySoundInputs.PITCH, String.valueOf(p.pitch))
                )
                .onlyIf(p.teleport != null && p.teleport != Teleport.CUSTOM, () -> new ActionBuilder()
                        .search(PlaySoundInputs.LOCATION)
                        .search(p.teleport.displayName)
                )
                .onlyIf(p.teleport == Teleport.CUSTOM, () -> new ActionBuilder()
                        .search(PlaySoundInputs.LOCATION)
                        .searchForAnvil(Teleport.CUSTOM.displayName, p.customLocation)
                )
                .onlyIf(p.note != null && !p.note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(p.note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionCompassTarget(RestrictionAction state, Teleport teleport, String customLocation, String note) {
        if (!state.isAllowed(AddAction.COMPASS_TARGET)) throw new InterruptActionException("Cannot add action 'Set Compass Target' here.");

        return new ActionBuilder()
                .addAction(AddAction.COMPASS_TARGET, state)
                .onlyIf(teleport != null && teleport != Teleport.CUSTOM, () -> new ActionBuilder()
                        .search(CompassTargetInputs.LOCATION)
                        .search(teleport.displayName)
                )
                .onlyIf(teleport == Teleport.CUSTOM, () -> new ActionBuilder()
                        .search(CompassTargetInputs.LOCATION)
                        .searchForAnvil(Teleport.CUSTOM.displayName, customLocation)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionGamemode(RestrictionAction state, Gamemode gamemode, String note) {
        if (!state.isAllowed(AddAction.SET_GAME_MODE)) throw new InterruptActionException("Cannot add action 'Set Game Mode' here.");

        return new ActionBuilder()
                .addAction(AddAction.SET_GAME_MODE, state)
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
                .get();
    }

    public static Action addActionChangeHealth(RestrictionAction state, String health, RestrictedOperator operator, String note) {
        if (!state.isAllowed(AddAction.CHANGE_HEALTH)) throw new InterruptActionException("Cannot add action 'Change Health' here.");

        return new ActionBuilder()
                .addAction(AddAction.CHANGE_HEALTH, state)
                .onlyIf(!health.equals("20"), () -> new ActionBuilder()
                        .searchForAnvil(HealthInputs.HEALTH, health)
                )
                .onlyIf(operator != RestrictedOperator.SET, () -> new ActionBuilder()
                        .search(HealthInputs.MODE)
                        .search(operator.displayName)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionChangeHunger(RestrictionAction state, String hunger, RestrictedOperator operator, String note) {
        if (!state.isAllowed(AddAction.CHANGE_HUNGER)) throw new InterruptActionException("Cannot add action 'Change Hunger' here.");

        return new ActionBuilder()
                .addAction(AddAction.CHANGE_HUNGER, state)
                .onlyIf(!hunger.equals("20"), () -> new ActionBuilder()
                        .searchForAnvil(HungerInputs.LEVEL, hunger)
                )
                .onlyIf(operator != RestrictedOperator.SET, () -> new ActionBuilder()
                        .search(HungerInputs.MODE)
                        .search(operator.displayName)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionRandom(RestrictionAction state, Action randoms, String note) {
        if (!state.isAllowed(AddAction.RANDOM)) throw new InterruptActionException("Cannot add action 'Random' here.");

        return new ActionBuilder()
                .addAction(AddAction.RANDOM, state)
                .onlyIf(randoms != null, () -> new ActionBuilder()
                        .search(RandomInputs.ACTIONS)
                        .append(randoms)
                        .clickSlotAndWait(Others.GENERAL_GO_BACK_H6, true)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionTriggerFunction(RestrictionAction state, String name, boolean forAll, String note) {
        if (!state.isAllowed(AddAction.TRIGGER_FUNCTION)) throw new InterruptActionException("Cannot add action 'Trigger Function' here.");

        return new ActionBuilder()
                .addAction(AddAction.TRIGGER_FUNCTION, state)
                .onlyIf(name != null && !name.trim().isEmpty(), () -> new ActionBuilder()
                        .search(TriggerFunctionInputs.FUNCTION)
                        .search(name)
                )
                .onlyIf(forAll, () -> new ActionBuilder()
                        .search(TriggerFunctionInputs.TRIGGER_FOR_ALL)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionApplyLayout(RestrictionAction state, String name, String note) {
        if (!state.isAllowed(AddAction.APPLY_LAYOUT)) throw new InterruptActionException("Cannot add action 'Apply Layout' here.");

        return new ActionBuilder()
                .addAction(AddAction.APPLY_LAYOUT, state)
                .onlyIf(name != null && !name.trim().isEmpty(), () -> new ActionBuilder()
                        .search(LayoutInputs.LAYOUT)
                        .search(name)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionExit(RestrictionAction state) {
        if (!state.isAllowed(AddAction.EXIT)) throw new InterruptActionException("Cannot add action 'Exit' here.");

        return new ActionBuilder()
                .addAction(AddAction.EXIT, state)
                .get();
    }

    public static Action addActionCancelEvent(RestrictionAction state) {
        if (!state.isAllowed(AddAction.CANCEL_EVENT)) throw new InterruptActionException("Cannot add action 'Cancel Event' here.");

        return new ActionBuilder()
                .addAction(AddAction.CANCEL_EVENT, state)
                .get();
    }

    public static Action addActionEnchantHeldItem(RestrictionAction state, Enchant enchant, int level, String note) {
        if (!state.isAllowed(AddAction.ENCHANT_HELD_ITEM)) throw new InterruptActionException("Cannot add action 'Enchant Held Item' here.");

        return new ActionBuilder()
                .addAction(AddAction.ENCHANT_HELD_ITEM, state)
                .onlyIf(enchant != null, () -> new ActionBuilder()
                        .search(EnchantHeldInputs.ENCHANTMENT)
                        .search(enchant.displayName)
                )
                .onlyIf(level != 1, () -> new ActionBuilder()
                        .searchForAnvil(EnchantHeldInputs.LEVEL, String.valueOf(level))
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionPause(RestrictionAction state, int timeToWait, String note) {
        if (!state.isAllowed(AddAction.PAUSE)) throw new InterruptActionException("Cannot add action 'Pause' here.");

        return new ActionBuilder()
                .addAction(AddAction.PAUSE, state)
                .onlyIf(timeToWait != 20, () -> new ActionBuilder()
                        .searchForAnvil(PauseInputs.TICKS, String.valueOf(timeToWait))
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionTeam(RestrictionAction state, String team, String note) {
        if (!state.isAllowed(AddAction.SET_TEAM)) throw new InterruptActionException("Cannot add action 'Set Team' here.");

        return new ActionBuilder()
                .addAction(AddAction.SET_TEAM, state)
                .onlyIf(team != null && !team.equals("None"), () -> new ActionBuilder()
                        .search(TeamInputs.TEAM)
                        .search(team)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionDisplayMenu(RestrictionAction state, String menu, String note) {
        if (!state.isAllowed(AddAction.DISPLAY_MENU)) throw new InterruptActionException("Cannot add action 'Display Menu' here.");

        return new ActionBuilder()
                .addAction(AddAction.DISPLAY_MENU, state)
                .onlyIf(menu != null, () -> new ActionBuilder()
                        .search(DisplayMenuInputs.MENU)
                        .search(menu)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionDropItem(RestrictionAction state, DropItemParams p) {
        if (!state.isAllowed(AddAction.DROP_ITEM)) throw new InterruptActionException("Cannot add action 'Drop Item' here.");

        return new ActionBuilder()
                .addAction(AddAction.DROP_ITEM, state)
                .onlyIf(!p.snbtData.isEmpty(), () -> new ActionBuilder()
                        .search(DropItemInputs.ITEM)
                        .customItem(p.snbtData)
                )
                .onlyIf(p.teleport != null && p.teleport != Teleport.CUSTOM, () -> new ActionBuilder()
                        .search(DropItemInputs.LOCATION)
                        .search(p.teleport.displayName)
                )
                .onlyIf(p.teleport == Teleport.CUSTOM, () -> new ActionBuilder()
                        .search(DropItemInputs.LOCATION)
                        .searchForAnvil(Teleport.CUSTOM.displayName, p.customLocation)
                )
                .onlyIf(!p.dropNaturally, () -> new ActionBuilder()
                        .search(DropItemInputs.DROP_NATURALLY)
                )
                .onlyIf(p.disableItemMerging, () -> new ActionBuilder()
                        .search(DropItemInputs.PREVENT_MERGE)
                )
                .onlyIf(p.despawnDuration != 6000, () -> new ActionBuilder()
                        .searchForAnvil(DropItemInputs.DESPAWN_DURATION, String.valueOf(p.despawnDuration))
                )
                .onlyIf(p.pickupDelay != 10, () -> new ActionBuilder()
                        .searchForAnvil(DropItemInputs.PICKUP_DELAY, String.valueOf(p.pickupDelay))
                )
                .onlyIf(p.prioritizePlayer, () -> new ActionBuilder()
                        .search(DropItemInputs.PRIORITIZE_PLAYER)
                )
                .onlyIf(p.fallbackToInventory, () -> new ActionBuilder()
                        .search(DropItemInputs.FALLBACK_INVENTORY)
                )
                .onlyIf(p.note != null && !p.note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(p.note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionChangeVelocity(RestrictionAction state, String velocityX, String velocityY, String velocityZ, String note) {
        if (!state.isAllowed(AddAction.CHANGE_VELOCITY)) throw new InterruptActionException("Cannot add action 'Change Velocity' here.");

        return new ActionBuilder()
                .addAction(AddAction.CHANGE_VELOCITY, state)
                .onlyIf(velocityX != null && !velocityX.equals("10"), () -> new ActionBuilder()
                        .searchForAnvil(ChangeVelocityInputs.X_DIRECTION, velocityX)
                )
                .onlyIf(velocityY != null && !velocityY.equals("10"), () -> new ActionBuilder()
                        .searchForAnvil(ChangeVelocityInputs.Y_DIRECTION, velocityY)
                )
                .onlyIf(velocityZ != null && !velocityZ.equals("10"), () -> new ActionBuilder()
                        .searchForAnvil(ChangeVelocityInputs.Z_DIRECTION, velocityZ)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionLaunchTarget(RestrictionAction state, Teleport teleport, String customLocation, String strength, String note) {
        if (!state.isAllowed(AddAction.LAUNCH_TO_TARGET)) throw new InterruptActionException("Cannot add action 'Launch to Target' here.");

        return new ActionBuilder()
                .addAction(AddAction.LAUNCH_TO_TARGET, state)
                .onlyIf(teleport != null && teleport != Teleport.CUSTOM, () -> new ActionBuilder()
                        .search(LaunchTargetInputs.TARGET_LOCATION)
                        .search(teleport.displayName)
                )
                .onlyIf(teleport == Teleport.CUSTOM, () -> new ActionBuilder()
                        .search(LaunchTargetInputs.TARGET_LOCATION)
                        .searchForAnvil(Teleport.CUSTOM.displayName, customLocation)
                )
                .onlyIf(strength != null && !strength.equals("2"), () -> new ActionBuilder()
                        .searchForAnvil(LaunchTargetInputs.STRENGTH, strength)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionWeather(RestrictionAction state, Weather weather, String note) {
        if (!state.isAllowed(AddAction.SET_WEATHER)) throw new InterruptActionException("Cannot add action 'Set Weather' here.");

        return new ActionBuilder()
                .addAction(AddAction.SET_WEATHER, state)
                .onlyIf(weather == Weather.SUNNY, () -> new ActionBuilder()
                        .search(SetWeatherInputs.WEATHER)
                )
                .onlyIf(weather == Weather.RAINING, () -> new ActionBuilder()
                        .search(SetWeatherInputs.WEATHER)
                        .search(SetWeatherInputs.WEATHER)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionTime(RestrictionAction state, Time time, String customTime, String note) {
        if (!state.isAllowed(AddAction.SET_TIME)) throw new InterruptActionException("Cannot add action 'Set Time' here.");

        return new ActionBuilder()
                .addAction(AddAction.SET_TIME, state)
                .onlyIf(time != null && time != Time.CUSTOM, () -> new ActionBuilder()
                        .search(SetTimeInputs.TIME)
                        .search(time.displayName)
                )
                .onlyIf(time == Time.CUSTOM, () -> new ActionBuilder()
                        .search(SetTimeInputs.TIME)
                        .searchForAnvil(Time.CUSTOM.displayName, customTime)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

    public static Action addActionCloseMenu(RestrictionAction state) {
        if (!state.isAllowed(AddAction.CLOSE_MENU)) throw new InterruptActionException("Cannot add action 'Close Menu' here.");

        return new ActionBuilder()
                .addAction(AddAction.CLOSE_MENU, state)
                .get();
    }

    public static Action addActionNametag(RestrictionAction state, boolean displayNametag, String note) {
        if (!state.isAllowed(AddAction.NAME_TAG_DISPLAY)) throw new InterruptActionException("Cannot add action 'Nametag Display' here.");

        return new ActionBuilder()
                .addAction(AddAction.NAME_TAG_DISPLAY, state)
                .onlyIf(!displayNametag, () -> new ActionBuilder()
                        .search(NameTagInputs.DISPLAY)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Others.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Others.GENERAL_GO_BACK_H4, true)
                .get();
    }

}
