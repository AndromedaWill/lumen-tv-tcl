package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.FrameLayout;
import android.media.MediaMetadataRetriever;
import java.io.File;

/** One same-video still. No launch barrier, screenshots of other apps, or background polling. */
final class VideoStillCover extends View {
    private final SurfaceView video;
    private final FrameLayout root;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF dest = new RectF();
    private final VideoCoverState state = new VideoCoverState();
    private Bitmap still;
    private boolean copying, closed;
    private int attempts, posterGeneration, revealTicket;
    private Runnable ready, copied, pendingLaunch;
    private final Runnable launchTimeout=this::finishLaunch;
    private final Runnable retry = this::capture;
    private final Runnable reveal = () -> {
        if (state.canReveal(revealTicket)) { state.revealed(revealTicket); setAlpha(0); }
    };

    VideoStillCover(Context c, FrameLayout root, SurfaceView video) {
        super(c); this.root=root; this.video = video; setWillNotDraw(false);
    }

    void poster(File file, int position) {
        final int generation = ++posterGeneration;
        new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            Bitmap frame = null; MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(file.getPath());
                frame = Build.VERSION.SDK_INT >= 27
                        ? retriever.getScaledFrameAtTime(Math.max(0, position) * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 960, 540)
                        : retriever.getFrameAtTime(Math.max(0, position) * 1000L);
            } catch (Exception | OutOfMemoryError unavailable) {
                // Preserve the current composition; never substitute another photograph.
            } finally { try { retriever.release(); } catch (Exception ignored) { } }
            Bitmap ready = frame;
            post(() -> {
                if (ready == null) return;
                if (closed || generation != posterGeneration || still != null) ready.recycle();
                else { still = ready; invalidate(); signalReady(); }
            });
        }, "home-video-poster").start();
    }

    /** Keep the still until PixelCopy verifies that the live surface owns a frame. */
    void begin() { removeCallbacks(reveal); state.begin(); attempts = 0; capture(); }

    /** Copy before teardown; the asynchronous hand-off is bounded to 48 ms. */
    void beforeAppLaunch(Runnable launch) {
        if(pendingLaunch!=null)return;
        removeCallbacks(reveal);removeCallbacks(retry);
        pendingLaunch=launch;attempts=0;capture();postDelayed(launchTimeout,48);
    }
    private void finishLaunch(){
        Runnable launch=pendingLaunch;pendingLaunch=null;removeCallbacks(launchTimeout);
        if(launch!=null&&!closed)launch.run();
    }
    void refreshSnapshot(Runnable action){copied=action;attempts=0;capture();}

    private void capture() {
        if (!state.acceptsCopy(state.ticket()) || copying || !video.isShown() || !video.getHolder().getSurface().isValid() || attempts >= 2) return;
        copying = true; attempts++;
        int ticket = state.ticket();
        int w = Math.min(1280, Math.max(1, video.getWidth()));
        int h = Math.max(1, Math.round(w * (float) Math.max(1, video.getHeight()) / Math.max(1, video.getWidth())));
        new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            Bitmap bitmap;
            try { bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); }
            catch (OutOfMemoryError unavailable) { post(() -> copying = false); return; }
            post(() -> {
                if (!state.acceptsCopy(ticket) || !video.isShown()) {
                    copying = false;bitmap.recycle();
                    if(state.waiting()&&video.isShown())post(retry);
                    return;
                }
                try {
                    PixelCopy.request(video, bitmap, result -> {
                        copying = false;
                        if (state.acceptsCopy(ticket) && result == PixelCopy.SUCCESS) {
                            // RenderThread may still own the old still. GC retires published bitmaps.
                            still = bitmap; invalidate();
                            signalReady();
                            Runnable action=copied;copied=null;if(action!=null)action.run();
                            if(pendingLaunch!=null){finishLaunch();return;}
                            if (state.canReveal(ticket)) {
                                revealTicket = ticket; postOnAnimation(reveal);
                            } else if (state.waiting()) capture();
                        } else {
                            bitmap.recycle();
                            if (!closed && state.waiting() && video.isShown() && attempts < 2) postDelayed(retry, 120);
                        }
                    }, new Handler(Looper.getMainLooper()));
                } catch (RuntimeException unavailable) { copying = false; bitmap.recycle(); }
            });
        }, "home-video-still").start();
    }

    void showStill() {
        state.hold(); removeCallbacks(retry); removeCallbacks(reveal);copied=null;
        animate().cancel(); setAlpha(1); invalidate();
    }
    boolean hasStill(){return still!=null;}
    Bitmap snapshot(){return still;}
    void whenReady(Runnable action){ready=action;if(still!=null)signalReady();}
    private void signalReady(){
        BackdropStartupGate.ready(root);Runnable action=ready;ready=null;if(action!=null&&!closed)action.run();
    }
    void close() {
        closed = true; state.close(); posterGeneration++; ready=null;
        copied=null;pendingLaunch=null;removeCallbacks(launchTimeout);
        removeCallbacks(retry); removeCallbacks(reveal); still = null;
    }
    @Override protected void onDraw(Canvas canvas) {
        if(still!=null){
            float scale=Math.max((float)getWidth()/still.getWidth(),(float)getHeight()/still.getHeight());
            float w=still.getWidth()*scale,h=still.getHeight()*scale;
            dest.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);
            canvas.drawBitmap(still,null,dest,paint);
        }
    }
}
