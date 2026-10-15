package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compiler.enums.ActionParamKind;
import com.drakou111.neohouse.compiler.enums.Param;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.AddCondition;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Placeholder;
import com.drakou111.neohouse.constants.RestrictionAction;
import com.drakou111.neohouse.constants.RestrictionCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Everything the help screen shows: written guides and reference pages generated from the
 * actions, conditions, events and placeholders the compiler knows, so they can't go out of date.
 */
final class HelpContent {

    static final String GUIDE = "Guide";
    static final String REFERENCE = "Reference";
    private static final int MAX_LISTED_VALUES = 12;

    /** One block of a topic. */
    sealed interface Block permits Heading, Text, Code, Preformatted, Entry {
        /** The text a search looks in. */
        String searchText();
    }

    record Heading(String text) implements Block {
        public String searchText() { return this.text; }
    }

    /** A paragraph, wrapped to the page width. */
    record Text(String text) implements Block {
        public String searchText() { return this.text; }
    }

    /** Script code shown as is, with a Copy button. */
    record Code(String code) implements Block {
        public String searchText() { return this.code; }
    }

    /** Text laid out as is (a table of shortcuts or commands); not a script. */
    record Preformatted(String text) implements Block {
        public String searchText() { return this.text; }
    }

    /** A reference entry: signature, icon (an item id or null), description, details and an example to copy. */
    record Entry(String signature, String icon, String description, List<String> details, String example) implements Block {
        public String searchText() { return this.signature + "\n" + this.description + "\n" + String.join("\n", this.details); }
    }

    record Topic(String section, String title, List<Block> blocks) {}

    private HelpContent() {
    }

    static List<Topic> topics() {
        List<Topic> topics = new ArrayList<>();
        topics.add(gettingStarted());
        topics.add(variables());
        topics.add(numbers());
        topics.add(text());
        topics.add(conditions());
        topics.add(functions());
        topics.add(entryPoints());
        topics.add(randomAndRepeat());
        topics.add(constantsAndEnums());
        topics.add(imports());
        topics.add(running());
        topics.add(settings());
        topics.add(editor());
        topics.add(filesAndMusic());
        topics.add(chatCommands());
        topics.add(compilerVariables());
        topics.add(examples());
        topics.add(actions());
        topics.add(conditionReference());
        topics.add(events());
        topics.add(placeholders());
        return topics;
    }

    /**
     * The blocks of {@code topic} matching every word of {@code query} (case-insensitive), with the
     * heading each match falls under so it keeps its context. Empty when nothing matches.
     */
    static List<Block> search(Topic topic, String query) {
        String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        boolean titleMatches = matchesAll(topic.title(), words);
        List<Block> result = new ArrayList<>();
        Heading lastHeading = null;
        boolean headingAdded = false;
        for (Block block : topic.blocks()) {
            if (block instanceof Heading h) {
                lastHeading = h;
                headingAdded = false;
                if (titleMatches || matchesAll(h.text(), words)) {
                    result.add(h);
                    headingAdded = true;
                }
                continue;
            }
            boolean headingMatches = headingAdded;
            if (!titleMatches && !headingMatches && !matchesAll(block.searchText(), words)) continue;
            if (lastHeading != null && !headingAdded) {
                result.add(lastHeading);
                headingAdded = true;
            }
            result.add(block);
        }
        return result;
    }

    private static boolean matchesAll(String text, String[] words) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String word : words) if (!word.isEmpty() && !lower.contains(word)) return false;
        return true;
    }

    // ---- guides ----

    private static Topic gettingStarted() {
        return new Topic(GUIDE, "Getting started", List.of(
                new Text("NeoHouse turns .nh scripts into Hypixel Housing functions, events, commands, loops and menus, "
                        + "and clicks through Housing's menus to build them for you."),
                new Heading("Your first script"),
                new Text("Open the file browser with /nh files (or Files on the /nh home screen), click + Script, "
                        + "write your code and save with Ctrl+S. Run it from the file browser or with /nh run <file>."),
                new Code("""
                        event onJoin {
                            message("Welcome to my house!");
                        }"""),
                new Heading("Before running"),
                new Text("Stand in your house in build mode with creative abilities. Turn on chat inputs in your Housing "
                        + "settings (/settings, Personal Housing Settings, Text Input Type: Chat): NeoHouse answers every text "
                        + "field in chat. Don't touch menus while a script runs; the box in the corner shows its progress."),
                new Heading("Where to go next"),
                new Text("The Guide topics on the left explain the language step by step. Examples has complete small "
                        + "scripts, and the Reference topics list every action, condition, event and placeholder. "
                        + "Type in the search box to search all of them at once.")));
    }

    private static Topic variables() {
        return new Topic(GUIDE, "Variables and types", List.of(
                new Heading("Holders"),
                new Text("Every variable belongs to a holder, written before its name:"),
                new Preformatted("""
                        p.name          the player's own
                        g.name          shared by the whole house
                        t[team].name    belongs to a team"""),
                new Code("""
                        p.coins += 5;
                        g.round = 2;
                        t[red].score += 1;"""),
                new Heading("Types"),
                new Preformatted("""
                        long      whole numbers          42, -7, 1_000
                        double    decimals               1.5, 2d
                        string    text                   "hello"
                        bool      true or false          true
                        var       anything, unchecked"""),
                new Heading("Declaring"),
                new Text("Declare variables at the top of a file, with their type. Only g. and t[] variables can have "
                        + "a starting value."),
                new Code("""
                        long p.coins;
                        double p.speed;
                        string g.motd = "Have fun";
                        bool p.vip;"""),
                new Text("A variable used without a declaration becomes a var. To catch typos like p.cions, set "
                        + "Undeclared variables to Warn or Error, or turn on Ban the var type."),
                new Heading("Unset variables and fallbacks"),
                new Text("A variable that was never given a value is unset. Any condition that uses an unset "
                        + "variable is false, whatever it is compared to (even p.test == \"\")."),
                new Text("Write ?value right after a variable to choose what to use while it is unset. The value "
                        + "must be a constant of the variable's type."),
                new Code("""
                        p.apple = p.test?0 + 2;    // 2 while p.test is unset
                        if (p.lives?3 > 0) {       // an unset p.lives counts as 3
                            message("still alive");
                        }"""),
                new Text("The ?value only belongs to the variable just before it: p.test?0 + 2 means (p.test?0) + 2."),
                new Text("The Never-set variables setting warns when the script uses a variable's value (in a "
                        + "calculation, a condition, an action's argument, or with += and the like) but never gives it "
                        + "one with =, and there is no ?value."),
                new Heading("Names"),
                new Text("Variable names are at most 16 characters (Housing's limit). '$' is reserved for the variables "
                        + "the compiler makes, so your own names can't contain it.")));
    }

    private static Topic numbers() {
        return new Topic(GUIDE, "Numbers and operators", List.of(
                new Heading("Number literals"),
                new Text("Whole numbers: 42, 1_000, 0xFF00 (hex), 0b1010 (binary). Decimals: 1.5, 1.5d, and 2d or 2D for "
                        + "a whole number used as a double. Built-in constants: PI, E, HALF_PI, TWO_PI, SQRT_TWO, "
                        + "SQRT_THREE, SQRT_FIVE, PHI."),
                new Heading("Operators"),
                new Preformatted("""
                        arithmetic    + - * / %
                        bitwise       & | ^ << >> >>>
                        comparison    == != < <= > >=
                        logical       && ||
                        unary         -x  !x  x++  x--
                        assignment    = += -= *= /= %= &= |= ^= <<= >>= >>>="""),
                new Text("% works on long and double values but not on an untyped var, because the steps it compiles to "
                        + "depend on the type; cast first: (long) p.v % 3. The same goes for -x, x++ and x-- on a var."),
                new Heading("Casting"),
                new Text("Types never mix on their own: a long and a double can't be added, compared or stored in "
                        + "each other. A cast converts a value: write the type in parentheses in front of it."),
                new Preformatted("""
                        (long) x      to a whole number, dropping the decimals: 2.7 becomes 2
                        (double) x    to a decimal: 3 becomes 3.0
                        (string) x    to text: 3 becomes "3\""""),
                new Code("""
                        long p.whole;
                        double p.speed;
                        double p.total;
                        string p.label;

                        event onJoin {
                            p.speed = 2.7;
                            p.whole = (long) p.speed;              // 2
                            p.total = p.speed + (double) p.whole;  // both sides are doubles
                            p.label = (string) p.whole;            // "2"
                            if ((long) p.speed > 1) {
                                message("Speed: " + p.speed);      // no cast needed when joining text
                            }
                        }"""),
                new Text("Text can't be cast to a number. Joining with + already turns numbers into text, so "
                        + "messages don't need (string). On an untyped var, (long) or (double) also tells the compiler "
                        + "which type to use, which %, -x, x++ and x-- need."),
                new Heading("Computed while compiling"),
                new Text("Arithmetic on constants is done by the compiler: p.x = 1.0 / 2.0 becomes p.x = 0.5, and "
                        + "(long) 2.7 becomes 2. Division by a constant zero is left for Housing.")));
    }

    private static Topic text() {
        return new Topic(GUIDE, "Text and placeholders", List.of(
                new Heading("Joining text"),
                new Text("Join text, variables and placeholders with +. The compiler turns it into one Housing "
                        + "placeholder string; a message can be at most 256 characters."),
                new Code("message(\"Hi \" + ph.playerName() + \", you have \" + p.coins + \" coins\");"),
                new Heading("Placeholders"),
                new Text("ph.name reads a value from Housing or the player; some take arguments: ph.name(arg). They "
                        + "can't be written to. See Placeholders in the Reference for the full list."),
                new Code("p.health = ph.playerHealth();")));
    }

    private static Topic conditions() {
        return new Topic(GUIDE, "Conditions", List.of(
                new Code("""
                        if (p.coins >= 100 && isSneaking(false)) {
                            message("Rich and sneaking");
                        } else {
                            message("Not yet");
                        }"""),
                new Text("Combine comparisons with && (all must hold) or || (any may hold), but not both in one "
                        + "condition. != is compiled as == with Housing's Invert option. Conditions such as isSneaking "
                        + "are listed under Conditions in the Reference; their first argument is Invert."),
                new Text("Housing allows at most 20 conditions of one type in a single if; the compiler reports more.")));
    }

    private static Topic functions() {
        return new Topic(GUIDE, "Functions", List.of(
                new Heading("Void and typed functions"),
                new Code("""
                        function greet() {
                            message("Hello!");
                        }

                        function long add(long a, long b) {
                            return a + b;
                        }

                        event onJoin {
                            greet();
                            p.total = add(p.total, 5);
                        }"""),
                new Text("Parameters are copies: changing one inside the function doesn't change the caller's variable. "
                        + "Any function can be called without using its result. Function names are at most 50 "
                        + "characters and can't be the name of an action."),
                new Heading("Returning several values"),
                new Code("""
                        function (double, double) halfAndDouble(double x) {
                            return (x / 2.0, x * 2.0);
                        }

                        event onJoin {
                            (p.half, p.twice) = halfAndDouble(5.0);
                        }"""),
                new Heading("Icon and description"),
                new Text("Optional named settings go before the parameters and set the function's icon and description "
                        + "in Housing."),
                new Code("function shop(icon: {id:\"minecraft:emerald\"}, description: \"Opens the shop\") { }"),
                new Heading("Calling for everyone"),
                new Text("global f(); runs a void function for every player in the house. A function called that way "
                        + "(and a loop) can't itself contain global calls."),
                new Heading("Calling a function twice"),
                new Text("Housing won't run the same function twice in one execution, so calling it twice (directly or "
                        + "through other functions) misbehaves. The Multi-call mode setting decides what happens: Error (the "
                        + "default) stops compiling, Warn only warns, Fix makes copies (f$0, f$1, ...) so each call has its "
                        + "own, and Off doesn't check."),
                new Heading("Recursion"),
                new Text("A function calling itself, directly or through others, isn't supported by Housing. The "
                        + "Recursion setting reports it as a warning (default), an error, or not at all.")));
    }

    private static Topic entryPoints() {
        return new Topic(GUIDE, "Events, commands, loops and menus", List.of(
                new Heading("Events"),
                new Text("An event block runs when the event happens. In events that allow it, Cancel(); stops the "
                        + "event's normal effect. Some events only allow some actions; the Reference says where each "
                        + "action and condition can be used."),
                new Code("""
                        event onDeath {
                            message("You died!");
                            Cancel();
                        }"""),
                new Heading("Commands"),
                new Text("Options: priority (0 to 20), listed (shown in the list of commands) and targeted (run on "
                        + "another player). Names are at least 3 characters."),
                new Code("""
                        command tospawn(listed: true, priority: 5) {
                            teleport(HOUSE_SPAWN, false);
                        }"""),
                new Heading("Loops"),
                new Text("A loop runs every N ticks (20 ticks is one second, the minimum is 4) for every player. "
                        + "Loops take no parameters; icon and description can go before the tick count."),
                new Code("""
                        long g.seconds = 0;

                        loop clock(20) {
                            g.seconds += 1;
                        }"""),
                new Heading("Menus"),
                new Text("A menu has 1 to 6 rows. Slots count from 0, left to right and top to bottom; each has an item "
                        + "and the actions run when it is clicked."),
                new Code("""
                        menu shop(3) {
                            slot13: ({id:"minecraft:diamond",count:1}) {
                                message("You clicked the diamond");
                                closeMenu();
                            }
                        }"""),
                new Text("Housing allows 300 functions, 50 commands and 200 menus per house. A script that needs more "
                        + "doesn't compile, and a run stops before starting when the house would go over.")));
    }

    private static Topic randomAndRepeat() {
        return new Topic(GUIDE, "Random and repeat", List.of(
                new Heading("Random"),
                new Text("Runs one of the blocks at random. N* in front of a block makes it N times as likely; weights "
                        + "are reduced by their common divisor first."),
                new Code("""
                        random [
                            3*{ message("Common"); }
                            { message("Rare"); }
                        ]"""),
                new Heading("Repeat"),
                new Text("repeat(N) copies its block N times while compiling. It isn't a loop at run time."),
                new Code("""
                        repeat(3) {
                            message("Hello");
                        }""")));
    }

    private static Topic constantsAndEnums() {
        return new Topic(GUIDE, "Constants and enums", List.of(
                new Heading("Constants"),
                new Text("Named values replaced by their value while compiling, at no cost in Housing. Types: long, "
                        + "double, string and bool. The value is a literal or another constant."),
                new Code("""
                        const long MAX_SCORE = 100;

                        event onJoin {
                            if (p.score?0 >= MAX_SCORE) { message("Max!"); }
                        }"""),
                new Heading("Enums"),
                new Text("A named type whose members are numbered from 0. They're stored as longs but only compare with "
                        + "the same enum, and can't be used in arithmetic. They work as parameter and return types."),
                new Code("""
                        enum Color { RED, GREEN, BLUE }
                        Color p.favorite;

                        event onJoin {
                            p.favorite = Color.GREEN;
                            if (p.favorite == Color.RED) { message("Red!"); }
                        }""")));
    }

    private static Topic imports() {
        return new Topic(GUIDE, "Imports and the library", List.of(
                new Heading("Importing files"),
                new Text("import pulls another .nh file in, by its path from the folder of the script being run; a path "
                        + "ending in * imports every .nh file of that folder. Each file is included once. A commented-out "
                        + "import is ignored, and errors in an imported file name that file."),
                new Code("""
                        import "lib/utils.nh";
                        import "neohouse/*";"""),
                new Text("A file sees what it imports, and what those import in turn, but not the other imports of the "
                        + "file that imported it."),
                new Heading("Built-in library"),
                new Text("The neohouse folder of your scripts holds ready-made functions. trig.nh: sin, cos, tan, asin, "
                        + "acos, atan, atan2 and getFacingVector. double_math.nh and long_math.nh: abs, min, max, clamp, "
                        + "sign and mod for each type (double_abs, long_max, ...) and sqrt. Files you edit are never "
                        + "replaced by updates.")));
    }

    private static Topic running() {
        return new Topic(GUIDE, "Running a script", List.of(
                new Heading("What a run does"),
                new Text("First it checks that the teams, layouts and groups the script uses exist in the house. Then it "
                        + "reads the lists of functions, commands and menus the script touches, so it knows what already "
                        + "exists and that the house limits aren't exceeded. Then it creates or fills each part."),
                new Heading("Existing parts"),
                new Text("The Override mode setting decides what happens to functions, events, loops, commands and "
                        + "menus that already exist:"),
                new Preformatted("""
                        Override If Empty   fill only the ones still empty (default)
                        Always Override     recreate them from scratch
                        Never Override      leave them as they are
                        Append              add the script's actions after theirs"""),
                new Text("Append first reads every page of the existing actions and skips the part, with a warning, if "
                        + "any action type would go over its Housing limit (for example a 26th Conditional). Menus only "
                        + "get their empty slots filled. Functions the compiler makes (names with $) are replaced "
                        + "instead. Running an Append script twice adds its actions twice."),
                new Heading("Lag"),
                new Text("NeoHouse waits for each menu to finish loading, and waits longer when the server is slow. If a "
                        + "menu or page never arrives, the run stops with a message instead of guessing; run the script "
                        + "again or raise Action timeout in the settings."),
                new Heading("Afterwards"),
                new Text("/nh rerun runs the last script again. /nh list functions and /nh list variables show the helper "
                        + "functions and extra variables the last run created. When a run ends (done, stopped or "
                        + "cancelled) after warnings, click Show warnings in chat, or use /nh list warnings, to see "
                        + "them all again.")));
    }

    private static Topic settings() {
        return new Topic(GUIDE, "Settings", List.of(
                new Text("Open them with /nh config or Settings on the home screen. Hover a setting's name for "
                        + "details. The first option listed is the default."),
                new Heading("Running"),
                new Preformatted("""
                        Override mode         what happens to parts that already exist
                                              (see Running a script)
                        Delay after click     ticks to wait after each click
                        Action timeout        ticks to wait for a menu before giving up"""),
                new Heading("Compiler"),
                new Preformatted("""
                        Multi-call mode         a function called twice in one execution
                                                Error, Warn, Fix, Off
                        Recursion               a function calling itself
                                                Warn, Error, Off
                        Global call rules       global calls inside globally called code
                                                Error, Warn, Off
                        Undeclared variables    a variable used without a declaration
                                                Allow, Warn, Error
                        Never-set variables     using a variable the script never sets
                                                Warn, Error, Off
                        Ban the var type        reject var everywhere, including
                                                undeclared variables"""),
                new Heading("Editor"),
                new Text("Tab width, parameter hints, value suggestions, auto indent, the number of error lines shown, "
                        + "and the syntax colours."),
                new Heading("Look"),
                new Text("Interface theme and opacity, and the mob shown spinning next to the progress box.")));
    }

    private static Topic editor() {
        return new Topic(GUIDE, "Code editor", List.of(
                new Heading("Shortcuts"),
                new Preformatted("""
                        Ctrl+S              save
                        Ctrl+F / Ctrl+R     find / find and replace
                        Ctrl+Z / Ctrl+Y     undo / redo
                        Ctrl+/              comment or uncomment lines
                        Ctrl+D              duplicate lines
                        Alt+Up / Alt+Down   move lines
                        Ctrl+click or F12   go to a function's definition
                        Tab / Shift+Tab     indent / unindent
                        Ctrl+X              cut (the whole line when nothing is selected)"""),
                new Text("Ctrl+/ uses the key where / is on a US keyboard."),
                new Heading("Hints and suggestions"),
                new Text("Grey hints show the name of each argument of a call, including the next one as soon as you type "
                        + "',' or '('. Suggestions offer event names after 'event ', placeholders after 'ph.', locations "
                        + "like INVOKER, and the allowed values of a text argument; Tab picks one."),
                new Heading("Errors and warnings"),
                new Text("Errors (red) and warnings (yellow) are underlined as you type. The bar at the bottom always "
                        + "shows an error; a warning shows there while the cursor is on it.")));
    }

    private static Topic filesAndMusic() {
        return new Topic(GUIDE, "Files and music", List.of(
                new Heading("File browser"),
                new Text("Scripts (.nh), music projects (.nhm) and files to import (.mid, .midi, .nbs) live in one "
                        + "folder. Double-click to open; select a file for its actions. Browse Folder opens the current "
                        + "folder in your system's file manager."),
                new Heading("Music editor"),
                new Text("Left click adds or selects a note and dragging moves it; right click deletes. Scrolling over a "
                        + "note changes its volume, Ctrl+scroll zooms. Click or drag the ruler above the notes to choose "
                        + "where playback starts; Space plays and stops, Home goes back to the start. Export writes the "
                        + "song as a .nh function next to it."),
                new Heading("Importing songs"),
                new Text("MIDI and Note Block Studio files open in an import screen and become .nhm projects.")));
    }

    private static Topic chatCommands() {
        return new Topic(GUIDE, "Chat commands", List.of(new Preformatted("""
                /nh                     home screen
                /nh help                this help
                /nh files               file browser (also /nh music)
                /nh config              settings
                /nh run <file>          compile and run a script
                /nh rerun               run the last script again
                /nh list functions      helper functions of the last run
                /nh list variables      extra variables of the last run
                /nh list warnings       warnings of the last run again
                /nh copyitem            copy the held item's data
                /nh giveitem <data>     get an item from its data
                /nh delete <kind>       delete all functions, events, commands or menus
                /nh nuke                delete everything""")));
    }

    private static Topic compilerVariables() {
        return new Topic(GUIDE, "Compiler variables", List.of(
                new Text("To compute expressions, pass arguments and return values, the compiler makes a few variables "
                        + "of its own; their names contain '$':"),
                new Preformatted("""
                        t$0, t$1 ...         parts of an expression in an event, command, loop or menu
                        f$t0, f$t1 ...       the same inside function f
                        f_p$0, f_p$1 ...     the arguments of function f
                        o$, o$0, o$1 ...     the value(s) a function returns"""),
                new Text("When f$t0 or f_p$0 would be longer than 16 characters, a 6-character code of the function name "
                        + "is used instead ($k3f9a2t0). These names only depend on the function itself, so changing one "
                        + "function never renames anything in another."),
                new Heading("Optimizations"),
                new Text("The compiler saves actions on its own variables only: it merges constant steps, builds results "
                        + "straight into return and argument slots, and compares p.a > 4 instead of computing p.a + 1 "
                        + "into a temporary first. Your variables keep every write you asked for; at most a plain copy "
                        + "p.r = t$0 becomes p.r = p.a.")));
    }

    private static Topic examples() {
        return new Topic(GUIDE, "Examples", List.of(
                new Heading("Join counter"),
                new Code("""
                        long p.joins;
                        long g.totalJoins = 0;

                        event onJoin {
                            p.joins = p.joins?0 + 1;
                            g.totalJoins += 1;
                            message("Visit " + p.joins + " (house total: " + g.totalJoins + ")");
                        }"""),
                new Heading("Score with a reward"),
                new Code("""
                        long p.score;
                        long g.highScore = 0;

                        function addScore(long n) {
                            p.score += n;
                            if (p.score > g.highScore) {
                                g.highScore = p.score;
                            }
                            if (p.score >= 1000) {
                                message("You won with " + p.score + " points!");
                                p.score = 0;
                            }
                        }

                        command score(listed: true) {
                            message("Score: " + p.score + " | Best: " + g.highScore);
                        }

                        event onKill { addScore(10); }"""),
                new Heading("Timer"),
                new Code("""
                        long g.seconds = 0;

                        loop clock(20) {
                            g.seconds += 1;
                        }"""),
                new Heading("Looking direction"),
                new Code("""
                        import "neohouse/trig.nh";

                        double p.x;
                        double p.y;
                        double p.z;

                        command look(listed: true) {
                            (p.x, p.y, p.z) = getFacingVector();
                            message("Facing " + p.x + ", " + p.y + ", " + p.z);
                        }""")));
    }

    // ---- reference ----

    private static Topic actions() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Text("Every action, with its arguments in order. Copy an example and change the values."));
        for (AddAction a : AddAction.values()) {
            if (!a.canBeUsed) continue;
            String call = a.name + "(" + exampleArguments(a.params) + ");";
            blocks.add(entry(a.name, a.params, a.texture, a.description, where(a), inAllowedPlace(a, call)));
        }
        return new Topic(REFERENCE, "Actions", blocks);
    }

    private static Topic conditionReference() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Text("Conditions go inside if (...). The first argument of most is Invert: true checks the opposite."));
        for (AddCondition c : AddCondition.values()) {
            if (!c.canBeUsed) continue;
            String check = "if (" + c.name + "(" + exampleArguments(c.params) + ")) { }";
            blocks.add(entry(c.name, c.params, c.texture, c.description, where(c), inAllowedPlace(c, check)));
        }
        return new Topic(REFERENCE, "Conditions", blocks);
    }

    private static Topic events() {
        List<Block> blocks = new ArrayList<>();
        for (Event e : Event.values())
            blocks.add(new Entry("event " + e.name, e.texture, e.description, List.of(), "event " + e.name + " {\n}"));
        return new Topic(REFERENCE, "Events", blocks);
    }

    private static Topic placeholders() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Text("Read-only values; use them in expressions and text."));
        for (Placeholder p : Placeholder.values()) {
            StringBuilder call = new StringBuilder("ph.").append(p.name).append("(");
            for (int i = 0; i < p.constTypes.size(); i++) {
                if (i > 0) call.append(", ");
                call.append(typeName(p.constTypes.get(i)));
            }
            call.append(")");
            blocks.add(new Entry(call + " : " + typeName(p.type), null, p.description, List.of(), null));
        }
        return new Topic(REFERENCE, "Placeholders", blocks);
    }

    private static Entry entry(String name, List<Param> params, String icon, String description,
                               Optional<String> where, String example) {
        List<String> labels = new ArrayList<>();
        List<String> details = new ArrayList<>();
        where.ifPresent(details::add);
        for (Param p : params) {
            labels.add(p.hintLabel());
            constraint(p).ifPresent(c -> details.add(p.hintLabel().split(":")[0] + ": " + c));
        }
        return new Entry(name + "(" + String.join(", ", labels) + ")", icon, description, details, example);
    }

    private static Optional<String> constraint(Param p) {
        if (p.hasEnumConstraint()) {
            String[] values = p.enumValues();
            List<String> shown = new ArrayList<>();
            for (int i = 0; i < Math.min(values.length, MAX_LISTED_VALUES); i++) shown.add(values[i]);
            String list = String.join(", ", shown);
            if (values.length > MAX_LISTED_VALUES)
                list += ", ... (" + values.length + " in all; the editor suggests them as you type)";
            if (p.allowsCustomValue()) list += ", or any other value";
            return Optional.of("one of " + list);
        }
        if (p.hasLongRangeConstraint()) return Optional.of("from " + p.rangeMin() + " to " + p.rangeMax());
        if (p.hasDoubleRangeConstraint()) return Optional.of("from " + p.decimalMin() + " to " + p.decimalMax());
        if (p.kind() == ActionParamKind.LOCATION)
            return Optional.of("HOUSE_SPAWN, INVOKER, CURRENT, or coordinates as text, as typed in Housing (\"10 64 -3\")");
        if (p.kind() == ActionParamKind.ITEM_LITERAL)
            return Optional.of("item data like {id:\"minecraft:diamond\",count:1} (/nh copyitem copies a held item's)");
        return Optional.empty();
    }

    // ---- where actions and conditions are allowed ----

    /** "Only in ..." or "Not in ..." for an action that isn't allowed everywhere. */
    private static Optional<String> where(AddAction a) {
        List<String> allowed = new ArrayList<>();
        List<String> blocked = new ArrayList<>();
        (RestrictionAction.MENU.isAllowed(a) ? allowed : blocked).add("menus");
        for (Event e : Event.values())
            (RestrictionAction.valueOf("EVENT_" + e.name()).isAllowed(a) ? allowed : blocked).add(e.name);
        if (!RestrictionAction.NONE.isAllowed(a)) return Optional.of("Only in: " + String.join(", ", allowed));
        List<String> notes = new ArrayList<>();
        if (!blocked.isEmpty()) notes.add("Not in: " + String.join(", ", blocked));
        if (!RestrictionAction.CONDITIONAL.isAllowed(a)) notes.add("not inside an if or random");
        return notes.isEmpty() ? Optional.empty() : Optional.of(String.join("; ", notes));
    }

    /** "Only in ..." for a condition limited to some events. */
    private static Optional<String> where(AddCondition c) {
        if (RestrictionCondition.NONE.isAllowed(c)) return Optional.empty();
        List<String> allowed = new ArrayList<>();
        for (Event e : Event.values())
            if (RestrictionCondition.valueOf("EVENT_" + e.name()).isAllowed(c)) allowed.add(e.name);
        return Optional.of("Only in: " + String.join(", ", allowed));
    }

    /** The example placed where the action may be used: as is, or in the first event or menu allowing it. */
    private static String inAllowedPlace(AddAction a, String code) {
        if (RestrictionAction.NONE.isAllowed(a)) return code;
        for (Event e : Event.values())
            if (RestrictionAction.valueOf("EVENT_" + e.name()).isAllowed(a)) return wrap("event " + e.name, code);
        return wrap("menu example(1) {\n    slot0: ({id:\"minecraft:stone\"})", code) + "\n}";
    }

    private static String inAllowedPlace(AddCondition c, String code) {
        if (RestrictionCondition.NONE.isAllowed(c)) return code;
        for (Event e : Event.values())
            if (RestrictionCondition.valueOf("EVENT_" + e.name()).isAllowed(c)) return wrap("event " + e.name, code);
        return code;
    }

    private static String wrap(String header, String code) {
        return header + " {\n    " + code + "\n}";
    }

    private static String exampleArguments(List<Param> params) {
        List<String> args = new ArrayList<>();
        for (Param p : params) args.add(exampleValue(p));
        return String.join(", ", args);
    }

    private static String exampleValue(Param p) {
        if (p.hasEnumConstraint() && p.enumValues().length > 0) return "\"" + p.enumValues()[0] + "\"";
        return switch (p.kind()) {
            case STRING_LITERAL -> "\"text\"";
            case BOOL_LITERAL -> "false";
            case LONG_LITERAL -> p.hasLongRangeConstraint() ? String.valueOf(p.rangeMin()) : "1";
            case DOUBLE_LITERAL -> p.hasDoubleRangeConstraint() ? String.valueOf(p.decimalMin()) : "1.0";
            case NORMAL_EXPRESSION -> "1";
            case ITEM_LITERAL -> "{id:\"minecraft:diamond\",count:1}";
            case LOCATION -> "INVOKER";
        };
    }

    private static String typeName(VariableType t) {
        return t.name().toLowerCase(Locale.ROOT);
    }
}
