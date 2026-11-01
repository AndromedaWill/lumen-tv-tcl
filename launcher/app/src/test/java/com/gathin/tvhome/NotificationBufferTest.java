package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class NotificationBufferTest {
    private NotificationBuffer.Notice item(String key,long time){return new NotificationBuffer.Notice(key,"example.app","Title","Body",time);}
    @Test public void retainsOnlyTwentyNewestActiveNotices(){
        NotificationBuffer b=new NotificationBuffer();for(int i=0;i<80;i++)b.put(item("key"+i,i));
        assertEquals(20,b.snapshot().size());assertEquals(79,b.snapshot().get(0).time);assertEquals(60,b.snapshot().get(19).time);
    }
    @Test public void updatesReplaceAndRemovalDoesNotLeaveHistory(){
        NotificationBuffer b=new NotificationBuffer();b.put(item("same",1));b.put(item("same",2));
        assertEquals(1,b.snapshot().size());assertEquals(2,b.snapshot().get(0).time);b.remove("same");assertTrue(b.snapshot().isEmpty());
    }
    @Test public void snapshotsDoNotExposeMutableStorage(){
        NotificationBuffer b=new NotificationBuffer();b.put(item("one",1));b.snapshot().clear();assertEquals(1,b.snapshot().size());b.clear();assertTrue(b.snapshot().isEmpty());
    }
    @Test public void boundsUntrustedTextAndRemovesDirectionControls(){
        NotificationBuffer.Notice n=new NotificationBuffer.Notice("k","p","x".repeat(500),"a\u202eb\nc",0);
        assertEquals(120,n.title.length());assertFalse(n.body.contains("\u202e"));assertFalse(n.body.contains("\n"));
    }
    @Test public void defaultsFilterQuietOngoingSummaryAndSecret(){
        assertFalse(NotificationBuffer.accepts(true,false,false,4,false));assertFalse(NotificationBuffer.accepts(false,true,false,4,false));
        assertFalse(NotificationBuffer.accepts(false,false,true,4,true));assertFalse(NotificationBuffer.accepts(false,false,false,2,false));
        assertTrue(NotificationBuffer.accepts(false,false,false,2,true));assertTrue(NotificationBuffer.accepts(false,false,false,3,false));
        assertTrue(NotificationBuffer.accepts(false,false,false,-1,false));
    }
}
