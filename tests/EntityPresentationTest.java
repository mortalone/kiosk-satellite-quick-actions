package me.jxl.kiosk.plugins.quickactionsoverlay;

public class EntityPresentationTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("Check " + checks); }
    private static void label(String entity, String state, String cls, String unit, String expected) {
        check(expected.equals(EntityPresentation.text(entity, state, cls, unit)));
    }
    public static void main(String[] args) {
        check(!EntityPresentation.visible(true, "person.child", "script.call", "not_home", ""));
        check(!EntityPresentation.visible(true, "person.child", "script.call", "School", ""));
        check(EntityPresentation.visible(true, "person.child", "script.call", "home", ""));
        check(EntityPresentation.visible(false, "person.child", "script.call", "not_home", ""));
        for (String state : new String[] {null, "", "unknown", "unavailable"}) {
            check(EntityPresentation.visible(true, "person.child", "script.call", state, ""));
            check(EntityPresentation.visible(true, "binary_sensor.leak", "", state, "moisture"));
        }
        for (String domain : new String[] {"light", "switch", "fan", "script", "scene", "button", "media_player"}) {
            for (String state : new String[] {"off", "on", "unavailable"}) {
                check(EntityPresentation.visible(true, domain + ".all", "", state, ""));
                check(EntityPresentation.visible(true, domain + ".all", "script.all_off", state, ""));
            }
        }
        for (String cls : new String[] {"door", "window", "motion", "occupancy", "moisture", "smoke", "problem"}) {
            check(!EntityPresentation.visible(true, "binary_sensor.status", "", "off", cls));
            check(!EntityPresentation.visible(true, "binary_sensor.status", "binary_sensor.status", "off", cls));
            check(EntityPresentation.visible(true, "binary_sensor.status", "", "on", cls));
            check(EntityPresentation.visible(true, "binary_sensor.status", "script.action", "off", cls));
        }
        check(EntityPresentation.visible(true, "sensor.battery", "", "100", "battery"));
        check(EntityPresentation.visible(true, "sensor.temperature", "", "0", "temperature"));
        check(EntityPresentation.visible(true, "binary_sensor.network", "", "off", "connectivity"));
        label("person.child", "home", "", "", "Hjemme");
        label("person.child", "not_home", "", "", "Ude");
        label("person.child", "School", "", "", "School");
        label("sensor.temp", "21.5", "temperature", "°C", "21.5 °C");
        label("sensor.temp", "unavailable", "temperature", "°C", "Utilgængelig");
        label("person.child", null, "", "", "Ukendt");
        label("binary_sensor.door", "off", "door", "", "Lukket");
        label("binary_sensor.leak", "on", "moisture", "", "Vand registreret");
        label("light.all", "off", "", "", "Slukket");
        label("fan.all", "on", "", "", "Tændt");
        label("script.call", "off", "", "", "");
        label("script.call", "on", "", "", "Kører");
        label("scene.all", "2026-10-16", "", "", "");
        label("media_player.room", "paused", "", "", "På pause");
        label("sensor.custom", "custom_state", "", "", "custom_state");
        System.out.println("Entity presentation: " + checks + " checks passed");
    }
}
