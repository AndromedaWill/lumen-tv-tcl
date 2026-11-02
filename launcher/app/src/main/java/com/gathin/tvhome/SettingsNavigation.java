package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;

/** Small page stack. Keeps semantic focus keys, never hidden page views or an Activity reference. */
final class SettingsNavigation {
    enum Page { ROOT, HOME, COLUMNS, ROWS, APPEARANCE, SYSTEM, MEDIA, CREDITS }
    private static final class Entry {
        final Page page;
        String focus;
        Entry(Page page, String focus) { this.page = page; this.focus = focus; }
    }
    private final List<Entry> stack = new ArrayList<>();

    SettingsNavigation() { stack.add(new Entry(Page.ROOT, "home")); }

    Page page() { return stack.get(stack.size() - 1).page; }
    String focus() { return stack.get(stack.size() - 1).focus; }
    void focus(String key) { stack.get(stack.size() - 1).focus = key; }

    void push(Page page, String focus) {
        if (!allows(page(), page)) throw new IllegalArgumentException("Invalid settings page transition");
        stack.add(new Entry(page, focus));
    }

    boolean pop() {
        if (stack.size() == 1) return false;
        stack.remove(stack.size() - 1);
        return true;
    }

    String[] pages() {
        String[] result = new String[stack.size()];
        for (int i = 0; i < result.length; i++) result[i] = stack.get(i).page.name();
        return result;
    }

    String[] focuses() {
        String[] result = new String[stack.size()];
        for (int i = 0; i < result.length; i++) result[i] = stack.get(i).focus;
        return result;
    }

    static SettingsNavigation restore(String[] pages, String[] focuses) {
        SettingsNavigation result = new SettingsNavigation();
        if (pages == null || focuses == null || pages.length != focuses.length
                || pages.length < 1 || pages.length > 3 || !"ROOT".equals(pages[0])) return result;
        try {
            result.focus(focuses[0]);
            for (int i = 1; i < pages.length; i++) result.push(Page.valueOf(pages[i]), focuses[i]);
            return result;
        } catch (IllegalArgumentException | NullPointerException invalid) {
            return new SettingsNavigation();
        }
    }

    private static boolean allows(Page from, Page to) {
        switch (from) {
            case ROOT: return to == Page.HOME || to == Page.APPEARANCE || to == Page.SYSTEM;
            case HOME: return to == Page.COLUMNS || to == Page.ROWS;
            case SYSTEM: return to == Page.CREDITS || to == Page.MEDIA;
            default: return false;
        }
    }
}
