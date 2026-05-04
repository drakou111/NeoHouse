package com.drakou111.neohouse;

public enum SyntaxTheme {
    VSCODE_DARK("VS Code Dark",
                0xFF569CD6,  // keyword
                0xFF4EC9B0,  // type
                0xFFB5CEA8,  // number
                0xFFCE9178,  // string
                0xFF6A9955,  // comment
                0xFF608B4E,  // note
                0xFFDCDCAA,  // action
                0xFFD4D4D4,  // plain
                0xFF4DAAFF,  // playerVar
                0xFFD4A0FF,  // globalVar
                0xFFFF9966,  // teamVar
                0xFF4FC1FF,  // phVar
                0xFF9CDCFE   // phNameValid
    ),
    MONOKAI("Monokai",
            0xFFF92672,  // keyword
            0xFF66D9E8,  // type
            0xFFAE81FF,  // number
            0xFFE6DB74,  // string
            0xFF75715E,  // comment
            0xFF75715E,  // note
            0xFFA6E22E,  // action
            0xFFF8F8F2,  // plain
            0xFFA6E22E,  // playerVar
            0xFFAE81FF,  // globalVar
            0xFFFD971F,  // teamVar
            0xFF66D9E8,  // phVar
            0xFF66D9E8   // phNameValid
    ),
    SOLARIZED_DARK("Solarized Dark",
                   0xFF268BD2, // keyword (blue)
                   0xFFB58900, // type (yellow)
                   0xFFD33682, // number (magenta)
                   0xFF2AA198, // string (cyan)
                   0xFF586E75, // comment
                   0xFF657B83, // note
                   0xFF859900, // action (green)
                   0xFFEEE8D5, // plain
                   0xFFCB4B16, // playerVar (orange)
                   0xFF6C71C4, // globalVar (violet)
                   0xFFDC322F, // teamVar (red)
                   0xFF268BD2, // phVar
                   0xFF2AA198  // phNameValid
    ),
    DRACULA("Dracula",
            0xFFFF79C6, // keyword (pink)
            0xFF8BE9FD, // type (cyan)
            0xFFBD93F9, // number (purple)
            0xFFF1FA8C, // string (yellow)
            0xFF6272A4, // comment
            0xFF6272A4, // note
            0xFF50FA7B, // action (green)
            0xFFF8F8F2, // plain
            0xFFFFB86C, // playerVar (orange)
            0xFFBD93F9, // globalVar
            0xFFFF5555, // teamVar (red)
            0xFF8BE9FD, // phVar
            0xFF50FA7B  // phNameValid
    ),
    ONE_DARK("One Dark",
             0xFFC678DD, // keyword (purple)
             0xFFE5C07B, // type (yellow)
             0xFFD19A66, // number (orange)
             0xFF98C379, // string (green)
             0xFF5C6370, // comment
             0xFF5C6370, // note
             0xFF61AFEF, // action (blue)
             0xFFABB2BF, // plain
             0xFFE06C75, // playerVar (red)
             0xFFC678DD, // globalVar
             0xFFD19A66, // teamVar
             0xFF56B6C2, // phVar (cyan)
             0xFF98C379  // phNameValid
    ),
    NORD("Nord",
         0xFF81A1C1, // keyword
         0xFF8FBCBB, // type
         0xFFB48EAD, // number
         0xFFA3BE8C, // string
         0xFF4C566A, // comment
         0xFF434C5E, // note
         0xFF88C0D0, // action
         0xFFD8DEE9, // plain
         0xFFEBCB8B, // playerVar
         0xFFB48EAD, // globalVar
         0xFFBF616A, // teamVar
         0xFF81A1C1, // phVar
         0xFF8FBCBB  // phNameValid
    );

    public final String displayName;
    public final int keyword, type, number, string, comment, note, action, plain;
    public final int playerVar, globalVar, teamVar, phVar, phNameValid;

    SyntaxTheme(String displayName,
                int keyword, int type, int number, int string,
                int comment, int note, int action, int plain,
                int playerVar, int globalVar, int teamVar, int phVar, int phNameValid) {
        this.displayName = displayName;
        this.keyword = keyword;
        this.type = type;
        this.number = number;
        this.string = string;
        this.comment = comment;
        this.note = note;
        this.action = action;
        this.plain = plain;
        this.playerVar = playerVar;
        this.globalVar = globalVar;
        this.teamVar = teamVar;
        this.phVar = phVar;
        this.phNameValid = phNameValid;
    }

    public static SyntaxTheme byId(String id) {
        for (SyntaxTheme t : values()) if (t.name().equals(id)) return t;
        return VSCODE_DARK;
    }

    public SyntaxTheme next() {
        SyntaxTheme[] vals = values();
        return vals[(ordinal() + 1) % vals.length];
    }

    public SyntaxTheme prev() {
        SyntaxTheme[] vals = values();
        return vals[(ordinal() + vals.length - 1) % vals.length];
    }
}
