package me.jxl.kiosk.plugins.quickactionsoverlay;

import java.util.Locale;

/** State presentation independent of Android; never infers numeric alert thresholds. */
final class EntityPresentation {
    private EntityPresentation() {}
    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static String lower(String s) { return clean(s).toLowerCase(Locale.ROOT); }
    private static String domain(String entity) {
        String s = lower(entity);
        int pipe = s.indexOf('|');
        if (pipe >= 0) s = s.substring(0, pipe).trim();
        int dot = s.indexOf('.');
        return dot < 0 ? s : s.substring(0, dot);
    }
    private static boolean uncertain(String state) {
        return state.isEmpty() || "unknown".equals(state) || "unavailable".equals(state);
    }
    private static boolean actionable(String entity) {
        String d = domain(entity);
        return "script".equals(d) || "scene".equals(d) || "button".equals(d) ||
                "input_button".equals(d) || "automation".equals(d) || "light".equals(d) ||
                "switch".equals(d) || "fan".equals(d) || "input_boolean".equals(d) ||
                "media_player".equals(d) || "cover".equals(d) || "climate".equals(d) || "lock".equals(d);
    }
    private static boolean statusOnlyClass(String c) {
        return "door".equals(c) || "window".equals(c) || "opening".equals(c) ||
                "motion".equals(c) || "occupancy".equals(c) || "presence".equals(c) ||
                "moisture".equals(c) || "smoke".equals(c) || "gas".equals(c) ||
                "carbon_monoxide".equals(c) || "problem".equals(c) || "safety".equals(c) || "tamper".equals(c);
    }
    static boolean visible(boolean smart, String entity, String action, String state, String deviceClass) {
        if (!smart) return true;
        String s = lower(state), d = domain(entity);
        // Missing data is visible, never presented as absence or a healthy sensor.
        if (uncertain(s)) return true;
        if ("person".equals(d)) return "home".equals(s);
        if (actionable(entity) || (!clean(action).isEmpty() && !clean(action).equals(clean(entity)))) return true;
        if ("binary_sensor".equals(d) && statusOnlyClass(lower(deviceClass))) return !"off".equals(s);
        return true;
    }
    static String text(String entity, String state, String deviceClass, String unit) {
        String raw = clean(state), s = lower(state), d = domain(entity), c = lower(deviceClass);
        if (s.isEmpty() || "unknown".equals(s)) return "Ukendt";
        if ("unavailable".equals(s)) return "Utilgængelig";
        if ("person".equals(d) || "device_tracker".equals(d)) {
            if ("home".equals(s)) return "Hjemme";
            if ("not_home".equals(s)) return "Ude";
            return raw;
        }
        if ("script".equals(d)) return "on".equals(s) ? "Kører" : "off".equals(s) ? "" : raw;
        if ("scene".equals(d) || "button".equals(d) || "input_button".equals(d)) return "";
        if ("on".equals(s) || "off".equals(s)) {
            boolean on = "on".equals(s);
            if ("binary_sensor".equals(d)) {
                switch (c) {
                    case "door": case "window": case "opening": return on ? "Åben" : "Lukket";
                    case "motion": return on ? "Bevægelse" : "Ingen bevægelse";
                    case "occupancy": case "presence": return on ? "Tilstedeværelse" : "Ingen tilstedeværelse";
                    case "moisture": return on ? "Vand registreret" : "Ingen lækage";
                    case "smoke": return on ? "Røg registreret" : "Ingen røg";
                    case "gas": return on ? "Gas registreret" : "Ingen gas";
                    case "carbon_monoxide": return on ? "Kulilte registreret" : "Ingen kulilte";
                    case "problem": return on ? "Problem registreret" : "Ingen problemer";
                    case "safety": return on ? "Usikker" : "Sikker";
                    case "tamper": return on ? "Manipulation registreret" : "Ingen manipulation";
                    case "battery": return on ? "Lavt batteri" : "Batteri OK";
                    case "battery_charging": return on ? "Oplader" : "Oplader ikke";
                    case "connectivity": return on ? "Forbundet" : "Afbrudt";
                    default: return on ? "Aktiv" : "Inaktiv";
                }
            }
            if ("automation".equals(d) || "input_boolean".equals(d)) return on ? "Aktiveret" : "Deaktiveret";
            if (actionable(entity)) return on ? "Tændt" : "Slukket";
        }
        if ("media_player".equals(d)) {
            switch (s) {
                case "playing": return "Afspiller";
                case "paused": return "På pause";
                case "idle": return "Stoppet";
                case "standby": return "Standby";
                case "buffering": return "Indlæser";
            }
        }
        if ("cover".equals(d)) {
            switch (s) {
                case "open": return "Åben";
                case "closed": return "Lukket";
                case "opening": return "Åbner";
                case "closing": return "Lukker";
            }
        }
        if ("lock".equals(d)) {
            if ("locked".equals(s)) return "Låst";
            if ("unlocked".equals(s)) return "Ulåst";
        }
        String u = clean(unit);
        return u.isEmpty() || raw.endsWith(u) ? raw : raw + " " + u;
    }

    static final class EntitySpec {
        final String displayEntity;
        final String linkedSensor;
        final String customLabel;
        final boolean emptyTitle;
        final boolean explicitHide;

        EntitySpec(String displayEntity, String linkedSensor, String customLabel, boolean emptyTitle, boolean explicitHide) {
            this.displayEntity = displayEntity == null ? "" : displayEntity.trim();
            this.linkedSensor = linkedSensor == null ? "" : linkedSensor.trim();
            this.customLabel = customLabel == null || customLabel.trim().isEmpty() ? null : customLabel.trim();
            this.emptyTitle = emptyTitle;
            this.explicitHide = explicitHide;
        }
    }

    static boolean isEntityId(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase(Locale.ROOT);
        int dot = t.indexOf('.');
        return dot > 0 && dot < t.length() - 1 && !t.contains(" ");
    }

    static boolean isSensorDomain(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase(Locale.ROOT);
        return t.startsWith("sensor.") || t.startsWith("binary_sensor.");
    }

    static EntitySpec parseSpec(String rawDisplay, String rawAction) {
        String disp = "", sensor = "", label = null;
        boolean hide = false, emptyTitle = false;

        if (rawDisplay != null) {
            String[] parts = rawDisplay.split("\\|", -1);
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i].trim();
                if (p.isEmpty()) {
                    if (i == parts.length - 1 && parts.length > 1) {
                        hide = true;
                    } else if (i > 0 && i < parts.length - 1) {
                        emptyTitle = true;
                    }
                    continue;
                }
                if (isEntityId(p)) {
                    if (isSensorDomain(p)) {
                        if (sensor.isEmpty()) sensor = p;
                        else if (disp.isEmpty()) disp = p;
                    } else {
                        if (disp.isEmpty()) disp = p;
                        else if (sensor.isEmpty()) sensor = p;
                    }
                } else {
                    label = p;
                }
            }
        }

        if (sensor.isEmpty() && rawAction != null) {
            String act = rawAction.trim();
            int pipe = act.indexOf('|');
            if (pipe >= 0) act = act.substring(0, pipe).trim();
            if (isSensorDomain(act)) {
                sensor = act;
            }
        }

        if (disp.isEmpty() && !sensor.isEmpty()) {
            disp = sensor;
            sensor = "";
        }

        return new EntitySpec(disp, sensor, label, emptyTitle, hide);
    }

    static String singleLineTitle(String title, int maxChars) {
        if (title == null) return "";
        String clean = title.trim();
        int nl = clean.indexOf('\n');
        if (nl >= 0) clean = clean.substring(0, nl).trim();
        if (clean.length() <= maxChars) return clean;
        return clean.substring(0, Math.max(0, maxChars - 1)).trim() + "…";
    }
}

