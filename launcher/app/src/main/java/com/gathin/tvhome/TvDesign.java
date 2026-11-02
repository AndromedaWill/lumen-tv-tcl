package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.TextView;

/** Shared native design primitives. No live blur, render effects, services or new dependencies. */
final class TvDesign {
    enum Type {
        TITLE(R.dimen.ds_type_title),SECTION(R.dimen.ds_type_section),BODY(R.dimen.ds_type_body),
        CONTROL(R.dimen.ds_type_control),COMPACT(R.dimen.ds_type_compact),META(R.dimen.ds_type_meta);
        final int resource;Type(int resource){this.resource=resource;}
    }
    static int color(Context c,int resource){return c.getColor(resource);}
    static int dp(Context c,float value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    static float radius(Context c,int resource){return c.getResources().getDimension(resource)/c.getResources().getDisplayMetrics().density;}
    static float dim(Context c){return c.getResources().getInteger(R.integer.ds_modal_dim_percent)/100f;}
    static int focusDuration(Context c){return c.getResources().getInteger(R.integer.ds_motion_focus_ms);}
    static void typography(TextView v,Type type){
        Context c=v.getContext();v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,c.getResources().getDimension(type.resource));
        v.setTypeface(c.getResources().getFont(type==Type.BODY||type==Type.META?R.font.inter_regular:R.font.inter_medium));
        v.setIncludeFontPadding(false);v.setTextColor(color(c,R.color.ds_text_primary));
    }
    static GradientDrawable screen(Context c){return gradient(c,R.color.ds_screen_top,R.color.ds_screen_bottom);}
    static android.graphics.drawable.Drawable panel(Activity c,View host){
        return new FrostedControlDrawable(c,host,false,radius(c,R.dimen.ds_radius_panel));
    }
    static android.graphics.drawable.Drawable overlayPanel(Activity c,View host){
        return new FrostedControlDrawable(c,host,false,radius(c,R.dimen.ds_radius_panel),R.color.ds_overlay_fill);
    }
    static void alertOverlay(Activity activity,android.app.AlertDialog dialog){
        android.view.Window window=dialog.getWindow();
        if(window!=null){
            ContextFrostDrawable frost=new ContextFrostDrawable(activity,window.getDecorView());
            window.setBackgroundDrawable(frost);window.setDimAmount(dim(activity));dialog.setOnDismissListener(ignored->frost.close());
        }
    }
    private static GradientDrawable gradient(Context c,int a,int b){return new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{color(c,a),color(c,b)});}
    static void control(Activity activity,TextView view,float radius){
        controlFrame(activity,view,radius);
        view.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_focused},{android.R.attr.state_pressed},{}},new int[]{color(activity,R.color.ds_text_focused),color(activity,R.color.ds_text_focused),color(activity,R.color.ds_text_primary)}));
    }
    static void controlFrame(Activity activity,View view,float radius){
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},new LiquidGlassDrawable(radius,color(activity,R.color.ds_focus_fill)));
        states.addState(new int[]{android.R.attr.state_pressed},new LiquidGlassDrawable(radius,color(activity,R.color.ds_focus_fill)));
        states.addState(new int[]{android.R.attr.state_selected},new LiquidGlassDrawable(radius,color(activity,R.color.ds_selected_fill)));
        states.addState(new int[]{},new LiquidGlassDrawable(radius,color(activity,R.color.ds_control_fill)));view.setBackground(states);
    }
    static void settingsTree(Activity activity,View root){
        if(root instanceof TextView){TextView text=(TextView)root;
            if(text.isFocusable()){typography(text,Type.BODY);control(activity,text,radius(activity,R.dimen.ds_radius_control));}
            else text.setTypeface(activity.getResources().getFont(R.font.inter_regular));
        }
        if(root instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)root;for(int i=0;i<group.getChildCount();i++)settingsTree(activity,group.getChildAt(i));}
    }
}
