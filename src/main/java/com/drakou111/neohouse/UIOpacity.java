package com.drakou111.neohouse;

public enum UIOpacity {
    OPAQUE ("Opaque",  0xFF, 0xFF, 0xCC),
    HIGH   ("High",    0xF0, 0xF5, 0xBB),
    DEFAULT("Default", 0xE0, 0xF2, 0xAA),
    MEDIUM ("Medium",  0xCC, 0xEE, 0x88),
    LOW    ("Low",     0xAA, 0xCC, 0x66);

    public final String displayName;
    /** Alpha for full-screen background and footer fills. */
    public final int bgAlpha;
    /** Alpha for header panels and dialog boxes. */
    public final int panelAlpha;
    /** Alpha for the black dim overlay behind dialogs. */
    public final int dimAlpha;

    UIOpacity(String displayName, int bgAlpha, int panelAlpha, int dimAlpha) {
        this.displayName = displayName;
        this.bgAlpha     = bgAlpha;
        this.panelAlpha  = panelAlpha;
        this.dimAlpha    = dimAlpha;
    }

    public static UIOpacity byId(String id) {
        for (UIOpacity o : values()) if (o.name().equals(id)) return o;
        return DEFAULT;
    }

    public UIOpacity prev() {
        UIOpacity[] v = values();
        return v[(ordinal() + v.length - 1) % v.length];
    }

    public UIOpacity next() {
        UIOpacity[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
