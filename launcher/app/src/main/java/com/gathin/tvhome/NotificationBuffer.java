package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/** Bounded current notices only; no persistence, PendingIntents, images or event history. */
final class NotificationBuffer {
    static final int LIMIT=20;
    private static final java.util.regex.Pattern UNSAFE=java.util.regex.Pattern.compile("[\\p{Cntrl}\\p{Cf}]");
    static final class Notice {
        final String key,pkg,title,body;
        final long time;
        Notice(String key,String pkg,String title,String body,long time){
            this.key=clean(key,512);this.pkg=clean(pkg,192);this.title=clean(title,120);this.body=clean(body,320);this.time=time;
        }
        private static String clean(String text,int limit){
            if(text==null)return "";
            String input=text.length()>limit?text.substring(0,limit):text;
            String value=UNSAFE.matcher(input).replaceAll(" ").trim();
            return value.length()>limit?value.substring(0,limit):value;
        }
    }
    private final LinkedHashMap<String,Notice> active=new LinkedHashMap<>();
    synchronized void put(Notice notice){
        if(notice.key.isEmpty())return;
        active.put(notice.key,notice);
        while(active.size()>LIMIT){
            Notice oldest=null;
            for(Notice item:active.values())if(oldest==null||item.time<oldest.time)oldest=item;
            if(oldest!=null)active.remove(oldest.key);
        }
    }
    synchronized void remove(String key){active.remove(Notice.clean(key,512));}
    synchronized void clear(){active.clear();}
    synchronized List<Notice> snapshot(){
        List<Notice> result=new ArrayList<>(active.values());result.sort(Comparator.comparingLong((Notice n)->n.time).reversed());return result;
    }
    static boolean accepts(boolean ongoing,boolean summary,boolean secret,int importance,boolean showQuiet){
        return !ongoing&&!summary&&!secret&&(showQuiet||importance<=0||importance>=3);
    }
}
