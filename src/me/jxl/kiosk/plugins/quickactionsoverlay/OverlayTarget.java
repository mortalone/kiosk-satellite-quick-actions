// SPDX-License-Identifier: MIT
package me.jxl.kiosk.plugins.quickactionsoverlay;

/** One persisted selector is the source of truth for all action surfaces. */
final class OverlayTarget {
    private static final String[] NAMES = {
        "Hidden", "Dashboard only", "Kiosk Satellite only", "Dashboard + Kiosk Satellite",
        "Fotoo only", "Dashboard + Fotoo", "Kiosk Satellite + Fotoo",
        "Dashboard + Kiosk Satellite + Fotoo"
    };
    static int mask(String name) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) return i;
        return 6; // Previous default; also safe for missing initial settings.
    }
    static String name(int mask) { return NAMES[mask & 7]; }
    static int withDashboard(int mask, boolean enabled) { return enabled ? mask | 1 : mask & ~1; }
}
