package com.drakou111.neohouse;

public enum UITheme {
    //                                bgTop     bgBottom  panel     dialog    accent    surface   highlight
    DARK_BLUE  ("Dark Blue",   0x0D0D18, 0x090912, 0x1A1A2A, 0x1E1E2B, 0x444466, 0x161620, 0x2A2A3A),
    DARK_PURPLE("Dark Purple", 0x120D1A, 0x0A0810, 0x1E1530, 0x241838, 0x5A4080, 0x16102A, 0x2A1E45),
    MIDNIGHT   ("Midnight",    0x08080E, 0x040408, 0x0F0F1E, 0x121225, 0x252545, 0x0A0A18, 0x181828),
    FOREST     ("Forest",      0x0C1A0E, 0x070E08, 0x152418, 0x1A2C1E, 0x3A6040, 0x101C12, 0x1E301E),
    CRIMSON    ("Crimson",     0x1A0D0D, 0x110808, 0x2A1616, 0x321A1A, 0x663333, 0x201313, 0x3A2020);

    public final String displayName;
    private final int bgTopRgb, bgBottomRgb, panelRgb, dialogRgb, accentRgb, surfaceRgb, highlightRgb;

    UITheme(String displayName,
            int bgTopRgb, int bgBottomRgb, int panelRgb, int dialogRgb,
            int accentRgb, int surfaceRgb, int highlightRgb) {
        this.displayName   = displayName;
        this.bgTopRgb      = bgTopRgb;
        this.bgBottomRgb   = bgBottomRgb;
        this.panelRgb      = panelRgb;
        this.dialogRgb     = dialogRgb;
        this.accentRgb     = accentRgb;
        this.surfaceRgb    = surfaceRgb;
        this.highlightRgb  = highlightRgb;
    }

    public int bgTop     (int alpha) { return (alpha << 24) | bgTopRgb;     }
    public int bgBottom  (int alpha) { return (alpha << 24) | bgBottomRgb;  }
    public int panel     (int alpha) { return (alpha << 24) | panelRgb;     }
    public int dialog    (int alpha) { return (alpha << 24) | dialogRgb;    }
    public int surface   (int alpha) { return (alpha << 24) | surfaceRgb;   }
    public int highlight (int alpha) { return (alpha << 24) | highlightRgb; }
    public int accent    ()          { return 0xFF000000    | accentRgb;     }
    public int accentTint(int alpha) { return (alpha << 24) | accentRgb;    }

    /** Mid-tone between panel and accent — for subtle content dividers. */
    public int divider() {
        int r = (((accentRgb >> 16) & 0xFF) + ((panelRgb >> 16) & 0xFF)) >> 1;
        int g = (((accentRgb >>  8) & 0xFF) + ((panelRgb >>  8) & 0xFF)) >> 1;
        int b = (( accentRgb        & 0xFF) + ( panelRgb         & 0xFF)) >> 1;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Accent scaled up so its brightest channel reaches 0xCC — for secondary/muted text. */
    public int muted() {
        int r = (accentRgb >> 16) & 0xFF;
        int g = (accentRgb >>  8) & 0xFF;
        int b =  accentRgb        & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        if (max == 0) return 0xFFAAAAAA;
        float scale = 0xCC / (float) max;
        return 0xFF000000
                | (Math.min(0xFF, (int)(r * scale)) << 16)
                | (Math.min(0xFF, (int)(g * scale)) <<  8)
                |  Math.min(0xFF, (int)(b * scale));
    }

    public static UITheme byId(String id) {
        for (UITheme t : values()) if (t.name().equals(id)) return t;
        return DARK_BLUE;
    }

    public UITheme prev() {
        UITheme[] v = values();
        return v[(ordinal() + v.length - 1) % v.length];
    }

    public UITheme next() {
        UITheme[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
