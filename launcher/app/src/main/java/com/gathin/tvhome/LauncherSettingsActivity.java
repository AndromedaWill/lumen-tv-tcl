package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** Settings controller. Only the current page is built; the stack stores page/focus keys. */
public final class LauncherSettingsActivity extends Activity {
    private static final int BACKGROUNDS = 73;
    static final String RECREATE_EXTRA = "com.gathin.tvhome.settings.RECREATE";
    private SharedPreferences choices;
    private SettingsNavigation navigation;
    private SettingsPageView page;
    private boolean changed;
    private boolean recreateHome;
    private int columns, rowsBelowDock;
    private boolean animations, advancedEffects;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        loadConfiguration();
        navigation = state == null ? new SettingsNavigation()
                : SettingsNavigation.restore(state.getStringArray("pages"), state.getStringArray("focuses"));
        changed = state != null && state.getBoolean("changed");
        recreateHome = state != null && state.getBoolean("recreateHome");
        if (changed) reportChanged();
        page = new SettingsPageView(this, navigation::focus); setContentView(page); render();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putStringArray("pages", navigation.pages()); state.putStringArray("focuses", navigation.focuses());
        state.putBoolean("changed", changed); state.putBoolean("recreateHome", recreateHome); super.onSaveInstanceState(state);
    }

    @Override public void onBackPressed() {
        if (navigation.pop()) render(); else super.onBackPressed();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == BACKGROUNDS) { if (result == RESULT_OK) markChanged(false); render(); }
    }
    @Override protected void onResume(){super.onResume();if(navigation!=null&&navigation.page()==SettingsNavigation.Page.MEDIA)render();}

    private String s(int id) { return getString(id); }
    private String yes(boolean value) { return s(value ? R.string.settings_yes : R.string.settings_no); }
    private void reportChanged() { setResult(RESULT_OK,new Intent().putExtra(RECREATE_EXTRA,recreateHome)); }
    private void markChanged() { markChanged(true); }
    private void markChanged(boolean rebuild) { changed = true; recreateHome |= rebuild; reportChanged(); }
    private void push(SettingsNavigation.Page target, String focus) { navigation.push(target, focus); render(); }
    private void choose(String key, int value) {
        choices.edit().putInt(key, value).apply();
        if ("columns".equals(key)) columns = value; else rowsBelowDock = value;
        markChanged(); navigation.pop(); render();
    }
    private void row(List<SettingsPageView.Row> rows, String key, int label, String value, int description,
            int icon, boolean child, boolean enabled, Runnable action) {
        rows.add(new SettingsPageView.Row(key, s(label), value, s(description), icon, child, false, enabled, action));
    }

    private void render() {
        List<SettingsPageView.Row> rows = new ArrayList<>(); int title = R.string.launcher_settings;
        switch (navigation.page()) {
            case ROOT:
                row(rows, "home", R.string.control_home, "", R.string.settings_home_description, R.drawable.ic_home_settings, true, true,
                        () -> push(SettingsNavigation.Page.HOME, "columns"));
                row(rows, "appearance", R.string.settings_appearance, "", R.string.settings_appearance_description, R.drawable.ic_settings, true, true,
                        () -> push(SettingsNavigation.Page.APPEARANCE, "animations"));
                row(rows, "backgrounds", R.string.settings_backgrounds, backgroundMode(), R.string.settings_backgrounds_description, R.drawable.ic_picture, true, true,
                        () -> startActivityForResult(new Intent(this, BackgroundSettingsActivity.class), BACKGROUNDS));
                row(rows, "system", R.string.settings_system, "", R.string.settings_system_description, R.drawable.ic_settings, true, true,
                        () -> push(SettingsNavigation.Page.SYSTEM, "native"));
                break;
            case HOME:
                title = R.string.control_home;
                row(rows, "columns", R.string.columns_label, Integer.toString(columns), R.string.settings_columns_description, R.drawable.ic_apps, true, true,
                        () -> push(SettingsNavigation.Page.COLUMNS, "columns_" + columns));
                row(rows, "rows", R.string.rows_below_dock_label, Integer.toString(rowsBelowDock), R.string.settings_rows_description, R.drawable.ic_apps, true, true,
                        () -> push(SettingsNavigation.Page.ROWS, "rows_" + rowsBelowDock));
                row(rows, "greeting", R.string.settings_greeting_name, getSharedPreferences("home_profile", 0).getString("name", "Will"), R.string.settings_greeting_description,
                        R.drawable.ic_home_settings, false, true, this::editGreeting);
                break;
            case COLUMNS:
            case ROWS:
                boolean columnPage = navigation.page() == SettingsNavigation.Page.COLUMNS;
                title = columnPage ? R.string.columns_label : R.string.rows_below_dock_label;
                for (int i = columnPage ? 4 : 0; i <= (columnPage ? 7 : 3); i++) {
                    int value = i; boolean selected = value == (columnPage ? columns : rowsBelowDock);
                    String label = columnPage ? getString(R.string.settings_app_count, i)
                            : i == 0 ? s(R.string.settings_no_rows) : getResources().getQuantityString(R.plurals.settings_row_count, i, i);
                    rows.add(new SettingsPageView.Row((columnPage ? "columns_" : "rows_") + i, label, "",
                            s(columnPage ? R.string.settings_columns_description : R.string.settings_rows_description), R.drawable.ic_apps,
                            false, selected, true, () -> choose(columnPage ? "columns" : Config.ROWS_BELOW_DOCK_KEY, value)));
                }
                break;
            case APPEARANCE:
                title = R.string.settings_appearance;
                row(rows, "animations", R.string.settings_animations, yes(animations), R.string.settings_animations_description, R.drawable.ic_picture, false, true, () -> {
                    boolean enabled = !animations; animations = enabled;
                    SharedPreferences.Editor edit = choices.edit().putBoolean("animations", enabled);
                    if (!enabled) { advancedEffects = false; edit.putBoolean("advanced_effects", false); }
                    edit.apply(); markChanged(); render();
                });
                row(rows, "advanced", R.string.advanced_effects, yes(animations && advancedEffects), R.string.advanced_effects_warning,
                        R.drawable.ic_picture, false, animations, () -> {
                            if (advancedEffects) setAdvanced(false);
                            else new AlertDialog.Builder(this).setTitle(R.string.advanced_effects).setMessage(R.string.advanced_effects_warning)
                                    .setPositiveButton(R.string.enable, (d, w) -> setAdvanced(true)).setNegativeButton(android.R.string.cancel, null).show();
                        });
                row(rows, "theme", R.string.follow_theme, s(hasChoices() ? R.string.settings_custom : R.string.settings_from_theme), R.string.settings_theme_description,
                        R.drawable.ic_settings, false, true, () -> new AlertDialog.Builder(this).setTitle(R.string.follow_theme)
                                .setMessage(R.string.settings_theme_description).setPositiveButton(R.string.settings_apply, (d, w) -> resetTheme())
                                .setNegativeButton(android.R.string.cancel, null).show());
                break;
            case SYSTEM:
                title = R.string.settings_system;
                row(rows, "media", R.string.media_playback, s(HomeNotificationService.hasAccess(this)?R.string.media_access_granted:R.string.media_access_optional), R.string.media_settings_description,
                        R.drawable.ic_music_note, true, true, () -> push(SettingsNavigation.Page.MEDIA, "media_access"));
                row(rows, "native", R.string.tv_settings, s(R.string.control_native), R.string.settings_native_description,
                        R.drawable.ic_settings, true, true, () -> open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
                row(rows, "default_home", R.string.home_settings, s(R.string.settings_open_picker), R.string.home_settings_help,
                        R.drawable.ic_home_settings, true, true, () -> open(new Intent(Settings.ACTION_HOME_SETTINGS)));
                row(rows, "credits", R.string.settings_credits, "", R.string.settings_credits_description, R.drawable.ic_picture, true, true,
                        () -> push(SettingsNavigation.Page.CREDITS, "font"));
                break;
            case MEDIA:
                title=R.string.media_playback;
                row(rows,"media_access",R.string.media_access_manage,s(HomeNotificationService.hasAccess(this)?R.string.media_access_granted:R.string.media_access_optional),R.string.media_access_summary,
                        R.drawable.ic_music_note,false,true,this::mediaAccess);
                break;
            case CREDITS:
                title = R.string.settings_credits;
                rows.add(new SettingsPageView.Row("font", "Inter", "SIL OFL 1.1", s(R.string.settings_font_credit), R.drawable.ic_apps, false, false, true, null));
                rows.add(new SettingsPageView.Row("icons", "Simple Icons", "CC0", s(R.string.settings_icons_credit), R.drawable.ic_apps, false, false, true, null));
                row(rows, "photo", R.string.settings_photo_credit, "CC BY-SA 3.0", R.string.background_credits, R.drawable.ic_picture, false, true, null);
                break;
        }
        page.show(s(title), rows, navigation.focus());
    }

    private String backgroundMode() {
        switch (BackgroundStore.load(this).mode) {
            case BackgroundStore.SCHEDULE: return s(R.string.settings_scheduled);
            case BackgroundStore.SLIDESHOW: return s(R.string.settings_slideshow);
            case BackgroundStore.LEGACY: return s(R.string.settings_from_theme);
            default: return s(R.string.settings_fixed);
        }
    }
    private boolean hasChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY) || choices.contains("animations")
                || choices.contains("advanced_effects") || choices.contains(Config.WALLPAPER_CHOICE_KEY);
    }
    private void resetTheme() {
        choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY).apply();
        loadConfiguration(); markChanged(); render();
    }
    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply(); markChanged(); render();
    }
    private void loadConfiguration() {
        Config config = Config.load(this);
        columns = config.columns; rowsBelowDock = config.rowsBelowDock;
        animations = config.animations; advancedEffects = config.animations && config.advancedEffects;
    }
    private void editGreeting() {
        EditText input = new EditText(this); input.setSingleLine(true);
        input.setText(getSharedPreferences("home_profile", 0).getString("name", "Will"));
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});
        new AlertDialog.Builder(this).setTitle(R.string.settings_greeting_name).setMessage(R.string.settings_greeting_description).setView(input)
                .setPositiveButton(R.string.settings_save, (d, w) -> {
                    String name = input.getText().toString().replaceAll("[\\p{Cntrl}]", "").trim();
                    getSharedPreferences("home_profile", 0).edit().putString("name", name).apply(); markChanged(false); render();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }
    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException missing) { Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show(); }
    }
    private void mediaAccess(){
        AlertDialog disclosure=new AlertDialog.Builder(this).setTitle(R.string.media_playback).setMessage(R.string.media_access_explanation)
                .setPositiveButton(R.string.notification_manage_access,(dialog,which)->{
                    Intent intent=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                    android.content.pm.ResolveInfo match=getPackageManager().resolveActivity(intent,0);
                    if(match==null||match.activityInfo==null||!match.activityInfo.exported||match.activityInfo.name.contains("CTSDummy")){
                        Toast.makeText(this,R.string.notification_access_unavailable,Toast.LENGTH_LONG).show();return;
                    }
                    open(intent);
                }).setNegativeButton(android.R.string.cancel,null).create();
        disclosure.setOnShowListener(dialog->TvDesign.alertOverlay(this,disclosure));disclosure.show();
    }
}
