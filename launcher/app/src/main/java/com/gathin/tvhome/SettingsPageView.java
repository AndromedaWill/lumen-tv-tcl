package com.gathin.tvhome;

import android.app.Activity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** One visible two-column page, shared by categories, values and choices. Native D-pad owns repetition. */
final class SettingsPageView extends LinearLayout {
    static final class Row {
        final String key, label, value, description;
        final int icon;
        final boolean chevron, checked, enabled;
        final Runnable activate;
        Row(String key, String label, String value, String description, int icon,
                boolean chevron, boolean checked, boolean enabled, Runnable activate) {
            this.key = key; this.label = label; this.value = value; this.description = description;
            this.icon = icon; this.chevron = chevron; this.checked = checked;
            this.enabled = enabled; this.activate = activate;
        }
    }
    private final Activity activity;
    private final TextView title, description;
    private final ImageView glyph;
    private final LinearLayout rows;
    private final ScrollView scroll;
    private final Consumer<String> focused;
    private int shownIcon;

    SettingsPageView(Activity activity, Consumer<String> focused) {
        super(activity); this.activity = activity; this.focused = focused;
        setId(R.id.root); setOrientation(VERTICAL); setBackground(TvDesign.screen(activity));
        int inset = dp(36); setPadding(inset, dp(24), inset, dp(24));
        title = text(TvDesign.Type.TITLE); addView(title, new LayoutParams(-1, dp(40)));
        LinearLayout content = new LinearLayout(activity);
        LayoutParams body = new LayoutParams(-1, 0, 1); body.topMargin = dp(32); addView(content, body);
        LinearLayout side = new LinearLayout(activity); side.setOrientation(VERTICAL);
        side.setPadding(0, dp(10), dp(32), 0); content.addView(side, new LayoutParams(0, -1, .38f));
        glyph = new ImageView(activity); glyph.setColorFilter(TvDesign.color(activity, R.color.ds_icon_primary));
        side.addView(glyph, new LayoutParams(dp(108), dp(108)));
        description = text(TvDesign.Type.BODY); description.setMaxLines(9);
        description.setGravity(Gravity.TOP);
        description.setTextColor(TvDesign.color(activity, R.color.ds_text_secondary));
        // Fixed available space: focus descriptions invalidate text, without remeasuring the page.
        LayoutParams note = new LayoutParams(-1, 0, 1); note.topMargin = dp(28); side.addView(description, note);
        scroll = new ScrollView(activity); scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false); scroll.setPadding(dp(4), dp(4), dp(4), dp(8));
        content.addView(scroll, new LayoutParams(0, -1, .62f));
        rows = new LinearLayout(activity); rows.setOrientation(VERTICAL); scroll.addView(rows);
    }

    private int dp(float n) { return TvDesign.dp(activity, n); }
    private TextView text(TvDesign.Type type) {
        TextView v = new TextView(activity); TvDesign.typography(v, type);
        v.setGravity(Gravity.CENTER_VERTICAL); return v;
    }

    void show(String heading, List<Row> entries, String focus) {
        title.setText(heading); rows.removeAllViews(); scroll.scrollTo(0, 0);
        List<View> controls = new ArrayList<>(); View wanted = null;
        for (Row entry : entries) {
            View row = row(entry); LayoutParams p = new LayoutParams(-1, dp(52));
            p.bottomMargin = dp(8); rows.addView(row, p);
            if (entry.enabled) controls.add(row);
            if (entry.key.equals(focus) && entry.enabled) wanted = row;
        }
        for (int i = 0; i < controls.size(); i++) {
            View v = controls.get(i);
            v.setNextFocusUpId(controls.get(Math.max(0, i - 1)).getId());
            v.setNextFocusDownId(controls.get(Math.min(controls.size() - 1, i + 1)).getId());
            v.setNextFocusLeftId(v.getId()); v.setNextFocusRightId(v.getId());
        }
        if (wanted == null && !controls.isEmpty()) wanted = controls.get(0);
        if (wanted != null) wanted.requestFocus();
    }

    private View row(Row item) {
        LinearLayout row = new LinearLayout(activity); row.setId(View.generateViewId()); row.setTag(item.key);
        row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(16), dp(8), dp(16), dp(8));
        row.setFocusable(item.enabled); row.setClickable(item.activate != null && item.enabled);
        row.setEnabled(item.enabled);
        row.setAlpha(item.enabled ? 1 : .5f);
        row.setContentDescription(item.label + (item.value.isEmpty() ? "" : ". " + item.value)
                + (item.checked ? ". " + activity.getString(R.string.settings_saved_value) : ""));
        float radius = TvDesign.radius(activity, R.dimen.ds_radius_control);
        TvDesign.controlFrame(activity, row, radius); row.setSelected(item.checked);
        TextView name = text(TvDesign.Type.BODY); name.setText(item.label); name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END); name.setMarqueeRepeatLimit(-1);
        row.addView(name, new LayoutParams(0, -1, 1));
        TextView value = text(TvDesign.Type.CONTROL); value.setText(item.value); value.setSingleLine(true);
        value.setMaxWidth(dp(144)); value.setEllipsize(TextUtils.TruncateAt.END); value.setMarqueeRepeatLimit(-1);
        LayoutParams vp = new LayoutParams(-2, -1); vp.leftMargin = dp(12); row.addView(value, vp);
        TextView end = text(TvDesign.Type.SECTION); end.setText(item.checked ? "✓" : item.chevron ? "›" : "");
        end.setGravity(Gravity.CENTER); LayoutParams ep = new LayoutParams(dp(24), -1); ep.leftMargin = dp(8); row.addView(end, ep);
        Runnable rest = () -> {
            boolean on = row.hasFocus();
            name.setTextColor(TvDesign.color(activity, on ? R.color.ds_text_focused : R.color.ds_text_primary));
            value.setTextColor(TvDesign.color(activity, on ? R.color.ds_text_secondary_focused : R.color.ds_text_secondary));
            end.setTextColor(TvDesign.color(activity, on ? R.color.ds_icon_focused : R.color.ds_icon_primary));
            name.setEllipsize(on ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.END); name.setSelected(on);
            value.setEllipsize(on ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.END); value.setSelected(on);
        };
        row.setOnFocusChangeListener((v, on) -> {
            rest.run(); if (!on) return;
            focused.accept(item.key); description.setText(item.description);
            if (shownIcon != item.icon) { shownIcon = item.icon; glyph.setImageResource(item.icon); }
        });
        row.setOnClickListener(v -> { if (item.activate != null && item.enabled) item.activate.run(); });
        rest.run(); return row;
    }
}
