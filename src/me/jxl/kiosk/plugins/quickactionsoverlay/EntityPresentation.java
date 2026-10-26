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
        final String displayAttribute;
        final String linkedSensor;
        final String sensorAttribute;
        final String secondarySensor;
        final String secondaryAttribute;
        final String customLabel;
        final boolean emptyTitle;
        final boolean explicitHide;

        EntitySpec(String displayEntity, String displayAttribute,
                   String linkedSensor, String sensorAttribute,
                   String secondarySensor, String secondaryAttribute,
                   String customLabel, boolean emptyTitle, boolean explicitHide) {
            this.displayEntity = displayEntity == null ? "" : displayEntity.trim();
            this.displayAttribute = displayAttribute == null || displayAttribute.trim().isEmpty() ? null : displayAttribute.trim();
            this.linkedSensor = linkedSensor == null ? "" : linkedSensor.trim();
            this.sensorAttribute = sensorAttribute == null || sensorAttribute.trim().isEmpty() ? null : sensorAttribute.trim();
            this.secondarySensor = secondarySensor == null ? "" : secondarySensor.trim();
            this.secondaryAttribute = secondaryAttribute == null || secondaryAttribute.trim().isEmpty() ? null : secondaryAttribute.trim();
            this.customLabel = customLabel == null || customLabel.trim().isEmpty() ? null : customLabel.trim();
            this.emptyTitle = emptyTitle;
            this.explicitHide = explicitHide;
        }

        EntitySpec(String displayEntity, String linkedSensor, String customLabel, boolean emptyTitle, boolean explicitHide) {
            this(displayEntity, null, linkedSensor, null, "", null, customLabel, emptyTitle, explicitHide);
        }
    }

    static boolean isEntityId(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase(Locale.ROOT);
        int hash = t.indexOf('#');
        if (hash >= 0) t = t.substring(0, hash).trim();
        int dot = t.indexOf('.');
        return dot > 0 && dot < t.length() - 1 && !t.contains(" ");
    }

    static boolean isSensorDomain(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase(Locale.ROOT);
        int hash = t.indexOf('#');
        if (hash >= 0) t = t.substring(0, hash).trim();
        return t.startsWith("sensor.") || t.startsWith("binary_sensor.");
    }

    static EntitySpec parseSpec(String rawDisplay, String rawAction) {
        String disp = "", dispAttr = null;
        String sensor = "", sensorAttr = null;
        String secondary = "", secondaryAttr = null;
        String label = null, pendingAttr = null;
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
                if (p.startsWith("#")) {
                    String attr = p.substring(1).trim();
                    if (!secondary.isEmpty()) {
                        secondaryAttr = attr;
                    } else if (!sensor.isEmpty()) {
                        sensorAttr = attr;
                    } else if (!disp.isEmpty()) {
                        dispAttr = attr;
                    } else {
                        pendingAttr = attr;
                    }
                    continue;
                }
                if (isEntityId(p)) {
                    int hash = p.indexOf('#');
                    String entityPart = hash >= 0 ? p.substring(0, hash).trim() : p;
                    String entityAttr = hash >= 0 ? p.substring(hash + 1).trim() : null;

                    if (isSensorDomain(entityPart)) {
                        if (sensor.isEmpty()) {
                            sensor = entityPart;
                            sensorAttr = entityAttr;
                        } else if (secondary.isEmpty()) {
                            secondary = entityPart;
                            secondaryAttr = entityAttr;
                        } else if (disp.isEmpty()) {
                            disp = entityPart;
                            dispAttr = entityAttr;
                        }
                    } else {
                        if (disp.isEmpty()) {
                            disp = entityPart;
                            dispAttr = entityAttr;
                        } else if (sensor.isEmpty()) {
                            sensor = entityPart;
                            sensorAttr = entityAttr;
                        } else if (secondary.isEmpty()) {
                            secondary = entityPart;
                            secondaryAttr = entityAttr;
                        }
                    }
                } else {
                    label = p;
                }
            }
        }

        if (rawAction != null) {
            String act = rawAction.trim();
            int pipe = act.indexOf('|');
            if (pipe >= 0) act = act.substring(0, pipe).trim();
            if (isSensorDomain(act)) {
                int hash = act.indexOf('#');
                String actEntity = hash >= 0 ? act.substring(0, hash).trim() : act;
                String actAttr = hash >= 0 ? act.substring(hash + 1).trim() : null;
                if (sensor.isEmpty()) {
                    sensor = actEntity;
                    if (actAttr != null && sensorAttr == null) sensorAttr = actAttr;
                } else if (secondary.isEmpty() && !actEntity.equals(sensor)) {
                    secondary = actEntity;
                    if (actAttr != null && secondaryAttr == null) secondaryAttr = actAttr;
                }
            }
        }

        if (disp.isEmpty() && !sensor.isEmpty()) {
            disp = sensor;
            dispAttr = sensorAttr;
            sensor = secondary;
            sensorAttr = secondaryAttr;
            secondary = "";
            secondaryAttr = null;
        }

        if (pendingAttr != null) {
            if (!secondary.isEmpty() && secondaryAttr == null) {
                secondaryAttr = pendingAttr;
            } else if (!sensor.isEmpty() && sensorAttr == null) {
                sensorAttr = pendingAttr;
            } else if (dispAttr == null) {
                dispAttr = pendingAttr;
            }
        }

        return new EntitySpec(disp, dispAttr, sensor, sensorAttr, secondary, secondaryAttr, label, emptyTitle, hide);
    }

    static Object getAttribute(java.util.Map<?, ?> attributes, String attrSpec) {
        if (attributes == null || attributes.isEmpty() || attrSpec == null || attrSpec.trim().isEmpty()) {
            return null;
        }
        String spec = attrSpec.trim();
        if (spec.startsWith("#")) spec = spec.substring(1).trim();
        if (spec.isEmpty()) return null;

        try {
            int idx = Integer.parseInt(spec);
            if (idx >= 1 && idx <= attributes.size()) {
                int count = 1;
                for (java.util.Map.Entry<?, ?> e : attributes.entrySet()) {
                    if (count == idx) {
                        return e.getValue();
                    }
                    count++;
                }
            }
        } catch (NumberFormatException ignored) {}

        if (attributes.containsKey(spec)) {
            return attributes.get(spec);
        }

        for (java.util.Map.Entry<?, ?> e : attributes.entrySet()) {
            if (String.valueOf(e.getKey()).equalsIgnoreCase(spec)) {
                return e.getValue();
            }
        }

        String norm = spec.toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        for (java.util.Map.Entry<?, ?> e : attributes.entrySet()) {
            String k = String.valueOf(e.getKey()).toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
            if (k.equals(norm) || k.replace("_", "").equals(norm.replace("_", ""))) {
                return e.getValue();
            }
        }

        for (java.util.Map.Entry<?, ?> e : attributes.entrySet()) {
            String k = String.valueOf(e.getKey()).toLowerCase(Locale.ROOT);
            if (k.contains(norm) && !k.contains("unit") && !k.contains("friendly")) {
                return e.getValue();
            }
        }

        return null;
    }

    static String formatAttributeValue(Object val, String attrName, java.util.Map<?, ?> allAttributes) {
        if (val == null) return "";
        if (val instanceof Boolean) {
            return ((Boolean) val) ? "Ja" : "Nej";
        }
        String raw = String.valueOf(val).trim();
        if (raw.endsWith(".0")) {
            raw = raw.substring(0, raw.length() - 2);
        }

        if (raw.length() >= 16 && raw.charAt(4) == '-' && raw.charAt(7) == '-' && raw.charAt(10) == 'T' && raw.charAt(13) == ':') {
            try {
                int t = raw.indexOf('T');
                if (t > 0 && t + 6 <= raw.length()) {
                    return raw.substring(t + 1, t + 6);
                }
            } catch (Throwable ignored) {}
        }

        String name = attrName == null ? "" : attrName.toLowerCase(Locale.ROOT);
        String unit = "";
        if (allAttributes != null) {
            Object u = allAttributes.get(attrName + "_unit");
            if (u == null) u = allAttributes.get(attrName + "_unit_of_measurement");
            if (u != null) {
                unit = String.valueOf(u).trim();
            } else {
                Object uom = allAttributes.get("unit_of_measurement");
                if (uom != null) {
                    String candidate = String.valueOf(uom).trim();
                    if (name.contains("temp") && (candidate.contains("°") || candidate.equalsIgnoreCase("c") || candidate.equalsIgnoreCase("f"))) {
                        unit = candidate;
                    } else if ((name.contains("range") || name.contains("distance") || name.contains("autonomi")) && (candidate.equalsIgnoreCase("km") || candidate.equalsIgnoreCase("mi"))) {
                        unit = candidate;
                    } else if ((name.contains("percent") || name.contains("soc") || name.contains("level") || name.contains("battery")) && candidate.equals("%")) {
                        unit = candidate;
                    }
                }
            }
        }

        if (unit.isEmpty()) {
            if (name.contains("range") || name.contains("autonomi") || name.contains("distance")) {
                if (!raw.endsWith("km") && !raw.endsWith("mi")) unit = "km";
            } else if (name.contains("temp")) {
                if (!raw.endsWith("°C") && !raw.endsWith("°F") && !raw.endsWith("°")) unit = "°C";
            } else if (name.contains("level") || name.contains("soc") || name.contains("percent")) {
                if (!raw.endsWith("%")) unit = "%";
            }
        }

        if (!unit.isEmpty() && !raw.endsWith(unit)) {
            return raw + " " + unit;
        }
        return raw;
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

