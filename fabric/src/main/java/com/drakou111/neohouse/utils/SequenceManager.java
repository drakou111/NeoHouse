package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ChatEvents;
import com.drakou111.neohouse.constants.HousingList;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;

public class SequenceManager {
    private static Action currentAction = null;
    private static long startTimeMs = 0;
    private static int totalSteps = 0;
    private static int completedSteps = 0;

    // Names of functions/loops the current run created as fresh shells. Used by "Never Override"
    // so a later pass fills only the entities it just created, leaving pre-existing ones alone.
    private static final java.util.Set<String> createdThisRun = new java.util.HashSet<>();

    public static void markCreated(String name) { createdThisRun.add(name); }
    public static boolean wasCreated(String name) { return createdThisRun.contains(name); }

    // Entries of each Housing list that already existed when this run started, recorded by
    // ReadListNamesAction. Lets later steps decide delete/create/skip without searching the list.
    private static final java.util.Map<HousingList, java.util.Set<String>> existingEntries = new java.util.EnumMap<>(HousingList.class);

    /** Records an entry by the item name the list shows for it (e.g. {@code /name} for commands). */
    public static boolean recordExisting(HousingList list, String displayName) {
        return existingEntries.computeIfAbsent(list, k -> new java.util.HashSet<>()).add(displayName);
    }

    public static boolean exists(HousingList list, String name) {
        return existingEntries.getOrDefault(list, java.util.Set.of()).contains(list.displayName(name));
    }

    public static java.util.Set<String> existingDisplayNames(HousingList list) {
        return java.util.Collections.unmodifiableSet(existingEntries.getOrDefault(list, java.util.Set.of()));
    }

    public static void startAction(Action action) {
        ChatEvents.clear(); // drop any listeners left over from a previous run
        RunWarnings.startIfIdle();
        createdThisRun.clear();
        existingEntries.clear();
        currentAction = action;
        startTimeMs = System.currentTimeMillis();
        totalSteps = action.countSteps();
        completedSteps = 0;
    }

    public static void cancelAction() {
        currentAction = null;
        ChatEvents.clear();
        RunWarnings.stop();
    }

    /** Stops the run and reports why, with a link to the run's warnings. */
    public static void stop(String message, boolean failed) {
        cancelAction();
        MessageUtils.runEndToChat(message, failed);
    }

    public static boolean isRunning() {
        return currentAction != null;
    }

    public static void stepCompleted() {
        completedSteps++;
    }

    public static void addSteps(int n) {
        totalSteps += n;
    }

    public static long getElapsedMs() { return System.currentTimeMillis() - startTimeMs; }
    public static int getTotalSteps()     { return totalSteps; }
    public static int getCompletedSteps() { return completedSteps; }

    public static void tick(Minecraft mc) {
        if (currentAction == null) return;

        if (!HousingCheck.isInHousing()) {
            stop("Script stopped: not in Housing.", true);
            return;
        }

        if (mc.player != null && !mc.player.getAbilities().instabuild && currentAction.requiresCreative()) {
            stop("Script stopped: creative mode is required for item actions.", true);
            return;
        }

        boolean finished;
        try {
            finished = currentAction.run(mc);
        } catch (InterruptActionException e) {
            stop(e.getMessage() != null ? e.getMessage() : e.toString(), true);
            return;
        } catch (Exception e) {
            stop("Script aborted due to unexpected error: " + e, true);
            return;
        }

        if (finished) stop("Done!", false);
    }
}
