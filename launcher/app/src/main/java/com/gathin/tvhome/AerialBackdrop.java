package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.File;

/**
 * An Apple aerial looping behind Home, as on tvOS. The video goes to a SurfaceView, which the TV composes
 * as a hardware overlay: no GPU work per frame for the launcher. Above it lie a cover that fades out once
 * the first frame is up (no flash of black or a frozen frame) and two scrims that keep the clock and the
 * icons readable; the lower one deepens while focus is in the app grid, where tvOS blurs the shelf away.
 *
 * The decoder is released whenever Home is not in front: TVs have one or two hardware decoders, and an app
 * opened from Home must never find them taken. The position is kept, so the flight picks up where it left.
 */
final class AerialBackdrop implements SurfaceHolder.Callback {
    private static final String TAG = "TvHome.Aerial";

    /** Not final: cycling swaps in the next aerial. */
    private File file;
    private final VideoStillCover cover;
    private final SurfaceView video;
    private final FrameLayout root;
    private final java.util.List<View> layers=new java.util.ArrayList<>();
    private final GridBackdropView gridDim;
    private final SharedPreferences prefs;
    private MediaPlayer player;
    private SurfaceHolder surface;
    private boolean resumed;
    private boolean dimmed;
    private boolean pausedForGrid;
    private final Runnable pauseGridPlayback = this::pauseVideoInGrid;

    private void pauseVideoInGrid() {
        if (resumed && dimmed && player != null && player.isPlaying()) {
            player.pause();
            pausedForGrid = true;
        }
    }

    private static final String PREFS = "aerial_backdrop", CHOSEN = "file", POSITION = "position";

    /**
     * The aerials on the TV, as {file, title}: the titles from aerials.json next to the videos (tools put it
     * there with them), then any other .mov or .mp4 in the folder under its file name. For the Background menu.
     */
    static java.util.List<String[]> catalog(Context context) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        File dir = context.getExternalFilesDir("aerials");
        if (dir == null) return out;
        java.util.Set<String> seen = new java.util.HashSet<>();
        File json = new File(dir, "aerials.json");
        if (json.isFile()) {
            try {
                String text = new String(java.nio.file.Files.readAllBytes(json.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                if (text.startsWith("\uFEFF")) text = text.substring(1);   // written by PowerShell, with a BOM
                org.json.JSONArray a = new org.json.JSONArray(text);
                for (int i = 0; i < a.length(); i++) {
                    org.json.JSONObject o = a.getJSONObject(i);
                    String f = o.optString("file");
                    if (f.isEmpty() || !new File(dir, f).canRead() || !seen.add(f)) continue;
                    out.add(new String[]{f, o.optString("title", f)});
                }
            } catch (Exception e) {
                Log.w(TAG, "cannot read aerials.json", e);
            }
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(".mov") || n.endsWith(".mp4"));
        if (files != null) {
            java.util.Arrays.sort(files);
            for (File f : files) {
                if (seen.add(f.getName())) out.add(new String[]{f.getName(), f.getName().replaceAll("\\.\\w+$", "").replace('_', ' ')});
            }
        }
        return out;
    }

    /** The Background menu's "Cycle through all": each aerial plays once, then the next, round the catalog. */
    static final String CYCLE = "*cycle*";
    /** While cycling: the aerial playing now, so Home picks up the rotation where it left it. */
    private static final String CYCLE_FILE = "cycle_file";

    /**
     * The aerial picked in the Background menu while it is still on the TV, CYCLE when cycling, else the
     * configured one.
     */
    static String chosen(Context context, String configured) {
        String f = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CHOSEN, null);
        if (CYCLE.equals(f)) return CYCLE;
        File dir = context.getExternalFilesDir("aerials");
        return f != null && dir != null && new File(dir, f).canRead() ? f : configured;
    }

    /** Remembers the Background menu's pick (a file, or CYCLE); the new flight starts from its beginning. */
    static void choose(Context context, String file) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CHOSEN, file).remove(POSITION).apply();
    }

    /**
     * Null when the configured aerial is not on the TV: Home then keeps its wallpaper or plain colour. fileName
     * CYCLE starts the rotation at the aerial it last reached.
     */
    static AerialBackdrop attach(Context context, FrameLayout root, int index, String fileName, int coverColor) {
        if (fileName == null || fileName.isEmpty()) return null;
        File dir = context.getExternalFilesDir("aerials");
        if (dir == null) return null;
        boolean cycle = CYCLE.equals(fileName);
        if (cycle) {
            java.util.List<String[]> all = catalog(context);
            if (all.isEmpty()) return null;
            String at = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CYCLE_FILE, null);
            fileName = at != null && new File(dir, at).canRead() ? at : all.get(0)[0];
        }
        File f = new File(dir, fileName);
        if (!f.canRead()) return null;
        return new AerialBackdrop(context, root, index, f, coverColor, cycle);
    }
    static AerialBackdrop attachFile(Context c,FrameLayout root,int index,File file){
        if(file!=null){SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);if(!file.getName().equals(p.getString("manual_file","")))p.edit().putString("manual_file",file.getName()).remove(POSITION).apply();}
        return file!=null&&file.canRead()?new AerialBackdrop(c,root,index,file,0xFF121A28,false):null;
    }

    private final Context context;
    private final boolean cycle;

    /** Cycling: the aerial after the one playing, in catalog order, wrapping round. */
    private File nextInCycle() {
        java.util.List<String[]> all = catalog(context);
        if (all.isEmpty()) return file;
        int i = 0;
        for (int k = 0; k < all.size(); k++) if (all.get(k)[0].equals(file.getName())) i = k + 1;
        return new File(file.getParentFile(), all.get(i % all.size())[0]);
    }

    /** Hold this video's sampled frame until the next video has rendered. */
    private void advance() {
        File next = nextInCycle();
        Log.i(TAG, "cycle: " + file.getName() + " -> " + next.getName());
        cover.showStill();release();
        prefs.edit().putString(CYCLE_FILE, next.getName()).remove(POSITION).apply();
        file=next;start();
    }

    private AerialBackdrop(Context context, FrameLayout root, int index, File file, int coverColor, boolean cycle) {
        this.root=root;
        root.setTag(R.id.background_video_visible,Boolean.TRUE);
        ScenicBackdropDrawable.setLower(root,Boolean.TRUE.equals(root.getTag(R.id.grid)),false);
        this.file = file;
        this.context = context.getApplicationContext();
        this.cycle = cycle;
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        FrameLayout.LayoutParams full = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        video = new SurfaceView(context);
        video.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        video.getHolder().addCallback(this);
        root.addView(video, index, full);
        layers.add(video);

        // The still and live surface share the same shades, avoiding a brightness jump on pause.
        cover = new VideoStillCover(context,root,video);
        root.setTag(R.id.video_still_owner,cover);
        cover.poster(file,prefs.getInt(POSITION,0));
        cover.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(cover, index + 1, full);
        layers.add(cover);

        // Top: a light shade under the clock and pills. Bottom: deeper, under the dock and the grid.
        View top = new View(context);
        top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x8C000000, 0x26000000, 0x00000000}));
        root.addView(top, index + 2, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (context.getResources().getDisplayMetrics().heightPixels * 0.42f)));
        layers.add(top);
        View bottom = new View(context);
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xB3000000, 0x40000000, 0x00000000}));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (context.getResources().getDisplayMetrics().heightPixels * 0.62f));
        lp.gravity = android.view.Gravity.BOTTOM;
        root.addView(bottom, index + 3, lp);
        layers.add(bottom);
        for (View v : new View[]{top, bottom}) {
            v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            v.forceHasOverlappingRendering(false);
        }

        gridDim = new GridBackdropView(context, 0x9E05070C);
        gridDim.setWillNotDraw(false);
        gridDim.setAlpha(0f);
        gridDim.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(gridDim, index + 4, full);
        layers.add(gridDim);

    }

    /** Focus in the grid below the dock: dim the aerial so the icons own the screen, as tvOS does. */
    void setGridFocused(boolean inGrid) {
        setGridFocused(inGrid,true);
    }
    void setGridFocused(boolean inGrid,boolean animate) {
        if (inGrid == dimmed) return;
        dimmed = inGrid;
        gridDim.focused(inGrid, animate);
        gridDim.removeCallbacks(pauseGridPlayback);
        if (inGrid) {
            gridDim.postDelayed(pauseGridPlayback, animate ? 280 : 0);
        } else if (pausedForGrid && player != null) {
            pausedForGrid = false;
            player.start();
        }
    }

    void resume() {
        if(resumed)return;
        resumed = true;
        gridDim.focused(dimmed,false);cover.showStill();
        root.setTag(R.id.background_video_visible,Boolean.TRUE);
        video.setVisibility(View.VISIBLE);
        start();
    }

    void pause() {
        resumed = false;
        gridDim.freeze();cover.showStill();
        release();
    }
    /** Decoder is already released in pause; retire the surface once the window is hidden. */
    void hidden(){if(!resumed)video.setVisibility(View.INVISIBLE);}
    boolean hasStill(){return cover.hasStill();}
    void whenReady(Runnable action){cover.whenReady(action);}
    void beforeAppLaunch(Runnable launch){if(resumed&&player!=null&&!pausedForGrid)cover.beforeAppLaunch(launch);else launch.run();}
    void close(){pause();gridDim.close();root.setTag(R.id.background_video_visible,Boolean.FALSE);if(root.getTag(R.id.video_still_owner)==cover)root.setTag(R.id.video_still_owner,null);cover.close();for(View v:layers)if(v.getParent() instanceof ViewGroup)((ViewGroup)v.getParent()).removeView(v);layers.clear();}

    private void start() {
        if (!resumed || surface == null || player != null) return;
        MediaPlayer mp = new MediaPlayer();
        player = mp;
        try {
            mp.setDataSource(file.getPath());
            mp.setDisplay(surface);
            mp.setVolume(0f, 0f);
            // One aerial loops; cycling plays each through once and then moves on.
            mp.setLooping(!cycle);
            if (cycle) mp.setOnCompletionListener(p -> {
                if (player == p) advance();
            });
            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            mp.setOnPreparedListener(p -> {
                if (player != p) return;
                int at = prefs.getInt("position", 0);
                if (at > 0 && at < p.getDuration() - 2000) p.seekTo(at, MediaPlayer.SEEK_CLOSEST_SYNC);
                p.start();
            });
            mp.setOnInfoListener((p, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START && player == p) {
                    cover.begin();
                    // Prepare one reusable veil while playback starts, away from D-pad input.
                    cover.refreshSnapshot(() -> {
                        if (resumed && player == p) gridDim.blur(cover.snapshot());
                    });
                    if (dimmed) {
                        gridDim.removeCallbacks(pauseGridPlayback);
                        gridDim.postDelayed(pauseGridPlayback, 280);
                    }
                }
                return false;
            });
            mp.setOnErrorListener((p, what, extra) -> {
                Log.w(TAG, "playback error " + what + "/" + extra + " for " + file.getName());
                cover.showStill();
                release();
                return true;
            });
            mp.prepareAsync();
        } catch (Exception e) {
            Log.w(TAG, "cannot play " + file, e);
            release();
        }
    }

    private void release() {
        gridDim.removeCallbacks(pauseGridPlayback);
        MediaPlayer mp = player;
        boolean wasPausedForGrid = pausedForGrid;
        pausedForGrid = false;
        player = null;
        if (mp == null) return;
        try {
            if (wasPausedForGrid || mp.isPlaying()) prefs.edit().putInt("position", mp.getCurrentPosition()).apply();
        } catch (IllegalStateException ignored) {
            // still preparing: keep the previous position
        }
        mp.release();
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        surface = holder;
        start();
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        cover.showStill();
        surface = null;
        release();
    }
}
