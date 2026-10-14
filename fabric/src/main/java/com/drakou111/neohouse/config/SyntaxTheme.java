package com.drakou111.neohouse.config;

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
    ),

    TOKYO_NIGHT("Tokyo Night",
                0xFF7AA2F7, // keyword (blue)
                0xFF2AC3DE, // type (cyan)
                0xFFBB9AF7, // number (purple)
                0xFF9ECE6A, // string (green)
                0xFF565F89, // comment
                0xFF414868, // note
                0xFF7DCFFF, // action
                0xFFC0CAF5, // plain
                0xFFFF9E64, // playerVar (orange)
                0xFFBB9AF7, // globalVar
                0xFFF7768E, // teamVar (red)
                0xFF2AC3DE, // phVar
                0xFF9ECE6A  // phNameValid
    ),

    CATPPUCCIN("Catppuccin",
               0xFFCBA6F7, // keyword (mauve)
               0xFF94E2D5, // type (teal)
               0xFFF5C2E7, // number (pink)
               0xFFA6E3A1, // string (green)
               0xFF6C7086, // comment
               0xFF585B70, // note
               0xFF89B4FA, // action (blue)
               0xFFCDD6F4, // plain
               0xFFFAB387, // playerVar (peach)
               0xFFCBA6F7, // globalVar
               0xFFF38BA8, // teamVar (red)
               0xFF94E2D5, // phVar
               0xFFA6E3A1  // phNameValid
    ),

    GRUVBOX_DARK("Gruvbox Dark",
                 0xFFFB4934, // keyword (red)
                 0xFFFE8019, // type (orange)
                 0xFFD3869B, // number (purple)
                 0xFFB8BB26, // string (green)
                 0xFF928374, // comment
                 0xFF7C6F64, // note
                 0xFFFABD2F, // action (yellow)
                 0xFFEBDBB2, // plain
                 0xFF83A598, // playerVar (blue)
                 0xFFD3869B, // globalVar
                 0xFFFB4934, // teamVar
                 0xFF8EC07C, // phVar
                 0xFFB8BB26  // phNameValid
    ),

    MATERIAL_DARK("Material Dark",
                  0xFFC792EA, // keyword (purple)
                  0xFF82AAFF, // type (blue)
                  0xFFF78C6C, // number (orange)
                  0xFFC3E88D, // string (green)
                  0xFF546E7A, // comment
                  0xFF455A64, // note
                  0xFF89DDFF, // action (cyan)
                  0xFFEEFFFF, // plain
                  0xFFFFCB6B, // playerVar (yellow)
                  0xFFC792EA, // globalVar
                  0xFFF07178, // teamVar (red)
                  0xFF82AAFF, // phVar
                  0xFFC3E88D  // phNameValid
    ),

    AYU_DARK("Ayu Dark",
             0xFFFFCC66, // keyword (yellow)
             0xFF73D0FF, // type (blue)
             0xFFD4BFFF, // number (purple)
             0xFFD5FF80, // string (green)
             0xFF5C6773, // comment
             0xFF707A8C, // note
             0xFF59C2FF, // action
             0xFFBFBDB6, // plain
             0xFFFF8F40, // playerVar (orange)
             0xFFD4BFFF, // globalVar
             0xFFFF6B6B, // teamVar
             0xFF73D0FF, // phVar
             0xFFD5FF80  // phNameValid
    ),

    NIGHT_OWL("Night Owl",
              0xFF82AAFF, // keyword
              0xFF7FDBCA, // type
              0xFFC792EA, // number
              0xFFE3D18A, // string
              0xFF637777, // comment
              0xFF5F7E97, // note
              0xFF80CBC4, // action
              0xFFD6DEEB, // plain
              0xFFF78C6C, // playerVar
              0xFFC792EA, // globalVar
              0xFFFF5370, // teamVar
              0xFF82AAFF, // phVar
              0xFF80CBC4  // phNameValid
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
