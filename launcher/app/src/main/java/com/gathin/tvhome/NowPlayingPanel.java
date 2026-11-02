package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** One material, bounded artwork and three explicit D-pad controls. No progress timer or animation loop. */
final class NowPlayingPanel extends LinearLayout {
    private final Activity activity;
    private final TextView title,source;
    private final ImageView art,previous,toggle,next;
    private final NowPlayingSessions sessions;
    private final boolean motion;
    NowPlayingPanel(Activity activity,NowPlayingSessions sessions,boolean motion){
        super(activity);this.activity=activity;this.sessions=sessions;this.motion=motion;
        setOrientation(VERTICAL);setPadding(dp(12),dp(8),dp(12),dp(6));
        setBackground(TvDesign.panel(activity,this));
        LinearLayout info=new LinearLayout(activity);info.setGravity(Gravity.CENTER_VERTICAL);addView(info,new LayoutParams(-1,dp(42)));
        art=new ImageView(activity);art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.setBackground(new android.graphics.drawable.GradientDrawable());
        ((android.graphics.drawable.GradientDrawable)art.getBackground()).setCornerRadius(dp(7));art.setClipToOutline(true);
        LayoutParams picture=new LayoutParams(dp(40),dp(40));picture.rightMargin=dp(10);info.addView(art,picture);
        LinearLayout text=new LinearLayout(activity);text.setOrientation(VERTICAL);text.setGravity(Gravity.CENTER_VERTICAL);info.addView(text,new LayoutParams(0,-1,1));
        title=label(TvDesign.Type.COMPACT);text.addView(title,new LayoutParams(-1,-2));
        source=label(TvDesign.Type.META);source.setTextColor(TvDesign.color(activity,R.color.ds_text_secondary));
        LayoutParams sub=new LayoutParams(-1,-2);sub.topMargin=dp(3);text.addView(source,sub);
        LinearLayout transport=new LinearLayout(activity);transport.setGravity(Gravity.CENTER);LayoutParams bar=new LayoutParams(-1,dp(32));bar.topMargin=dp(4);addView(transport,bar);
        previous=button(transport,R.drawable.ic_media_previous,R.string.media_previous,MediaSessionPolicy.PREVIOUS);
        toggle=button(transport,R.drawable.ic_media_pause,R.string.media_pause,MediaSessionPolicy.TOGGLE);
        next=button(transport,R.drawable.ic_media_next,R.string.media_next,MediaSessionPolicy.NEXT);
        previous.setNextFocusLeftId(previous.getId());next.setNextFocusRightId(next.getId());
    }
    private int dp(int value){return TvDesign.dp(activity,value);}
    private TextView label(TvDesign.Type type){TextView v=new TextView(activity);TvDesign.typography(v,type);v.setSingleLine();v.setEllipsize(android.text.TextUtils.TruncateAt.END);return v;}
    private ImageView button(LinearLayout row,int resource,int description,long action){
        ImageView v=new ImageView(activity);v.setId(View.generateViewId());v.setImageResource(resource);v.setContentDescription(activity.getString(description));
        v.setPadding(dp(17),dp(7),dp(17),dp(7));v.setFocusable(true);v.setClickable(true);
        row.addView(v,new LayoutParams(0,-1,1));
        v.setOnFocusChangeListener((view,focus)->{
            style(v,focus);
            boolean moving=motion&&hasFocus();
            title.setEllipsize(moving?android.text.TextUtils.TruncateAt.MARQUEE:android.text.TextUtils.TruncateAt.END);
            title.setMarqueeRepeatLimit(-1);title.setSelected(moving);
        });style(v,false);
        v.setOnClickListener(view->sessions.command(action));return v;
    }
    private void style(ImageView button,boolean focus){
        button.setBackground(focus?new LiquidGlassDrawable(16,TvDesign.color(activity,R.color.ds_focus_fill)):new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        button.setColorFilter(TvDesign.color(activity,focus?R.color.ds_icon_focused:R.color.ds_icon_primary));
    }
    private void enabled(ImageView v,boolean enabled){v.setEnabled(enabled);v.setFocusable(enabled);v.setAlpha(enabled?1f:.3f);}
    void show(NowPlayingSessions.Snapshot snapshot){
        title.setText(snapshot.title);source.setText(snapshot.artist.isEmpty()?snapshot.source:snapshot.source+" · "+snapshot.artist);
        title.setContentDescription(snapshot.title+". "+snapshot.artist+". "+snapshot.source);
        if(snapshot.artwork==null){art.setScaleType(ImageView.ScaleType.CENTER_INSIDE);art.setImageResource(R.drawable.ic_music_note);art.setColorFilter(TvDesign.color(activity,R.color.ds_icon_primary));}
        else{art.clearColorFilter();art.setScaleType(ImageView.ScaleType.CENTER_CROP);art.setImageBitmap(snapshot.artwork);}
        boolean playing=MediaSessionPolicy.pausable(snapshot.state);
        toggle.setImageResource(playing?R.drawable.ic_media_pause:R.drawable.ic_media_play);
        toggle.setContentDescription(activity.getString(playing?R.string.media_pause:R.string.media_play));
        enabled(previous,MediaSessionPolicy.supports(snapshot.actions,MediaSessionPolicy.PREVIOUS));
        enabled(toggle,MediaSessionPolicy.canToggle(snapshot.state,snapshot.actions));
        enabled(next,MediaSessionPolicy.supports(snapshot.actions,MediaSessionPolicy.NEXT));
        toggle.setNextFocusLeftId(previous.isFocusable()?previous.getId():toggle.getId());
        toggle.setNextFocusRightId(next.isFocusable()?next.getId():toggle.getId());
        previous.setNextFocusRightId(toggle.isFocusable()?toggle.getId():next.isFocusable()?next.getId():previous.getId());
        next.setNextFocusLeftId(toggle.isFocusable()?toggle.getId():previous.isFocusable()?previous.getId():next.getId());
        for(View button:new View[]{previous,toggle,next})button.setNextFocusDownId(button.getId());
        if(hasFocus()&&!previous.hasFocus()&&!toggle.hasFocus()&&!next.hasFocus())focusFirst();
    }
    void clear(){title.setText("");source.setText("");art.setImageDrawable(null);}
    void focusFirst(){if(toggle.isFocusable())toggle.requestFocus();else if(previous.isFocusable())previous.requestFocus();else if(next.isFocusable())next.requestFocus();}
    boolean hasControls(){return toggle.isFocusable()||previous.isFocusable()||next.isFocusable();}
    View lastControl(){return toggle.isFocusable()?toggle:next.isFocusable()?next:previous;}
    void upTo(View left,View right){previous.setNextFocusUpId(left.getId());toggle.setNextFocusUpId(left.getId());next.setNextFocusUpId(right.getId());}
}
