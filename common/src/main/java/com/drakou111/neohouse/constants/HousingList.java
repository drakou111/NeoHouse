package com.drakou111.neohouse.constants;

/** The paginated Housing lists the script can add entries to, with how many entries Housing allows. */
public enum HousingList {
    FUNCTIONS(Commands.FUNCTIONS, 300, "", "function"),
    COMMANDS(Commands.COMMANDS, 50, "/", "command"),
    MENUS(Commands.CUSTOM_MENUS, 200, "", "menu");

    /** Chat command that opens the list. */
    public final String openCommand;
    public final int maxEntries;
    /** What the list shows in front of an entry's name (commands are listed as {@code /name}). */
    private final String displayPrefix;
    public final String singularName;

    HousingList(String openCommand, int maxEntries, String displayPrefix, String singularName) {
        this.openCommand = openCommand;
        this.maxEntries = maxEntries;
        this.displayPrefix = displayPrefix;
        this.singularName = singularName;
    }

    /** The item name an entry called {@code name} has in the list. */
    public String displayName(String name) {
        return this.displayPrefix + name;
    }
}
