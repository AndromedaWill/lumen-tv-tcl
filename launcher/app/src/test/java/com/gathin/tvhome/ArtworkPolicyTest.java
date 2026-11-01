package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class ArtworkPolicyTest {
    @Test public void installedIconsReplaceOldGoogleAndXuperBanners() {
        assertTrue(ArtworkPolicy.preferIcon("com.google.android.youtube.tv","YouTube"));
        assertTrue(ArtworkPolicy.preferIcon("tv.example","XUPER TV"));
        assertFalse(ArtworkPolicy.preferIcon("com.netflix.ninja","Netflix"));
    }
}
