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
        label("person.child|Malte", "home", "", "", "Hjemme");
        label("person.child|", "home", "", "", "Hjemme");
        label("sensor.citroen_batteri|Citroën", "69", "battery", "%", "69 %");

        // Multi-entity pipe parsing checks
        EntityPresentation.EntitySpec s0 = EntityPresentation.parseSpec("image.citroen_logo | | sensor.citroen_batteri", "");
        check("image.citroen_logo".equals(s0.displayEntity));
        check("sensor.citroen_batteri".equals(s0.linkedSensor));
        check(s0.emptyTitle);
        check(!s0.explicitHide);

        EntityPresentation.EntitySpec s1 = EntityPresentation.parseSpec("image.citroen_logo | sensor.citroen_batteri", "");
        check("image.citroen_logo".equals(s1.displayEntity));
        check("sensor.citroen_batteri".equals(s1.linkedSensor));
        check(!s1.emptyTitle);
        check(s1.customLabel == null);
        check(!s1.explicitHide);

        EntityPresentation.EntitySpec s2 = EntityPresentation.parseSpec("image.citroen_logo | sensor.citroen_batteri | Citroën", "");
        check("image.citroen_logo".equals(s2.displayEntity));
        check("sensor.citroen_batteri".equals(s2.linkedSensor));
        check("Citroën".equals(s2.customLabel));
        check(!s2.explicitHide);

        EntityPresentation.EntitySpec s3 = EntityPresentation.parseSpec("image.citroen_logo | sensor.citroen_batteri |", "");
        check("image.citroen_logo".equals(s3.displayEntity));
        check("sensor.citroen_batteri".equals(s3.linkedSensor));
        check(s3.customLabel == null);
        check(s3.explicitHide);

        // Reverse order: sensor first, image second
        EntityPresentation.EntitySpec s4 = EntityPresentation.parseSpec("sensor.citroen_batteri | image.citroen_logo", "");
        check("image.citroen_logo".equals(s4.displayEntity));
        check("sensor.citroen_batteri".equals(s4.linkedSensor));

        // Sensor from action entity
        EntityPresentation.EntitySpec s5 = EntityPresentation.parseSpec("image.mg_logo|MG", "sensor.lsjwx4098tn028453_soc");
        check("image.mg_logo".equals(s5.displayEntity));
        check("sensor.lsjwx4098tn028453_soc".equals(s5.linkedSensor));
        check("MG".equals(s5.customLabel));

        // Attribute parsing checks
        EntityPresentation.EntitySpec s6 = EntityPresentation.parseSpec("image.citroen_logo | sensor.citroen_batteri | #range", "");
        check("image.citroen_logo".equals(s6.displayEntity));
        check("sensor.citroen_batteri".equals(s6.linkedSensor));
        check("range".equals(s6.sensorAttribute));
        check(s6.customLabel == null);

        EntityPresentation.EntitySpec s7 = EntityPresentation.parseSpec("sensor.lsjwx4098tn028453_soc#range", "");
        check("sensor.lsjwx4098tn028453_soc".equals(s7.displayEntity));
        check("range".equals(s7.displayAttribute));

        EntityPresentation.EntitySpec s8 = EntityPresentation.parseSpec("image.mg_logo | sensor.lsjwx4098tn028453_soc | sensor.lsjwx4098tn028453_range", "");
        check("image.mg_logo".equals(s8.displayEntity));
        check("sensor.lsjwx4098tn028453_soc".equals(s8.linkedSensor));
        check("sensor.lsjwx4098tn028453_range".equals(s8.secondarySensor));

        EntityPresentation.EntitySpec s9 = EntityPresentation.parseSpec("image.citroen_logo | sensor.citroen_batteri | #3", "");
        check("image.citroen_logo".equals(s9.displayEntity));
        check("sensor.citroen_batteri".equals(s9.linkedSensor));
        check("3".equals(s9.sensorAttribute));

        // Attribute extraction & formatting tests
        java.util.Map<String, Object> attrs = new java.util.LinkedHashMap<>();
        attrs.put("state_class", "measurement");
        attrs.put("Last updated", "2026-10-25T15:17:55Z");
        attrs.put("range", 240.0);
        attrs.put("unit_of_measurement", "km");
        attrs.put("current_temperature", 21.5);

        // Lookup by exact name, case-insensitive, and index
        check(Double.valueOf(240.0).equals(EntityPresentation.getAttribute(attrs, "range")));
        check(Double.valueOf(240.0).equals(EntityPresentation.getAttribute(attrs, "Range")));
        check(Double.valueOf(240.0).equals(EntityPresentation.getAttribute(attrs, "#range")));
        check("measurement".equals(EntityPresentation.getAttribute(attrs, "#1")));
        check("2026-10-25T15:17:55Z".equals(EntityPresentation.getAttribute(attrs, "#2")));
        check(Double.valueOf(240.0).equals(EntityPresentation.getAttribute(attrs, "#3")));

        // Formatting
        check("240 km".equals(EntityPresentation.formatAttributeValue(240.0, "range", attrs)));
        check("21.5 °C".equals(EntityPresentation.formatAttributeValue(21.5, "current_temperature", attrs)));
        check("15:17".equals(EntityPresentation.formatAttributeValue("2026-10-25T15:17:55Z", "Last updated", attrs)));
        check("Ja".equals(EntityPresentation.formatAttributeValue(true, "charging", attrs)));

        System.out.println("Entity presentation: " + checks + " checks passed");
    }
}
