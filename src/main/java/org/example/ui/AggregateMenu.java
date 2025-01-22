package org.example.ui;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import org.example.JsonNode;
import org.example.cursor.FindCursor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AggregateMenu {

    private int row = 0;
    // the max allowed row value
    private int maxRow = 2;

    public enum Choice {
        // leave the menu visible
        NONE,
        // close the menu, do nothing
        CANCEL,
        // Aggregation choices:
        UNIQUE_FIELDS,
        AGG_TOTAL,
        REMOVE_AGGREGATE,
    }

    public void init() {
        row = 0;
        maxRow = 1;
    }

    public void draw(TextGraphics g) {

        String menu =
                        "╭─────────[ AGGREGATE ]─────────╮\n"+
                        "│ u: unique keys                │\n"+
                        "│ t: total                      │\n"+
                        "│ x: remove aggregate           │\n"+
                        "├───────────────────────────────┤\n"+
                        "│ esc : cancel                  │\n"+
                        "╰───────────────────────────────╯\n";

        String[] lines = menu.split("\n");
        TerminalPosition top = TerminalPosition.TOP_LEFT_CORNER.withRelativeColumn(4).withRelativeRow(1);
        TerminalPosition pos = top;
        int i=0;
        int highlight = row+1;
        // the final choice is after the separator
        if (row==maxRow) highlight++;
        for (String l : lines) {
            if (i==highlight) {
                // selected row
                g.putString(pos, ">");
                g.putString(pos.withRelativeColumn(1), l.substring(1, l.length()-1), SGR.REVERSE);
                g.putString(pos.withRelativeColumn(l.length()-1), "<");
            } else {
                // rest of the menu
                g.putString(pos, l);
            }
            i++;
            pos = pos.withRelativeRow(1);
        }

    }

    public Choice update(KeyStroke key) {
        if (key.getKeyType()==KeyType.ArrowUp && row>0) {
            row -= 1;
        }
        if (key.getKeyType()==KeyType.ArrowDown && row<maxRow) {
            row++;
        }
        if (key.getKeyType()==KeyType.Escape) {
            return Choice.CANCEL;
        }
        if (key.getKeyType()==KeyType.Enter) {
            if (row==maxRow) return Choice.CANCEL;
            if (row==0) return Choice.UNIQUE_FIELDS;
            if (row==1) return Choice.AGG_TOTAL;
            if (row==2) return Choice.REMOVE_AGGREGATE;
        }
        if (key.getKeyType()==KeyType.Character) {
            switch (key.getCharacter()) {
                case 'u': return Choice.UNIQUE_FIELDS;
                case 't': return Choice.AGG_TOTAL;
                case 'x': return Choice.REMOVE_AGGREGATE;
                case 'q': return Choice.CANCEL;
            }
        }
        return Choice.NONE;
    }
}