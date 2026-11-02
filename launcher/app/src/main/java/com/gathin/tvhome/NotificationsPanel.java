package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Compact, on-demand viewer inside Control Center. Permission disclosure expands only on request. */
final class NotificationsPanel extends ScrollView {
    private final Activity activity;
    private final Runnable configure;
    private final LinearLayout body;
    private final Runnable changed=this::render;
    private boolean details;
    private String expanded;
    private final Map<String,android.graphics.drawable.Drawable> icons=new HashMap<>();
    private View first;
    NotificationsPanel(Activity activity,Runnable configure){
        super(activity);this.activity=activity;this.configure=configure;setFillViewport(false);setVerticalScrollBarEnabled(false);
        setClipChildren(true);setClipToPadding(true);setOverScrollMode(OVER_SCROLL_NEVER);
        body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);addView(body);render();
    }
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    @Override protected void onMeasure(int w,int h){
        int limit=dp(100);
        if(MeasureSpec.getMode(h)!=MeasureSpec.UNSPECIFIED)limit=Math.min(limit,MeasureSpec.getSize(h));
        super.onMeasure(w,MeasureSpec.makeMeasureSpec(limit,MeasureSpec.AT_MOST));
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        // The Control Center deliberately permits focus elevation outside its rows. This viewport
        // must still clip its scrolled descendants, including backgrounds, to its own fixed region.
        setClipBounds(new android.graphics.Rect(0,0,w,h));
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();HomeNotificationService.observe(changed);render();}
    @Override protected void onDetachedFromWindow(){HomeNotificationService.observe(null);icons.clear();super.onDetachedFromWindow();}
    private TextView text(String value,int size,int color){TextView v=new TextView(activity);TvDesign.typography(v,size==12?TvDesign.Type.META:TvDesign.Type.COMPACT);v.setText(value);v.setTextColor(color);return v;}
    private TextView action(String title,String subtitle,Runnable click){
        TextView v=text(title+(subtitle.isEmpty()?"":"\n"+subtitle),13,Color.WHITE);v.setId(View.generateViewId());v.setTag(title);
        v.setMaxLines(2);v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(dp(12),dp(5),dp(12),dp(5));
        v.setFocusable(true);v.setClickable(true);style(v,false);v.setOnFocusChangeListener((x,focus)->style(v,focus));v.setOnClickListener(x->click.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(44));p.topMargin=dp(2);body.addView(v,p);
        if(first==null)first=v;return v;
    }
    private void style(TextView v,boolean focus){
        v.setBackground(new FrostedControlDrawable(activity,v,focus));v.setTextColor(TvDesign.color(activity,focus?R.color.ds_text_focused:R.color.ds_text_primary));
    }
    void focusFirst(){if(first!=null)first.requestFocus();}
    private void restoreFocus(Object tag){View view=tag==null?null:body.findViewWithTag(tag);if(view!=null)view.requestFocus();}
    boolean back(){if(!details)return false;details=false;render();focusFirst();return true;}
    private void render(){
        Object focused=body.findFocus()==null?null:body.findFocus().getTag();body.removeAllViews();first=null;
        boolean access=HomeNotificationService.hasAccess(activity),enabled=HomeNotificationService.enabled(activity);
        setBackground(details?TvDesign.panel(activity,this):null);setClipToOutline(details);
        int padding=details?dp(12):0;body.setPadding(padding,details?dp(10):0,padding,details?dp(10):0);
        if(details||enabled)body.addView(text(activity.getString(R.string.control_notifications),13,TvDesign.color(activity,R.color.ds_text_secondary)));
        if(!enabled)icons.clear();
        if(!access){HomeNotificationService.BUFFER.clear();if(HomeNotificationService.prefs(activity).getBoolean("enabled",false))HomeNotificationService.setEnabled(activity,false);}
        if(details){
            boolean connected=HomeNotificationService.connected();
            TextView explanation=text(activity.getString(!access?R.string.notification_permission_explanation:enabled?(connected?R.string.notification_active_explanation:R.string.notification_disconnected_explanation):R.string.notification_paused_explanation),12,TvDesign.color(activity,R.color.ds_text_secondary));
            explanation.setPadding(0,dp(8),0,dp(4));explanation.setFocusable(true);explanation.setTag("permission-explanation");body.addView(explanation);first=explanation;
            action(activity.getString(access?R.string.notification_manage_access:R.string.notification_configure_access),"",configure);
            if(enabled&&!connected)action(activity.getString(R.string.notification_reconnect),"",()->{HomeNotificationService.setEnabled(activity,true);render();});
            if(access)action(activity.getString(enabled?R.string.notification_pause:R.string.notification_enable),"",()->{HomeNotificationService.setEnabled(activity,!enabled);render();});
            if(enabled){boolean quiet=HomeNotificationService.prefs(activity).getBoolean("show_quiet",false);
                action(activity.getString(quiet?R.string.notification_quiet_shown:R.string.notification_quiet_hidden),"",()->{HomeNotificationService.prefs(activity).edit().putBoolean("show_quiet",!quiet).apply();HomeNotificationService.refreshFilters();render();});}
        }else if(!enabled){
            action(access?"Visor pausado":"Notificaciones desactivadas","",()->{details=true;render();focusFirst();});
        }else{
            List<NotificationBuffer.Notice> notices=HomeNotificationService.BUFFER.snapshot();
            if(!HomeNotificationService.connected()){
                action(activity.getString(R.string.notification_disconnected),"",()->{details=true;render();focusFirst();});
                restoreFocus(focused);
                return;
            }
            if(notices.isEmpty())action("Sin notificaciones pendientes","Opciones",()->{details=true;render();focusFirst();});
            for(NotificationBuffer.Notice n:notices){
                String name;try{name=String.valueOf(activity.getPackageManager().getApplicationLabel(activity.getPackageManager().getApplicationInfo(n.pkg,0)));}catch(Exception unavailable){name=n.pkg;}
                String source=Cards.cleanLabel(name,n.pkg);
                TextView v=action(n.title.isEmpty()?source:n.title,source+" · "+n.body,()->{expanded=n.key.equals(expanded)?null:n.key;render();});
                v.setTag("notice:"+n.key);
                if(n.key.equals(expanded)){v.setMaxLines(8);v.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;}
                v.setContentDescription(source+". "+n.title+". "+n.body);
                try{android.graphics.drawable.Drawable icon=icons.get(n.pkg);if(icon==null){icon=activity.getPackageManager().getApplicationIcon(n.pkg);icons.put(n.pkg,icon);}icon.setBounds(0,0,dp(20),dp(20));v.setCompoundDrawables(icon,null,null,null);v.setCompoundDrawablePadding(dp(8));}catch(Exception unavailable){ }
            }
            Set<String> current=new HashSet<>();for(NotificationBuffer.Notice n:notices)current.add(n.pkg);icons.keySet().retainAll(current);
            if(!notices.isEmpty())action("Opciones","",()->{details=true;render();focusFirst();});
        }
        restoreFocus(focused);
    }
}
