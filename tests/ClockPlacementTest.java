package me.jxl.kiosk.plugins.quickactionsoverlay;
public final class ClockPlacementTest {
    public static void main(String[] args) {
        String[] anchors = {"Top left", "Top center", "Top right", "Center left", "Center right", "Bottom left", "Bottom center", "Bottom right"};
        for (String anchor : anchors) {
            int offset = ClockPlacement.offset(anchor, anchor, true, 100, 40, 12);
            if (offset != (anchor.startsWith("Center ") ? 82 : 112)) throw new AssertionError(anchor);
            // Top: clock below rail. Bottom: clock above rail. Center: clock below rail.
            double railTop = anchor.startsWith("Top ") ? 16 : anchor.startsWith("Bottom ") ? 1000 - 16 - 100 : 500 - 50;
            double clockTop = anchor.startsWith("Top ") ? 16 + offset : anchor.startsWith("Bottom ") ? 1000 - 16 - offset - 40 : 500 - 20 + offset;
            if (!(clockTop >= railTop + 100 + 12 || clockTop + 40 + 12 <= railTop)) throw new AssertionError("Overlap " + anchor);
            if (ClockPlacement.offset(anchor, anchor, false, 100, 40, 12) != 0) throw new AssertionError("Hidden rail");
            if (ClockPlacement.offset(anchor, anchor, true, 0, 40, 12) != 0) throw new AssertionError("Empty rail");
        }
        if (ClockPlacement.offset("Top center", "Bottom center", true, 100, 40, 12) != 0) throw new AssertionError("Independent anchors");
        if (ClockPlacement.offset("Top center", "Top center", true, 180, 40, 12) != 192) throw new AssertionError("Rail resized");
        if (ClockPlacement.offset("Center left", "Center left", true, 101, 41, 12) != 83) throw new AssertionError("Odd dimensions");
        System.out.println("Clock placement checks passed: eight shared anchors, gap, hidden/empty rail and resizing.");
    }
}
