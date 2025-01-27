package org.example;

import com.googlecode.lanterna.input.KeyStroke;
import org.junit.Test;

/**
 * Test the main class, feeding it keystrokes.
 * This is an integration test of sorts.
 **/
public class MainTest {

    static final String[] ONE_MAP = new String[]{
            "{ \"hello\": 12, \"bye\": 13,",
            "  \"hello2\": 12, \"bye2\": 13,",
            "  \"hello3\": 12, \"bye3\": 13,",
            "  \"hello4\": 12, \"bye4\": 13,",
            "  \"hello5\": 12, \"bye5\": 13",
            "}"
    };

    static final String[] NESTED_MAPS = new String[]{
            "{ \"a\": ",
            "  { \"b\": ",
            "    { \"c\": ",
            "      { \"d\": ",
            " \"innermost\" } } } }"
    };

    static final String[] MIX_OF_TYPES = new String[]{
            "{ \"map1\": {",
            "  \"map2\": {",
            "      \"alist\": [ ",
            "          { \"number\": 12, \"string\": \"hello\", \"boolean\": true },",
            "          { \"number\": 12, \"string\": \"hello\", \"boolean\": true }",
            "       ],",
            "       \"another_bool\": false",
            "} } }"
    };

    static final String[] MAP_OF_NUMBERS = new String[]{
            "{ \"a\": 3,",
            "  \"b\": 2,",
            "  \"c\": 1}"
    };

    @Test
    public void testEmptyName() throws Exception {
        Main.main(new String[]{});
        // we shouldn't throw an exception
    }

    @Test
    public void testScrollDoesntCrash() throws Exception {
        Main main = Main.fromLinesAndVirtual(ONE_MAP, 80, 5);
        main.display();
        main.actOnKey(KeyStroke.fromString("<Down>"));
        main.display();
        for (int i=0; i<20; i++) {
            main.actOnKey(KeyStroke.fromString("<Down>"));
            main.display();
        }
        main.actOnKey(KeyStroke.fromString("<Home>"));
        main.display();
        main.actOnKey(KeyStroke.fromString("<End>"));
        main.display();
    }

    @Test
    public void testFoldNested() throws Exception {
        Main main = Main.fromLinesAndVirtual(NESTED_MAPS, 40, 5);
        main.display();
        // fold top level
        main.actOnKey(KeyStroke.fromString("<Left>"));
        main.display();
        main.actOnKey(KeyStroke.fromString("<Right>"));
        main.display();
        // go deeper
        for (int i=0; i<5; i++) {
            main.actOnKey(KeyStroke.fromString("<Down>"));
            main.display();
        }
        // fold on your way up
        for (int i=0; i<5; i++) {
            main.actOnKey(KeyStroke.fromString("<Left>"));
            main.display();
            main.actOnKey(KeyStroke.fromString("<Right>"));
            main.display();
            main.actOnKey(KeyStroke.fromString("<Left>"));
            main.display();
            main.actOnKey(KeyStroke.fromString("<Up>"));
            main.display();
        }

    }

    @Test
    public void testFoldNested2() throws Exception {
        Main main = Main.fromLinesAndVirtual(MIX_OF_TYPES, 40, 5);
        main.display();
        // fold top level
        main.actOnKey(KeyStroke.fromString("<Left>"));
        main.display();
        main.actOnKey(KeyStroke.fromString("<Right>"));
        main.display();
        // go deeper
        for (int i=0; i<10; i++) {
            main.actOnKey(KeyStroke.fromString("<Down>"));
            main.display();
        }
        // fold on your way up
        for (int i=0; i<10; i++) {
            main.actOnKey(KeyStroke.fromString("<Left>"));
            main.display();
            main.actOnKey(KeyStroke.fromString("<Right>"));
            main.display();
            main.actOnKey(KeyStroke.fromString("<Left>"));
            main.display();
            main.actOnKey(KeyStroke.fromString("<Up>"));
            main.display();
        }

    }

    @Test
    public void testTotalOnAMap() throws Exception {
        Main main = Main.fromLinesAndVirtual(MAP_OF_NUMBERS, 40, 5);
        main.display();
        // total
        main.actOnKey(KeyStroke.fromString("a"));
        main.actOnKey(KeyStroke.fromString("t"));
        main.display();
        for (int i=0; i<2; i++) {
            main.actOnKey(KeyStroke.fromString("<Down>"));
            main.display();
        }

    }

}
