package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.NeoHouseConfig;
import com.drakou111.neohouse.UIOpacity;
import com.drakou111.neohouse.UITheme;
import com.drakou111.neohouse.compiler.enums.ActionParamKind;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.AddCondition;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Placeholder;
import com.drakou111.neohouse.compat.PoseHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import com.drakou111.neohouse.compat.ScreenBase;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.drakou111.neohouse.compat.RegistryHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class HelpScreen extends ScreenBase {

    private static final int PAD       = 8;
    private static final int HEADER_H  = 28;
    private static final int TAB_BTN_H = 20;
    private static final int CONTENT_Y = HEADER_H + TAB_BTN_H + 2;
    private static final int LINE_H    = 13;
    private static final int ICON_SIZE = 12;
    private static final int ICON_PAD  = ICON_SIZE + 3;
    private static final int SB_W      = 4;
    private static final int SB_PAD    = 3;

    private static final int COL_H = 0xFFFFCC55; // header
    private static final int COL_C = 0xFF88CCFF; // signature / code
    private static final int COL_T = 0xFFCCCCCC; // normal text
    private static final int COL_D = 0xFF888888; // dim

    private enum Tab {
        OVERVIEW("Overview"),
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
        m.put(Tab.OVERVIEW,     buildOverview());
        m.put(Tab.EXAMPLES,     buildExamples());
        m.put(Tab.ACTIONS,      buildActions());
        m.put(Tab.CONDITIONS,   buildConditions());
        m.put(Tab.EVENTS,       buildEvents());
        m.put(Tab.PLACEHOLDERS, buildPlaceholders());
        TAB_LINES = Collections.unmodifiableMap(m);
    }

    private static List<HelpLine> buildOverview() {
        List<HelpLine> l = new ArrayList<>();

        l.add(new HelpLine(COL_H, "Overview"));
        l.add(new HelpLine(COL_T, "NeoHouse compiles .nh scripts into Hypixel Housing actions."));
        l.add(new HelpLine(COL_T, "Open scripts via the file browser or run them with /neohouse run."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Commands"));
        l.add(new HelpLine(COL_C, "  /neohouse                  — open home screen"));
        l.add(new HelpLine(COL_C, "  /neohouse help             — open this help screen"));
        l.add(new HelpLine(COL_C, "  /neohouse files            — open file browser"));
        l.add(new HelpLine(COL_C, "  /neohouse run <file>       — compile & run a script"));
        l.add(new HelpLine(COL_C, "  /neohouse run latest       — re-run last script"));
        l.add(new HelpLine(COL_C, "  /neohouse config           — open config screen"));
        l.add(new HelpLine(COL_C, "  /neohouse delete functions — delete all functions"));
        l.add(new HelpLine(COL_C, "  /neohouse delete events    — delete all event actions"));
        l.add(new HelpLine(COL_C, "  /neohouse delete commands  — delete all commands"));
        l.add(new HelpLine(COL_C, "  /neohouse delete menus     — delete all custom menus"));
        l.add(new HelpLine(COL_C, "  /neohouse nuke             — delete everything"));
        l.add(new HelpLine(COL_D, ""));

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
        l.add(new HelpLine(COL_C, "    string g.word = \"N/A\";"));
        l.add(new HelpLine(COL_C, "    long g.count;"));
        l.add(new HelpLine(COL_D, "  Only g. and t[]. variables can have initializers."));
        l.add(new HelpLine(COL_D, "  Undeclared variables are treated as global var."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Placeholders"));
        l.add(new HelpLine(COL_T, "  Read-only values from the housing or player:"));
        l.add(new HelpLine(COL_C, "    ph.placeholderName"));
        l.add(new HelpLine(COL_C, "    ph.placeholderName(arg1, arg2)"));
        l.add(new HelpLine(COL_T, "  See the Placeholders tab for the full list."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "String Concatenation"));
        l.add(new HelpLine(COL_T, "  Use + to join strings, variables, and placeholders:"));
        l.add(new HelpLine(COL_C, "    message(\"Score: \" + g.score + \"!\");"));
        l.add(new HelpLine(COL_T, "  Compiled into Housing placeholder format at build time."));
        l.add(new HelpLine(COL_T, "  Result must be at most 256 characters."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Casting"));
        l.add(new HelpLine(COL_C, "  (long)x     — truncate to whole number"));
        l.add(new HelpLine(COL_C, "  (double)x   — convert to decimal"));
        l.add(new HelpLine(COL_C, "  (string)x   — convert to string"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Operators"));
        l.add(new HelpLine(COL_C, "  Arithmetic:   + - * /"));
        l.add(new HelpLine(COL_C, "  Bitwise:      & | ^ << >> >>>"));
        l.add(new HelpLine(COL_C, "  Unary:        a++  a--  -a  !a"));
        l.add(new HelpLine(COL_C, "  Comparison:   == != < <= > >="));
        l.add(new HelpLine(COL_C, "  Logical:      && ||"));
        l.add(new HelpLine(COL_C, "  Assignment:   = += -= *= /= &= |= ^= <<= >>= >>>="));
        l.add(new HelpLine(COL_D, "  Constants:    PI  E  HALF_PI  TWO_PI  PHI  SQRT_TWO"));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Functions"));
        l.add(new HelpLine(COL_T, "  Void function (no return value):"));
        l.add(new HelpLine(COL_C, "    function greet() { message(\"Hi!\"); }"));
        l.add(new HelpLine(COL_T, "  Typed function (returns a value):"));
        l.add(new HelpLine(COL_C, "    function long add(long a, long b) {"));
        l.add(new HelpLine(COL_C, "      return a + b;"));
        l.add(new HelpLine(COL_C, "    }"));
        l.add(new HelpLine(COL_C, "    long result = add(1, 2);"));
        l.add(new HelpLine(COL_T, "  Parameters are passed by value — changes inside"));
        l.add(new HelpLine(COL_T, "  the function do not affect the original variable."));
        l.add(new HelpLine(COL_D, "  Function names cannot clash with built-in actions."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Loops"));
        l.add(new HelpLine(COL_T, "  Repeats on a fixed tick interval (minimum 4):"));
        l.add(new HelpLine(COL_C, "    loop myLoop(10) { ... }"));
        l.add(new HelpLine(COL_D, "  Cannot be typed or have parameters."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Conditionals"));
        l.add(new HelpLine(COL_C, "    if (p.score >= 100 && isSneaking(false)) {"));
        l.add(new HelpLine(COL_C, "      message(\"Done!\");"));
        l.add(new HelpLine(COL_C, "    } else { ... }"));
        l.add(new HelpLine(COL_D, "  Use && (AND) or || (OR) — not both in one expression."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Random"));
        l.add(new HelpLine(COL_T, "  Executes one block at random:"));
        l.add(new HelpLine(COL_C, "    random [ { kill(); }  { heal(); } ]"));
        l.add(new HelpLine(COL_T, "  Use N* to weight an option:"));
        l.add(new HelpLine(COL_C, "    random [ 3*{ message(\"Common\"); }  { kill(); } ]"));
        l.add(new HelpLine(COL_D, "  Weights are reduced by their GCD before compiling."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Repeat"));
        l.add(new HelpLine(COL_T, "  Unrolls its body N times at compile time:"));
        l.add(new HelpLine(COL_C, "    repeat(3) { message(\"tick\"); }"));
        l.add(new HelpLine(COL_D, "  Not a runtime loop — the actions are literally copied."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Events"));
        l.add(new HelpLine(COL_T, "  Triggered automatically by in-game events:"));
        l.add(new HelpLine(COL_C, "    event onJoin { message(\"Welcome!\"); }"));
        l.add(new HelpLine(COL_T, "  See the Events tab for the full list."));
        l.add(new HelpLine(COL_D, "  Some events have restricted action counts."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Commands"));
        l.add(new HelpLine(COL_C, "    command myCmd(priority: 5, listed: false) {"));
        l.add(new HelpLine(COL_C, "      kill();"));
        l.add(new HelpLine(COL_C, "    }"));
        l.add(new HelpLine(COL_D, "  Options: priority (0-20), listed, targeted."));
        l.add(new HelpLine(COL_D, "  Name must be at least 3 characters."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Menus"));
        l.add(new HelpLine(COL_C, "    menu myMenu(3) {"));
        l.add(new HelpLine(COL_C, "      slot0: ({count:1,id:\"minecraft:diamond\"}) {"));
        l.add(new HelpLine(COL_C, "        message(\"Clicked!\");"));
        l.add(new HelpLine(COL_C, "      }"));
        l.add(new HelpLine(COL_C, "    }"));
        l.add(new HelpLine(COL_D, "  The number is the row count (1-6). Slots are 0-indexed."));
        l.add(new HelpLine(COL_D, ""));

        l.add(new HelpLine(COL_H, "Import"));
        l.add(new HelpLine(COL_T, "  Include another .nh file inline:"));
        l.add(new HelpLine(COL_C, "    import \"path/to/other.nh\";"));
        l.add(new HelpLine(COL_D, ""));

        return l;
    }

    private static List<HelpLine> buildExamples() {
        List<HelpLine> l = new ArrayList<>();

        // ── 1. Hello World ────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "1. Hello World"));
        l.add(new HelpLine(COL_T, "  Send a message to the player when they join."));
        l.add(new HelpLine(COL_C, "  event onJoin {"));
        l.add(new HelpLine(COL_C, "    message(\"Welcome to my house!\");"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_D, "  The event block runs automatically on the trigger."));
        l.add(new HelpLine(COL_D, ""));

        // ── 2. Variables ──────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "2. Variables"));
        l.add(new HelpLine(COL_T, "  Count how many times each player has joined."));
        l.add(new HelpLine(COL_C, "  long p.joins;"));
        l.add(new HelpLine(COL_C, ""));
        l.add(new HelpLine(COL_C, "  event onJoin {"));
        l.add(new HelpLine(COL_C, "    p.joins += 1;"));
        l.add(new HelpLine(COL_C, "    message(\"You have joined \" + p.joins + \" time(s)!\");"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_D, "  p. variables are per-player. g. variables are shared."));
        l.add(new HelpLine(COL_D, "  Declare without a holder at the top of the file."));
        l.add(new HelpLine(COL_D, ""));

        // ── 3. Global Counter ─────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "3. Global Counter"));
        l.add(new HelpLine(COL_T, "  Track total joins across all players."));
        l.add(new HelpLine(COL_C, "  long g.totalJoins = 0;"));
        l.add(new HelpLine(COL_C, ""));
        l.add(new HelpLine(COL_C, "  event onJoin {"));
        l.add(new HelpLine(COL_C, "    g.totalJoins += 1;"));
        l.add(new HelpLine(COL_C, "    message(\"Total joins: \" + g.totalJoins);"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_D, "  g. variables persist until the housing resets."));
        l.add(new HelpLine(COL_D, ""));

        // ── 4. Conditionals ───────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "4. Conditionals"));
        l.add(new HelpLine(COL_T, "  Reward players who reach a score threshold."));
        l.add(new HelpLine(COL_C, "  long p.score;"));
        l.add(new HelpLine(COL_C, ""));
        l.add(new HelpLine(COL_C, "  function checkScore() {"));
        l.add(new HelpLine(COL_C, "    if (p.score >= 100) {"));
        l.add(new HelpLine(COL_C, "      message(\"You reached 100 points!\");"));
        l.add(new HelpLine(COL_C, "      heal();"));
        l.add(new HelpLine(COL_C, "    } else {"));
        l.add(new HelpLine(COL_C, "      message(\"Keep going: \" + p.score + \" / 100\");"));
        l.add(new HelpLine(COL_C, "    }"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_D, "  Use && (AND) or || (OR) in conditions, not both at once."));
        l.add(new HelpLine(COL_D, ""));

        // ── 5. Functions ──────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "5. Functions"));
        l.add(new HelpLine(COL_T, "  Reusable logic — void and typed."));
        l.add(new HelpLine(COL_C, "  function greet() {"));
        l.add(new HelpLine(COL_C, "    message(\"Hello, player!\");"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_C, ""));
        l.add(new HelpLine(COL_C, "  function long double(long n) {"));
        l.add(new HelpLine(COL_C, "    return n * 2;"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_C, ""));
        l.add(new HelpLine(COL_C, "  event onJoin {"));
        l.add(new HelpLine(COL_C, "    greet();"));
        l.add(new HelpLine(COL_C, "    long result = double(5);"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_D, "  Parameters are copied — changes inside don't affect callers."));
        l.add(new HelpLine(COL_D, ""));

        // ── 6. Custom Command ─────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "6. Custom Command"));
        l.add(new HelpLine(COL_T, "  Let players type /heal to restore themselves."));
        l.add(new HelpLine(COL_C, "  command heal(listed: true, priority: 5) {"));
        l.add(new HelpLine(COL_C, "    heal();"));
        l.add(new HelpLine(COL_C, "    message(\"You have been healed!\");"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_D, "  priority: 0-20. listed: appears in /. targeted: per-player."));
        l.add(new HelpLine(COL_D, "  Name must be at least 3 characters."));
        l.add(new HelpLine(COL_D, ""));

        // ── 7. Loops ──────────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "7. Loops"));
        l.add(new HelpLine(COL_T, "  Run code on a fixed tick interval (minimum 4 ticks)."));
        l.add(new HelpLine(COL_C, "  long g.seconds = 0;"));
        l.add(new HelpLine(COL_C, ""));
        l.add(new HelpLine(COL_C, "  loop clock(20) {"));
        l.add(new HelpLine(COL_C, "    g.seconds += 1;"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_D, "  20 ticks = 1 second. Loops cannot have parameters."));
        l.add(new HelpLine(COL_D, ""));

        // ── 8. Random ─────────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "8. Random"));
        l.add(new HelpLine(COL_T, "  Execute one outcome at random. Weight with N*."));
        l.add(new HelpLine(COL_C, "  event onJoin {"));
        l.add(new HelpLine(COL_C, "    random ["));
        l.add(new HelpLine(COL_C, "      3*{ message(\"Common outcome\"); }"));
        l.add(new HelpLine(COL_C, "         { message(\"Rare outcome A\"); }"));
        l.add(new HelpLine(COL_C, "         { message(\"Rare outcome B\"); }"));
        l.add(new HelpLine(COL_C, "    ]"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_D, "  Weights are reduced by GCD. 3:1:1 above means 60%:20%:20%."));
        l.add(new HelpLine(COL_D, ""));

        // ── 9. Menus ──────────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "9. Menus"));
        l.add(new HelpLine(COL_T, "  A 3-row inventory GUI with clickable slots."));
        l.add(new HelpLine(COL_C, "  menu shop(3) {"));
        l.add(new HelpLine(COL_C, "    slot0: ({count:1,id:\"minecraft:diamond\"}) {"));
        l.add(new HelpLine(COL_C, "      message(\"You clicked the diamond!\");"));
        l.add(new HelpLine(COL_C, "      closeMenu();"));
        l.add(new HelpLine(COL_C, "    }"));
        l.add(new HelpLine(COL_C, "    slot8: ({count:1,id:\"minecraft:barrier\"}) {"));
        l.add(new HelpLine(COL_C, "      closeMenu();"));
        l.add(new HelpLine(COL_C, "    }"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_C, ""));
        l.add(new HelpLine(COL_C, "  event onJoin { displayMenu(\"shop\"); }"));
        l.add(new HelpLine(COL_D, "  Rows: 1-6. Slots are 0-indexed left to right, top to bottom."));
        l.add(new HelpLine(COL_D, ""));

        // ── 10. Repeat ────────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "10. Repeat"));
        l.add(new HelpLine(COL_T, "  Unrolls a block N times at compile time (not runtime)."));
        l.add(new HelpLine(COL_C, "  event onJoin {"));
        l.add(new HelpLine(COL_C, "    repeat(3) {"));
        l.add(new HelpLine(COL_C, "      message(\"Spam!\");"));
        l.add(new HelpLine(COL_C, "    }"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_D, "  Equivalent to writing the same action 3 times manually."));
        l.add(new HelpLine(COL_D, ""));

        // ── 11. String Building ───────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "11. String Building"));
        l.add(new HelpLine(COL_T, "  Join text, variables, and placeholders with +."));
        l.add(new HelpLine(COL_C, "  long p.score;"));
        l.add(new HelpLine(COL_C, ""));
        l.add(new HelpLine(COL_C, "  event onJoin {"));
        l.add(new HelpLine(COL_C, "    message(\"Hi \" + ph.playerName + \"! Score: \" + p.score);"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_D, "  Folded into a single Housing placeholder string at compile time."));
        l.add(new HelpLine(COL_D, "  Result must be at most 256 characters."));
        l.add(new HelpLine(COL_D, ""));

        // ── 12. Events & Cancel ───────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "12. Events & Cancel"));
        l.add(new HelpLine(COL_T, "  Cancel an event to prevent its default behavior."));
        l.add(new HelpLine(COL_C, "  event onDeath {"));
        l.add(new HelpLine(COL_C, "    message(\"You died!\");"));
        l.add(new HelpLine(COL_C, "    cancel;"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_D, "  cancel; is a statement, not a function call."));
        l.add(new HelpLine(COL_D, ""));

        // ── 13. Import ────────────────────────────────────────────────────────
        l.add(new HelpLine(COL_H, "13. Import"));
        l.add(new HelpLine(COL_T, "  Split large scripts across multiple .nh files."));
        l.add(new HelpLine(COL_C, "  // main.nh"));
        l.add(new HelpLine(COL_C, "  import \"lib/utils.nh\";"));
        l.add(new HelpLine(COL_C, ""));
        l.add(new HelpLine(COL_C, "  event onJoin {"));
        l.add(new HelpLine(COL_C, "    greet();  // defined in lib/utils.nh"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_D, "  The imported file's contents are inlined before compiling."));
        l.add(new HelpLine(COL_D, ""));

        // ── 14. Putting it all together ───────────────────────────────────────
        l.add(new HelpLine(COL_H, "14. Full Example"));
        l.add(new HelpLine(COL_T, "  A score system with a command and win condition."));
        l.add(new HelpLine(COL_C, "  long p.score;"));
        l.add(new HelpLine(COL_C, "  long g.highScore = 0;"));
        l.add(new HelpLine(COL_C, ""));
        l.add(new HelpLine(COL_C, "  function addScore(long n) {"));
        l.add(new HelpLine(COL_C, "    p.score += n;"));
        l.add(new HelpLine(COL_C, "    if (p.score > g.highScore) {"));
        l.add(new HelpLine(COL_C, "      g.highScore = p.score;"));
        l.add(new HelpLine(COL_C, "    }"));
        l.add(new HelpLine(COL_C, "    if (p.score >= 1000) {"));
        l.add(new HelpLine(COL_C, "      message(\"You won with score \" + p.score + \"!\");"));
        l.add(new HelpLine(COL_C, "      p.score = 0;"));
        l.add(new HelpLine(COL_C, "    }"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_C, ""));
        l.add(new HelpLine(COL_C, "  command score(listed: true) {"));
        l.add(new HelpLine(COL_C, "    message(\"Score: \" + p.score + \" | Best: \" + g.highScore);"));
        l.add(new HelpLine(COL_C, "  }"));
        l.add(new HelpLine(COL_C, ""));
        l.add(new HelpLine(COL_C, "  event onKill { addScore(10); }"));
        l.add(new HelpLine(COL_C, "  event onDeath { addScore(-5); }"));
        l.add(new HelpLine(COL_D, ""));

        return l;
    }

    private static String kindStr(ActionParamKind k) {
        return switch (k) {
            case STRING_LITERAL    -> "string";
            case BOOL_LITERAL      -> "bool";
            case LONG_LITERAL      -> "int";
            case DOUBLE_LITERAL    -> "float";
            case NORMAL_EXPRESSION -> "expr";
            case ITEM_LITERAL      -> "item";
        };
    }

    private static String typeStr(VariableType t) {
        return switch (t) {
            case STRING -> "string";
            case LONG   -> "int";
            case DOUBLE -> "float";
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
            l.add(new HelpLine(COL_C, sig.toString(), a.texture));
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
            l.add(new HelpLine(COL_C, sig.toString(), c.texture));
            if (!c.description.isEmpty())
                l.add(new HelpLine(COL_D, "    " + c.description));
            l.add(new HelpLine(COL_D, ""));
        }
        return l;
    }

    private static List<HelpLine> buildEvents() {
        List<HelpLine> l = new ArrayList<>();
        for (Event e : Event.values()) {
            l.add(new HelpLine(COL_C, e.name, e.texture));
            if (!e.description.isEmpty())
                l.add(new HelpLine(COL_D, "    " + e.description));
            l.add(new HelpLine(COL_D, ""));
        }
        return l;
    }

    private static List<HelpLine> buildPlaceholders() {
        List<HelpLine> l = new ArrayList<>();
        for (Placeholder p : Placeholder.values()) {
            StringBuilder sig = new StringBuilder(p.name);
            if (!p.constTypes.isEmpty()) {
                sig.append("(");
                for (int i = 0; i < p.constTypes.size(); i++) {
                    if (i > 0) sig.append(", ");
                    sig.append(typeStr(p.constTypes.get(i)));
                }
                sig.append(")");
            }
            sig.append(" : ").append(typeStr(p.type));
            l.add(new HelpLine(COL_C, sig.toString()));
            if (!p.description.isEmpty())
                l.add(new HelpLine(COL_D, "    " + p.description));
            l.add(new HelpLine(COL_D, ""));
        }
        return l;
    }

    private Tab activeTab = Tab.OVERVIEW;
    private final Map<Tab, Integer> scrollPos = new EnumMap<>(Tab.class);
    private List<TabButton> tabButtons = Collections.emptyList();

    private boolean sbDragging = false;
    private int sbDragStartY;
    private int sbDragStartScroll;

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
        UITheme t   = UITheme.byId(NeoHouseConfig.get().uiTheme);
        UIOpacity o = UIOpacity.byId(NeoHouseConfig.get().uiOpacity);
        g.fillGradient(0, 0, this.width, this.height, t.bgTop(o.bgAlpha), t.bgBottom(o.bgAlpha));
        g.fill(0, 0, this.width, HEADER_H, t.panel(o.panelAlpha));
        g.drawString(this.font, "NeoHouse — Help", PAD + 56, 9, 0xFFFFFFFF);
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
        int scroll  = clampScroll(lines);
        int listH   = this.height - CONTENT_Y - 4;
        int maxRows = listH / LINE_H;
        int end     = Math.min(scroll + maxRows, lines.size());

        int contentRight = this.width - SB_W - SB_PAD - 2;
        g.enableScissor(0, CONTENT_Y, contentRight, this.height - 4);
        for (int i = scroll; i < end; i++) {
            HelpLine line = lines.get(i);
            int y     = CONTENT_Y + (i - scroll) * LINE_H;
            int textX = PAD;
            if (line.icon() != null && !line.text().isEmpty()) {
                renderIcon(g, line.icon(), PAD, y);
                textX = PAD + ICON_PAD;
            }
            g.drawString(this.font, line.text(), textX, y, line.color(), false);
        }
        g.disableScissor();

        renderScrollbar(g, lines, scroll, maxRows, t);
    }

    private void renderScrollbar(GuiGraphics g, List<HelpLine> lines, int scroll, int maxRows, UITheme t) {
        int total = lines.size();
        if (total <= maxRows) return;

        int trackX = this.width - SB_W - SB_PAD;
        int trackY = CONTENT_Y;
        int trackH = this.height - CONTENT_Y - 4;
        g.fill(trackX, trackY, trackX + SB_W, trackY + trackH, 0x33FFFFFF);

        int maxOff = total - maxRows;
        int thumbH = Math.max(16, trackH * maxRows / total);
        int thumbY = trackY + (maxOff > 0 ? (trackH - thumbH) * scroll / maxOff : 0);
        int thumbColor = sbDragging ? 0xCCDDDDDD : (0x99000000 | (t.muted() & 0xFFFFFF));
        g.fill(trackX, thumbY, trackX + SB_W, thumbY + thumbH, thumbColor);
    }

    private int clampScroll(List<HelpLine> lines) {
        int listH   = this.height - CONTENT_Y - 4;
        int maxRows = Math.max(1, listH / LINE_H);
        int max     = Math.max(0, lines.size() - maxRows);
        int cur     = scrollPos.get(activeTab);
        if (cur > max) { scrollPos.put(activeTab, max); return max; }
        if (cur < 0)   { scrollPos.put(activeTab, 0);   return 0; }
        return cur;
    }

    private void renderIcon(GuiGraphics g, String texture, int x, int y) {
        Item item = RegistryHelper.lookupItem(texture).orElse(Items.AIR);
        if (item == Items.AIR) return;
        PoseHelper.push(g);
        PoseHelper.translate(g, x, y);
        PoseHelper.scale(g, 0.75f, 0.75f);
        g.renderItem(new ItemStack(item), 0, 0);
        PoseHelper.pop(g);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<HelpLine> lines = TAB_LINES.get(activeTab);
            int scroll  = scrollPos.get(activeTab);
            int listH   = this.height - CONTENT_Y - 4;
            int maxRows = Math.max(1, listH / LINE_H);
            if (lines.size() > maxRows) {
                int trackX = this.width - SB_W - SB_PAD;
                int trackY = CONTENT_Y;
                int trackH = this.height - CONTENT_Y - 4;
                if (mouseX >= trackX - 2 && mouseX <= trackX + SB_W + 2) {
                    int maxOff = lines.size() - maxRows;
                    int thumbH = Math.max(16, trackH * maxRows / lines.size());
                    int thumbY = trackY + (maxOff > 0 ? (trackH - thumbH) * scroll / maxOff : 0);
                    if (mouseY >= thumbY && mouseY < thumbY + thumbH) {
                        sbDragging = true;
                        sbDragStartY = (int) mouseY;
                        sbDragStartScroll = scroll;
                    } else if (mouseY >= trackY && mouseY < trackY + trackH) {
                        int newScroll = (int) ((mouseY - trackY - thumbH / 2.0) * maxOff / (trackH - thumbH));
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
                    return true;
                }
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean onMouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (sbDragging && button == 0) {
            List<HelpLine> lines = TAB_LINES.get(activeTab);
            int listH   = this.height - CONTENT_Y - 4;
            int maxRows = Math.max(1, listH / LINE_H);
            int trackH  = listH;
            int thumbH  = Math.max(16, trackH * maxRows / lines.size());
            int maxOff  = lines.size() - maxRows;
            int slideH  = trackH - thumbH;
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
        if (sbDragging && button == 0) { sbDragging = false; return true; }
        return superMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int cur = scrollPos.get(activeTab);
        if (scrollY > 0 && cur > 0) scrollPos.put(activeTab, cur - 1);
        else if (scrollY < 0)       scrollPos.put(activeTab, cur + 1);
        return true;
    }

    @Override
    protected boolean onKeyPressed(int key, int scan, int mods) {
        int cur   = scrollPos.get(activeTab);
        List<HelpLine> lines = TAB_LINES.get(activeTab);
        switch (key) {
            case GLFW.GLFW_KEY_UP        -> { if (cur > 0) scrollPos.put(activeTab, cur - 1); return true; }
            case GLFW.GLFW_KEY_DOWN      -> { scrollPos.put(activeTab, cur + 1); return true; }
            case GLFW.GLFW_KEY_PAGE_UP   -> { scrollPos.put(activeTab, Math.max(0, cur - 10)); return true; }
            case GLFW.GLFW_KEY_PAGE_DOWN -> { scrollPos.put(activeTab, cur + 10); return true; }
            case GLFW.GLFW_KEY_HOME      -> { scrollPos.put(activeTab, 0); return true; }
            case GLFW.GLFW_KEY_END       -> { scrollPos.put(activeTab, lines.size()); return true; }
        }
        return superKeyPressed(key, scan, mods);
    }


    @Override
    public boolean isPauseScreen() { return false; }
}
