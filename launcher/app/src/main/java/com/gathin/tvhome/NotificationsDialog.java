package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.LinkedHashMap;
import java.util.List;

/** Secondary, local notification viewer. Permission and local switch are separate, explicit actions. */
final class NotificationsDialog extends Dialog {
    private final Activity activity;
    private LinearLayout body;
    private ScrollView scroll;
    private final Runnable configureAction;
    private final Runnable changed=this::render;
    private final java.util.Map<String,android.graphics.drawable.Drawable> icons=new java.util.HashMap<>();
    NotificationsDialog(Activity activity,Runnable configure){super(activity);this.activity=activity;this.configureAction=configure;}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){
        TextView v=new TextView(activity);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setIncludeFontPadding(false);return v;
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);requestWindowFeature(Window.FEATURE_NO_TITLE);
        scroll=new ScrollView(activity){
            @Override protected void onMeasure(int width,int height){super.onMeasure(width,View.MeasureSpec.makeMeasureSpec(dp(400),View.MeasureSpec.AT_MOST));}
        };
        scroll.setClipToPadding(false);
        body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(22),dp(20),dp(22),dp(20));
        body.setBackground(new FrostedControlDrawable(activity,body,false));scroll.addView(body);setContentView(scroll);
        Window w=getWindow();if(w!=null){
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setLayout(dp(440),WindowManager.LayoutParams.WRAP_CONTENT);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams p=w.getAttributes();p.dimAmount=.65f;p.gravity=Gravity.CENTER;w.setAttributes(p);
        }
    }
    @Override protected void onStart(){super.onStart();HomeNotificationService.observe(changed);render();}
    @Override protected void onStop(){HomeNotificationService.observe(null);icons.clear();super.onStop();}
    private void button(String title,Runnable click){
        TextView v=text(title,16,Color.WHITE);v.setTag(title);v.setFocusable(true);v.setClickable(true);v.setGravity(Gravity.CENTER_VERTICAL);
        v.setPadding(dp(14),0,dp(14),0);v.setBackgroundResource(R.drawable.settings_choice_bg);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(40));p.topMargin=dp(8);body.addView(v,p);v.setOnClickListener(view->click.run());
    }
    private void configure(){configureAction.run();}
    private void render(){
        if(body==null)return;
        View old=body.findFocus();Object tag=old==null?null:old.getTag();int y=scroll.getScrollY();body.removeAllViews();
        TextView title=text(activity.getString(R.string.control_notifications),25,Color.WHITE);body.addView(title);
        boolean access=HomeNotificationService.hasAccess(activity),enabled=HomeNotificationService.enabled(activity);
        if(!access){
            HomeNotificationService.BUFFER.clear();
            if(HomeNotificationService.prefs(activity).getBoolean("enabled",false))
                HomeNotificationService.prefs(activity).edit().putBoolean("enabled",false).apply();
        }
        if(!enabled)icons.clear();
        String explanation=activity.getString(!access?R.string.notification_permission_explanation:enabled?R.string.notification_active_explanation:R.string.notification_paused_explanation);
        TextView info=text(explanation,13,0xDDE9EFF8);info.setPadding(0,dp(10),0,dp(6));body.addView(info);
        button(activity.getString(access?R.string.notification_manage_access:R.string.notification_configure_access),this::configure);
        if(access)button(activity.getString(enabled?R.string.notification_pause:R.string.notification_enable),()->{
            HomeNotificationService.setEnabled(activity,!HomeNotificationService.enabled(activity));render();
        });
        if(enabled){
            boolean quiet=HomeNotificationService.prefs(activity).getBoolean("show_quiet",false);
            button(activity.getString(quiet?R.string.notification_quiet_shown:R.string.notification_quiet_hidden),()->{
                HomeNotificationService.prefs(activity).edit().putBoolean("show_quiet",!quiet).apply();HomeNotificationService.refreshFilters();render();
            });
            List<NotificationBuffer.Notice> notices=HomeNotificationService.BUFFER.snapshot();
            if(notices.isEmpty()){
                TextView empty=text(activity.getString(R.string.notification_empty),15,0xCCDDE7F4);empty.setPadding(0,dp(18),0,dp(8));body.addView(empty);
            }
            LinkedHashMap<String,java.util.List<NotificationBuffer.Notice>> groups=new LinkedHashMap<>();
            for(NotificationBuffer.Notice notice:notices)groups.computeIfAbsent(notice.pkg,k->new java.util.ArrayList<>()).add(notice);
            icons.keySet().retainAll(groups.keySet());
            for(java.util.Map.Entry<String,java.util.List<NotificationBuffer.Notice>> group:groups.entrySet()){
                String name=appName(group.getKey());TextView source=text(name,14,0xCCDAE7F5);source.setPadding(dp(4),dp(18),0,dp(6));body.addView(source);
                for(NotificationBuffer.Notice notice:group.getValue())notice(notice,name);
            }
        }
        View restored=tag==null?null:body.findViewWithTag(tag);
        if(restored!=null){restored.requestFocus();scroll.post(()->scroll.scrollTo(0,y));}
        else if(body.getChildCount()>2)body.getChildAt(2).requestFocus();
    }
    private String appName(String pkg){
        try{return Cards.cleanLabel(String.valueOf(activity.getPackageManager().getApplicationLabel(activity.getPackageManager().getApplicationInfo(pkg,0))),pkg);}
        catch(android.content.pm.PackageManager.NameNotFoundException|RuntimeException hidden){return pkg;}
    }
    private android.graphics.drawable.Drawable icon(String pkg){
        if(icons.containsKey(pkg))return icons.get(pkg);
        android.graphics.drawable.Drawable value;
        try{value=activity.getPackageManager().getApplicationIcon(pkg);}
        catch(android.content.pm.PackageManager.NameNotFoundException|RuntimeException hidden){value=activity.getDrawable(R.drawable.ic_bell);}
        icons.put(pkg,value);return value;
    }
    private void notice(NotificationBuffer.Notice notice,String source){
        LinearLayout row=new LinearLayout(activity);row.setTag("notice:"+notice.key);row.setFocusable(true);row.setClickable(true);
        row.setPadding(dp(12),dp(10),dp(12),dp(10));row.setBackgroundResource(R.drawable.settings_choice_bg);
        row.setGravity(Gravity.TOP);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(6);body.addView(row,p);
        ImageView icon=new ImageView(activity);icon.setImageDrawable(icon(notice.pkg));row.addView(icon,new LinearLayout.LayoutParams(dp(30),dp(30)));
        LinearLayout words=new LinearLayout(activity);words.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);wp.leftMargin=dp(12);row.addView(words,wp);
        TextView title=text(notice.title.isEmpty()?source:notice.title,17,Color.WHITE);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(title);
        TextView message=text(notice.body,14,0xFFD1DDEB);message.setMaxLines(3);message.setEllipsize(android.text.TextUtils.TruncateAt.END);message.setPadding(0,dp(5),0,dp(4));words.addView(message);
        TextView time=text(android.text.format.DateFormat.getTimeFormat(activity).format(new java.util.Date(notice.time)),11,0xBBD3DFED);words.addView(time);
        row.setOnClickListener(v->message.setMaxLines(message.getMaxLines()==3?10:3));
    }
}
