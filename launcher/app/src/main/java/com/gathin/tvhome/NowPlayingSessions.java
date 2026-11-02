package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/** Panel-scoped framework sessions. No polling, audio focus, playback service or private history. */
final class NowPlayingSessions implements AutoCloseable {
    interface Observer { void changed(Snapshot snapshot); }
    interface Source {
        boolean available();
        void listen(MediaSessionManager.OnActiveSessionsChangedListener observer,Handler handler);
        List<MediaController> current();
        void close();
    }
    private static final class FrameworkSource implements Source {
        private final Context context;
        private final MediaSessionManager manager;
        private MediaSessionManager.OnActiveSessionsChangedListener observer;
        FrameworkSource(Context context){this.context=context;manager=context.getSystemService(MediaSessionManager.class);}
        public boolean available(){return manager!=null&&HomeNotificationService.hasAccess(context);}
        public void listen(MediaSessionManager.OnActiveSessionsChangedListener listener,Handler handler){
            observer=controllers->listener.onActiveSessionsChanged(external(controllers));
            manager.addOnActiveSessionsChangedListener(observer,HomeNotificationService.component(context),handler);
        }
        public List<MediaController> current(){return external(manager.getActiveSessions(HomeNotificationService.component(context)));}
        private List<MediaController> external(List<MediaController> controllers){
            List<MediaController> result=new ArrayList<>();
            if(controllers!=null)for(MediaController controller:controllers)if(!context.getPackageName().equals(controller.getPackageName()))result.add(controller);
            return result;
        }
        public void close(){if(observer!=null){manager.removeOnActiveSessionsChangedListener(observer);observer=null;}}
    }
    static final class Snapshot {
        final String title, artist, source;
        final int state;
        final long actions;
        final Bitmap artwork;
        Snapshot(String title,String artist,String source,int state,long actions,Bitmap artwork) {
            this.title=title;this.artist=artist;this.source=source;this.state=state;this.actions=actions;this.artwork=artwork;
        }
    }
    private final Context context;
    private final Observer observer;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final List<Binding> bindings=new ArrayList<>();
    private final Source source;
    private final MediaSessionManager.OnActiveSessionsChangedListener listChanged=this::bind;
    private HandlerThread thread;
    private Handler worker;
    private volatile boolean running;
    private boolean registered;
    private MediaController selected;
    private Bitmap originalArt, smallArt;
    private String sourcePackage="",sourceLabel="";
    private Snapshot lastPublished;

    NowPlayingSessions(Context context,Observer observer) {
        this(context,observer,null);
    }
    NowPlayingSessions(Context context,Observer observer,Source source) {
        this.context=context.getApplicationContext();this.observer=observer;
        this.source=source==null?new FrameworkSource(this.context):source;
    }
    void start() {
        if(running||!source.available())return;
        running=true;thread=new HandlerThread("HomeMediaPanel",android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();worker=new Handler(thread.getLooper());
        worker.post(()->{
            if(!running)return;
            try {
                source.listen(listChanged,worker);
                registered=true;
                bind(source.current());
            } catch(RuntimeException unavailable) { publish(null); }
        });
    }
    private final class Binding extends MediaController.Callback {
        final MediaController controller;
        Binding(MediaController controller){this.controller=controller;}
        @Override public void onPlaybackStateChanged(PlaybackState state){refresh();}
        @Override public void onMetadataChanged(MediaMetadata metadata){if(controller==selected)refresh();}
        @Override public void onSessionDestroyed(){
            try{controller.unregisterCallback(this);}catch(RuntimeException disconnected){ }
            bindings.remove(this);refresh();
        }
    }
    private void bind(List<MediaController> controllers) {
        if(!running)return;
        for(Binding binding:bindings)binding.controller.unregisterCallback(binding);
        bindings.clear();
        if(controllers!=null)for(MediaController controller:controllers){
            if(bindings.size()>=16)break;
            Binding binding=new Binding(controller);
            try {controller.registerCallback(binding,worker);bindings.add(binding);}
            catch(RuntimeException disconnected){ /* A session can disappear between query and binding. */ }
        }
        refresh();
    }
    private void refresh() {
        if(!running)return;
        MediaController best=null;int rank=0;
        for(Binding binding:bindings){
            try {
                PlaybackState state=binding.controller.getPlaybackState();
                int candidate=state==null?0:MediaSessionPolicy.rank(state.getState());
                boolean same=selected!=null&&selected.getSessionToken().equals(binding.controller.getSessionToken());
                if(candidate>rank||(candidate>0&&candidate==rank&&same)){best=binding.controller;rank=candidate;}
            }catch(RuntimeException disconnected){ }
        }
        selected=best;
        if(best==null){originalArt=null;smallArt=null;sourcePackage="";sourceLabel="";publish(null);return;}
        try {
            PlaybackState state=best.getPlaybackState();
            if(state==null){publish(null);return;}
            MediaMetadata metadata=best.getMetadata();
            Bitmap art=null;
            if(metadata!=null){
                art=metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                if(art==null)art=metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if(art==null)art=metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
            }
            if(art!=originalArt){
                originalArt=art;smallArt=null;
                if(art!=null&&!art.isRecycled()){
                    float scale=Math.min(1f,128f/Math.max(art.getWidth(),art.getHeight()));
                    smallArt=Bitmap.createScaledBitmap(art,Math.max(1,Math.round(art.getWidth()*scale)),Math.max(1,Math.round(art.getHeight()*scale)),true);
                }
            }
            if(!best.getPackageName().equals(sourcePackage)){
                sourcePackage=best.getPackageName();sourceLabel=sourcePackage;
                try {sourceLabel=bounded(context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(sourcePackage,0)),48);}
                catch(android.content.pm.PackageManager.NameNotFoundException missing){ }
            }
            String title=meta(metadata,MediaMetadata.METADATA_KEY_DISPLAY_TITLE,MediaMetadata.METADATA_KEY_TITLE,160);
            String artist=meta(metadata,MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,MediaMetadata.METADATA_KEY_ARTIST,96);
            publish(new Snapshot(title.isEmpty()?context.getString(R.string.media_playback):title,artist,sourceLabel,state.getState(),state.getActions(),smallArt));
        }catch(RuntimeException malformed){publish(null);}
    }
    private static String meta(MediaMetadata metadata,String primary,String fallback,int limit){
        if(metadata==null)return "";CharSequence value=metadata.getText(primary);
        return bounded(value==null||value.length()==0?metadata.getText(fallback):value,limit);
    }
    private static String bounded(CharSequence text,int limit){
        if(text==null)return "";int count=Math.min(limit,text.length());
        if(count>0&&Character.isHighSurrogate(text.charAt(count-1)))count--;
        return text.subSequence(0,count).toString().replace('\n',' ').replace('\r',' ');
    }
    private void publish(Snapshot snapshot){
        Snapshot previous=lastPublished;
        if(snapshot==null&&previous==null)return;
        if(snapshot!=null&&previous!=null&&snapshot.state==previous.state&&snapshot.actions==previous.actions
                &&snapshot.artwork==previous.artwork&&snapshot.title.equals(previous.title)
                &&snapshot.artist.equals(previous.artist)&&snapshot.source.equals(previous.source))return;
        lastPublished=snapshot;ui.post(()->{if(running)observer.changed(snapshot);});
    }
    void command(long action){
        Handler target=worker;if(!running||target==null)return;
        target.post(()->{
            if(!running||selected==null)return;
            try {
                PlaybackState state=selected.getPlaybackState();if(state==null)return;
                long actions=state.getActions();MediaController.TransportControls transport=selected.getTransportControls();
                if(action==MediaSessionPolicy.PREVIOUS&&MediaSessionPolicy.supports(actions,action))transport.skipToPrevious();
                else if(action==MediaSessionPolicy.NEXT&&MediaSessionPolicy.supports(actions,action))transport.skipToNext();
                else if(action==MediaSessionPolicy.TOGGLE&&MediaSessionPolicy.canToggle(state.getState(),actions)){
                    boolean playing=MediaSessionPolicy.pausable(state.getState());
                    if(MediaSessionPolicy.supports(actions,playing?MediaSessionPolicy.PAUSE:MediaSessionPolicy.PLAY)){
                        if(playing)transport.pause();else transport.play();
                    }else{
                        selected.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                        selected.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                    }
                }
            }catch(RuntimeException disconnected){refresh();}
        });
    }
    @Override public void close(){
        if(!running)return;running=false;ui.removeCallbacksAndMessages(null);
        Handler target=worker;HandlerThread oldThread=thread;worker=null;thread=null;
        target.post(()->{
            if(registered){try{source.close();}catch(RuntimeException unavailable){ }registered=false;}
            for(Binding binding:bindings){try{binding.controller.unregisterCallback(binding);}catch(RuntimeException disconnected){ }}
            bindings.clear();selected=null;originalArt=null;smallArt=null;lastPublished=null;sourcePackage="";sourceLabel="";
            oldThread.quitSafely();
        });
    }
}
