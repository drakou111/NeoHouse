package com.drakou111.neohouse.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * The warnings shown while a script compiled and ran, so they can be shown again once the run is
 * over (they're easy to miss while chat scrolls past). Each is kept as the call that showed it.
 */
public final class RunWarnings {
    private static final List<Runnable> warnings = new ArrayList<>();
    private static boolean recording = false;
    private static boolean replaying = false;

    private RunWarnings() {}

    /** Starts a fresh log for a new run. */
    public static void start() {
        warnings.clear();
        recording = true;
    }

    /** Starts a fresh log unless one is already open (a script run opens it before compiling). */
    public static void startIfIdle() {
        if (!recording) start();
    }

    /** Stops logging; the warnings stay available to {@link #replay}. */
    public static void stop() {
        recording = false;
    }

    /** Logs a warning that was just shown, as the call that shows it again. */
    static void record(Runnable show) {
        if (recording && !replaying) warnings.add(show);
    }

    public static int count() {
        return warnings.size();
    }

    /** Shows every warning of the last run again, in order. */
    public static void replay() {
        if (warnings.isEmpty()) {
            MessageUtils.debugToChat("The last run had no warnings.");
            return;
        }
        MessageUtils.debugToChat("Warnings of the last run (" + warnings.size() + "):");
        replaying = true;
        try {
            for (Runnable show : warnings) show.run();
        } finally {
            replaying = false;
        }
    }
}
