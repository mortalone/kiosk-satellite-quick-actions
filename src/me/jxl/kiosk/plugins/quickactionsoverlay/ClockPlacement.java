package me.jxl.kiosk.plugins.quickactionsoverlay;

/** Pixel offset from the clock's configured anchor when it shares the action rail's anchor. */
public final class ClockPlacement {
    private ClockPlacement() {}
    public static int offset(String clockPosition, String railPosition, boolean railVisible,
                             int railHeight, int clockHeight, int gap) {
        if (!railVisible || !clockPosition.equals(railPosition) || railHeight <= 0) return 0;
        int spacing = Math.max(0, gap);
        if (clockPosition.startsWith("Center ")) return (railHeight + Math.max(0, clockHeight) + 1) / 2 + spacing;
        return railHeight + spacing;
    }
}
