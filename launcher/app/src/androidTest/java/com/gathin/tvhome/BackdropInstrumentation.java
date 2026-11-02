package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;

/** Disposable emulator: same-video blur and real pause/resume, no grants or HOME changes. */
public final class BackdropInstrumentation extends Instrumentation {
    private Activity activity;
    private File output;
    private String saved;
    private boolean hadSaved;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private interface Condition{boolean ok()throws Exception;}
    private void await(Condition test,String label)throws Exception{
        for(int i=0;i<60;i++){if(test.ok())return;Thread.sleep(100);}throw new AssertionError(label);
    }
    private Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private void shot(String name)throws Exception{
        Thread.sleep(300);waitForIdleSync();Bitmap b=getUiAutomation().takeScreenshot();
        try(FileOutputStream stream=new FileOutputStream(new File(output,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,stream);}b.recycle();
    }
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            if(!android.os.Build.HARDWARE.matches(".*(ranchu|goldfish).*"))throw new AssertionError("Emulator only");
            Context context=getTargetContext();output=new File(context.getExternalFilesDir(null),"backdrop-review");output.mkdirs();
            hadSaved=BackgroundStore.prefs(context).contains("collection");saved=BackgroundStore.prefs(context).getString("collection",null);
            BackgroundStore s=BackgroundStore.defaults(context);s.mode=BackgroundStore.FIXED;s.fixed="default-2";s.save(context);
            activity=startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            await(()->activity.findViewById(R.id.root).getBackground() instanceof ScenicBackdropDrawable,"Photo ready");
            shot("photo-dock");sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);Thread.sleep(800);shot("photo-grid");
            View root=activity.findViewById(R.id.root);
            if((float)field(root.getBackground(),"blend")<.99f)throw new AssertionError("Photo grid veil visible");
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            File video=new File(BackgroundStore.directory(context),"emulator-review.mp4");
            Files.copy(new File(context.getExternalFilesDir(null),"fixture.mp4").toPath(),video.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            s.items.add(new BackgroundStore.Item("emulator-review","Video local","file:emulator-review.mp4",true,false,0));s.fixed="emulator-review";s.save(context);
            runOnMainSync(()->{try{((PersonalBackground)field(activity,"personalBackground")).update();}catch(Exception e){throw new RuntimeException(e);}});
            await(()->root.getTag(R.id.video_still_owner)!=null&&((VideoStillCover)root.getTag(R.id.video_still_owner)).hasStill(),"Video frame");
            Thread.sleep(1200);shot("video-dock");sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
            PersonalBackground personal=(PersonalBackground)field(activity,"personalBackground");AerialBackdrop aerial=(AerialBackdrop)field(personal,"video");
            View veil=(View)field(aerial,"gridDim");
            await(()->field(veil,"image")!=null&&veil.getAlpha()>.99f,"Same-video blur visible after D-pad Down");shot("video-grid");
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(400);
            for(int i=0;i<3;i++){
                runOnMainSync(()->aerial.beforeAppLaunch(()->getTargetContext().startActivity(new Intent().setClassName("pe.tclhome.fixture.files","pe.tclhome.fixture.FixtureActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK))));
                await(()->field(aerial,"player")==null,"Decoder released away");Thread.sleep(1000);
                try(android.os.ParcelFileDescriptor.AutoCloseInputStream stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand("input keyevent KEYCODE_BACK"))){while(stream.read()!=-1){}}
                Thread.sleep(1000);
                await(()->field(aerial,"player")!=null,"Decoder resumes");shot("video-return-"+i);
            }
            result.putString("result","PASS: photo deferred crossfade, same-video grid blur via D-pad, decoder released/resumed across three app cycles");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("failure",error.toString());finish(Activity.RESULT_CANCELED,result);}
        finally{
            if(hadSaved)BackgroundStore.prefs(getTargetContext()).edit().putString("collection",saved).commit();
            else BackgroundStore.prefs(getTargetContext()).edit().remove("collection").commit();
        }
    }
}
