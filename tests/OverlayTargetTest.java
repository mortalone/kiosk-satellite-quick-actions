package me.jxl.kiosk.plugins.quickactionsoverlay;
public final class OverlayTargetTest {
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        for (int mask = 0; mask < 8; mask++) {
            check(OverlayTarget.mask(OverlayTarget.name(mask)) == mask);
            check((OverlayTarget.withDashboard(mask, true) & 6) == (mask & 6));
            check((OverlayTarget.withDashboard(mask, false) & 6) == (mask & 6));
            check((OverlayTarget.withDashboard(mask, true) & 1) == 1);
            check((OverlayTarget.withDashboard(mask, false) & 1) == 0);
        }
        check(OverlayTarget.mask("Kiosk Satellite + Fotoo") == 6);
        check(OverlayTarget.mask("Kiosk Satellite only") == 2);
        check(OverlayTarget.mask("Fotoo only") == 4);
        check(OverlayTarget.mask("") == 6);
        System.out.println("44 selector round-trip, existing option and action-preservation checks passed");
    }
}
