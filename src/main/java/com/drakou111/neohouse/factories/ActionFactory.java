package com.drakou111.neohouse.factories;

import com.drakou111.neohouse.ActionBuilder;
import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.*;
import com.drakou111.neohouse.exceptions.InteruptActionException;

public class ActionFactory {

    public static Action addActionConditional(RestrictionAction state, boolean matchAny, Action conditions, Action ifs, Action elses, String note) {
        if (state.pageSlotFor(AddAction.CONDITIONAL) == null) throw new InteruptActionException("Cannot add action 'Conditional' here.");

        return new ActionBuilder()
                .addAction(AddAction.CONDITIONAL, state)
                .onlyIf(matchAny, () ->  new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_CONDITIONAL_MATCH_ANY)
                )
                .onlyIf(conditions != null, () ->  new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_CONDITIONAL_CONDITIONALS)
                        .append(conditions)
                        .clickSlotAndWait(Slots.GENERAL_GO_BACK_H6)
                )
                .onlyIf(ifs != null, () ->  new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_CONDITIONAL_IF)
                        .append(ifs)
                        .clickSlotAndWait(Slots.GENERAL_GO_BACK_H6)
                )
                .onlyIf(elses != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_CONDITIONAL_ELSE)
                        .append(elses)
                        .clickSlotAndWait(Slots.GENERAL_GO_BACK_H6)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionChangeGroup(RestrictionAction state, String groupName, boolean demotionProtection, String note) {
        if (state.pageSlotFor(AddAction.CHANGE_GROUP) == null) throw new InteruptActionException("Cannot add action 'Change Group' here.");

        return new ActionBuilder()
                .addAction(AddAction.CHANGE_GROUP, state)
                .onlyIf(groupName != null && !groupName.trim().isEmpty(), () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_PLAYER_GROUP_VAL)
                        .searchAndWait(groupName, null)
                )
                .onlyIf(!demotionProtection, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_PLAYER_GROUP_DEMOTION_PROTECTION)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();

    }

    public static Action addActionKillPlayer(RestrictionAction state) {
        if (state.pageSlotFor(AddAction.KILL) == null) throw new InteruptActionException("Cannot add action 'Kill' here.");

        return new ActionBuilder()
                .addAction(AddAction.KILL, state)
                .get();
    }

    public static Action addActionFullHeal(RestrictionAction state) {
        if (state.pageSlotFor(AddAction.FULL_HEAL) == null) throw new InteruptActionException("Cannot add action 'Full Heal' here.");

        return new ActionBuilder()
                .addAction(AddAction.FULL_HEAL, state)
                .get();
    }

    public static Action addActionTitle(RestrictionAction state, String title, String subtitle, int fadeIn, int stay, int fadeOut, String note) {
        if (state.pageSlotFor(AddAction.TITLE) == null) throw new InteruptActionException("Cannot add action 'Title' here.");

        String finalTitle;
        if (title == null || title.trim().isEmpty()) finalTitle = "&l";
        else finalTitle = title;

        String finalSubTitle;
        if (subtitle == null || subtitle.trim().isEmpty()) finalSubTitle = "&l";
        else finalSubTitle = subtitle;

        return new ActionBuilder()
                .addAction(AddAction.TITLE, state)
                .onlyIf(!finalTitle.equals("Hello World!"), () -> new ActionBuilder()
                        .chatInput(finalTitle, Slots.ACTION_TITLE_TITLE)
                )
                .onlyIf(!finalSubTitle.equals("&l"), () -> new ActionBuilder()
                        .chatInput(finalSubTitle, Slots.ACTION_TITLE_SUBTITLE)
                )
                .onlyIf(fadeIn != 1, () -> new ActionBuilder()
                        .anvilInput(String.valueOf(fadeIn), Slots.ACTION_TITLE_FADE_IN)
                )
                .onlyIf(stay != 5, () -> new ActionBuilder()
                        .anvilInput(String.valueOf(stay), Slots.ACTION_TITLE_STAY)
                )
                .onlyIf(fadeOut != 1, () -> new ActionBuilder()
                        .anvilInput(String.valueOf(fadeOut), Slots.ACTION_TITLE_FADE_OUT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionActionBar(RestrictionAction state, String text, String note) {
        if (state.pageSlotFor(AddAction.ACTION_BAR) == null) throw new InteruptActionException("Cannot add action 'Action Bar' here.");

        String finalText;
        if (text == null || text.trim().isEmpty()) finalText = "&l";
        else finalText = text;

        return new ActionBuilder()
                .addAction(AddAction.ACTION_BAR, state)
                .onlyIf(!finalText.equals("Hello World!"), () -> new ActionBuilder()
                        .chatInput(finalText, Slots.ACTION_ACTION_BAR_TEXT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionResetInventory(RestrictionAction state) {
        if (state.pageSlotFor(AddAction.CLEAR_INVENTORY) == null) throw new InteruptActionException("Cannot add action 'Clear Inventory' here.");

        return new ActionBuilder()
                .addAction(AddAction.CLEAR_INVENTORY, state)
                .get();
    }

    public static Action addActionMaxHealth(RestrictionAction state, String maxHealth, RestrictedOperator operator, boolean healOnChange, String note) {
        if (state.pageSlotFor(AddAction.MAX_HEALTH) == null) throw new InteruptActionException("Cannot add action 'Max Health' here.");

        return new ActionBuilder()
                .addAction(AddAction.MAX_HEALTH, state)
                .onlyIf(maxHealth != null && !maxHealth.equals("20"), () -> new ActionBuilder()
                        .anvilInput(String.valueOf(maxHealth), Slots.ACTION_MAX_HEALTH_VAL)
                )
                .onlyIf(operator != RestrictedOperator.SET, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_MAX_HEALTH_OPERATION)
                        .clickAndSelectSlot(operator.getPage(), operator.getSlotID())
                )
                .onlyIf(!healOnChange, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_MAX_HEALTH_HEAL_ON_CHANGE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionParkourCheckpoint(RestrictionAction state) {
        if (state.pageSlotFor(AddAction.PARKOUR_CHECKPOINT) == null) throw new InteruptActionException("Cannot add action 'Parkour Checkpoint' here.");

        return new ActionBuilder()
                .addAction(AddAction.PARKOUR_CHECKPOINT, state)
                .get();
    }

    public static Action addActionGiveItem(RestrictionAction state, Integer inventoryItemSlotID, boolean allowMultiple, InventorySlots inventorySlot, boolean replaceExistingItem, int manualInputIndex, String note) {
        if (state.pageSlotFor(AddAction.GIVE_ITEM) == null) throw new InteruptActionException("Cannot add action 'Give Item' here.");

        return new ActionBuilder()
                .addAction(AddAction.GIVE_ITEM, state)
                .onlyIf(inventoryItemSlotID != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_GIVE_ITEM_ITEM)
                        .clickSlotAndWait(inventoryItemSlotID)
                )
                .onlyIf(allowMultiple, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_GIVE_ITEM_ALLOW_MULTIPLE)
                )
                .onlyIf(inventorySlot != InventorySlots.FIRST_AVAILABLE_SLOT && inventorySlot != InventorySlots.MANUAL_INPUT, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_GIVE_ITEM_SLOT)
                        .clickAndSelectSlot(inventorySlot.getPage(), inventorySlot.getSlotID())
                )
                .onlyIf(inventorySlot == InventorySlots.MANUAL_INPUT, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_GIVE_ITEM_SLOT)
                        .anvilInput(String.valueOf(manualInputIndex), InventorySlots.MANUAL_INPUT.getSlotID())
                )
                .onlyIf(replaceExistingItem, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_GIVE_ITEM_REPLACE_EXISTING)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionRemoveItem(RestrictionAction state, Integer inventoryItemSlotID, String note) {
        if (state.pageSlotFor(AddAction.REMOVE_ITEM) == null) throw new InteruptActionException("Cannot add action 'Remove Item' here.");

        return new ActionBuilder()
                .addAction(AddAction.REMOVE_ITEM, state)
                .onlyIf(inventoryItemSlotID != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_REMOVE_ITEM_ITEM)
                        .clickSlotAndWait(inventoryItemSlotID)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionSendMessage(RestrictionAction state, String text, String note) {
        if (state.pageSlotFor(AddAction.CHAT_MESSAGE) == null) throw new InteruptActionException("Cannot add action 'Send Chat Message' here.");

        String finalText;
        if (text == null || text.trim().isEmpty()) finalText = "&l";
        else finalText = text;

        return new ActionBuilder()
                .addAction(AddAction.CHAT_MESSAGE, state)
                .onlyIf(!finalText.equals("Hello!"), () -> new ActionBuilder()
                        .chatInput(finalText, Slots.ACTION_CHAT_MESSAGE_TEXT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionApplyPotion(RestrictionAction state, Potion potion, int duration, int level, boolean override, boolean showIcon, String note) {
        if (state.pageSlotFor(AddAction.APPLY_POTION) == null) throw new InteruptActionException("Cannot add action 'Apply Potion' here.");

        return new ActionBuilder()
                .addAction(AddAction.APPLY_POTION, state)
                .onlyIf(potion != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_APPLY_POTION_EFFECT)
                        .clickAndSelectSlot(potion.getPage(), potion.getSlotID())
                )
                .onlyIf(duration != 60, () -> new ActionBuilder()
                        .anvilInput(String.valueOf(duration), Slots.ACTION_APPLY_POTION_DURATION)
                )
                .onlyIf(level != 1, () -> new ActionBuilder()
                        .anvilInput(String.valueOf(level), Slots.ACTION_APPLY_POTION_LEVEL)
                )
                .onlyIf(override, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_APPLY_POTION_OVERRIDE)
                )
                .onlyIf(!showIcon, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_APPLY_POTION_SHOW_ICON)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionClearPotion(RestrictionAction state) {
        if (state.pageSlotFor(AddAction.CLEAR_POTION) == null) throw new InteruptActionException("Cannot add action 'Clear Potion' here.");

        return new ActionBuilder()
                .addAction(AddAction.CLEAR_POTION, state)
                .get();
    }

    public static Action addActionGiveXP(RestrictionAction state, int xp, String note) {
        if (state.pageSlotFor(AddAction.GIVE_XP) == null) throw new InteruptActionException("Cannot add action 'Give XP' here.");

        return new ActionBuilder()
                .addAction(AddAction.GIVE_XP, state)
                .onlyIf(xp != 1, () -> new ActionBuilder()
                        .anvilInput(String.valueOf(xp), Slots.ACTION_GIVE_EXPERIENCE_VAL)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionSendLobby(RestrictionAction state, Lobby lobby, String note) {
        if (state.pageSlotFor(AddAction.SEND_LOBBY) == null) throw new InteruptActionException("Cannot add action 'Send to Lobby' here.");

        return new ActionBuilder()
                .addAction(AddAction.SEND_LOBBY, state)
                .onlyIf(lobby != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_SEND_LOBBY_LOBBY)
                        .clickAndSelectSlot(lobby.getPage(), lobby.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionVariable(RestrictionAction state, Holder holder, String variable, Operator operator, String value, boolean automaticUnset, String note) {
        if (state.pageSlotFor(AddAction.VARIABLE) == null) throw new InteruptActionException("Cannot add action 'Variable' here.");

        return new ActionBuilder()
                .addAction(AddAction.VARIABLE, state)
                .onlyIf(holder != Holder.PLAYER, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_SET_VARIABLE_HOLDER)
                        .clickAndSelectSlot(holder.getPage(), holder.getSlotID())
                )
                .onlyIf(!variable.equals("Kills"), () -> new ActionBuilder()
                        .chatInput(variable, Slots.ACTION_SET_VARIABLE_VAR)
                )
                .onlyIf(operator != Operator.INCREMENT, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_SET_VARIABLE_OPERATION)
                        .clickAndSelectSlot(operator.getPage(), operator.getSlotID())
                )
                .onlyIf(!value.equals("1"), () -> new ActionBuilder()
                        .chatInput(value, Slots.ACTION_SET_VARIABLE_VALUE)
                )
                .onlyIf(automaticUnset, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_SET_VARIABLE_AUTOMATIC_UNSET)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionTeleport(RestrictionAction state, Teleport teleport, String customLocation, boolean preventInsideBlocks, String note) {
        if (state.pageSlotFor(AddAction.TELEPORT) == null) throw new InteruptActionException("Cannot add action 'Teleport' here.");

        return new ActionBuilder()
                .addAction(AddAction.TELEPORT, state)
                .onlyIf(teleport != null && teleport != Teleport.CUSTOM, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_TELEPORT_LOCATION)
                        .clickAndSelectSlot(teleport.getPage(), teleport.getSlotID())
                )
                .onlyIf(teleport == Teleport.CUSTOM, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_TELEPORT_LOCATION)
                        .anvilInput(customLocation, Teleport.CUSTOM.getSlotID())
                )
                .onlyIf(preventInsideBlocks, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_TELEPORT_PREVENT_INSIDE_BLOCK)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionFailParkour(RestrictionAction state, String text, String note) {
        if (state.pageSlotFor(AddAction.FAIL_PARKOUR) == null) throw new InteruptActionException("Cannot add action 'Fail Parkour' here.");

        String finalText;
        if (text == null || text.trim().isEmpty()) finalText = "&l";
        else finalText = text;

        return new ActionBuilder()
                .addAction(AddAction.FAIL_PARKOUR, state)
                .onlyIf(!finalText.equals("Failed!"), () -> new ActionBuilder()
                        .chatInput(finalText, Slots.ACTION_FAIL_PARKOUR_TEXT)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionPlaySound(RestrictionAction state, Sound sound, double volume, double pitch, Teleport teleport, String customLocation, String note) {
        if (state.pageSlotFor(AddAction.PLAY_SOUND) == null) throw new InteruptActionException("Cannot add action 'Play Sound' here.");

        return new ActionBuilder()
                .addAction(AddAction.PLAY_SOUND, state)

                .onlyIf(sound != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_PLAY_SOUND_TYPE)
                        .clickAndSelectSlot(sound.getPage(), sound.getSlotID())
                )
                .onlyIf(volume != 0.7D, () -> new ActionBuilder()
                        .anvilInput(String.valueOf(volume), Slots.ACTION_PLAY_SOUND_VOLUME)
                )
                .onlyIf(pitch != 1.0D, () -> new ActionBuilder()
                        .anvilInput(String.valueOf(pitch), Slots.ACTION_PLAY_SOUND_PITCH)
                )
                .onlyIf(teleport != null && teleport != Teleport.CUSTOM, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_PLAY_SOUND_LOCATION)
                        .clickAndSelectSlot(teleport.getPage(), teleport.getSlotID())
                )
                .onlyIf(teleport == Teleport.CUSTOM, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_PLAY_SOUND_LOCATION)
                        .anvilInput(customLocation, Teleport.CUSTOM.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionCompassTarget(RestrictionAction state, Teleport teleport, String customLocation, String note) {
        if (state.pageSlotFor(AddAction.COMPASS_TARGET) == null) throw new InteruptActionException("Cannot add action 'Set Compass Target' here.");

        return new ActionBuilder()
                .addAction(AddAction.COMPASS_TARGET, state)
                .onlyIf(teleport != null && teleport != Teleport.CUSTOM, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_COMPASS_TARGET_LOCATION)
                        .clickAndSelectSlot(teleport.getPage(), teleport.getSlotID())
                )
                .onlyIf(teleport == Teleport.CUSTOM, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_COMPASS_TARGET_LOCATION)
                        .anvilInput(customLocation, Teleport.CUSTOM.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionGamemode(RestrictionAction state, Gamemode gamemode, String note) {
        if (state.pageSlotFor(AddAction.SET_GAME_MODE) == null) throw new InteruptActionException("Cannot add action 'Set Game Mode' here.");

        return new ActionBuilder()
                .addAction(AddAction.SET_GAME_MODE, state)
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

    public static Action addActionChangeHealth(RestrictionAction state, String health, RestrictedOperator operator, String note) {
        if (state.pageSlotFor(AddAction.CHANGE_HEALTH) == null) throw new InteruptActionException("Cannot add action 'Change Health' here.");

        return new ActionBuilder()
                .addAction(AddAction.CHANGE_HEALTH, state)
                .onlyIf(!health.equals("20"), () -> new ActionBuilder()
                        .anvilInput(health, Slots.ACTION_HEALTH_VAL)
                )
                .onlyIf(operator != RestrictedOperator.SET, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_HEALTH_OPERATION)
                        .clickAndSelectSlot(operator.getPage(), operator.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionChangeHunger(RestrictionAction state, String hunger, RestrictedOperator operator, String note) {
        if (state.pageSlotFor(AddAction.CHANGE_HUNGER) == null) throw new InteruptActionException("Cannot add action 'Change Hunger' here.");

        return new ActionBuilder()
                .addAction(AddAction.CHANGE_HUNGER, state)
                .onlyIf(!hunger.equals("20"), () -> new ActionBuilder()
                        .anvilInput(hunger, Slots.ACTION_HUNGER_VAL)
                )
                .onlyIf(operator != RestrictedOperator.SET, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_HUNGER_OPERATION)
                        .clickAndSelectSlot(operator.getPage(), operator.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionRandom(RestrictionAction state, Action randoms, String note) {
        if (state.pageSlotFor(AddAction.RANDOM) == null) throw new InteruptActionException("Cannot add action 'Random' here.");

        return new ActionBuilder()
                .addAction(AddAction.RANDOM, state)
                .onlyIf(randoms != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_RANDOM_ACTIONS)
                        .append(randoms)
                        .clickSlotAndWait(Slots.GENERAL_GO_BACK_H6)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionTriggerFunction(RestrictionAction state, String name, boolean forAll, String note) {
        if (state.pageSlotFor(AddAction.TRIGGER_FUNCTION) == null) throw new InteruptActionException("Cannot add action 'Trigger Function' here.");

        return new ActionBuilder()
                .addAction(AddAction.TRIGGER_FUNCTION, state)
                .onlyIf(name != null && !name.trim().isEmpty(), () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_TRIGGER_FUNCTION_TYPE)
                        .searchAndWait(name, null)
                )
                .onlyIf(forAll, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_TRIGGER_FUNCTION_ALL)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionApplyLayout(RestrictionAction state, String name, String note) {
        if (state.pageSlotFor(AddAction.APPLY_LAYOUT) == null) throw new InteruptActionException("Cannot add action 'Apply Layout' here.");

        return new ActionBuilder()
                .addAction(AddAction.APPLY_LAYOUT, state)
                .onlyIf(name != null && !name.trim().isEmpty(), () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_LAYOUT_TYPE)
                        .searchAndWait(name, null)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionExit(RestrictionAction state) {
        if (state.pageSlotFor(AddAction.EXIT) == null) throw new InteruptActionException("Cannot add action 'Exit' here.");

        return new ActionBuilder()
                .addAction(AddAction.EXIT, state)
                .get();
    }

    public static Action addActionEnchantHeldItem(RestrictionAction state, Enchant enchant, int level, String note) {
        if (state.pageSlotFor(AddAction.ENCHANT_HELD_ITEM) == null) throw new InteruptActionException("Cannot add action 'Enchant Held Item' here.");

        return new ActionBuilder()
                .addAction(AddAction.ENCHANT_HELD_ITEM, state)
                .onlyIf(enchant != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_ENCHANT_HELD_ENCHANT)
                        .clickAndSelectSlot(enchant.getPage(), enchant.getSlotID())
                )
                .onlyIf(level != 1, () -> new ActionBuilder()
                        .anvilInput(String.valueOf(level), Slots.ACTION_ENCHANT_HELD_LEVEL)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionPause(RestrictionAction state, int timeToWait, String note) {
        if (state.pageSlotFor(AddAction.PAUSE) == null) throw new InteruptActionException("Cannot add action 'Pause' here.");

        return new ActionBuilder()
                .addAction(AddAction.PAUSE, state)
                .onlyIf(timeToWait != 20, () -> new ActionBuilder()
                        .anvilInput(String.valueOf(timeToWait), Slots.ACTION_PAUSE_VAL)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionTeam(RestrictionAction state, String team, String note) {
        if (state.pageSlotFor(AddAction.SET_TEAM) == null) throw new InteruptActionException("Cannot add action 'Set Team' here.");

        return new ActionBuilder()
                .addAction(AddAction.SET_TEAM, state)
                .onlyIf(team != null && !team.equals("None"), () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_TEAM_TYPE)
                        .searchAndWait(team, null)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionDisplayMenu(RestrictionAction state, String menu, String note) {
        if (state.pageSlotFor(AddAction.DISPLAY_MENU) == null) throw new InteruptActionException("Cannot add action 'Display Menu' here.");

        return new ActionBuilder()
                .addAction(AddAction.DISPLAY_MENU, state)
                .onlyIf(menu != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_DISPLAY_MENU_TYPE)
                        .searchAndWait(menu, null)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionDropItem(RestrictionAction state, Integer inventoryItemSlotID, Teleport teleport, String customLocation, boolean dropNaturally, boolean disableItemMerging, boolean prioritizePlayer, boolean fallbackToInventory, String note) {
        if (state.pageSlotFor(AddAction.DROP_ITEM) == null) throw new InteruptActionException("Cannot add action 'Drop Item' here.");

        return new ActionBuilder()
                .addAction(AddAction.DROP_ITEM, state)
                .onlyIf(inventoryItemSlotID != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_DROP_ITEM_ITEM)
                        .clickSlotAndWait(inventoryItemSlotID)
                )
                .onlyIf(teleport != null && teleport != Teleport.CUSTOM, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_DROP_ITEM_LOCATION)
                        .clickAndSelectSlot(teleport.getPage(), teleport.getSlotID())
                )
                .onlyIf(teleport == Teleport.CUSTOM, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_DROP_ITEM_LOCATION)
                        .anvilInput(customLocation, Teleport.CUSTOM.getSlotID())
                )
                .onlyIf(!dropNaturally, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_DROP_ITEM_DROP_NATURALLY)
                )
                .onlyIf(disableItemMerging, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_DROP_ITEM_DISABLE_MERGE)
                )
                .onlyIf(prioritizePlayer, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_DROP_ITEM_PRIORITIZE_PLAYER)
                )
                .onlyIf(fallbackToInventory, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_DROP_ITEM_FALLBACK_INVENTORY)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionChangeVelocity(RestrictionAction state, String velocityX, String velocityY, String velocityZ, String note) {
        if (state.pageSlotFor(AddAction.CHANGE_VELOCITY) == null) throw new InteruptActionException("Cannot add action 'Change Velocity' here.");

        return new ActionBuilder()
                .addAction(AddAction.CHANGE_VELOCITY, state)
                .onlyIf(velocityX != null && !velocityX.equals("10"), () -> new ActionBuilder()
                        .anvilInput(velocityX, Slots.ACTION_CHANGE_VELOCITY_X)
                )
                .onlyIf(velocityY != null && !velocityY.equals("10"), () -> new ActionBuilder()
                        .anvilInput(velocityY, Slots.ACTION_CHANGE_VELOCITY_Y)
                )
                .onlyIf(velocityZ != null && !velocityZ.equals("10"), () -> new ActionBuilder()
                        .anvilInput(velocityZ, Slots.ACTION_CHANGE_VELOCITY_Z)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionLaunchTarget(RestrictionAction state, Teleport teleport, String customLocation, String strength, String note) {
        if (state.pageSlotFor(AddAction.LAUNCH_TO_TARGET) == null) throw new InteruptActionException("Cannot add action 'Launch to Target' here.");

        return new ActionBuilder()
                .addAction(AddAction.LAUNCH_TO_TARGET, state)
                .onlyIf(teleport != null && teleport != Teleport.CUSTOM, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_LAUNCH_TARGET_LOCATION)
                        .clickAndSelectSlot(teleport.getPage(), teleport.getSlotID())
                )
                .onlyIf(teleport == Teleport.CUSTOM, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_LAUNCH_TARGET_LOCATION)
                        .anvilInput(customLocation, Teleport.CUSTOM.getSlotID())
                )
                .onlyIf(strength != null && !strength.equals("2"), () -> new ActionBuilder()
                        .anvilInput(strength, Slots.ACTION_LAUNCH_TARGET_STRENGTH)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionWeather(RestrictionAction state, Weather weather, String note) {
        if (state.pageSlotFor(AddAction.SET_WEATHER) == null) throw new InteruptActionException("Cannot add action 'Set Weather' here.");

        return new ActionBuilder()
                .addAction(AddAction.SET_WEATHER, state)
                .onlyIf(weather != null, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_SET_WEATHER_WEATHER)
                        .clickAndSelectSlot(weather.getPage(), weather.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionTime(RestrictionAction state, Time time, String customTime, String note) {
        if (state.pageSlotFor(AddAction.SET_TIME) == null) throw new InteruptActionException("Cannot add action 'Set Time' here.");

        return new ActionBuilder()
                .addAction(AddAction.SET_TIME, state)
                .onlyIf(time != null && time != Time.CUSTOM, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_SET_TIME_TIME)
                        .clickAndSelectSlot(time.getPage(), time.getSlotID())
                )
                .onlyIf(time == Time.CUSTOM, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_SET_TIME_TIME)
                        .anvilInput(customTime, time.getSlotID())
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }

    public static Action addActionNametag(RestrictionAction state, boolean displayNametag, String note) {
        if (state.pageSlotFor(AddAction.NAME_TAG_DISPLAY) == null) throw new InteruptActionException("Cannot add action 'Nametag Display' here.");

        return new ActionBuilder()
                .addAction(AddAction.NAME_TAG_DISPLAY, state)
                .onlyIf(!displayNametag, () -> new ActionBuilder()
                        .clickSlotAndWait(Slots.ACTION_NAME_TAG_TOGGLE)
                )
                .onlyIf(note != null && !note.trim().isEmpty(), () -> new ActionBuilder()
                        .chatInput(note, Slots.GENERAL_NOTE_H4)
                )
                .clickSlotAndWait(Slots.GENERAL_GO_BACK_H4)
                .get();
    }
}
