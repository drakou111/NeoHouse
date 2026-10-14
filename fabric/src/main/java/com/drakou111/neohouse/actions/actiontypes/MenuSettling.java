package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.compat.ClientUi;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Decides when an open Housing menu has finished loading: it shows at least one item and its item
 * names haven't changed for long enough. Servers send a menu's contents after opening it (and a page
 * turn may send them piece by piece), so reading a menu any earlier can miss items that are there.
 *
 * <p>"Long enough" grows with lag: at least the base window, the expected lag (ping, or how long the
 * last page turn took to answer), and twice the longest pause seen between two updates of this
 * menu, so contents arriving in slow bursts aren't mistaken for a finished menu.</p>
 */
final class MenuSettling {
    // Caps the wait so even a very laggy server gets a decision well before the action timeout.
    static final int MAX_STABLE_TICKS = 40;
    private static final int MS_PER_TICK = 50;

    private final int baseStableTicks;
    private int expectedLagTicks = 0;
    private List<String> lastSnapshot = null;
    private boolean loaded = false;
    private int stableTicks = 0;
    private int longestGap = 0;

    MenuSettling(int baseStableTicks) {
        this.baseStableTicks = baseStableTicks;
    }

    /** Call once per tick; true once the snapshot has been loaded and unchanged long enough. */
    boolean update(List<String> snapshot) {
        boolean hasItems = snapshot.stream().anyMatch(name -> !name.isEmpty());
        if (!hasItems || !snapshot.equals(this.lastSnapshot)) {
            // A change after the menu first showed items: remember how long that update took to come.
            if (this.loaded && hasItems) this.longestGap = Math.max(this.longestGap, this.stableTicks + 1);
            this.loaded |= hasItems;
            this.lastSnapshot = snapshot;
            this.stableTicks = 0;
            return false;
        }
        return ++this.stableTicks >= requiredStableTicks();
    }

    /** How many unchanged ticks currently count as loaded. */
    int requiredStableTicks() {
        int needed = Math.max(this.baseStableTicks, Math.max(this.expectedLagTicks, 2 * this.longestGap));
        return Math.min(MAX_STABLE_TICKS, needed);
    }

    /** Raises the window to at least {@code ticks}, e.g. the ping or a page turn's response time. */
    void expectLag(int ticks) {
        this.expectedLagTicks = Math.max(this.expectedLagTicks, ticks);
    }

    /** Starts over for a new menu or page; the expected lag is cleared too. */
    void reset() {
        this.lastSnapshot = null;
        this.loaded = false;
        this.stableTicks = 0;
        this.longestGap = 0;
        this.expectedLagTicks = 0;
    }

    /** The player's ping from the tab list, in ticks (rounded up); 0 when unknown. */
    static int pingTicks(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) return 0;
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (info == null) return 0;
        return (Math.max(0, info.getLatency()) + MS_PER_TICK - 1) / MS_PER_TICK;
    }

    /** Whether {@code menu} is a real open menu, not the player's own inventory left after a close. */
    static boolean isOpenMenu(Minecraft mc, AbstractContainerMenu menu) {
        return mc.player != null && menu != null && menu != mc.player.inventoryMenu;
    }

    /** An item's display name without color or formatting codes. */
    static String plainName(ItemStack stack) {
        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        return name == null ? "" : name;
    }

    /** The arrow Housing puts in a list's last slot to turn the page ("Go Back" and "Previous Page" are arrows too). */
    static boolean isNextPageArrow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = plainName(stack).toLowerCase(Locale.ROOT);
        if (name.contains("next")) return true;
        return stack.is(Items.ARROW) && !name.contains("back") && !name.contains("previous");
    }

    /** Item names of the menu's own slots (not the player's inventory); "" for an empty slot. */
    static List<String> snapshot(AbstractContainerMenu menu) {
        List<String> names = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) continue;
            names.add(slot.hasItem() ? slot.getItem().getHoverName().getString() : "");
        }
        return names;
    }

    /** The menu's own slots, without the player's inventory. */
    static List<Slot> containerSlots(AbstractContainerMenu menu) {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) if (!(slot.container instanceof Inventory)) slots.add(slot);
        return slots;
    }

    /** A custom menu's element grid shows an unused element as a gray "Empty Slot" glass pane. */
    static boolean isEmptyMenuElement(Slot slot) {
        return !slot.hasItem() || ClientUi.isGrayGlassPane(slot.getItem());
    }
}
