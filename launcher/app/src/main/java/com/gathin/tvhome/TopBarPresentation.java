package com.gathin.tvhome;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.time.LocalTime;

/** Small transient labels and one greeting per activity session; no account access or boot service. */
final class TopBarPresentation {
    private final Activity activity;
    private final FrameLayout root;
    private final TextView label, greeting;
    private static long lastGreeting=-1;
    private final boolean motion;
    private final Runnable fade;
    TopBarPresentation(Activity activity,FrameLayout root,boolean motion){
        this.activity=activity;this.root=root;this.motion=motion;
        label=make(12);greeting=make(19);
        fade=()->{if(motion)greeting.animate().alpha(0).setDuration(500).start();else greeting.setAlpha(0);};
    }
    private int dp(float n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private TextView make(int size){
        TextView v=new TextView(activity);v.setTextColor(0xEEFFFFFF);v.setTextSize(size);v.setSingleLine(true);
        v.setGravity(Gravity.CENTER);v.setIncludeFontPadding(false);v.setAlpha(0);v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(v,new FrameLayout.LayoutParams(-2,dp(28)));return v;
    }
    void focus(View button,boolean focused){
        if(!focused){label.setAlpha(0);return;}
        if(button.getId()==R.id.clockBlock)return;
        label.setText(button.getContentDescription());label.measure(0,0);
        int[] pos=new int[2],origin=new int[2];button.getLocationInWindow(pos);root.getLocationInWindow(origin);
        label.setTranslationX(Math.max(dp(16),Math.min(root.getWidth()-label.getMeasuredWidth()-dp(16),
                pos[0]-origin[0]+(button.getWidth()-label.getMeasuredWidth())/2f)));
        label.setTranslationY(pos[1]-origin[1]+button.getHeight()-dp(1));label.setAlpha(1);
    }
    void welcome(){
        long now=android.os.SystemClock.elapsedRealtime();
        if((lastGreeting>=0 && now-lastGreeting<30*60_000L)||!activity.hasWindowFocus())return;
        if(activity.findViewById(R.id.clockBlock)==null)return;
        lastGreeting=now;
        String name=activity.getSharedPreferences("home_profile",0).getString("name","Will");
        int hour=LocalTime.now().getHour();int res=hour<5||hour>=19?R.string.good_night:hour<12?R.string.good_morning:R.string.good_afternoon;
        greeting.setEllipsize(android.text.TextUtils.TruncateAt.END);
        greeting.setMaxWidth(Math.max(dp(100),root.getWidth()-dp(500)));
        greeting.setText(name.isEmpty()?activity.getString(res):activity.getString(R.string.greeting_with_name,activity.getString(res),name));
        root.post(()->{
            if(!activity.hasWindowFocus())return;
            View clock=activity.findViewById(R.id.clockBlock);int[] p=new int[2],o=new int[2];
            clock.getLocationInWindow(p);root.getLocationInWindow(o);
            greeting.setTranslationX(p[0]-o[0]+clock.getWidth()+dp(26));greeting.setTranslationY(p[1]-o[1]+dp(8));
            greeting.setAlpha(1);root.postDelayed(fade,4200);
        });
    }
    void stop(){root.removeCallbacks(fade);greeting.animate().cancel();greeting.setAlpha(0);label.setAlpha(0);}
    void hideLabel(){label.setAlpha(0);}
}
