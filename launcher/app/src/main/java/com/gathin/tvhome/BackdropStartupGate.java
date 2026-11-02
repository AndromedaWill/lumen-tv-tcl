package com.gathin.tvhome;

import android.view.View;
import android.view.ViewTreeObserver;

/** First draw only: never expose the fallback landscape while the selected media is being prepared. */
final class BackdropStartupGate implements ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
    private final View root;
    private final Runnable timeout=this::open;
    private boolean waiting=true;
    private BackdropStartupGate(View root){
        this.root=root;root.setTag(R.id.backdrop_startup_gate,this);
        root.getViewTreeObserver().addOnPreDrawListener(this);root.addOnAttachStateChangeListener(this);
        // Broken files must not trap navigation behind an indefinitely cancelled first frame.
        root.postDelayed(timeout,1500);
    }
    static void arm(View root){new BackdropStartupGate(root);}
    static void ready(View root){Object gate=root.getTag(R.id.backdrop_startup_gate);if(gate instanceof BackdropStartupGate)((BackdropStartupGate)gate).open();}
    private void open(){
        if(!waiting)return;waiting=false;root.removeCallbacks(timeout);
        if(root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnPreDrawListener(this);
        root.removeOnAttachStateChangeListener(this);root.setTag(R.id.backdrop_startup_gate,null);root.invalidate();
    }
    @Override public boolean onPreDraw(){return !waiting;}
    @Override public void onViewAttachedToWindow(View v){}
    @Override public void onViewDetachedFromWindow(View v){open();}
}
