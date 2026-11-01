package com.gathin.tvhome;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.*;

/** Small versioned collection. Sources are packaged assets or files owned by this app, never arbitrary paths. */
final class BackgroundStore {
    static final String PREFS="background_collection_v1",FIXED="fixed",SCHEDULE="schedule",SLIDESHOW="slideshow",LEGACY="legacy";
    static final int LIMIT=24;
    static final class Item {
        String id,label,source;boolean video,scheduled;int minute;
        Item(String id,String label,String source,boolean video,boolean scheduled,int minute){this.id=id;this.label=label;this.source=source;this.video=video;this.scheduled=scheduled;this.minute=minute;}
    }
    final List<Item> items=new ArrayList<>();
    String mode=SCHEDULE,fixed="default-2";
    int intervalSeconds=300;boolean random,transitions=true;
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,0);}
    static BackgroundStore defaults(Context c){
        BackgroundStore s=new BackgroundStore();int[] times={300,420,540,960,1110,1230};
        for(int i=0;i<6;i++)s.items.add(new Item("default-"+i,PersonalBackground.PHASES[i],
                PersonalBackground.imported(c,i).isFile()?"legacy:"+i:"asset:"+(i+1),false,true,times[i]));
        android.content.SharedPreferences old=PersonalBackground.prefs(c);
        s.mode=old.getString("mode",SCHEDULE);s.fixed="default-"+Math.max(0,Math.min(5,old.getInt("phase",2)));
        s.transitions=!old.getBoolean("no_transition",false);return s;
    }
    static synchronized BackgroundStore load(Context c){
        String raw=prefs(c).getString("collection",null);
        if(raw==null)return defaults(c);
        try{
            JSONObject o=new JSONObject(raw);if(o.getInt("version")!=1)throw new JSONException("Version");
            BackgroundStore s=new BackgroundStore();s.mode=o.optString("mode",SCHEDULE);s.fixed=o.optString("fixed","");
            if(!Arrays.asList(FIXED,SCHEDULE,SLIDESHOW,LEGACY).contains(s.mode))s.mode=FIXED;
            s.intervalSeconds=Math.max(10,Math.min(86400,o.optInt("interval",300)));s.random=o.optBoolean("random");s.transitions=o.optBoolean("transitions",true);
            JSONArray a=o.getJSONArray("items");Set<String> seen=new HashSet<>();
            for(int i=0;i<Math.min(LIMIT,a.length());i++){
                JSONObject x=a.getJSONObject(i);String id=x.getString("id"),source=x.getString("source");
                if(!seen.add(id)||!validSource(source))continue;
                s.items.add(new Item(id,Cards.cleanLabel(x.optString("label","Fondo"),"Fondo"),source,x.optBoolean("video"),x.optBoolean("scheduled"),Math.max(0,Math.min(1439,x.optInt("minute",540)))));
            }
            return s.items.isEmpty()?defaults(c):s;
        }catch(JSONException|RuntimeException invalid){return defaults(c);}
    }
    synchronized void save(Context c){persist(c,false);}
    private boolean persist(Context c,boolean blocking){
        try{
            JSONObject o=new JSONObject().put("version",1).put("mode",mode).put("fixed",fixed).put("interval",intervalSeconds).put("random",random).put("transitions",transitions);
            JSONArray a=new JSONArray();for(Item x:items)a.put(new JSONObject().put("id",x.id).put("label",x.label).put("source",x.source).put("video",x.video).put("scheduled",x.scheduled).put("minute",x.minute));
            o.put("items",a);android.content.SharedPreferences.Editor edit=prefs(c).edit().putString("collection",o.toString());
            if(blocking){String previous=prefs(c).getString("collection",null);if(edit.commit())return true;
                android.content.SharedPreferences.Editor rollback=prefs(c).edit();if(previous==null)rollback.remove("collection");else rollback.putString("collection",previous);rollback.apply();return false;}
            edit.apply();return true;
        }catch(JSONException impossible){throw new IllegalStateException(impossible);}
    }
    static boolean validSource(String source){return source.matches("asset:[1-6]")||source.matches("legacy:[0-5]")||source.matches("file:[a-zA-Z0-9._-]{1,100}")&&!source.contains("..")||source.startsWith("aerial:")&&!source.substring(7).contains("/")&&!source.substring(7).contains("\\")&&!source.contains("..");}
    static File directory(Context c){File d=new File(c.getFilesDir(),"wallpapers");if(!d.isDirectory())d.mkdirs();return d;}
    static File file(Context c,Item x){
        if(!validSource(x.source))return null;
        if(x.source.startsWith("file:"))return new File(directory(c),x.source.substring(5));
        if(x.source.startsWith("legacy:"))return PersonalBackground.imported(c,Integer.parseInt(x.source.substring(7)));
        if(x.source.startsWith("aerial:")){File dir=c.getExternalFilesDir("aerials");return dir==null?null:new File(dir,x.source.substring(7));}
        return null;
    }
    static InputStream open(Context c,Item x)throws IOException{
        File f=file(c,x);if(f!=null)return new FileInputStream(f);
        try{return c.getAssets().open("personal/"+x.source.substring(6)+".jpg");}
        catch(IOException missing){return c.getResources().openRawResource(R.raw.hercules);}
    }
    Item find(String id){for(Item x:items)if(x.id.equals(id))return x;return items.isEmpty()?null:items.get(0);}
    private Item exact(String id){for(Item x:items)if(x.id.equals(id))return x;return null;}
    static synchronized void acceptImport(Context c,Item item,String replacement)throws IOException{
        BackgroundStore s=load(c);Item old=replacement==null?null:s.exact(replacement);
        if(replacement!=null&&old==null){discardManaged(c,item);throw new IOException("El fondo a reemplazar ya no está en la colección.");}
        if(old!=null){item.id=old.id;item.minute=old.minute;item.scheduled=!item.video&&old.scheduled;s.items.set(s.items.indexOf(old),item);}
        else{
            if(s.items.size()>=LIMIT){discardManaged(c,item);throw new IOException("Colección completa.");}
            Set<Integer> used=new HashSet<>(starts(s.images(true)));int start=0;while(used.contains(start)&&start<1380)start+=60;
            item.minute=start;item.scheduled=!item.video&&s.images(true).size()<12;s.items.add(item);
        }
        if(!s.persist(c,true)){discardManaged(c,item);throw new IOException("No se pudo guardar la colección. Se conserva el fondo anterior.");}
        if(old!=null)discardManaged(c,old);
    }
    List<Item> images(boolean schedule){List<Item> out=new ArrayList<>();for(Item x:items)if(!x.video&&(!schedule||x.scheduled))out.add(x);return out;}
    static List<Integer> starts(List<Item> items){List<Integer> out=new ArrayList<>();for(Item x:items)out.add(x.minute);return out;}
    static void discardManaged(Context c,Item old){if(old.source.startsWith("file:")){File f=file(c,old);if(f!=null)f.delete();}}
    static synchronized void discardUnused(Context c,List<Item> old){
        BackgroundStore current=load(c);if(!current.persist(c,true))return;
        for(Item item:old){boolean used=false;for(Item x:current.items)if(x.source.equals(item.source)){used=true;break;}if(!used)discardManaged(c,item);}
    }
}
