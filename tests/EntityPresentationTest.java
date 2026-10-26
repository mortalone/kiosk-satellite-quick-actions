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

        // Title truncation
        check("Citroën".equals(EntityPresentation.singleLineTitle("Citroën", 14)));
        check("Citroen Batte…".equals(EntityPresentation.singleLineTitle("Citroen Batteri", 14)));
        check("MG".equals(EntityPresentation.singleLineTitle("MG\nSoC", 14)));

        System.out.println("Entity presentation: " + checks + " checks passed");
    }
}
