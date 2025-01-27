package org.example;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import org.example.ui.Theme;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Drawer {

    static final int INDENT = 2;

    static final Pattern colorPattern = Pattern.compile("#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})");

    // Where on the screen we drew the cursor.
    // If that was too low, maybe you'll want to adjust and try again?
    private int cursorScreenLine = 0;
    private boolean drewCursor = false;

    public static void printMaybeReversed(TextGraphics g, TerminalPosition pos, String s, boolean bolded) {
        if (bolded) {
            g.putString(pos, s, SGR.REVERSE);
        } else {
            g.putString(pos, s);
        }
    }

    public Drawer() {}

    public int getCursorLineLastTime() {
        return this.cursorScreenLine;
    }

    // inFoldedContext = we're folded, only print pinned rows.
    public int printJsonMap(TextGraphics g, JsonNodeMap jsonMap, TerminalPosition start, int initialOffset, boolean inFoldedContext, boolean inSyntheticContext) {
        int line = 0;
        Collection<String> keys = jsonMap.getKeysInOrder();
        int indent = start.getColumn();
        TerminalPosition pos = start;

        // we mark out aggregate data so it is visually distinct.
        String prefix = "";
        if (inSyntheticContext) prefix = "//   ";

        // In a folded context, we only show pinned things.
        if (jsonMap.getFolded()) inFoldedContext = true;
        // Pinning an object means we show the whole object
        if (jsonMap.getPinned()) inFoldedContext = false;

        if (inFoldedContext) {
            if (!jsonMap.hasPins()) {
                printMaybeReversed(g, pos.withRelativeColumn(initialOffset),  "{ ... }", jsonMap.isAtCursor());
                return 1;
            }
            // we contain at least one thing that'll be shown, so open up.
            printMaybeReversed(g, pos.withRelativeColumn(initialOffset),   "{ ...", jsonMap.isAtCursor());
        } else {
            printMaybeReversed(g, pos.withRelativeColumn(initialOffset),  "{", jsonMap.isAtCursor());
        }

        int myIndent = INDENT;
        //if (inSyntheticContext) myIndent += 3;
        pos = pos.withRelativeColumn(myIndent).withRelativeRow(1);

        line += 1;
        for (JsonNodeIterator it = jsonMap.iterateChildren(); it!=null; it=it.next()) {
            JsonNode child = it.get();
            String key = (String)it.key();
            if (inFoldedContext && !child.hasPins()) {
                // skip this child
                continue;
            }
            String aggComment = "";
            if (inSyntheticContext && child.aggregateComment != null && !child.aggregateComment.isEmpty()) {
                aggComment = child.aggregateComment + " ";
            }

            TextGraphics g2 = g;
            printMaybeReversed(g, pos, aggComment + "\"" + key + "\"", jsonMap.isAtCursor(key));
            TerminalPosition pos2 = pos.withRelativeColumn(aggComment.length() + 2 + key.length());
            // normal case, user data.
            if (child instanceof JsonNodeValue) {
                int height;
                if (inSyntheticContext) {
                    printGutterIndicator(g, pos, child);
                    height = 1;
                } else {
                    JsonNodeValue v = (JsonNodeValue) child;
                    Object val = v.getValue();
                    TerminalPosition pos4 = pos2;
                    if (it.isAggregate()) {
                        g2 = Theme.withColor(g, Theme.synthetic);
                        g2.putString(pos4.withColumn(2), "//");
                        String intro = jsonMap.aggregateComment + "() ";
                        if (pos4.getColumn()<=5) {
                            // move to the right to make room for the comment symbols
                            pos4 = pos4.withColumn(5);
                        }
                        g2.putString(pos4, intro);
                    }
                    g2.putString(pos4, ": ");
                    height = printJsonSubtree(g2, pos, pos4.getColumn() - pos.getColumn() + 2, child, inFoldedContext, inSyntheticContext || v.isSynthetic());
                }
                line += height;
                pos = pos.withRelativeRow(height);
            } else {
                g.putString(pos2, ": ");
                int childOffset = aggComment.length() + key.length() + 4;
                int childHeight = printJsonSubtree(g, pos, childOffset, child, inFoldedContext, inSyntheticContext);
                line += childHeight;
                pos = pos.withRelativeRow(childHeight);
            }
            // stop drawing if we're off the screen.
            if (drewCursor && pos.getRow() > g.getSize().getRows() + 10) break;
        }
        line += 1;
        pos = pos.withRelativeColumn(-myIndent);
        g.putString(pos.withColumn(2), prefix);
        g.putString(pos, "}");
        return line;
    }

    // Returns how many lines it went down, beyond the initial one.
    // jsonObj can be String, List, LinkedHashMap<String, Object>, ...
    public int printJsonTree(TextGraphics g, TerminalPosition start, int initialOffset, JsonNode json) {
        this.drewCursor = false;
        return printJsonSubtree(g, start, initialOffset, json, false, false);
    }

    public void printGutterIndicator(TextGraphics g, TerminalPosition start, JsonNode json) {
        if (json.isAtPrimaryCursor()) {
            this.cursorScreenLine = start.getRow();
            this.drewCursor = true;
            if (json.parent!=null) {
                g.putString(start.withColumn(0), ">>");
            }
        }
        if (json.getPinned()) {
            // draw the pin
            g.putString(start.withColumn(0), "P");
        }
        if (json.isAtFork()) {
            g.putString(start.withColumn(0), "*");
        }
    }

    // Returns how many lines it went down, beyond the initial one.
    // jsonObj can be String, List, LinkedHashMap<String, Object>, ...
    public int printJsonSubtree(TextGraphics g, TerminalPosition start, int initialOffset, JsonNode json, boolean inFoldedContext, boolean inSyntheticContext) {
        int line = 0;
        printGutterIndicator(g, start, json);
        if (json instanceof JsonNodeValue) {
            JsonNodeValue jsonValue = (JsonNodeValue) json;
            int lines = 0;
            if (inFoldedContext && !json.hasPins()) {
                // skip
                return 0;
            }
            String annotation = jsonValue.getAnnotation();
            if (!annotation.isEmpty()) {
                TextGraphics gg = Theme.withColor(g, Theme.synthetic);
                printMaybeReversed(gg, start.withRelativeColumn(initialOffset), "// " + annotation, false);
                start = start.withRelativeRow(1);
                lines++;
            }

            Object value = jsonValue.getValue();
            if (value instanceof String) {
                String str = (String)value;
                printMaybeReversed(g, start.withRelativeColumn(initialOffset), "\"" + str + "\"", json.isAtCursor());
                lines += 1;
                Matcher colorMatcher = colorPattern.matcher(str);
                boolean found = colorMatcher.find();
                if (found && colorMatcher.groupCount()==3) {
                    int cr = Integer.parseInt(colorMatcher.group(1), 16);
                    int cg = Integer.parseInt(colorMatcher.group(2), 16);
                    int cb = Integer.parseInt(colorMatcher.group(3), 16);
                    TextGraphics gg = Theme.withColor(g, Theme.synthetic);
                    gg.putString(start.withRelativeColumn(initialOffset + 3 + str.length()), "//");
                    TextColor col = TextColor.Indexed.fromRGB(cr, cg, cb);
                    gg.setForegroundColor(col);
                    gg.putString(start.withRelativeColumn(initialOffset + 6 + str.length()), "██");
                }
                return lines;
            } else {
                printMaybeReversed(g, start.withRelativeColumn(initialOffset), value.toString(), json.isAtCursor());
                return lines+1;
            }
        }
        if (json instanceof JsonNodeList) {
            JsonNodeList jsonList = (JsonNodeList)json;
            inFoldedContext = (jsonList.folded || inFoldedContext) && !jsonList.getPinned();
            TerminalPosition pos = start;
            TerminalPosition pos2 = pos.withRelativeColumn(initialOffset);
            JsonNode dad = jsonList.getParent();
            if (json.isAtCursor() && (dad==null || dad instanceof JsonNodeList)) {
                // we have no label, so let's make the bracket bold.
                g.putString(pos2, "[", SGR.REVERSE);
            } else {
                g.putString(pos2, "[");
            }
            pos2 = pos2.withRelativeColumn(1);
            if (inFoldedContext) {
                if (jsonList.hasPins()) {
                    g.putString(pos2, " ...");
                    pos2 = pos2.withRelativeColumn(4);
                } else {
                    g.putString(pos2, " ... ]");
                    pos2 = pos2.withRelativeColumn(6);
                }
            }
            String countAnno = " // " + jsonList.getAnnotation();
            if (!jsonList.getAnnotation().isEmpty()) countAnno += ", ";
            int c = jsonList.childCount();
            countAnno += c;
            if (c==1) countAnno += " entry";
            else countAnno += " entries";
            TextGraphics green = Theme.withColor(g, Theme.synthetic);
            green.putString(pos2, countAnno);
            if (inFoldedContext && !jsonList.hasPins()) {
                return 1;
            }
            TerminalPosition pos3 = pos.withRelativeColumn(INDENT);
            line += 1;
            pos3 = pos3.withRelativeRow(1);

            for (JsonNodeIterator it = jsonList.iterateChildren(); it!=null; it=it.next()) {
                JsonNode child = it.get();
                if (inFoldedContext && !child.hasPins()) {
                    // skip that one, we're folded and it's not pinned.
                    continue;
                }
                TextColor oldColor = g.getForegroundColor();
                String intro = "";
                TerminalPosition pos4 = pos3;
                if (it.isAggregate()) {
                    g.setForegroundColor(Theme.synthetic);
                    g.putString(pos4.withColumn(2), "//");
                    intro = jsonList.aggregateComment + "() ";
                    if (pos4.getColumn()<=5) {
                        // move to the right to make room for the comment symbols
                        pos4 = pos4.withColumn(5);
                    }
                    g.putString(pos4, intro);
                }
                int height = printJsonSubtree(g, pos4, intro.length(), child, inFoldedContext, inSyntheticContext || it.isAggregate());
                g.setForegroundColor(oldColor);
                line += height;
                pos3 = pos3.withRelativeRow(height);
                // stop drawing if we're off the screen.
                if (drewCursor && pos3.getRow() > g.getSize().getRows() + 10) break;
            }
            pos = pos3.withRelativeColumn(-INDENT);
            g.putString(pos, "]");
            return line + 1;
        }
        else if (json instanceof JsonNodeMap) {
            JsonNodeMap jsonMap = (JsonNodeMap) json;
            if (inFoldedContext && !(jsonMap.getPinned() || jsonMap.hasPins())) {
                return 0; // hidden in the fold
            }
            return printJsonMap(g, jsonMap, start, initialOffset, inFoldedContext, inSyntheticContext);
        }

        throw new RuntimeException("Unrecognized type: " + json.getClass());
    }

}
