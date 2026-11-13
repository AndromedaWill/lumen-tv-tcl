package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.content.Intent;
import android.os.Bundle;

/** Same native controls for the optional settings_key_panel entry; no accessibility is enabled here. */
public final class QuickSettingsActivity extends Activity {
    private ControlCenterDialog panel;
    private Dialog inputDialog;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        panel = new ControlCenterDialog(this,
                this::showInputs,
                () -> openHome(MainActivity.ACTION_LAUNCHER_SETTINGS),
                () -> openHome(MainActivity.ACTION_NOTIFICATIONS), false);
        panel.setOnDismissListener(d -> { if (inputDialog == null) finish(); });
        panel.show();
    }
    private void showInputs() {
        InputMenu inputs = new InputMenu(this, Config.load(this));
        if (inputs.hasSourcePanel() && inputs.liveTvIntent() == null) {
            inputs.openSourcePanel();
            return;
        }
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = getLayoutInflater().inflate(R.layout.popup_inputs, null);
        LinearLayout rows = content.findViewById(R.id.rows);
        inputs.populate(rows, dialog::dismiss);
        if (rows.getChildCount() == 0) {
            Toast.makeText(this, R.string.no_inputs, Toast.LENGTH_SHORT).show();
            return;
        }
        inputDialog = dialog;
        dialog.setContentView(content);
        dialog.setOnDismissListener(d -> finish());
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setGravity(Gravity.TOP | Gravity.END);
        android.view.WindowManager.LayoutParams params = window.getAttributes();
        params.x = TvDesign.dp(this, 36);
        params.y = TvDesign.dp(this, 80);
        window.setAttributes(params);
        dialog.show();
        window.setLayout(TvDesign.dp(this, 260), -2);
        rows.getChildAt(0).requestFocus();
    }
    private void openHome(String action) {
        startActivity(new Intent(this, MainActivity.class).setAction(action));
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); finish(); }
    @Override protected void onPause() {
        super.onPause();
        if (panel != null) panel.dismiss();
        if (inputDialog != null) inputDialog.dismiss();
        finish();
    }
}
