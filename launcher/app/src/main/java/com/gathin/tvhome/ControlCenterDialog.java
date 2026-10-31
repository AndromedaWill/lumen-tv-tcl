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
    private final Runnable inputs, customize;
    private final boolean motion;
    private final java.util.List<View> controls = new java.util.ArrayList<>();

    ControlCenterDialog(Activity activity, Runnable inputs, Runnable customize, boolean motion) {
        super(activity);
        this.activity = activity;
        this.inputs = inputs;
        this.customize = customize;
        this.motion = motion;
    }

    private int dp(float n) { return Math.round(n * activity.getResources().getDisplayMetrics().density); }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(12), dp(14), dp(12));
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        TextView title = text(R.string.control_center, 21, Color.WHITE);
        panel.addView(title);
        TextView caption = text(R.string.control_center_hint, 12, 0xFFCCD1DD);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(-1, dp(24));
        panel.addView(caption, captionParams);
        LinearLayout row = row(panel);
        add(row, R.drawable.ic_settings, R.string.control_tv, R.string.control_native,
                () -> launch(Settings.ACTION_SETTINGS));
        add(row, R.drawable.ic_wifi, R.string.control_network, R.string.control_open,
                () -> launch(Settings.ACTION_WIFI_SETTINGS));
        row = row(panel);
        add(row, R.drawable.ic_volume, R.string.control_sound, R.string.control_open,
                () -> launch(Settings.ACTION_SOUND_SETTINGS));
        add(row, R.drawable.ic_picture, R.string.control_picture, R.string.control_open,
                () -> launch(Settings.ACTION_DISPLAY_SETTINGS));
        row = row(panel);
        add(row, R.drawable.ic_bluetooth, R.string.control_bluetooth, R.string.control_open,
                () -> launch(Settings.ACTION_BLUETOOTH_SETTINGS));
        add(row, R.drawable.ic_input, R.string.control_inputs, R.string.control_sources, inputs);
        row = row(panel);
        add(row, R.drawable.ic_apps, R.string.control_apps, R.string.control_open,
                () -> launch(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        add(row, R.drawable.ic_home_settings, R.string.control_home, R.string.control_customize, customize);
        // Explicit neighbours prevent Android focus search from jumping diagonally between unequal text.
        for (int i = 0; i < controls.size(); i++) {
            View v = controls.get(i);
            v.setNextFocusLeftId(controls.get(i % 2 == 0 ? i : i - 1).getId());
            v.setNextFocusRightId(controls.get(i % 2 == 0 ? i + 1 : i).getId());
            v.setNextFocusUpId(controls.get(i < 2 ? i : i - 2).getId());
            v.setNextFocusDownId(controls.get(i >= 6 ? i : i + 2).getId());
        }
        setContentView(panel);
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
        params.y = dp(54);
        params.width = dp(356);
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.dimAmount = .65f;
        w.setAttributes(params);
    }

    private LinearLayout row(LinearLayout panel) {
        LinearLayout row = new LinearLayout(activity);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.topMargin = dp(8);
        panel.addView(row, lp);
        return row;
    }

    private TextView text(int res, float size, int color) {
        TextView t = new TextView(activity);
        t.setText(res);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setTypeface(activity.getResources().getFont(R.font.inter_medium));
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private void add(LinearLayout row, int icon, int title, int subtitle, Runnable action) {
        LinearLayout tile = new LinearLayout(activity);
        tile.setId(View.generateViewId());
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(12), dp(6), dp(8), dp(6));
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setContentDescription(activity.getString(title) + ". " + activity.getString(subtitle));
        tile.setBackground(new FrostedControlDrawable(activity, tile, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        if (row.getChildCount() > 0) lp.leftMargin = dp(12);
        row.addView(tile, lp);
        ImageView glyph = new ImageView(activity);
        glyph.setImageResource(icon);
        glyph.setColorFilter(0xFFF2F4FB);
        LinearLayout.LayoutParams glyphParams = new LinearLayout.LayoutParams(dp(21), dp(21));
        glyphParams.rightMargin = dp(8);
        tile.addView(glyph, glyphParams);
        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(title, 13, Color.WHITE);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(name);
        TextView sub = text(subtitle, 11, 0xFFBAC3D4);
        labels.addView(sub);
        tile.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        tile.setOnFocusChangeListener((v, focused) -> {
            tile.setBackground(new FrostedControlDrawable(activity, tile, focused));
            name.setTextColor(focused ? 0xFF182133 : Color.WHITE);
            sub.setTextColor(focused ? 0xFF45546C : 0xFFBAC3D4);
            glyph.setColorFilter(focused ? 0xFF233C61 : 0xFFF2F4FB);
            v.animate().cancel();
            if (motion) v.animate().scaleX(focused ? 1.035f : 1).scaleY(focused ? 1.035f : 1).setDuration(160).start();
        });
        tile.setOnClickListener(v -> { dismiss(); action.run(); });
        controls.add(tile);
    }

    private void launch(String action) {
        try { activity.startActivity(SystemSettings.intent(activity, action)); }
        catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(activity, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
    }
}
