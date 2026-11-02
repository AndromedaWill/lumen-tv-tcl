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
import android.widget.TextView;
import android.widget.Toast;

/** Native interpretation of Lumen's modular Control Center. No privileged toggles or services. */
final class ControlCenterDialog extends Dialog {
    private final Activity activity;
    private final Runnable inputs, customize, notifications;
    private final boolean motion;
    private NotificationsPanel notificationPanel;
    private NowPlayingPanel musicPanel;
    private NowPlayingSessions media;
    private boolean hasMusic;
    private final NowPlayingSessions.Source mediaSource;
    private final NotificationsPanel.Source noticeSource;
    private final java.util.List<View> controls = new java.util.ArrayList<>();

    ControlCenterDialog(Activity activity, Runnable inputs, Runnable customize, Runnable notifications, boolean motion) {
        this(activity,inputs,customize,notifications,motion,null,null);
    }
    ControlCenterDialog(Activity activity,Runnable inputs,Runnable customize,Runnable notifications,boolean motion,NowPlayingSessions.Source mediaSource){
        this(activity,inputs,customize,notifications,motion,mediaSource,null);
    }
    ControlCenterDialog(Activity activity,Runnable inputs,Runnable customize,Runnable notifications,boolean motion,NowPlayingSessions.Source mediaSource,NotificationsPanel.Source noticeSource){
        super(activity);
        this.activity = activity;
        this.inputs = inputs;
        this.customize = customize;
        this.notifications=notifications;
        this.motion = motion;
        this.mediaSource=mediaSource;
        this.noticeSource=noticeSource==null?NotificationsPanel.live(activity):noticeSource;
    }

    private int dp(float n) { return Math.round(n * activity.getResources().getDisplayMetrics().density); }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(10), dp(14), dp(24));
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        LinearLayout row = row(panel);
        add(row, R.drawable.ic_settings, R.string.control_tv, R.string.control_native,
                () -> launch(Settings.ACTION_SETTINGS));
        add(row, R.drawable.ic_wifi, R.string.control_network, R.string.control_open,
                () -> launch(Settings.ACTION_WIFI_SETTINGS));
        row=row(panel);
        add(row,R.drawable.ic_input,R.string.control_inputs,R.string.control_sources,inputs);
        add(row,R.drawable.ic_home_settings,R.string.control_home,R.string.control_customize,customize);
        row=row(panel);
        add(row,R.drawable.ic_home_settings,R.string.control_google_home,
                NativePanels.resolve(activity,NativePanels.HOME)==null?R.string.control_unavailable_short:R.string.control_native_panel,
                ()->NativePanels.open(activity,NativePanels.HOME));
        add(row,R.drawable.ic_bell,R.string.control_notifications,
                noticeSource.current().enabled?R.string.notification_local_viewer:R.string.notification_optional,
                this::focusNotifications);
        controls.get(5).setOnClickListener(v->focusNotifications());
        row=row(panel,38);
        LinearLayout pair=pair(row);
        addCompact(pair,R.drawable.ic_volume,R.string.control_sound,()->launch(Settings.ACTION_SOUND_SETTINGS));
        addCompact(pair,R.drawable.ic_picture,R.string.control_picture,()->launch(Settings.ACTION_DISPLAY_SETTINGS));
        pair=pair(row);
        addCompact(pair,R.drawable.ic_bluetooth,R.string.control_bluetooth,()->launch(Settings.ACTION_BLUETOOTH_SETTINGS));
        addCompact(pair,R.drawable.ic_apps,R.string.control_apps,()->launch(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        notificationPanel=new NotificationsPanel(activity,notifications,noticeSource);
        media=new NowPlayingSessions(activity,this::updateMusic,mediaSource);
        musicPanel=new NowPlayingPanel(activity,media,motion);musicPanel.setVisibility(View.GONE);
        musicPanel.upTo(controls.get(6),controls.get(9));
        LinearLayout.LayoutParams musicParams=new LinearLayout.LayoutParams(-1,-2);musicParams.topMargin=dp(8);panel.addView(musicPanel,musicParams);
        LinearLayout.LayoutParams noticeParams=new LinearLayout.LayoutParams(-1,-2);noticeParams.topMargin=dp(8);panel.addView(notificationPanel,noticeParams);
        notificationPanel.onEmptyFocus=()->{if(hasMusic&&musicPanel.hasControls())musicPanel.focusFirst();else controls.get(5).requestFocus();};
        int[][] positions={{0,1},{2,3},{4,5},{6,7,8,9}};
        for(int r=0;r<positions.length;r++)for(int col=0;col<positions[r].length;col++){
            int[] here=positions[r];View v=controls.get(here[col]);
            v.setNextFocusLeftId(controls.get(here[Math.max(0,col-1)]).getId());
            v.setNextFocusRightId(controls.get(here[Math.min(here.length-1,col+1)]).getId());
            for(int direction:new int[]{-1,1}){
                int targetRow=Math.max(0,Math.min(positions.length-1,r+direction));int[] target=positions[targetRow];
                int targetCol=Math.min(target.length-1,col*target.length/here.length);
                int id=controls.get(targetRow==r?here[col]:target[targetCol]).getId();
                if(direction<0)v.setNextFocusUpId(id);else v.setNextFocusDownId(id);
            }
        }
        EdgeFadeScrollView scroll=new EdgeFadeScrollView(activity);
        scroll.addView(panel);setContentView(scroll);
        controls.get(0).requestFocus();
    }

    @Override public void show() {
        super.show();
        Window w = getWindow();
        if (w == null) return;
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = w.getAttributes();
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dp(40);
        params.y = dp(72);
        params.width = dp(330);
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.dimAmount = TvDesign.dim(activity);
        w.setAttributes(params);
    }

    @Override protected void onStart(){super.onStart();if(media!=null)media.start();}
    @Override protected void onStop(){if(media!=null)media.close();if(musicPanel!=null)musicPanel.clear();super.onStop();}
    private void updateMusic(NowPlayingSessions.Snapshot snapshot){
        boolean focused=musicPanel.hasFocus();hasMusic=snapshot!=null;
        if(snapshot!=null)musicPanel.show(snapshot);else musicPanel.clear();
        musicPanel.setVisibility(hasMusic?View.VISIBLE:View.GONE);
        notificationPanel.upTo(hasMusic&&musicPanel.hasControls()?musicPanel.lastControl():controls.get(8));
        if(focused&&(!hasMusic||!musicPanel.hasControls()))controls.get(6).requestFocus();
    }

    private LinearLayout row(LinearLayout panel) {
        return row(panel,44);
    }
    private LinearLayout row(LinearLayout panel,int heightDp) {
        LinearLayout row = new LinearLayout(activity);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(heightDp));
        lp.topMargin = dp(5);
        panel.addView(row, lp);
        return row;
    }

    private TextView text(int res, float size, int color) {
        TextView t = new TextView(activity);
        t.setText(res);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setTypeface(activity.getResources().getFont(R.font.inter_medium));
        t.setIncludeFontPadding(false);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private void add(LinearLayout row, int icon, int title, int subtitle, Runnable action) {
        LinearLayout tile = new LinearLayout(activity);
        tile.setId(View.generateViewId());
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(10), dp(4), dp(10), dp(4));
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setContentDescription(activity.getString(title) + ". " + activity.getString(subtitle));
        tile.setBackground(new FrostedControlDrawable(activity, tile, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        if (row.getChildCount() > 0) lp.leftMargin = dp(12);
        row.addView(tile, lp);
        ImageView glyph = new ImageView(activity);
        glyph.setImageResource(icon);
        glyph.setColorFilter(TvDesign.color(activity,R.color.ds_icon_primary));
        LinearLayout.LayoutParams glyphParams = new LinearLayout.LayoutParams(dp(21), dp(21));
        glyphParams.rightMargin = dp(8);
        tile.addView(glyph, glyphParams);
        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(title, 13, Color.WHITE);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setMarqueeRepeatLimit(-1);
        labels.addView(name,new LinearLayout.LayoutParams(-1,-2));
        TextView sub = text(subtitle, 11, TvDesign.color(activity,R.color.ds_text_secondary));
        sub.setSingleLine(true);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        sub.setMarqueeRepeatLimit(-1);
        LinearLayout.LayoutParams subParams=new LinearLayout.LayoutParams(-1,-2);subParams.topMargin=dp(1);labels.addView(sub,subParams);
        tile.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        tile.setOnFocusChangeListener((v, focused) -> {
            tile.setBackground(new FrostedControlDrawable(activity, tile, focused));
            name.setEllipsize(focused&&motion?android.text.TextUtils.TruncateAt.MARQUEE:android.text.TextUtils.TruncateAt.END);
            sub.setEllipsize(focused&&motion?android.text.TextUtils.TruncateAt.MARQUEE:android.text.TextUtils.TruncateAt.END);
            name.setSelected(focused&&motion);
            sub.setSelected(focused&&motion);
            name.setTextColor(TvDesign.color(activity,focused?R.color.ds_text_focused:R.color.ds_text_primary));
            sub.setTextColor(TvDesign.color(activity,focused?R.color.ds_text_secondary_focused:R.color.ds_text_secondary));
            glyph.setColorFilter(TvDesign.color(activity,focused?R.color.ds_icon_focused:R.color.ds_icon_primary));
            v.animate().cancel();
            if (motion) v.animate().scaleX(focused ? 1.035f : 1).scaleY(focused ? 1.035f : 1).setDuration(TvDesign.focusDuration(activity)).start();
        });
        tile.setOnClickListener(v -> { dismiss(); action.run(); });
        controls.add(tile);
    }

    private LinearLayout pair(LinearLayout row){
        LinearLayout pair=new LinearLayout(activity);pair.setClipChildren(false);pair.setClipToPadding(false);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
        if(row.getChildCount()>0)p.leftMargin=dp(12);
        row.addView(pair,p);return pair;
    }

    private void addCompact(LinearLayout row,int icon,int title,Runnable action){
        android.widget.FrameLayout tile=new android.widget.FrameLayout(activity);
        tile.setId(View.generateViewId());tile.setFocusable(true);tile.setClickable(true);
        tile.setContentDescription(activity.getString(title));
        boolean left=row.getChildCount()==0;
        float[] radii=left?new float[]{27,27,4,4,4,4,27,27}:new float[]{4,4,27,27,27,27,4,4};
        tile.setBackground(new FrostedControlDrawable(activity,tile,false,radii));
        LinearLayout.LayoutParams position=new LinearLayout.LayoutParams(0,-1,1);
        if(!left)position.leftMargin=dp(6);
        row.addView(tile,position);
        ImageView glyph=new ImageView(activity);glyph.setImageResource(icon);glyph.setColorFilter(TvDesign.color(activity,R.color.ds_icon_primary));
        tile.addView(glyph,new android.widget.FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER));
        tile.setOnFocusChangeListener((v,focused)->{
            tile.setBackground(new FrostedControlDrawable(activity,tile,focused,radii));
            glyph.setColorFilter(TvDesign.color(activity,focused?R.color.ds_icon_focused:R.color.ds_icon_primary));
            v.animate().cancel();
            if(motion)v.animate().scaleX(focused?1.025f:1).scaleY(focused?1.025f:1).setDuration(TvDesign.focusDuration(activity)).start();
        });
        tile.setOnClickListener(v->{dismiss();action.run();});controls.add(tile);
    }

    private void launch(String action) {
        try { activity.startActivity(SystemSettings.intent(activity, action)); }
        catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(activity, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
    }
    void focusNotifications(){if(notificationPanel!=null)notificationPanel.focusFirst();}
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event){
        if(event.getAction()==android.view.KeyEvent.ACTION_DOWN&&event.getKeyCode()==android.view.KeyEvent.KEYCODE_DPAD_DOWN){
            for(int i=6;i<controls.size();i++)if(controls.get(i).hasFocus()){
                if(hasMusic&&musicPanel.hasControls()){musicPanel.focusFirst();return true;}
                notificationPanel.focusIfAny();return true;
            }
            if(musicPanel!=null&&musicPanel.hasFocus()){notificationPanel.focusIfAny();return true;}
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed(){
        if(notificationPanel!=null&&notificationPanel.hasFocus()){
            if(!notificationPanel.back())controls.get(5).requestFocus();
        }else if(musicPanel!=null&&musicPanel.hasFocus()){
            controls.get(6).requestFocus();
        }else super.onBackPressed();
    }
}
