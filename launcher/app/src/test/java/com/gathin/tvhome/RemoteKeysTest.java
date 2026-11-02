package com.gathin.tvhome;

import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RemoteKeysTest {
    @Test public void foregroundSettingsKeysOpenControlCenter(){
        assertTrue(RemoteKeys.isControlCenterKey(176,new int[0]));
        assertTrue(RemoteKeys.isControlCenterKey(83,new int[0]));
        assertTrue(RemoteKeys.isControlCenterKey(190,new int[]{190}));
    }
    @Test public void ordinaryRemoteButtonsAreNotSettingsKeys(){
        for(int key:new int[]{3,4,19,20,21,22,23,24,25,26,164})
            org.junit.Assert.assertFalse(RemoteKeys.isControlCenterKey(key,new int[0]));
    }
    private static Map<Integer, String> parse(String... entries) {
        // Key names resolved by a stand-in: android.view.KeyEvent isn't available in JVM tests.
        return RemoteKeys.parseAppKeys(java.util.Arrays.asList(entries),
                name -> name.equals("KEYCODE_BUTTON_3") ? 190 : 0);
    }

    @Test public void keyNamesWorkLikeNumbers() {
        Map<Integer, String> keys = parse("KEYCODE_BUTTON_3:com.google.android.youtube.tv", "KEYCODE_NOT_A_KEY:a.b");
        assertEquals(1, keys.size());
        assertEquals("com.google.android.youtube.tv", keys.get(190));
    }

    @Test public void keycodeAndPackage() {
        Map<Integer, String> keys = parse(
                "190:com.google.android.youtube.tv", " 193 : com.netflix.ninja ");
        assertEquals(2, keys.size());
        assertEquals("com.google.android.youtube.tv", keys.get(190));
        assertEquals("com.netflix.ninja", keys.get(193));
    }

    @Test public void badEntriesAreSkipped() {
        Map<Integer, String> keys = parse(
                "com.google.android.youtube.tv", "youtube:com.google.android.youtube.tv", "190:", "");
        assertTrue(keys.isEmpty());
    }

    @Test public void theLastEntryForAKeycodeWins() {
        Map<Integer, String> keys = parse("190:a.b", "190:c.d");
        assertEquals("c.d", keys.get(190));
    }
}
