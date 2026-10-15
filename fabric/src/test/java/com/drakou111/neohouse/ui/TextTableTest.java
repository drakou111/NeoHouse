package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.ui.TextTable.Cell;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextTableTest {

    @Test
    void cellsAreSplitAtTwoOrMoreSpaces() {
        List<List<Cell>> table = TextTable.parse("""
                Override If Empty   fill only the ones still empty
                Append              add the script's actions""");

        assertThat(table.get(0)).containsExactly(new Cell(0, "Override If Empty"), new Cell(1, "fill only the ones still empty"));
        assertThat(table.get(1)).containsExactly(new Cell(0, "Append"), new Cell(1, "add the script's actions"));
    }

    @Test
    void anIndentedLineContinuesInTheColumnItReaches() {
        List<List<Cell>> table = TextTable.parse("""
                Recursion     a function calling itself
                              Warn, Error, Off""");

        assertThat(table.get(1)).containsExactly(new Cell(1, "Warn, Error, Off"));
    }

    @Test
    void columnsArePlacedAfterTheWidestCellBeforeThem() {
        List<List<Cell>> table = TextTable.parse("""
                a      bbbbbbbbbb   c
                aaaa   b            c""");

        // Width is one pixel per character here.
        assertThat(TextTable.columnX(table, String::length, 2)).containsExactly(0, 6, 18);
    }

    @Test
    void aLastCellDoesNotPushLaterColumns() {
        List<List<Cell>> table = TextTable.parse("""
                long   whole numbers   42
                var    anything, unchecked and much longer""");

        assertThat(TextTable.columnX(table, String::length, 2)).containsExactly(0, 6, 21);
    }
}
