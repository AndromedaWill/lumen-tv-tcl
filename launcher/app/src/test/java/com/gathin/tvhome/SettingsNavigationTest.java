package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.gathin.tvhome.SettingsNavigation.Page.*;

public class SettingsNavigationTest {
    @Test public void backReturnsToExactParentRow() {
        SettingsNavigation n = new SettingsNavigation();
        n.focus("home"); n.push(HOME, "columns"); n.focus("rows"); n.push(ROWS, "rows_2");
        n.focus("rows_3"); assertTrue(n.pop()); assertEquals(HOME, n.page()); assertEquals("rows", n.focus());
        assertTrue(n.pop()); assertEquals(ROOT, n.page()); assertEquals("home", n.focus());
    }
    @Test public void rootBackDoesNotRemoveRoot() {
        SettingsNavigation n = new SettingsNavigation(); assertFalse(n.pop()); assertEquals(ROOT, n.page());
    }
    @Test public void recreationRetainsDepthAndEveryFocus() {
        SettingsNavigation n = new SettingsNavigation(); n.focus("home"); n.push(HOME, "columns"); n.push(COLUMNS, "columns_6");
        SettingsNavigation restored = SettingsNavigation.restore(n.pages(), n.focuses());
        assertEquals(COLUMNS, restored.page()); assertEquals("columns_6", restored.focus());
        restored.pop(); assertEquals("columns", restored.focus()); restored.pop(); assertEquals("home", restored.focus());
    }
    @Test(expected = IllegalArgumentException.class) public void aChoiceCannotOpenWithoutItsParent() {
        new SettingsNavigation().push(COLUMNS, "columns_4");
    }
    @Test public void damagedSavedStateReturnsToSafeRoot() {
        assertEquals(ROOT, SettingsNavigation.restore(new String[]{"ROOT", "SYSTEM", "COLUMNS"}, new String[]{"system", "x", "y"}).page());
        assertEquals(ROOT, SettingsNavigation.restore(new String[]{"ROOT", "UNKNOWN"}, new String[]{"home", "x"}).page());
        assertEquals(ROOT, SettingsNavigation.restore(null, null).page());
    }
    @Test public void serializedArraysDoNotMutateLiveStack() {
        SettingsNavigation n = new SettingsNavigation(); String[] pages = n.pages(), focuses = n.focuses();
        pages[0] = "SYSTEM"; focuses[0] = "other"; assertEquals(ROOT, n.page()); assertEquals("home", n.focus());
    }
    @Test public void creditsReturnToSystemThenOriginalCategory() {
        SettingsNavigation n = new SettingsNavigation(); n.focus("system"); n.push(SYSTEM, "credits"); n.push(CREDITS, "photo");
        n.pop(); assertEquals("credits", n.focus()); n.pop(); assertEquals("system", n.focus());
    }
}
