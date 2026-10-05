package me.jxl.kiosk.plugins.quickactionsoverlay;

public final class BatteryLevelTest {
    public static void main(String[] args) {
        check(BatteryLevel.isBattery("battery", ""), "battery device class");
        check(BatteryLevel.isBattery("", "mdi:battery-30"), "battery icon");
        check(!BatteryLevel.isBattery("", "mdi:percent"), "other percentage sensor");
        check(BatteryLevel.parse("0") == 0, "empty battery");
        check(BatteryLevel.parse("100") == 100, "full battery");
        check(BatteryLevel.parse("49,6") == 50, "decimal comma");
        check(BatteryLevel.parse("-8") == 0, "lower clamp");
        check(BatteryLevel.parse("180") == 100, "upper clamp");
        for (String state : new String[] {null, "", "unknown", "unavailable", "NaN", "Infinity", "-Infinity"}) {
            check(BatteryLevel.parse(state) == -1, "unknown battery: " + state);
        }
        System.out.println("Battery level checks passed");
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
