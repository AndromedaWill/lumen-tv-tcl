package com.gathin.tvhome;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Emulator-only real framework MediaSession transport tests. Discovery is injected, no access grant. */
public final class MediaPanelInstrumentation extends Instrumentation {
    private Activity activity;
    private ControlCenterDialog dialog;
    private MediaSession session,other;
    private final AtomicInteger pauses=new AtomicInteger(),plays=new AtomicInteger(),skips=new AtomicInteger();
    private long actions=PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT;
    private volatile boolean sourceClosed;
    private File output;
    private final List<NotificationBuffer.Notice> notices=new ArrayList<>();
    private Runnable noticesChanged;
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            check(android.os.Build.HARDWARE.contains("ranchu")||android.os.Build.HARDWARE.contains("goldfish"),"Emulator test only");
            output=new File(getTargetContext().getExternalFilesDir(null),"media-review");check(output.mkdirs()||output.isDirectory(),"Output directory");
            activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
            session=new MediaSession(getTargetContext(),"Synthetic music fixture");
            session.setCallback(new MediaSession.Callback(){
                @Override public void onPause(){pauses.incrementAndGet();state(session,PlaybackState.STATE_PAUSED);}
                @Override public void onPlay(){plays.incrementAndGet();state(session,PlaybackState.STATE_PLAYING);}
                @Override public void onSkipToNext(){skips.incrementAndGet();metadata(session,"Segunda pista · Prueba multimedia");}
            },new Handler(Looper.getMainLooper()));
            metadata(session,"Primera pista · Prueba multimedia");state(session,PlaybackState.STATE_PLAYING);session.setActive(true);
            other=new MediaSession(getTargetContext(),"Paused secondary fixture");metadata(other,"Otra sesión pausada");state(other,PlaybackState.STATE_PAUSED);other.setActive(true);
            final List<MediaController> controllers=Arrays.asList(other.getController(),session.getController());
            NowPlayingSessions.Source source=new NowPlayingSessions.Source(){
                public boolean available(){return true;}
                public void listen(MediaSessionManager.OnActiveSessionsChangedListener listener,Handler handler){ }
                public List<MediaController> current(){return controllers;}
                public void close(){sourceClosed=true;}
            };
            NotificationsPanel.Source noticeSource=new NotificationsPanel.Source(){
                public NotificationsPanel.State current(){return new NotificationsPanel.State(true,true,true,new ArrayList<>(notices));}
                public void observe(Runnable observer){noticesChanged=observer;}
            };
            runOnMainSync(()->{dialog=new ControlCenterDialog(activity,()->{},()->{},()->{},false,source,noticeSource);dialog.show();});
            awaitText("Primera pista · Prueba multimedia");shot("playing-unfocused");
            check(!hasText("Sin notificaciones pendientes"),"No empty notice placeholder");
            for(int i=0;i<4;i++){sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);checkFocusClear();}
            check("Pausar".equals(focusLabel()),"D-pad reaches pause from main controls");shot("playing-focused");
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);await(()->pauses.get()==1,"pause callback");awaitFocusLabel("Reproducir");shot("paused");
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);await(()->plays.get()==1,"play callback");awaitFocusLabel("Pausar");
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT);check("Pausar".equals(focusLabel()),"Unsupported previous cannot take focus");
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);check("Pista siguiente".equals(focusLabel()),"Next reachable");
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);await(()->skips.get()==1,"next callback");awaitText("Segunda pista · Prueba multimedia");shot("next");
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);check("Aplicaciones".equals(focusLabel()),"Up returns to control row");
            final int[] topBefore={0};runOnMainSync(()->{int[] pos=new int[2];findDescription("Televisor.").getLocationOnScreen(pos);topBefore[0]=pos[1];});
            runOnMainSync(()->{
                for(int i=0;i<5;i++)notices.add(new NotificationBuffer.Notice("fixture-"+i,getTargetContext().getPackageName(),"Aviso de prueba "+(i+1),"Contenido sintético para comprobar navegación",i));
                noticesChanged.run();dialog.focusNotifications();
            });shot("notifications-below-music");
            check(hasText("Segunda pista · Prueba multimedia"),"Music remains above notifications");
            checkFocusClear();
            for(int i=0;i<4;i++){sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);checkFocusClear();}
            shot("column-scrolled");
            runOnMainSync(()->{int[] pos=new int[2];findDescription("Televisor.").getLocationOnScreen(pos);check(pos[1]<topBefore[0],"Controls scroll with notification column");});
            for(int i=0;i<8;i++){sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);checkFocusClear();}
            shot("column-top-restored");
            runOnMainSync(()->dialog.focusNotifications());
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);shot("music-restored");
            check(hasText("Segunda pista · Prueba multimedia"),"Back restores music slot");
            runOnMainSync(()->{notices.clear();noticesChanged.run();});shot("notices-cleared");
            check(!hasText("Notificaciones desactivadas")&&!hasText("Sin notificaciones pendientes"),"Empty section disappears");
            runOnMainSync(()->state(other,PlaybackState.STATE_PLAYING));runOnMainSync(()->state(session,PlaybackState.STATE_PAUSED));
            awaitText("Otra sesión pausada");shot("active-session-switch");
            runOnMainSync(()->{other.release();other=null;});awaitText("Segunda pista · Prueba multimedia");
            runOnMainSync(()->{session.release();session=null;});
            await(()->!hasText("Segunda pista · Prueba multimedia"),"Destroyed sessions disappear");shot("sessions-ended");
            runOnMainSync(()->dialog.dismiss());await(()->sourceClosed,"source unregistered on dismiss");
            await(()->!hasMediaThread(),"No media worker remains after dismiss");
            result.putString("result","PASS: framework transport, unsupported action, session selection, D-pad, stacked notifications, whole-column scroll, empty removal and cleanup");
            result.putString("scope","Own synthetic sessions; injected discovery; no Spotify or notification permission test");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("failure",error.toString());finish(Activity.RESULT_CANCELED,result);}
        finally{
            runOnMainSync(()->{if(dialog!=null)dialog.dismiss();});
            if(session!=null)session.release();if(other!=null)other.release();
        }
    }
    private void state(MediaSession target,int value){target.setPlaybackState(new PlaybackState.Builder().setState(value,0,value==PlaybackState.STATE_PLAYING?1:0).setActions(actions).build());}
    private void metadata(MediaSession target,String title){
        Bitmap art=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(art);Paint paint=new Paint();
        paint.setColor(0xff454545);canvas.drawPaint(paint);paint.setColor(0xffeeeeee);paint.setTextSize(90);canvas.drawText("♪",87,165,paint);
        target.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,title).putString(MediaMetadata.METADATA_KEY_ARTIST,"Sesión local de prueba").putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,art).build());
    }
    private interface Condition{boolean get();}
    private void checkFocusClear(){
        waitForIdleSync();runOnMainSync(()->{
            View root=dialog.getWindow().getDecorView(),focus=root.findFocus();check(focus!=null,"Focus present");
            EdgeFadeScrollView scroll=null;for(View v:views(root))if(v instanceof EdgeFadeScrollView)scroll=(EdgeFadeScrollView)v;
            check(scroll!=null&&scroll.getBackground()==null,"Independent cards, no outer box");
            int[] p=new int[2],s=new int[2];focus.getLocationOnScreen(p);scroll.getLocationOnScreen(s);
            int fade=scroll.getVerticalFadingEdgeLength();
            check(p[1]>=s[1]+(scroll.canScrollVertically(-1)?fade:0),"Focus above top mask");
            check(p[1]+focus.getHeight()<=s[1]+scroll.getHeight()-(scroll.canScrollVertically(1)?fade:0),"Focus below bottom mask");
        });
    }
    private void await(Condition condition,String label)throws Exception{for(int i=0;i<50;i++){if(condition.get())return;Thread.sleep(100);}throw new AssertionError(label);}
    private void awaitText(String text)throws Exception{await(()->hasText(text),"Visible metadata: "+text);}
    private void awaitFocusLabel(String text)throws Exception{await(()->text.equals(focusLabel()),"Focused transport: "+text);}
    private String focusLabel(){final String[] value={""};runOnMainSync(()->{View v=dialog.getWindow().getDecorView().findFocus();if(v!=null)value[0]=String.valueOf(v.getContentDescription());});return value[0];}
    private boolean hasText(String text){final boolean[] found={false};runOnMainSync(()->{for(View v:views(dialog.getWindow().getDecorView()))if(v.isShown()&&v instanceof TextView&&text.equals(((TextView)v).getText().toString()))found[0]=true;});return found[0];}
    private List<View> views(View root){List<View> result=new ArrayList<>();result.add(root);if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)result.addAll(views(group.getChildAt(i)));}return result;}
    private View findDescription(String prefix){for(View view:views(dialog.getWindow().getDecorView()))if(view.getContentDescription()!=null&&view.getContentDescription().toString().startsWith(prefix))return view;throw new AssertionError(prefix);}
    private void shot(String name)throws Exception{waitForIdleSync();Thread.sleep(200);Bitmap image=getUiAutomation().takeScreenshot();check(image!=null,"Screenshot");try(FileOutputStream file=new FileOutputStream(new File(output,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,file);}image.recycle();}
    private boolean hasMediaThread(){for(Thread thread:Thread.getAllStackTraces().keySet())if(thread.isAlive()&&"HomeMediaPanel".equals(thread.getName()))return true;return false;}
    private static void check(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
