package com.drakou111.neohouse.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A preformatted help block laid out as a table. In the source text, cells are separated by two or
 * more spaces, and cells starting at the same character position form a column; an indented line
 * continues in the column its indent reaches. Columns are then placed by pixel width, since the
 * game font isn't monospaced and lining text up with spaces would leave the columns ragged.
 */
final class TextTable {
    /** A cell's text and the column it belongs to. */
    record Cell(int column, String text) {}

    // Words joined by single spaces stay one cell.
    private static final Pattern CELL = Pattern.compile("\\S+(?: \\S+)*");

    private TextTable() {
    }

    /** The cells of each line of {@code text}, in order. */
    static List<List<Cell>> parse(String text) {
        // First pass: each cell keyed by the character position it starts at.
        List<List<Cell>> byPosition = new ArrayList<>();
        TreeSet<Integer> starts = new TreeSet<>();
        for (String line : text.split("\n", -1)) {
            List<Cell> cells = new ArrayList<>();
            Matcher matcher = CELL.matcher(line);
            while (matcher.find()) {
                cells.add(new Cell(matcher.start(), matcher.group()));
                starts.add(matcher.start());
            }
            byPosition.add(cells);
        }

        List<Integer> columnStarts = new ArrayList<>(starts);
        List<List<Cell>> table = new ArrayList<>();
        for (List<Cell> line : byPosition) {
            List<Cell> cells = new ArrayList<>();
            for (Cell cell : line) cells.add(new Cell(columnStarts.indexOf(cell.column()), cell.text()));
            table.add(cells);
        }
        return table;
    }

    /**
     * The x offset of each column: every column starts {@code gap} after the widest cell of the one
     * before. A line's last cell doesn't widen its column, since nothing follows it on that line.
     */
    static int[] columnX(List<List<Cell>> table, ToIntFunction<String> width, int gap) {
        int columns = 0;
        for (List<Cell> line : table) for (Cell cell : line) columns = Math.max(columns, cell.column() + 1);

        int[] widest = new int[columns];
        for (List<Cell> line : table)
            for (int j = 0; j + 1 < line.size(); j++)
                widest[line.get(j).column()] = Math.max(widest[line.get(j).column()], width.applyAsInt(line.get(j).text()));

        int[] x = new int[columns];
        for (int c = 1; c < columns; c++) x[c] = x[c - 1] + (widest[c - 1] == 0 ? 0 : widest[c - 1] + gap);
        return x;
    }
}
