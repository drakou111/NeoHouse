package com.drakou111.neohouse.config;

public enum UITheme {
    //                                bgTop     bgBottom  panel     dialog    accent    surface   highlight
    DARK_BLUE  ("Dark Blue",   0x0D0D18, 0x090912, 0x1A1A2A, 0x1E1E2B, 0x444466, 0x161620, 0x2A2A3A),
    DARK_PURPLE("Dark Purple", 0x120D1A, 0x0A0810, 0x1E1530, 0x241838, 0x5A4080, 0x16102A, 0x2A1E45),
    MIDNIGHT   ("Midnight",    0x08080E, 0x040408, 0x0F0F1E, 0x121225, 0x252545, 0x0A0A18, 0x181828),
    FOREST     ("Forest",      0x0C1A0E, 0x070E08, 0x152418, 0x1A2C1E, 0x3A6040, 0x101C12, 0x1E301E),
    CRIMSON    ("Crimson",     0x1A0D0D, 0x110808, 0x2A1616, 0x321A1A, 0x663333, 0x201313, 0x3A2020),
    SUNSET     ("Sunset",      0x1A0F0A, 0x110807, 0x2A1A14, 0x33201A, 0xFF7A3C, 0x20120D, 0x3D241C),
    OCEAN      ("Ocean",       0x0A1A1F, 0x071115, 0x12303A, 0x163A45, 0x3CA6A6, 0x0E2228, 0x1C3F48),
    CYBERPUNK  ("Cyberpunk",   0x0A0A0F, 0x050508, 0x1A1A25, 0x202030, 0xFF2E88, 0x141420, 0x2E2E44),
    GOLD       ("Gold",        0x1A160A, 0x110E06, 0x2A2412, 0x332C16, 0xD4AF37, 0x201B0D, 0x3A321A),
    ICE        ("Ice",         0x0F1820, 0x0A1218, 0x1E2C38, 0x243645, 0x7FDBFF, 0x16222C, 0x2A3E50),
    LAVENDER   ("Lavender",    0x140F18, 0x0C0A10, 0x241A2C, 0x2C2035, 0xB57EDC, 0x1A1220, 0x362845),
    TERMINAL   ("Terminal",    0x08110A, 0x050B07, 0x0F1F14, 0x13261A, 0x33FF77, 0x0A1810, 0x1A3324),
    MONO       ("Monochrome",  0x121212, 0x0A0A0A, 0x1E1E1E, 0x242424, 0xAAAAAA, 0x161616, 0x2A2A2A),
    ROSE       ("Rose",        0x1A0F14, 0x110A0E, 0x2A1A22, 0x33202A, 0xFF4F81, 0x20121A, 0x3D2430),
    VOLT       ("Volt",        0x0D0F0A, 0x080A06, 0x1A2412, 0x1F2C16, 0xBFFF00, 0x141A0D, 0x2A331A),
    SKY        ("Sky",         0x0A141A, 0x070E12, 0x142430, 0x1A2C38, 0x4DA6FF, 0x0F1C24, 0x243A4A),
    AMBER      ("Amber",       0x1A140A, 0x110D06, 0x2A220F, 0x332914, 0xFFBF00, 0x20190D, 0x3A2F18),
    WARDEN     ("Warden",      0x05080A, 0x030506, 0x0E1A1E, 0x122228, 0x00E5D8, 0x091316, 0x163238);

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
