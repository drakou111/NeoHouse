package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.SyntaxTheme;
import com.drakou111.neohouse.config.UIOpacity;
import com.drakou111.neohouse.config.UITheme;

import com.drakou111.neohouse.compat.PoseHelper;
import com.drakou111.neohouse.compat.RegistryHelper;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.ScreenBase;
import com.drakou111.neohouse.compiler.enums.ActionParamKind;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.AddCondition;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Placeholder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class HelpScreen extends ScreenBase {

    private static final int PAD       = 16;
    private static final int HEADER_H  = 28;
    private static final int TAB_BTN_H = 20;
    private static final int CONTENT_Y = HEADER_H + TAB_BTN_H + 2;
    private static final int LINE_H    = 16;
    private static final int ICON_SIZE = 16;
    private static final int ICON_PAD  = ICON_SIZE + 3;
    private static final int SB_W          = 4;
    private static final int SB_PAD        = 3;
    private static final int HEADER_LINE_H = 22;
    private static final int SPACER_H      = 14;

    private static final int COL_H    = 0xFFFFCC55;
    private static final int COL_C    = 0xFF88CCFF;
    private static final int COL_CODE = 0xFF88CCFE;
    private static final int COL_T    = 0xFFCCCCCC;
    private static final int COL_D    = 0xFF888888;

    private enum Tab {
        GENERAL("General"),
        SYNTAX("Syntax"),
        EXAMPLES("Examples"),
        ACTIONS("Actions"),
        CONDITIONS("Conditions"),
        EVENTS("Events"),
        PLACEHOLDERS("Placeholders");

        final String label;
        Tab(String label) { this.label = label; }
    }

    private record HelpLine(int color, String text, String icon) {
        HelpLine(int color, String text) { this(color, text, null); }
    }

    private record TabButton(Tab tab, int x, int w) {}

    private static final Map<Tab, List<HelpLine>> TAB_LINES;
    static {
        Map<Tab, List<HelpLine>> m = new EnumMap<>(Tab.class);
        m.put(Tab.GENERAL,      buildGeneral());
        m.put(Tab.SYNTAX,       buildSyntax());
        m.put(Tab.EXAMPLES,     buildExamples());
        m.put(Tab.ACTIONS,      buildActions());
        m.put(Tab.CONDITIONS,   buildConditions());
        m.put(Tab.EVENTS,       buildEvents());
        m.put(Tab.PLACEHOLDERS, buildPlaceholders());
        TAB_LINES = Collections.unmodifiableMap(m);
    }

    private static List<HelpLine> buildGeneral() {
        List<HelpLine> l = new ArrayList<>();

        l.add(new HelpLine(COL_H, "About"));
        l.add(new HelpLine(COL_T, "NeoHouse compiles .nh scripts into Hypixel Housing actions."));
        l.add(new HelpLine(COL_T, "Open scripts via the file browser or run them with /nh run."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Commands"));
        l.add(new HelpLine(COL_C, "  /nh                    — open home screen"));
        l.add(new HelpLine(COL_C, "  /nh help               — open this help screen"));
        l.add(new HelpLine(COL_C, "  /nh copyitem           — copies the data of the held item"));
        l.add(new HelpLine(COL_C, "  /nh giveitem           — gives the item with the given data"));
        l.add(new HelpLine(COL_C, "  /nh files              — open file browser"));
        l.add(new HelpLine(COL_C, "  /nh run <file>         — compile & run a script"));
        l.add(new HelpLine(COL_C, "  /nh rerun              — re-run last script"));
        l.add(new HelpLine(COL_C, "  /nh config             — open config screen"));
        l.add(new HelpLine(COL_C, "  /nh list functions     — list helper functions from last run"));
        l.add(new HelpLine(COL_C, "  /nh list variables     — list extra variables from last run"));
        l.add(new HelpLine(COL_C, "  /nh delete <section>   — deletes a specific section"));
        l.add(new HelpLine(COL_C, "  /nh nuke               — delete everything"));
        l.add(new HelpLine(COL_D, ""));

        return l;
    }

    private static List<HelpLine> buildSyntax() {
        List<HelpLine> l = new ArrayList<>();

        l.add(new HelpLine(COL_H, "Types"));
        l.add(new HelpLine(COL_C, "  long   — whole numbers (e.g. 42)"));
        l.add(new HelpLine(COL_C, "  double — decimal numbers (e.g. 3.14d)"));
        l.add(new HelpLine(COL_C, "  string — text (e.g. \"hello\")"));
        l.add(new HelpLine(COL_C, "  bool   — true or false"));
        l.add(new HelpLine(COL_C, "  var    — untyped, no compile-time checking"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Variables & Holders"));
        l.add(new HelpLine(COL_T, "  Variables are scoped to a holder:"));
        l.add(new HelpLine(COL_C, "    p.name        — player variable"));
        l.add(new HelpLine(COL_C, "    g.name        — global variable (shared)"));
        l.add(new HelpLine(COL_C, "    t[team].name  — team variable"));
        l.add(new HelpLine(COL_T, "  Declare at the top of your file:"));
        l.add(new HelpLine(COL_CODE, "    string g.word = \"N/A\";"));
        l.add(new HelpLine(COL_CODE, "    long g.count;"));
        l.add(new HelpLine(COL_D, "  Only g. and t[]. variables can have initializers."));
        l.add(new HelpLine(COL_D, "  Undeclared variables are treated as global var."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Fallback Values"));
        l.add(new HelpLine(COL_T, "  When a variable is read, add ?value to supply a fallback"));
        l.add(new HelpLine(COL_T, "  used when it hasn't been initialized yet:"));
        l.add(new HelpLine(COL_CODE, "    p.apple = p.test?0 + 2;   // p.test defaults to 0"));
        l.add(new HelpLine(COL_CODE, "    message(\"Name: \" + p.nick?\"none\");"));
        l.add(new HelpLine(COL_D, "  Works on p., g. and t[]. reads (not ph. placeholders)."));
        l.add(new HelpLine(COL_D, "  The fallback must be a constant of the SAME type as the"));
        l.add(new HelpLine(COL_D, "  variable (any constant is allowed for var / undeclared)."));
        l.add(new HelpLine(COL_D, "  Binds tightly: p.test?0 + 2 means (p.test?0) + 2."));
        l.add(new HelpLine(COL_D, "  Compiles to Housing's %var.player/name fallback% form."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Constants"));
        l.add(new HelpLine(COL_T, "  Named compile-time values — inlined with no runtime cost:"));
        l.add(new HelpLine(COL_CODE, "    const long MAX_SCORE = 100;"));
        l.add(new HelpLine(COL_CODE, "    const string PREFIX = \"[Game]\";"));
        l.add(new HelpLine(COL_T, "  Use the name anywhere a literal is valid:"));
        l.add(new HelpLine(COL_CODE, "    if (p.score >= MAX_SCORE) { ... }"));
        l.add(new HelpLine(COL_D, "  Types: long, double, string, bool (not var)."));
        l.add(new HelpLine(COL_D, "  Value must be a literal or another constant."));
        l.add(new HelpLine(COL_D, "  Declare at the top of the file, before functions."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Enums"));
        l.add(new HelpLine(COL_T, "  A named type whose members are numbered from 0:"));
        l.add(new HelpLine(COL_CODE, "    enum Color { RED, GREEN, BLUE }"));
        l.add(new HelpLine(COL_T, "  Use the enum name as a type; members are Enum.MEMBER:"));
        l.add(new HelpLine(COL_CODE, "    Color p.fav;"));
        l.add(new HelpLine(COL_CODE, "    p.fav = Color.GREEN;      // stored as 1"));
        l.add(new HelpLine(COL_CODE, "    if (p.fav == Color.RED) { ... }"));
        l.add(new HelpLine(COL_D, "  Stored as longs, but treated as their own type:"));
        l.add(new HelpLine(COL_D, "  you can only compare a Color with a Color, and enums"));
        l.add(new HelpLine(COL_D, "  cannot be used in arithmetic or other expressions."));
        l.add(new HelpLine(COL_T, "  Usable as function parameter and return types:"));
        l.add(new HelpLine(COL_CODE, "    function Color favColor() { return Color.RED; }"));
        l.add(new HelpLine(COL_CODE, "    function paint(Color c) { ... }"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Operators"));
        l.add(new HelpLine(COL_C, "  Arithmetic:   + - * / %"));
        l.add(new HelpLine(COL_C, "  Bitwise:      & | ^ << >> >>>"));
        l.add(new HelpLine(COL_C, "  Unary:        a++  a--  -a  !a"));
        l.add(new HelpLine(COL_C, "  Comparison:   == != < <= > >="));
        l.add(new HelpLine(COL_C, "  Logical:      && ||"));
        l.add(new HelpLine(COL_C, "  Assignment:   = += -= *= /= %= &= |= ^= <<= >>= >>>="));
        l.add(new HelpLine(COL_C, "  Numbers:      42  1_000  0xFF00  0b1010  1.5"));
        l.add(new HelpLine(COL_D, "  Constants:    PI  E  HALF_PI  TWO_PI  PHI  SQRT_TWO"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Casting"));
        l.add(new HelpLine(COL_C, "  (long)x     — truncate to whole number"));
        l.add(new HelpLine(COL_C, "  (double)x   — convert to decimal"));
        l.add(new HelpLine(COL_C, "  (string)x   — convert to string"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "String Concatenation"));
        l.add(new HelpLine(COL_T, "  Use + to join strings, variables, and placeholders:"));
        l.add(new HelpLine(COL_CODE, "    message(\"Score: \" + g.score + \"!\");"));
        l.add(new HelpLine(COL_T, "  Compiled into Housing placeholder format at build time."));
        l.add(new HelpLine(COL_T, "  Result must be at most 256 characters."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Placeholders"));
        l.add(new HelpLine(COL_T, "  Read-only values from the housing or player:"));
        l.add(new HelpLine(COL_C, "    ph.placeholderName"));
        l.add(new HelpLine(COL_C, "    ph.placeholderName(arg1, arg2)"));
        l.add(new HelpLine(COL_T, "  See the Placeholders tab for the full list."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Functions"));
        l.add(new HelpLine(COL_T, "  Void function (no return value):"));
        l.add(new HelpLine(COL_CODE, "    function greet() { message(\"Hi!\"); }"));
        l.add(new HelpLine(COL_T, "  Typed function (returns a value):"));
        l.add(new HelpLine(COL_CODE, "    function long add(long a, long b) {"));
        l.add(new HelpLine(COL_CODE, "      return a + b;"));
        l.add(new HelpLine(COL_CODE, "    }"));
        l.add(new HelpLine(COL_CODE, "    long result = add(1, 2);"));
        l.add(new HelpLine(COL_T, "  Parameters are passed by value — changes inside"));
        l.add(new HelpLine(COL_T, "  the function do not affect the original variable."));
        l.add(new HelpLine(COL_D, "  Function names cannot clash with built-in actions."));
        l.add(new HelpLine(COL_T, "  Any function can be called as void (return discarded):"));
        l.add(new HelpLine(COL_CODE, "    add(1, 2);"));
        l.add(new HelpLine(COL_T, "  Optional named params (before regular params):"));
        l.add(new HelpLine(COL_CODE, "    function greet(icon: {id:\"minecraft:diamond\"},"));
        l.add(new HelpLine(COL_CODE, "                   description: \"Says hi\") {"));
        l.add(new HelpLine(COL_CODE, "      message(\"Hi!\");"));
        l.add(new HelpLine(COL_CODE, "    }"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Tuple Functions"));
        l.add(new HelpLine(COL_T, "  Return multiple values from a single function:"));
        l.add(new HelpLine(COL_CODE, "    function (double, double) getVec() {"));
        l.add(new HelpLine(COL_CODE, "      return (2.0, 1.0);"));
        l.add(new HelpLine(COL_CODE, "    }"));
        l.add(new HelpLine(COL_T, "  Capture with a tuple destructure (vars must exist,"));
        l.add(new HelpLine(COL_T, "  types must match the function's return types):"));
        l.add(new HelpLine(COL_CODE, "    (p.x, p.y) = getVec();"));
        l.add(new HelpLine(COL_T, "  Any assignment operator is allowed:"));
        l.add(new HelpLine(COL_CODE, "    (p.x, p.y) += getVec();"));
        l.add(new HelpLine(COL_T, "  Or call as void to discard the outputs:"));
        l.add(new HelpLine(COL_CODE, "    getVec();"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Global Function Call"));
        l.add(new HelpLine(COL_T, "  Triggers a void function for ALL players in the housing:"));
        l.add(new HelpLine(COL_CODE, "    global greet();"));
        l.add(new HelpLine(COL_D, "  Only void functions can be called globally."));
        l.add(new HelpLine(COL_D, "  This maps to Hypixel's \"Trigger Function (for all)\" action."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Conditionals"));
        l.add(new HelpLine(COL_CODE, "    if (p.score >= 100 && isSneaking(false)) {"));
        l.add(new HelpLine(COL_CODE, "      message(\"Done!\");"));
        l.add(new HelpLine(COL_CODE, "    } else { ... }"));
        l.add(new HelpLine(COL_D, "  Use && (AND) or || (OR) — not both in one expression."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Override Modes"));
        l.add(new HelpLine(COL_T, "  Set in the config. Decides what happens to functions,"));
        l.add(new HelpLine(COL_T, "  events, loops, commands and menus that already exist:"));
        l.add(new HelpLine(COL_D, "  Always Override: recreate everything from scratch."));
        l.add(new HelpLine(COL_D, "  Never Override: leave existing ones untouched."));
        l.add(new HelpLine(COL_D, "  Override If Empty: fill existing ones only when they"));
        l.add(new HelpLine(COL_D, "  have no content yet (the default)."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Loops"));
        l.add(new HelpLine(COL_T, "  Repeats on a fixed tick interval (minimum 4):"));
        l.add(new HelpLine(COL_CODE, "    loop myLoop(10) { ... }"));
        l.add(new HelpLine(COL_D, "  Cannot be typed or have parameters."));
        l.add(new HelpLine(COL_T, "  Supports icon/description before the tick delay:"));
        l.add(new HelpLine(COL_CODE, "    loop clock(icon: {id:\"minecraft:clock\"},"));
        l.add(new HelpLine(COL_CODE, "              description: \"Fires every second\", 20) { ... }"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Random"));
        l.add(new HelpLine(COL_T, "  Executes one block at random:"));
        l.add(new HelpLine(COL_CODE, "    random [ { kill(); }  { heal(); } ]"));
        l.add(new HelpLine(COL_T, "  Use N* to weight an option:"));
        l.add(new HelpLine(COL_CODE, "    random [ 3*{ message(\"Common\"); }  { kill(); } ]"));
        l.add(new HelpLine(COL_D, "  Weights are reduced by their GCD before compiling."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Repeat"));
        l.add(new HelpLine(COL_T, "  Unrolls its body N times at compile time:"));
        l.add(new HelpLine(COL_CODE, "    repeat(3) { message(\"tick\"); }"));
        l.add(new HelpLine(COL_D, "  Not a runtime loop — the actions are literally copied."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Events"));
        l.add(new HelpLine(COL_T, "  Triggered automatically by in-game events:"));
        l.add(new HelpLine(COL_CODE, "    event onJoin { message(\"Welcome!\"); }"));
        l.add(new HelpLine(COL_T, "  See the Events tab for the full list."));
        l.add(new HelpLine(COL_D, "  Some events have restricted action counts."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Commands"));
        l.add(new HelpLine(COL_CODE, "    command myCmd(priority: 5, listed: false) {"));
        l.add(new HelpLine(COL_CODE, "      kill();"));
        l.add(new HelpLine(COL_CODE, "    }"));
        l.add(new HelpLine(COL_D, "  Options: priority (0-20), listed, targeted."));
        l.add(new HelpLine(COL_D, "  Name must be at least 3 characters."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Menus"));
        l.add(new HelpLine(COL_CODE, "    menu myMenu(3) {"));
        l.add(new HelpLine(COL_CODE, "      slot0: ({count:1,id:\"minecraft:diamond\"}) {"));
        l.add(new HelpLine(COL_CODE, "        message(\"Clicked!\");"));
        l.add(new HelpLine(COL_CODE, "      }"));
        l.add(new HelpLine(COL_CODE, "    }"));
        l.add(new HelpLine(COL_D, "  The number is the row count (1-6). Slots are 0-indexed."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "File Import"));
        l.add(new HelpLine(COL_T, "  Include another .nh file inline:"));
        l.add(new HelpLine(COL_CODE, "    import \"path/to/other.nh\";"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Recursion"));
        l.add(new HelpLine(COL_T, "  Functions that call themselves directly or indirectly."));
        l.add(new HelpLine(COL_T, "  The compiler detects cycles in the call graph, e.g.:"));
        l.add(new HelpLine(COL_T, "  \"Recursive call: a → b → a\". The recursionMode config"));
        l.add(new HelpLine(COL_T, "  controls how it is handled:"));
        l.add(new HelpLine(COL_C, "    Off   — allowed silently"));
        l.add(new HelpLine(COL_C, "    Warn  — allowed, but reported (default)"));
        l.add(new HelpLine(COL_C, "    Error — compilation fails"));
        l.add(new HelpLine(COL_D, "  Hypixel Housing does not officially support recursive"));
        l.add(new HelpLine(COL_D, "  functions — enable only if you know what you are doing."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Multi-call behavior"));
        l.add(new HelpLine(COL_T, "  Calling the same function more than once in a single"));
        l.add(new HelpLine(COL_T, "  execution context (directly or transitively) causes issues"));
        l.add(new HelpLine(COL_T, "  in Hypixel Housing. The multiCallMode config controls this:"));
        l.add(new HelpLine(COL_C, "    Off  — no check performed"));
        l.add(new HelpLine(COL_C, "    Warn — compile error if a multi-call is detected"));
        l.add(new HelpLine(COL_C, "    Fix  — auto-create copies (foo$0, foo$1, ...) so"));
        l.add(new HelpLine(COL_C, "           each invocation uses a unique function name"));
        l.add(new HelpLine(COL_D, "  Configure via /nh config or in neohouse-config.json."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Global call rules"));
        l.add(new HelpLine(COL_T, "  Functions called via 'global foo()' are triggered for"));
        l.add(new HelpLine(COL_T, "  every player. Two restrictions apply:"));
        l.add(new HelpLine(COL_C, "    1. A globally-called function cannot itself contain"));
        l.add(new HelpLine(COL_C, "       'global ...' calls — this would be doubly-global."));
        l.add(new HelpLine(COL_C, "    2. Loops run for all players and cannot contain"));
        l.add(new HelpLine(COL_C, "       'global ...' calls for the same reason."));
        l.add(new HelpLine(COL_T, "  The globalCallMode config controls enforcement:"));
        l.add(new HelpLine(COL_C, "    Off   — allowed silently"));
        l.add(new HelpLine(COL_C, "    Warn  — allowed, but reported"));
        l.add(new HelpLine(COL_C, "    Error — compilation fails (default)"));
        l.add(new HelpLine(COL_D, ""));

        return l;
    }

    private static List<HelpLine> buildExamples() {
        List<HelpLine> l = new ArrayList<>();

        // ── 1. Hello World ────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "1. Hello World"));
        l.add(new HelpLine(COL_T, "  Send a message to the player when they join."));
        l.add(new HelpLine(COL_CODE, "  event onJoin {"));
        l.add(new HelpLine(COL_CODE, "    message(\"Welcome to my house!\");"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_D, "  The event block runs automatically on the trigger."));
        l.add(new HelpLine(COL_D, ""));

        // ── 2. Variables ──────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "2. Variables"));
        l.add(new HelpLine(COL_T, "  Count how many times each player has joined."));
        l.add(new HelpLine(COL_CODE, "  long p.joins;"));
        l.add(new HelpLine(COL_CODE, ""));
        l.add(new HelpLine(COL_CODE, "  event onJoin {"));
        l.add(new HelpLine(COL_CODE, "    p.joins += 1;"));
        l.add(new HelpLine(COL_CODE, "    message(\"You have joined \" + p.joins + \" time(s)!\");"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_D, "  p. variables are per-player. g. variables are shared."));
        l.add(new HelpLine(COL_D, "  Declare without a holder at the top of the file."));
        l.add(new HelpLine(COL_D, ""));

        // ── 3. Global Counter ─────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "3. Global Counter"));
        l.add(new HelpLine(COL_T, "  Track total joins across all players."));
        l.add(new HelpLine(COL_CODE, "  long g.totalJoins = 0;"));
        l.add(new HelpLine(COL_CODE, ""));
        l.add(new HelpLine(COL_CODE, "  event onJoin {"));
        l.add(new HelpLine(COL_CODE, "    g.totalJoins += 1;"));
        l.add(new HelpLine(COL_CODE, "    message(\"Total joins: \" + g.totalJoins);"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_D, "  g. variables persist until the housing resets."));
        l.add(new HelpLine(COL_D, ""));

        // ── 4. Conditionals ───────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "4. Conditionals"));
        l.add(new HelpLine(COL_T, "  Reward players who reach a score threshold."));
        l.add(new HelpLine(COL_CODE, "  long p.score;"));
        l.add(new HelpLine(COL_CODE, ""));
        l.add(new HelpLine(COL_CODE, "  function checkScore() {"));
        l.add(new HelpLine(COL_CODE, "    if (p.score >= 100) {"));
        l.add(new HelpLine(COL_CODE, "      message(\"You reached 100 points!\");"));
        l.add(new HelpLine(COL_CODE, "      heal();"));
        l.add(new HelpLine(COL_CODE, "    } else {"));
        l.add(new HelpLine(COL_CODE, "      message(\"Keep going: \" + p.score + \" / 100\");"));
        l.add(new HelpLine(COL_CODE, "    }"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_D, "  Use && (AND) or || (OR) in conditions, not both at once."));
        l.add(new HelpLine(COL_D, ""));

        // ── 5. Functions ──────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "5. Functions"));
        l.add(new HelpLine(COL_T, "  Reusable logic — void and typed."));
        l.add(new HelpLine(COL_CODE, "  function greet() {"));
        l.add(new HelpLine(COL_CODE, "    message(\"Hello, player!\");"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_CODE, ""));
        l.add(new HelpLine(COL_CODE, "  function long double(long n) {"));
        l.add(new HelpLine(COL_CODE, "    return n * 2;"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_CODE, ""));
        l.add(new HelpLine(COL_CODE, "  event onJoin {"));
        l.add(new HelpLine(COL_CODE, "    greet();"));
        l.add(new HelpLine(COL_CODE, "    long result = double(5);"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_D, "  Parameters are copied — changes inside don't affect callers."));
        l.add(new HelpLine(COL_D, ""));

        // ── 6. Custom Command ─────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "6. Custom Command"));
        l.add(new HelpLine(COL_T, "  Let players type /heal to restore themselves."));
        l.add(new HelpLine(COL_CODE, "  command heal(listed: true, priority: 5) {"));
        l.add(new HelpLine(COL_CODE, "    heal();"));
        l.add(new HelpLine(COL_CODE, "    message(\"You have been healed!\");"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_D, "  priority: 0-20. listed: appears in /. targeted: per-player."));
        l.add(new HelpLine(COL_D, "  Name must be at least 3 characters."));
        l.add(new HelpLine(COL_D, ""));

        // ── 7. Loops ──────────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "7. Loops"));
        l.add(new HelpLine(COL_T, "  Run code on a fixed tick interval (minimum 4 ticks)."));
        l.add(new HelpLine(COL_CODE, "  long g.seconds = 0;"));
        l.add(new HelpLine(COL_CODE, ""));
        l.add(new HelpLine(COL_CODE, "  loop clock(20) {"));
        l.add(new HelpLine(COL_CODE, "    g.seconds += 1;"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_D, "  20 ticks = 1 second. Loops cannot have parameters."));
        l.add(new HelpLine(COL_D, ""));

        // ── 8. Random ─────────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "8. Random"));
        l.add(new HelpLine(COL_T, "  Execute one outcome at random. Weight with N*."));
        l.add(new HelpLine(COL_CODE, "  event onJoin {"));
        l.add(new HelpLine(COL_CODE, "    random ["));
        l.add(new HelpLine(COL_CODE, "      3*{ message(\"Common outcome\"); }"));
        l.add(new HelpLine(COL_CODE, "         { message(\"Rare outcome A\"); }"));
        l.add(new HelpLine(COL_CODE, "         { message(\"Rare outcome B\"); }"));
        l.add(new HelpLine(COL_CODE, "    ]"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_D, "  Weights are reduced by GCD. 3:1:1 above means 60%:20%:20%."));
        l.add(new HelpLine(COL_D, ""));

        // ── 9. Menus ──────────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "9. Menus"));
        l.add(new HelpLine(COL_T, "  A 3-row inventory GUI with clickable slots."));
        l.add(new HelpLine(COL_CODE, "  menu shop(3) {"));
        l.add(new HelpLine(COL_CODE, "    slot0: ({count:1,id:\"minecraft:diamond\"}) {"));
        l.add(new HelpLine(COL_CODE, "      message(\"You clicked the diamond!\");"));
        l.add(new HelpLine(COL_CODE, "      closeMenu();"));
        l.add(new HelpLine(COL_CODE, "    }"));
        l.add(new HelpLine(COL_CODE, "    slot8: ({count:1,id:\"minecraft:barrier\"}) {"));
        l.add(new HelpLine(COL_CODE, "      closeMenu();"));
        l.add(new HelpLine(COL_CODE, "    }"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_CODE, ""));
        l.add(new HelpLine(COL_CODE, "  event onJoin { displayMenu(\"shop\"); }"));
        l.add(new HelpLine(COL_D, "  Rows: 1-6. Slots are 0-indexed left to right, top to bottom."));
        l.add(new HelpLine(COL_D, ""));

        // ── 10. Repeat ────────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "10. Repeat"));
        l.add(new HelpLine(COL_T, "  Unrolls a block N times at compile time (not runtime)."));
        l.add(new HelpLine(COL_CODE, "  event onJoin {"));
        l.add(new HelpLine(COL_CODE, "    repeat(3) {"));
        l.add(new HelpLine(COL_CODE, "      message(\"Spam!\");"));
        l.add(new HelpLine(COL_CODE, "    }"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_D, "  Equivalent to writing the same action 3 times manually."));
        l.add(new HelpLine(COL_D, ""));

        // ── 11. String Building ───────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "11. String Building"));
        l.add(new HelpLine(COL_T, "  Join text, variables, and placeholders with +."));
        l.add(new HelpLine(COL_CODE, "  long p.score;"));
        l.add(new HelpLine(COL_CODE, ""));
        l.add(new HelpLine(COL_CODE, "  event onJoin {"));
        l.add(new HelpLine(COL_CODE, "    message(\"Hi \" + ph.playerName + \"! Score: \" + p.score);"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_D, "  Folded into a single Housing placeholder string at compile time."));
        l.add(new HelpLine(COL_D, "  Result must be at most 256 characters."));
        l.add(new HelpLine(COL_D, ""));

        // ── 12. Events & Cancel ───────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "12. Events & Cancel"));
        l.add(new HelpLine(COL_T, "  Cancel an event to prevent its default behavior."));
        l.add(new HelpLine(COL_CODE, "  event onDeath {"));
        l.add(new HelpLine(COL_CODE, "    message(\"You died!\");"));
        l.add(new HelpLine(COL_CODE, "    cancel;"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_D, "  cancel; is a statement, not a function call."));
        l.add(new HelpLine(COL_D, ""));

        // ── 13. Import ────────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "13. Import"));
        l.add(new HelpLine(COL_T, "  Split large scripts across multiple .nh files."));
        l.add(new HelpLine(COL_CODE, "  // main.nh"));
        l.add(new HelpLine(COL_CODE, "  import \"lib/utils.nh\";"));
        l.add(new HelpLine(COL_CODE, ""));
        l.add(new HelpLine(COL_CODE, "  event onJoin {"));
        l.add(new HelpLine(COL_CODE, "    greet();  // defined in lib/utils.nh"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_D, "  The imported file's contents are inlined before compiling."));
        l.add(new HelpLine(COL_D, ""));

        // ── 14. Putting it all together ───────────────────────────────────────
        l.add(new HelpLine(COL_H, "14. Full Example"));
        l.add(new HelpLine(COL_T, "  A score system with a command and win condition."));
        l.add(new HelpLine(COL_CODE, "  long p.score;"));
        l.add(new HelpLine(COL_CODE, "  long g.highScore = 0;"));
        l.add(new HelpLine(COL_CODE, ""));
        l.add(new HelpLine(COL_CODE, "  function addScore(long n) {"));
        l.add(new HelpLine(COL_CODE, "    p.score += n;"));
        l.add(new HelpLine(COL_CODE, "    if (p.score > g.highScore) {"));
        l.add(new HelpLine(COL_CODE, "      g.highScore = p.score;"));
        l.add(new HelpLine(COL_CODE, "    }"));
        l.add(new HelpLine(COL_CODE, "    if (p.score >= 1000) {"));
        l.add(new HelpLine(COL_CODE, "      message(\"You won with score \" + p.score + \"!\");"));
        l.add(new HelpLine(COL_CODE, "      p.score = 0;"));
        l.add(new HelpLine(COL_CODE, "    }"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_CODE, ""));
        l.add(new HelpLine(COL_CODE, "  command score(listed: true) {"));
        l.add(new HelpLine(COL_CODE, "    message(\"Score: \" + p.score + \" | Best: \" + g.highScore);"));
        l.add(new HelpLine(COL_CODE, "  }"));
        l.add(new HelpLine(COL_CODE, ""));
        l.add(new HelpLine(COL_CODE, "  event onKill { addScore(10); }"));
        l.add(new HelpLine(COL_CODE, "  event onDeath { addScore(-5); }"));
        l.add(new HelpLine(COL_D, ""));

        return l;
    }

    private static String kindStr(ActionParamKind k) {
        return switch (k) {
            case STRING_LITERAL    -> "string";
            case BOOL_LITERAL      -> "bool";
            case LONG_LITERAL      -> "long";
            case DOUBLE_LITERAL    -> "double";
            case NORMAL_EXPRESSION -> "expr";
            case ITEM_LITERAL      -> "item";
            case LOCATION          -> "location";
        };
    }

    private static String typeStr(VariableType t) {
        return switch (t) {
            case STRING -> "string";
            case LONG   -> "long";
            case DOUBLE -> "double";
            case BOOL   -> "bool";
            case VAR    -> "var";
        };
    }

    private static List<HelpLine> buildActions() {
        List<HelpLine> l = new ArrayList<>();
        for (AddAction a : AddAction.values()) {
            if (!a.canBeUsed) continue;
            StringBuilder sig = new StringBuilder(a.name).append("(");
            for (int i = 0; i < a.params.size(); i++) {
                if (i > 0) sig.append(", ");
                sig.append(kindStr(a.params.get(i).kind()));
            }
            sig.append(")");
            l.add(new HelpLine(COL_CODE, sig.toString(), a.texture));
            if (!a.description.isEmpty())
                l.add(new HelpLine(COL_D, "    " + a.description));
            l.add(new HelpLine(COL_D, ""));
        }
        return l;
    }

    private static List<HelpLine> buildConditions() {
        List<HelpLine> l = new ArrayList<>();
        for (AddCondition c : AddCondition.values()) {
            if (!c.canBeUsed) continue;
            StringBuilder sig = new StringBuilder(c.name).append("(");
            for (int i = 0; i < c.params.size(); i++) {
                if (i > 0) sig.append(", ");
                sig.append(kindStr(c.params.get(i).kind()));
            }
            sig.append(")");
            l.add(new HelpLine(COL_CODE, sig.toString(), c.texture));
            if (!c.description.isEmpty())
                l.add(new HelpLine(COL_D, "    " + c.description));
            l.add(new HelpLine(COL_D, ""));
        }
        return l;
    }

    private static List<HelpLine> buildEvents() {
        List<HelpLine> l = new ArrayList<>();
        for (Event e : Event.values()) {
            l.add(new HelpLine(COL_CODE, "event " + e.name, e.texture));
            if (!e.description.isEmpty())
                l.add(new HelpLine(COL_D, "    " + e.description));
            l.add(new HelpLine(COL_D, ""));
        }
        return l;
    }

    private static List<HelpLine> buildPlaceholders() {
        List<HelpLine> l = new ArrayList<>();
        for (Placeholder p : Placeholder.values()) {
            StringBuilder sig = new StringBuilder("ph.").append(p.name);
            if (!p.constTypes.isEmpty()) {
                sig.append("(");
                for (int i = 0; i < p.constTypes.size(); i++) {
                    if (i > 0) sig.append(", ");
                    sig.append(typeStr(p.constTypes.get(i)));
                }
                sig.append(")");
            }
            sig.append(" : ").append(typeStr(p.type));
            l.add(new HelpLine(COL_CODE, sig.toString()));
            if (!p.description.isEmpty())
                l.add(new HelpLine(COL_D, "    " + p.description));
            l.add(new HelpLine(COL_D, ""));
        }
        return l;
    }

    private static int lineH(HelpLine line) {
        if (line.color() == COL_H)                          return HEADER_LINE_H;
        if (line.color() == COL_D && line.text().isEmpty()) return SPACER_H;
        return LINE_H;
    }

    private static int totalH(List<HelpLine> lines) {
        int h = 0;
        for (HelpLine l : lines) h += lineH(l);
        return h;
    }

    private final SyntaxHighlighter highlighter = new SyntaxHighlighter();

    private Tab activeTab = Tab.GENERAL;
    private final Map<Tab, Integer> scrollPos = new EnumMap<>(Tab.class);
    private List<TabButton> tabButtons = Collections.emptyList();

    private boolean sbDragging = false;
    private int sbDragStartY;
    private int sbDragStartScroll;

    private int selAnchorLine = -1;
    private int selAnchorChar = -1;
    private int selHeadLine   = -1;
    private int selHeadChar   = -1;
    private boolean selecting  = false;
    private Tab selTab         = null;

    private final Screen parent;

    public HelpScreen(Screen parent) {
        super(Component.literal("NeoHouse Help"));
        this.parent = parent;
        for (Tab t : Tab.values()) scrollPos.put(t, 0);
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("< Back"), b -> minecraft.setScreen(parent))
                .bounds(PAD, 5, 52, 18).build());
        addRenderableWidget(Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), b -> onClose())
                .bounds(this.width - PAD - 18, 5, 18, 18).build());

        List<TabButton> tabs = new ArrayList<>();
        int x = PAD;
        for (Tab t : Tab.values()) {
            int w = font.width(t.label) + 14;
            tabs.add(new TabButton(t, x, w));
            x += w + 2;
        }
        tabButtons = Collections.unmodifiableList(tabs);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UITheme t   = NeoHouseConfig.get().uiTheme;
        UIOpacity o = NeoHouseConfig.get().uiOpacity;
        g.fillGradient(0, 0, this.width, this.height, t.bgTop(o.bgAlpha), t.bgBottom(o.bgAlpha));
        g.fill(0, 0, this.width, HEADER_H, t.panel(o.panelAlpha));
        RenderHelper.renderLogo(g, PAD + 56, 4, 18, t.accent());
        g.drawString(this.font, "NeoHouse — Help", PAD + 56 + 22, 9, 0xFFFFFFFF);
        g.fill(0, HEADER_H, this.width, HEADER_H + 1, t.accent());

        int tabY = HEADER_H + 1;
        for (TabButton tb : tabButtons) {
            boolean active = tb.tab() == activeTab;
            boolean hover  = mouseX >= tb.x() && mouseX < tb.x() + tb.w()
                          && mouseY >= tabY    && mouseY < tabY + TAB_BTN_H;
            int bg = active ? t.highlight(0xFF) : hover ? t.panel(0xFF) : t.surface(0xFF);
            g.fill(tb.x(), tabY, tb.x() + tb.w(), tabY + TAB_BTN_H, bg);
            int labelX = tb.x() + (tb.w() - font.width(tb.tab().label)) / 2;
            int labelY = tabY + (TAB_BTN_H - 8) / 2;
            int textColor = active ? 0xFFFFFF88 : hover ? 0xFFCCCCCC : 0xFF888899;
            g.drawString(this.font, tb.tab().label, labelX, labelY, textColor, false);
            if (active)
                g.fill(tb.x(), tabY + TAB_BTN_H - 2, tb.x() + tb.w(), tabY + TAB_BTN_H, t.accent());
        }
        g.fill(0, tabY + TAB_BTN_H, this.width, tabY + TAB_BTN_H + 1, t.accent());

        super.render(g, mouseX, mouseY, partialTick);

        List<HelpLine> lines = TAB_LINES.get(activeTab);
        int scrollPx     = clampScroll(lines);
        int listH        = this.height - CONTENT_Y - 4;
        int totalHpx     = totalH(lines);
        int contentRight = this.width - SB_W - SB_PAD - 2;

        g.fill(0, CONTENT_Y, contentRight, this.height - 4, t.panel(0x33));

        int[] sel   = (selTab == activeTab && selAnchorLine >= 0 && selHeadLine >= 0) ? normalizedSel() : null;
        boolean hasSel = sel != null && (sel[0] != sel[2] || sel[1] != sel[3]);

        g.enableScissor(0, CONTENT_Y, contentRight, this.height - 4);
        int cumY = CONTENT_Y - scrollPx;
        int cardTopY = -1;
        for (int i = 0; i < lines.size(); i++) {
            HelpLine line = lines.get(i);
            int lh    = lineH(line);
            int nextY = cumY + lh;
            if (nextY <= CONTENT_Y) { cumY = nextY; continue; }
            if (cumY  >= this.height - 4) break;

            int y     = cumY;
            int textX = PAD;
            int textY = y + (lh - font.lineHeight) / 2;

            if (line.color() == COL_H) {
                if (cardTopY >= 0) {
                    RenderHelper.renderOutline(g, 1, cardTopY, contentRight - 2, cumY - cardTopY, t.accentTint(0x44));
                    cardTopY = -1;
                }
                g.fill(0, y, contentRight, y + lh, t.panel(0xEE));
                g.fill(0, y, 3, y + lh, t.accent());
                g.fill(3, y + lh - 1, contentRight, y + lh, t.accentTint(0x55));
                g.drawString(font, line.text(), PAD + 5, textY, line.color(), false);

            } else if (line.color() == COL_D && line.text().isEmpty()) {
                if (cardTopY >= 0) {
                    RenderHelper.renderOutline(g, 1, cardTopY, contentRight - 2, cumY - cardTopY, t.accentTint(0x44));
                    cardTopY = -1;
                }
                boolean nextIsIcon = i < lines.size() - 1 && lines.get(i + 1).icon() != null;
                if (nextIsIcon) {
                    int divY = y + SPACER_H / 2;
                    g.fill(PAD * 2, divY, contentRight - PAD * 2, divY + 1, t.accentTint(0x33));
                }

            } else {
                boolean isCodeBg = line.color() == COL_C || line.color() == COL_CODE;
                boolean isCodeHL = line.color() == COL_CODE;
                boolean hasIcon  = line.icon() != null;
                boolean prevCode = i > 0 && (lines.get(i-1).color() == COL_C || lines.get(i-1).color() == COL_CODE);
                boolean nextCode = i < lines.size()-1 && (lines.get(i+1).color() == COL_C || lines.get(i+1).color() == COL_CODE);
                boolean isEntryDesc = line.color() == COL_D && !line.text().isEmpty()
                        && i > 0 && lines.get(i-1).icon() != null;

                if (isCodeBg) {
                    g.fill(0, y, contentRight, y + lh, t.surface(0xAA));
                    if (!hasIcon) g.fill(0, y, 2, y + lh, t.accentTint(0xCC));
                    if (!prevCode) g.fill(0, y, contentRight, y + 1, t.accentTint(0x55));
                    if (!nextCode) g.fill(0, y + lh - 1, contentRight, y + lh, t.accentTint(0x55));
                } else if (isEntryDesc) {
                    g.fill(0, y, contentRight, y + lh, t.surface(0x55));
                }

                if (hasIcon && !line.text().isEmpty()) {
                    if (cardTopY < 0) cardTopY = y;
                    int iconY = y + (lh - ICON_SIZE) / 2;
                    renderIcon(g, line.icon(), PAD, iconY);
                    textX = PAD + ICON_PAD;
                }

                if (isCodeHL && !line.text().isEmpty()) {
                    renderSyntaxLine(g, line.text(), textX, textY);
                } else {
                    g.drawString(font, line.text(), textX, textY, line.color(), false);
                }
            }

            if (hasSel && i >= sel[0] && i <= sel[2]) {
                String txt = line.text();
                int txX = textXForLine(line);
                int hiL, hiR;
                if (i == sel[0] && i == sel[2]) {
                    hiL = txX + font.width(txt.substring(0, Math.min(sel[1], txt.length())));
                    hiR = txX + font.width(txt.substring(0, Math.min(sel[3], txt.length())));
                } else if (i == sel[0]) {
                    hiL = txX + font.width(txt.substring(0, Math.min(sel[1], txt.length())));
                    hiR = contentRight;
                } else if (i == sel[2]) {
                    hiL = 0;
                    hiR = txX + font.width(txt.substring(0, Math.min(sel[3], txt.length())));
                } else {
                    hiL = 0; hiR = contentRight;
                }
                if (hiR > hiL) g.fill(hiL, y, hiR, y + lh, 0x664488FF);
            }
            cumY = nextY;
        }
        if (cardTopY >= 0)
            RenderHelper.renderOutline(g, 1, cardTopY, contentRight - 2, cumY - cardTopY, t.accentTint(0x44));
        g.disableScissor();

        renderScrollbar(g, totalHpx, scrollPx, listH, t);
    }

    private void renderScrollbar(GuiGraphics g, int totalHpx, int scrollPx, int trackH, UITheme t) {
        if (totalHpx <= trackH) return;
        int trackX = this.width - SB_W - SB_PAD;
        int trackY = CONTENT_Y;
        g.fill(trackX, trackY, trackX + SB_W, trackY + trackH, 0x33FFFFFF);
        int maxOff = totalHpx - trackH;
        int thumbH = Math.max(16, trackH * trackH / totalHpx);
        int thumbY = trackY + (maxOff > 0 ? (trackH - thumbH) * scrollPx / maxOff : 0);
        int thumbColor = sbDragging ? 0xCCDDDDDD : (0x99000000 | (t.muted() & 0xFFFFFF));
        g.fill(trackX, thumbY, trackX + SB_W, thumbY + thumbH, thumbColor);
    }

    private int clampScroll(List<HelpLine> lines) {
        int listH = this.height - CONTENT_Y - 4;
        int max   = Math.max(0, totalH(lines) - listH);
        int cur   = scrollPos.get(activeTab);
        if (cur > max) { scrollPos.put(activeTab, max); return max; }
        if (cur < 0)   { scrollPos.put(activeTab, 0);   return 0; }
        return cur;
    }

    private void renderIcon(GuiGraphics g, String texture, int x, int y) {
        Item item = RegistryHelper.lookupItem(texture).orElse(Items.AIR);
        if (item == Items.AIR) return;
        PoseHelper.push(g);
        PoseHelper.translate(g, x, y);
        PoseHelper.scale(g, 1.0f, 1.0f);
        g.renderItem(new ItemStack(item), 0, 0);
        PoseHelper.pop(g);
    }

    private void renderSyntaxLine(GuiGraphics g, String text, int x, int y) {
        SyntaxTheme st = NeoHouseConfig.get().syntaxTheme;
        List<SyntaxHighlighter.Span> spList = highlighter.buildSpans(text);
        int curX = x;
        int drawn = 0;
        for (SyntaxHighlighter.Span sp : spList) {
            int lo = Math.max(drawn, sp.start());
            int hi = Math.min(text.length(), sp.end());
            if (lo >= hi) continue;
            if (lo > drawn) {
                String gap = text.substring(drawn, lo);
                g.drawString(font, gap, curX, y, st.plain, false);
                curX += font.width(gap);
                drawn = lo;
            }
            String seg = text.substring(drawn, hi);
            int col = sp.color() == 0 ? st.plain : sp.color();
            g.drawString(font, seg, curX, y, col, false);
            curX += font.width(seg);
            drawn = hi;
        }
        if (drawn < text.length()) {
            g.drawString(font, text.substring(drawn), curX, y, st.plain, false);
        }
    }

    private int lineAtMouseY(List<HelpLine> lines, int scrollPx, double mouseY) {
        if (mouseY < CONTENT_Y || mouseY >= this.height - 4) return -1;
        int cumY = CONTENT_Y - scrollPx;
        for (int i = 0; i < lines.size(); i++) {
            int lh = lineH(lines.get(i));
            if (mouseY >= cumY && mouseY < cumY + lh) return i;
            cumY += lh;
        }
        return -1;
    }

    private int charAtX(String text, int textX, int clickX) {
        int relX = Math.max(0, clickX - textX);
        for (int i = 1; i <= text.length(); i++) {
            int wPrev = font.width(text.substring(0, i - 1));
            int wCur  = font.width(text.substring(0, i));
            if (relX < (wPrev + wCur) / 2) return i - 1;
        }
        return text.length();
    }

    private static int textXForLine(HelpLine line) {
        if (line.color() == COL_H) return PAD + 5;
        return (line.icon() != null && !line.text().isEmpty()) ? PAD + ICON_PAD : PAD;
    }

    private int[] normalizedSel() {
        if (selAnchorLine < selHeadLine ||
                (selAnchorLine == selHeadLine && selAnchorChar <= selHeadChar))
            return new int[]{selAnchorLine, selAnchorChar, selHeadLine, selHeadChar};
        return new int[]{selHeadLine, selHeadChar, selAnchorLine, selAnchorChar};
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<HelpLine> lines = TAB_LINES.get(activeTab);
            int scrollPx    = scrollPos.get(activeTab);
            int listH       = this.height - CONTENT_Y - 4;
            int totalHpx    = totalH(lines);
            int contentRight = this.width - SB_W - SB_PAD - 2;
            if (mouseX >= 0 && mouseX < contentRight && mouseY >= CONTENT_Y && mouseY < this.height - 4) {
                int idx = lineAtMouseY(lines, scrollPx, mouseY);
                if (idx >= 0) {
                    int ch = charAtX(lines.get(idx).text(), textXForLine(lines.get(idx)), (int) mouseX);
                    selAnchorLine = selHeadLine = idx;
                    selAnchorChar = selHeadChar = ch;
                    selTab = activeTab;
                    selecting = true;
                    return true;
                }
            }
            if (totalHpx > listH) {
                int trackX = this.width - SB_W - SB_PAD;
                int trackY = CONTENT_Y;
                if (mouseX >= trackX - 2 && mouseX <= trackX + SB_W + 2) {
                    int maxOff = totalHpx - listH;
                    int thumbH = Math.max(16, listH * listH / totalHpx);
                    int thumbY = trackY + (maxOff > 0 ? (listH - thumbH) * scrollPx / maxOff : 0);
                    if (mouseY >= thumbY && mouseY < thumbY + thumbH) {
                        sbDragging = true;
                        sbDragStartY = (int) mouseY;
                        sbDragStartScroll = scrollPx;
                    } else if (mouseY >= trackY && mouseY < trackY + listH) {
                        int newScroll = (int) ((mouseY - trackY - thumbH / 2.0) * maxOff / (listH - thumbH));
                        scrollPos.put(activeTab, Math.max(0, Math.min(maxOff, newScroll)));
                    }
                    return true;
                }
            }
        }
        int tabY = HEADER_H + 1;
        if (mouseY >= tabY && mouseY < tabY + TAB_BTN_H) {
            for (TabButton tb : tabButtons) {
                if (mouseX >= tb.x() && mouseX < tb.x() + tb.w()) {
                    activeTab = tb.tab();
                    selAnchorLine = selHeadLine = -1;
                    selTab = null;
                    return true;
                }
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean onMouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (selecting && button == 0) {
            List<HelpLine> lines = TAB_LINES.get(activeTab);
            int scrollPx = scrollPos.get(activeTab);
            int lineIdx  = lineAtMouseY(lines, scrollPx, mouseY);
            if (lineIdx < 0) lineIdx = mouseY < CONTENT_Y ? 0 : lines.size() - 1;
            lineIdx = Math.max(0, Math.min(lines.size() - 1, lineIdx));
            selHeadLine = lineIdx;
            selHeadChar = charAtX(lines.get(lineIdx).text(), textXForLine(lines.get(lineIdx)), (int) mouseX);
            return true;
        }
        if (sbDragging && button == 0) {
            List<HelpLine> lines = TAB_LINES.get(activeTab);
            int listH    = this.height - CONTENT_Y - 4;
            int totalHpx = totalH(lines);
            int thumbH   = Math.max(16, listH * listH / totalHpx);
            int maxOff   = totalHpx - listH;
            int slideH   = listH - thumbH;
            if (slideH > 0) {
                int delta = (int) (mouseY - sbDragStartY);
                scrollPos.put(activeTab, Math.max(0, Math.min(maxOff, sbDragStartScroll + delta * maxOff / slideH)));
            }
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    protected boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (selecting && button == 0) selecting = false;
        if (sbDragging && button == 0) { sbDragging = false; return true; }
        return superMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollPos.put(activeTab, scrollPos.get(activeTab) - (int)(scrollY * LINE_H * 3));
        return true;
    }

    @Override
    protected boolean onKeyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_C && (mods & GLFW.GLFW_MOD_CONTROL) != 0) {
            if (selTab == activeTab && selAnchorLine >= 0 && selHeadLine >= 0) {
                int[] sel = normalizedSel();
                if (sel[0] != sel[2] || sel[1] != sel[3]) {
                    List<HelpLine> all = TAB_LINES.get(activeTab);
                    StringBuilder sb = new StringBuilder();
                    for (int i = sel[0]; i <= sel[2]; i++) {
                        if (i > sel[0]) sb.append('\n');
                        String txt = all.get(i).text();
                        int s = (i == sel[0]) ? Math.min(sel[1], txt.length()) : 0;
                        int e = (i == sel[2]) ? Math.min(sel[3], txt.length()) : txt.length();
                        sb.append(txt, s, e);
                    }
                    minecraft.keyboardHandler.setClipboard(sb.toString());
                    return true;
                }
            }
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && selAnchorLine >= 0) {
            selAnchorLine = selHeadLine = -1;
            selTab = null;
            return true;
        }
        int cur   = scrollPos.get(activeTab);
        List<HelpLine> lines = TAB_LINES.get(activeTab);
        int listH = this.height - CONTENT_Y - 4;
        switch (key) {
            case GLFW.GLFW_KEY_UP        -> { scrollPos.put(activeTab, cur - LINE_H);            return true; }
            case GLFW.GLFW_KEY_DOWN      -> { scrollPos.put(activeTab, cur + LINE_H);            return true; }
            case GLFW.GLFW_KEY_PAGE_UP   -> { scrollPos.put(activeTab, cur - listH);             return true; }
            case GLFW.GLFW_KEY_PAGE_DOWN -> { scrollPos.put(activeTab, cur + listH);             return true; }
            case GLFW.GLFW_KEY_HOME      -> { scrollPos.put(activeTab, 0);                       return true; }
            case GLFW.GLFW_KEY_END       -> { scrollPos.put(activeTab, totalH(lines));           return true; }
        }
        return superKeyPressed(key, scan, mods);
    }


    @Override
    public boolean isPauseScreen() { return false; }
}
