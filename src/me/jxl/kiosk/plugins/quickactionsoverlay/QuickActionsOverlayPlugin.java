// SPDX-License-Identifier: MIT
package me.jxl.kiosk.plugins.quickactionsoverlay;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import me.jxl.kiosk.plugins.KioskPlugin;
import me.jxl.kiosk.plugins.PluginHost;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class QuickActionsOverlayPlugin implements KioskPlugin {
    private static final int ITEM_COUNT = 6;

    private PluginHost host;
    private Context context;
    private WindowManager windowManager;
    private final Handler main = new Handler(Looper.getMainLooper());
    private ExecutorService io;

    private Application application;
    private Application.ActivityLifecycleCallbacks lifecycleCallbacks;
    private Activity currentActivity;
    private BroadcastReceiver dreamReceiver;

    private boolean dreaming;
    private boolean kioskScreensaverActive;
    private String kioskScreensaverView = "";
    private boolean showOnKiosk = true;
    private boolean showOnFotoo = true;
    private boolean forcePreview;

    private String position = "Center left";
    private String layout = "Vertical";
    private int itemSizeDp = 64;
    private int spacingDp = 8;
    private int opacity = 90;
    private boolean showLabels = true;
    private boolean coloredBattery;
    private int[] itemOrder = new int[] {0, 1, 2, 3, 4, 5};
    private String haBaseUrl = "";

    private final String[] displayEntities = new String[ITEM_COUNT];
    private final String[] actionEntities = new String[ITEM_COUNT];
    private final String[] visibilityEntities = new String[ITEM_COUNT];
    private final String[] visibilityConditions = new String[ITEM_COUNT];
    private final String[] visibilityValues = new String[ITEM_COUNT];
    private final Map<String, EntitySnapshot> snapshots = new HashMap<>();
    private final Set<String> subscriptions = new HashSet<>();

    private LinearLayout rail;
    private final List<ItemViews> itemViews = new ArrayList<>();

    @Override
    public synchronized void start(PluginHost host, Map<String, Object> settings) {
        this.host = host;
        this.context = applicationContext(host);
        if (context == null) {
            host.status("Could not obtain Android application context.", true);
            return;
        }
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        coloredBattery = context.getSharedPreferences("quick_actions_preferences", Context.MODE_PRIVATE)
                .getBoolean("colored_battery", false);
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) {
            host.status("Grant Display over other apps to Kiosk Satellite.", true);
            return;
        }

        io = Executors.newFixedThreadPool(3);
        registerDreamReceiver();
        registerActivityLifecycle();
        currentActivity = findResumedActivity();
        host.subscribe("screensaver.state");
        host.subscribe("screensaver.view");
        readHomeAssistantBaseUrl();
        applySettings(settings);
        readInitialScreensaverState();
        host.status("Ready. Quick Actions follow the selected screensavers.", false);
    }

    @Override
    public synchronized void configure(Map<String, Object> settings) {
        applySettings(settings);
    }

    @Override
    public synchronized void execute(String command, Map<String, Object> arguments) {
        if ("batteryTextColor".equals(command) || "batteryLevelColor".equals(command)) {
            final boolean useLevelColor = "batteryLevelColor".equals(command);
            main.post(() -> {
                coloredBattery = useLevelColor;
                context.getSharedPreferences("quick_actions_preferences", Context.MODE_PRIVATE)
                        .edit().putBoolean("colored_battery", useLevelColor).apply();
                refreshRail();
            });
        } else if ("show".equals(command) || "test".equals(command)) {
            forcePreview = true;
            main.post(this::updatePresentation);
        } else if ("hide".equals(command)) {
            forcePreview = false;
            main.post(this::hideRail);
        } else {
            throw new IllegalArgumentException("Unknown command: " + command);
        }
    }

    @Override
    public synchronized void onEvent(String event, Map<String, Object> payload) {
        if ("ks.screensaver.state".equals(event)) {
            kioskScreensaverActive = Boolean.TRUE.equals(payload.get("active"));
            if (!kioskScreensaverActive) kioskScreensaverView = "";
            main.post(this::updatePresentation);
            return;
        }
        if ("ks.screensaver.view".equals(event)) {
            Object value = payload.get("view");
            kioskScreensaverView = value == null ? "" : String.valueOf(value);
            main.post(this::updatePresentation);
            return;
        }
        if (!event.startsWith("ks.ha.entity.")) return;

        Object idValue = payload.get("entityId");
        String id = idValue == null
                ? event.substring("ks.ha.entity.".length())
                : String.valueOf(idValue);
        String state = payload.get("state") == null ? "" : String.valueOf(payload.get("state"));
        Object attrs = payload.get("attributes");
        snapshots.put(id, new EntitySnapshot(
                state,
                attrs instanceof Map ? (Map<?, ?>) attrs : Collections.emptyMap()));
        main.post(this::refreshRail);
    }

    @Override
    public synchronized void stop() {
        for (String entity : new HashSet<>(subscriptions)) {
            try { host.unsubscribe("ha.entity." + entity); } catch (Throwable ignored) {}
        }
        subscriptions.clear();
        if (context != null && dreamReceiver != null) {
            try { context.unregisterReceiver(dreamReceiver); } catch (Throwable ignored) {}
        }
        if (application != null && lifecycleCallbacks != null) {
            try { application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks); } catch (Throwable ignored) {}
        }
        main.post(this::hideRail);
        if (io != null) io.shutdownNow();
        io = null;
        currentActivity = null;
        host = null;
    }

    private void applySettings(Map<String, Object> values) {
        String target = stringSetting(values, "overlayTarget");
        showOnKiosk = !"Fotoo only".equals(target);
        showOnFotoo = !"Kiosk Satellite only".equals(target);

        String nextPosition = stringSetting(values, "position");
        if (!nextPosition.isEmpty()) position = nextPosition;
        String nextLayout = stringSetting(values, "layout");
        if (!nextLayout.isEmpty()) layout = nextLayout;
        itemSizeDp = intSetting(values, "itemSize", 64, 48, 120);
        spacingDp = intSetting(values, "spacing", 8, 0, 24);
        opacity = intSetting(values, "opacity", 90, 30, 100);
        showLabels = values.get("showLabels") == null ||
                Boolean.TRUE.equals(values.get("showLabels"));
        String itemRules = stringSetting(values, "itemOrder");
        itemOrder = parseItemOrder(itemRules);
        parseVisibilityRules(itemRules);

        Set<String> wanted = new HashSet<>();
        for (int i = 0; i < ITEM_COUNT; i++) {
            int slot = i + 1;
            displayEntities[i] = stringSetting(values, "item" + slot + "Entity");
            actionEntities[i] = stringSetting(values, "item" + slot + "Action");
            // Display and visibility entities need live state. Action-only
            // entities do not: taps can call their service without consuming
            // one of the plugin host's 16 entity subscriptions.
            if (!displayEntities[i].isEmpty()) wanted.add(displayEntities[i]);
            if (!visibilityEntities[i].isEmpty() &&
                    !"Always".equals(visibilityConditions[i]) &&
                    !"Time between".equals(visibilityConditions[i])) {
                wanted.add(visibilityEntities[i]);
            }
        }

        for (String old : new HashSet<>(subscriptions)) {
            if (!wanted.contains(old)) {
                try { host.unsubscribe("ha.entity." + old); } catch (Throwable ignored) {}
                subscriptions.remove(old);
            }
        }
        for (String entity : wanted) {
            if (subscriptions.add(entity)) host.subscribe("ha.entity." + entity);
            pollEntity(entity);
        }

        main.post(() -> {
            hideRail();
            updatePresentation();
        });
    }

    private void readHomeAssistantBaseUrl() {
        host.executeCommand("getDashboardState", Collections.emptyMap(), (ok, data, error) -> {
            if (!ok || !(data instanceof Map)) return;
            Object value = ((Map<?, ?>) data).get("homeAssistantUrl");
            if (value != null) haBaseUrl = String.valueOf(value);
        });
    }

    private void pollEntity(String entity) {
        if (entity == null || entity.isEmpty() || host == null) return;
        Map<String, Object> args = new HashMap<>();
        args.put("entityId", entity);
        host.executeCommand("getHaEntityState", args, (ok, data, error) -> {
            if (!ok || !(data instanceof Map)) return;
            Map<?, ?> m = (Map<?, ?>) data;
            String state = m.get("state") == null ? "" : String.valueOf(m.get("state"));
            Object attrs = m.get("attributes");
            snapshots.put(entity, new EntitySnapshot(
                    state,
                    attrs instanceof Map ? (Map<?, ?>) attrs : Collections.emptyMap()));
            main.post(this::refreshRail);
        });
    }

    private boolean overlayActive() {
        if (context != null) {
            android.content.SharedPreferences party = context.getSharedPreferences("party_mode_presentation", Context.MODE_PRIVATE);
            if (party.getBoolean("party_fullscreen", false) && party.getLong("party_until_ms", 0) > System.currentTimeMillis()) return party.getBoolean("allow_quick_actions", false);
        }
        if (context != null) {
            android.content.SharedPreferences presentation = context.getSharedPreferences("now_playing_presentation", Context.MODE_PRIVATE);
            if (presentation.getBoolean("party_fullscreen", false) &&
                    presentation.getLong("party_until_ms", 0) > System.currentTimeMillis()) return false;
        }
        boolean kiosk = showOnKiosk && kioskScreensaverActive &&
                !"black".equals(kioskScreensaverView) &&
                !"blank".equals(kioskScreensaverView);
        return forcePreview || kiosk || (showOnFotoo && dreaming);
    }

    private void updatePresentation() {
        if (!overlayActive()) {
            hideRail();
            return;
        }
        ensureRail();
        refreshRail();
    }

    private void ensureRail() {
        if (rail != null || context == null) return;

        rail = new LinearLayout(context);
        rail.setTag("quick-actions-overlay:rail");
        rail.setOrientation("Horizontal".equals(layout)
                ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER);
        rail.setAlpha(opacity / 100f);

        itemViews.clear();
        for (int orderIndex = 0; orderIndex < ITEM_COUNT; orderIndex++) {
            final int i = itemOrder[orderIndex];
            if (displayEntities[i] == null || displayEntities[i].isEmpty()) continue;
            final int index = i;

            LinearLayout item = new LinearLayout(context);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            int pad = dp(6);
            item.setPadding(pad, pad, pad, pad);
            item.setBackground(cardBackground(0xC7222328, 999));
            item.setClickable(true);
            item.setOnClickListener(v -> performAction(index));

            LeadingView leading = new LeadingView(context);
            int avatarSize = dp(itemSizeDp);
            item.addView(leading, new LinearLayout.LayoutParams(avatarSize, avatarSize));

            TextView label = new TextView(context);
            label.setTextColor(Color.WHITE);
            label.setTextSize(14);
            label.setMaxLines(2);
            label.setPadding(dp(8), 0, dp(6), 0);
            if (showLabels) {
                item.addView(label, new LinearLayout.LayoutParams(
                        dp(130), ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    avatarSize + dp(12));
            if ("Horizontal".equals(layout)) itemParams.rightMargin = dp(spacingDp);
            else itemParams.bottomMargin = dp(spacingDp);
            rail.addView(item, itemParams);
            itemViews.add(new ItemViews(index, item, leading, label));
        }

        int gravity = gravityForPosition(position);
        try {
            if (!addOverlayView(
                    rail,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    gravity,
                    dp(16))) {
                throw new IllegalStateException("No overlay host available");
            }
        } catch (Throwable error) {
            host.status("Quick Actions overlay failed: " + safeMessage(error), true);
            rail = null;
            itemViews.clear();
        }
    }

    private void refreshRail() {
        if (rail == null) return;
        for (ItemViews item : itemViews) {
            boolean visible = itemVisible(item.index);
            item.root.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (!visible) continue;

            String entity = displayEntities[item.index];
            EntitySnapshot snapshot = snapshots.get(entity);
            if (snapshot == null) {
                item.label.setText(entity);
                continue;
            }

            String friendly = attr(snapshot.attributes, "friendly_name", entity);
            String state = snapshot.state;
            String unit = attr(snapshot.attributes, "unit_of_measurement", "").trim();
            String deviceClass = attr(snapshot.attributes, "device_class", "");
            boolean battery = BatteryLevel.isBattery(
                    deviceClass, attr(snapshot.attributes, "icon", ""));
            int batteryLevel = battery ? BatteryLevel.parse(state) : -1;
            String badge = percentageBadge(state, unit, deviceClass);

            String displayState = state;
            if (!state.isEmpty() &&
                    !"unknown".equalsIgnoreCase(state) &&
                    !"unavailable".equalsIgnoreCase(state) &&
                    !unit.isEmpty() &&
                    !state.endsWith(unit)) {
                displayState = state + ("%".equals(unit) ? " %" : " " + unit);
            }

            // Batteries show a filled icon and one percentage beside it.
            // Other percentage sensors keep the number inside the circle.
            item.label.setText(
                    battery && batteryLevel >= 0
                            ? friendly + "\n" + batteryLevel + " %"
                            : !badge.isEmpty()
                            ? friendly
                            : (displayState.isEmpty() ||
                               "unknown".equalsIgnoreCase(displayState)
                                    ? friendly
                                    : friendly + "\n" + displayState));

            String picture = attr(snapshot.attributes, "entity_picture", "");
            if (battery) {
                item.picture = "";
                item.leading.setBatteryLevel(batteryLevel, coloredBattery, item.label.getCurrentTextColor());
            } else if (!picture.isEmpty()) {
                loadPicture(item, picture);
            } else {
                item.picture = "";
                item.leading.setBadgeText(
                        badge.isEmpty() ? initials(friendly) : badge);
            }
        }
    }

    private boolean itemVisible(int index) {
        String condition = visibilityConditions[index];
        if (condition == null || condition.isEmpty() || "Always".equals(condition)) {
            return true;
        }
        String value = visibilityValues[index] == null ? "" : visibilityValues[index].trim();

        if ("Time between".equals(condition)) {
            return timeBetween(value);
        }

        String entity = visibilityEntities[index];
        if (entity == null || entity.isEmpty()) return true;
        EntitySnapshot snapshot = snapshots.get(entity);
        if (snapshot == null) return false;
        String state = snapshot.state == null ? "" : snapshot.state.trim();

        if ("Active".equals(condition)) return activeState(state);
        if ("Inactive".equals(condition)) return !activeState(state);
        if ("State equals".equals(condition)) return state.equalsIgnoreCase(value);
        if ("State not equals".equals(condition)) return !state.equalsIgnoreCase(value);

        Double number = parseNumber(state);
        if (number == null) return false;
        if ("Numeric above".equals(condition)) {
            Double threshold = parseNumber(value);
            return threshold != null && number > threshold;
        }
        if ("Numeric below".equals(condition)) {
            Double threshold = parseNumber(value);
            return threshold != null && number < threshold;
        }
        if ("Numeric between".equals(condition)) {
            double[] bounds = parseRange(value);
            return bounds != null && number >= Math.min(bounds[0], bounds[1]) &&
                    number <= Math.max(bounds[0], bounds[1]);
        }
        return true;
    }

    private static boolean activeState(String state) {
        String s = state == null ? "" : state.trim().toLowerCase(java.util.Locale.ROOT);
        return "on".equals(s) || "true".equals(s) || "home".equals(s) ||
                "playing".equals(s) || "open".equals(s) || "detected".equals(s) ||
                "occupied".equals(s) || "present".equals(s);
    }

    private static Double parseNumber(String value) {
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static double[] parseRange(String value) {
        if (value == null) return null;
        String[] parts = value.trim().split("\\.\\.");
        if (parts.length != 2) return null;
        Double a = parseNumber(parts[0]);
        Double b = parseNumber(parts[1]);
        return a == null || b == null ? null : new double[] {a, b};
    }

    private static boolean timeBetween(String value) {
        if (value == null) return true;
        String[] parts = value.trim().split("\\s*-\\s*");
        if (parts.length != 2) return true;
        try {
            LocalTime from = LocalTime.parse(parts[0].trim());
            LocalTime until = LocalTime.parse(parts[1].trim());
            LocalTime now = LocalTime.now();
            if (from.equals(until)) return true;
            if (from.isBefore(until)) {
                return !now.isBefore(from) && now.isBefore(until);
            }
            // Overnight window, e.g. 22:00-06:00.
            return !now.isBefore(from) || now.isBefore(until);
        } catch (DateTimeParseException ignored) {
            return true;
        }
    }

    private void loadPicture(ItemViews item, String picture) {
        if (picture.equals(item.picture) || io == null) return;
        item.picture = picture;
        String url = resolveHaUrl(picture);
        if (url == null) return;
        io.execute(() -> {
            Bitmap bitmap = fetchBitmap(url);
            main.post(() -> {
                if (bitmap != null && picture.equals(item.picture)) {
                    item.leading.setPicture(bitmap);
                }
            });
        });
    }

    private void performAction(int index) {
        String entity = actionEntities[index];
        if (entity == null || entity.isEmpty()) entity = displayEntities[index];
        if (entity == null || entity.isEmpty() || host == null) return;

        int dot = entity.indexOf('.');
        if (dot <= 0) return;
        String domain = entity.substring(0, dot);
        String service;

        if ("script".equals(domain)) service = "turn_on";
        else if ("button".equals(domain) || "input_button".equals(domain)) service = "press";
        else if ("automation".equals(domain)) service = "trigger";
        else if ("scene".equals(domain)) service = "turn_on";
        else if ("switch".equals(domain) || "input_boolean".equals(domain) ||
                "light".equals(domain) || "fan".equals(domain)) service = "toggle";
        else {
            domain = "homeassistant";
            service = "toggle";
        }

        Map<String, Object> args = new HashMap<>();
        args.put("domain", domain);
        args.put("service", service);
        args.put("entity_id", entity);
        final String actionEntity = entity;
        host.executeCommand("haCallService", args, (ok, data, error) -> {
            if (!ok) {
                host.status(
                        "Quick action failed: " +
                                (error == null ? actionEntity : error),
                        true);
            }
        });
    }

    private void hideRail() {
        View view = rail;
        rail = null;
        itemViews.clear();
        removeOverlayView(view);
    }

    private boolean preferInAppOverlay() {
        return showOnKiosk && kioskScreensaverActive;
    }

    private boolean addOverlayView(
            View view, int width, int height, int gravity, int edge) {
        if (preferInAppOverlay()) {
            Activity activity = activeKioskActivity();
            if (activity != null) {
                View content = activity.findViewById(android.R.id.content);
                if (content instanceof FrameLayout) {
                    FrameLayout root = (FrameLayout) content;
                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(width, height, gravity);
                    if ((gravity & Gravity.LEFT) == Gravity.LEFT) params.leftMargin = edge;
                    if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) params.rightMargin = edge;
                    if ((gravity & Gravity.TOP) == Gravity.TOP) params.topMargin = edge;
                    if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) params.bottomMargin = edge;
                    root.addView(view, params);
                    if (Build.VERSION.SDK_INT >= 21) view.setZ(100100f);
                    view.bringToFront();
                    return true;
                }
            }
        }

        if (windowManager == null) return false;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width,
                height,
                Build.VERSION.SDK_INT >= 26
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = gravity;
        params.x = ((gravity & Gravity.LEFT) == Gravity.LEFT ||
                (gravity & Gravity.RIGHT) == Gravity.RIGHT) ? edge : 0;
        params.y = ((gravity & Gravity.TOP) == Gravity.TOP ||
                (gravity & Gravity.BOTTOM) == Gravity.BOTTOM) ? edge : 0;
        windowManager.addView(view, params);
        return true;
    }

    private void removeOverlayView(View view) {
        if (view == null) return;
        try {
            ViewParent parent = view.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(view);
                return;
            }
        } catch (Throwable ignored) {}
        if (windowManager != null) {
            try { windowManager.removeViewImmediate(view); } catch (Throwable ignored) {}
        }
    }

    private int gravityForPosition(String p) {
        if ("Top right".equals(p)) return Gravity.TOP | Gravity.RIGHT;
        if ("Center left".equals(p)) return Gravity.CENTER_VERTICAL | Gravity.LEFT;
        if ("Center right".equals(p)) return Gravity.CENTER_VERTICAL | Gravity.RIGHT;
        if ("Bottom left".equals(p)) return Gravity.BOTTOM | Gravity.LEFT;
        if ("Bottom right".equals(p)) return Gravity.BOTTOM | Gravity.RIGHT;
        return Gravity.TOP | Gravity.LEFT;
    }

    private void readInitialScreensaverState() {
        host.executeCommand(
                "isScreensaverActive",
                Collections.emptyMap(),
                (ok, data, error) -> {
                    if (ok && data instanceof Boolean) {
                        kioskScreensaverActive = (Boolean) data;
                        main.post(this::updatePresentation);
                    }
                });
    }

    private void registerDreamReceiver() {
        dreamReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ignored, Intent intent) {
                if ("me.jxl.kiosk.plugins.PARTY_PRESENTATION_CHANGED".equals(intent.getAction())) {
                    updatePresentation();
                } else if (Intent.ACTION_DREAMING_STARTED.equals(intent.getAction())) {
                    dreaming = true;
                    updatePresentation();
                } else if (Intent.ACTION_DREAMING_STOPPED.equals(intent.getAction())) {
                    dreaming = false;
                    forcePreview = false;
                    updatePresentation();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("me.jxl.kiosk.plugins.PARTY_PRESENTATION_CHANGED");
        filter.addAction(Intent.ACTION_DREAMING_STARTED);
        filter.addAction(Intent.ACTION_DREAMING_STOPPED);
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(dreamReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(dreamReceiver, filter);
        }
    }

    private void registerActivityLifecycle() {
        Context appContext = context.getApplicationContext();
        if (!(appContext instanceof Application)) return;
        application = (Application) appContext;
        lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityResumed(Activity a) {
                if (a.getPackageName().equals(context.getPackageName())) currentActivity = a;
            }
            @Override public void onActivityPaused(Activity a) {
                if (currentActivity == a) currentActivity = null;
            }
            @Override public void onActivityStopped(Activity a) {
                if (currentActivity == a) currentActivity = null;
            }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {
                if (currentActivity == a) currentActivity = null;
            }
        };
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
    }

    private Activity activeKioskActivity() {
        Activity a = currentActivity;
        if (a != null && !a.isFinishing() &&
                (Build.VERSION.SDK_INT < 17 || !a.isDestroyed())) return a;
        a = findResumedActivity();
        if (a != null) currentActivity = a;
        return a;
    }

    private Activity findResumedActivity() {
        try {
            Class<?> threadClass = Class.forName("android.app.ActivityThread");
            Method currentThread =
                    threadClass.getDeclaredMethod("currentActivityThread");
            currentThread.setAccessible(true);
            Object thread = currentThread.invoke(null);
            if (thread == null) return null;

            Field activitiesField = threadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Object activitiesObject = activitiesField.get(thread);
            if (!(activitiesObject instanceof Map)) return null;

            Activity fallback = null;
            for (Object record : ((Map<?, ?>) activitiesObject).values()) {
                if (record == null) continue;
                Field activityField = record.getClass().getDeclaredField("activity");
                activityField.setAccessible(true);
                Object value = activityField.get(record);
                if (!(value instanceof Activity)) continue;
                Activity a = (Activity) value;
                if (!a.getPackageName().equals(context.getPackageName()) ||
                        a.isFinishing() ||
                        (Build.VERSION.SDK_INT >= 17 && a.isDestroyed())) continue;
                if (a.hasWindowFocus()) return a;
                fallback = a;
            }
            return fallback;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Context applicationContext(PluginHost host) {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentApplication =
                    activityThread.getDeclaredMethod("currentApplication");
            currentApplication.setAccessible(true);
            Object value = currentApplication.invoke(null);
            if (value instanceof Application) {
                return ((Application) value).getApplicationContext();
            }
            if (value instanceof Context) {
                return ((Context) value).getApplicationContext();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private Bitmap fetchBitmap(String source) {
        URLConnection connection = null;
        InputStream stream = null;
        try {
            connection = new URL(source).openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setInstanceFollowRedirects(true);
            }
            stream = connection.getInputStream();
            return BitmapFactory.decodeStream(stream);
        } catch (Throwable ignored) {
            return null;
        } finally {
            try { if (stream != null) stream.close(); } catch (Throwable ignored) {}
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
    }

    private String resolveHaUrl(String path) {
        if (path == null || path.isEmpty()) return null;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        if (haBaseUrl == null || haBaseUrl.isEmpty()) return null;
        String base = haBaseUrl.endsWith("/")
                ? haBaseUrl.substring(0, haBaseUrl.length() - 1)
                : haBaseUrl;
        return base + (path.startsWith("/") ? path : "/" + path);
    }

    private GradientDrawable cardBackground(int color, int radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radiusDp));
        return bg;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private void parseVisibilityRules(String raw) {
        for (int i = 0; i < ITEM_COUNT; i++) {
            visibilityEntities[i] = "";
            visibilityConditions[i] = "Always";
            visibilityValues[i] = "";
        }

        if (raw == null) return;
        int marker = raw.indexOf('#');
        if (marker < 0 || marker + 1 >= raw.length()) return;

        String rules = raw.substring(marker + 1).trim();
        if (rules.isEmpty()) return;

        for (String entry : rules.split("\\s*;\\s*")) {
            if (entry == null || entry.trim().isEmpty()) continue;
            int equals = entry.indexOf('=');
            if (equals <= 0) continue;

            int slot;
            try {
                slot = Integer.parseInt(entry.substring(0, equals).trim()) - 1;
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (slot < 0 || slot >= ITEM_COUNT) continue;

            String spec = entry.substring(equals + 1).trim();
            String[] parts = spec.split("\\|", -1);
            if (parts.length < 2) continue;

            String entity = parts[0].trim();
            String condition = canonicalVisibilityCondition(parts[1]);
            StringBuilder value = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                if (i > 2) value.append('|');
                value.append(parts[i]);
            }

            visibilityConditions[slot] = condition;
            visibilityValues[slot] = value.toString().trim();

            if (!"Time between".equals(condition) &&
                    !"time".equalsIgnoreCase(entity) &&
                    !"@time".equalsIgnoreCase(entity)) {
                visibilityEntities[slot] = entity;
            }
        }
    }

    private static String canonicalVisibilityCondition(String raw) {
        String value = raw == null
                ? ""
                : raw.trim().toLowerCase(java.util.Locale.ROOT);
        if ("active".equals(value)) return "Active";
        if ("inactive".equals(value)) return "Inactive";
        if ("state equals".equals(value)) return "State equals";
        if ("state not equals".equals(value)) return "State not equals";
        if ("numeric above".equals(value)) return "Numeric above";
        if ("numeric below".equals(value)) return "Numeric below";
        if ("numeric between".equals(value)) return "Numeric between";
        if ("time between".equals(value)) return "Time between";
        return "Always";
    }

    private static int[] parseItemOrder(String raw) {
        int[] fallback = new int[] {0, 1, 2, 3, 4, 5};
        if (raw == null || raw.trim().isEmpty()) return fallback;

        int marker = raw.indexOf('#');
        if (marker >= 0) raw = raw.substring(0, marker);
        if (raw.trim().isEmpty()) return fallback;

        int[] result = new int[ITEM_COUNT];
        boolean[] used = new boolean[ITEM_COUNT];
        int count = 0;

        String[] parts = raw.split("[,;\\s]+");
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) continue;
            try {
                int slot = Integer.parseInt(part.trim()) - 1;
                if (slot >= 0 && slot < ITEM_COUNT && !used[slot]) {
                    result[count++] = slot;
                    used[slot] = true;
                }
            } catch (NumberFormatException ignored) {}
        }

        for (int slot = 0; slot < ITEM_COUNT; slot++) {
            if (!used[slot]) result[count++] = slot;
        }
        return result;
    }

    private static String stringSetting(Map<String, Object> values, String key) {
        Object value = values == null ? null : values.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static int intSetting(
            Map<String, Object> values,
            String key,
            int fallback,
            int min,
            int max) {
        Object value = values == null ? null : values.get(key);
        int result = value instanceof Number
                ? ((Number) value).intValue()
                : fallback;
        return Math.max(min, Math.min(max, result));
    }

    private static String attr(Map<?, ?> attrs, String key, String fallback) {
        Object value = attrs == null ? null : attrs.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static String safeMessage(Throwable error) {
        String value = error == null ? null : error.getMessage();
        return value == null || value.isEmpty()
                ? (error == null ? "Unknown error" : error.getClass().getSimpleName())
                : value;
    }

    private static final class EntitySnapshot {
        final String state;
        final Map<?, ?> attributes;
        EntitySnapshot(String state, Map<?, ?> attributes) {
            this.state = state == null ? "" : state;
            this.attributes = attributes == null
                    ? Collections.emptyMap()
                    : attributes;
        }
    }

    private static String percentageBadge(
            String state,
            String unit,
            String deviceClass) {
        boolean percentage = "%".equals(unit) || "battery".equalsIgnoreCase(deviceClass);
        if (!percentage || state == null) return "";
        try {
            double value = Double.parseDouble(state.trim().replace(',', '.'));
            int rounded = (int) Math.round(value);
            return Math.max(0, Math.min(100, rounded)) + "%";
        } catch (NumberFormatException ignored) {
            return "";
        }
    }

    private static String initials(String value) {
        if (value == null || value.trim().isEmpty()) return "•";
        String[] parts = value.trim().split("\\s+");
        if (parts.length == 1) {
            String p = parts[0];
            return p.substring(0, Math.min(2, p.length())).toUpperCase(java.util.Locale.ROOT);
        }
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * Circular leading content for each pill. A real entity_picture is
     * center-cropped inside a hard circular clip so it can never spill out of
     * the pill. Batteries use a filled icon; other percentage sensors use
     * their value in the same circle. Other entities fall back to initials.
     */
    private static final class LeadingView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clip = new Path();
        private final Rect src = new Rect();
        private final RectF dst = new RectF();
        private Bitmap bitmap;
        private String badgeText = "•";
        private boolean battery;
        private int batteryLevel = -1;
        private boolean coloredBattery;
        private int batteryColor = Color.WHITE;

        LeadingView(Context context) {
            super(context);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(
                    Math.max(1f, context.getResources().getDisplayMetrics().density));
            border.setColor(0x66FFFFFF);

            text.setColor(Color.WHITE);
            text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            text.setTextAlign(Paint.Align.CENTER);
        }

        void setPicture(Bitmap value) {
            battery = false;
            bitmap = value;
            invalidate();
        }

        void setBadgeText(String value) {
            battery = false;
            bitmap = null;
            badgeText = value == null || value.isEmpty() ? "•" : value;
            invalidate();
        }

        void setBatteryLevel(int value, boolean colored, int textColor) {
            bitmap = null;
            battery = true;
            batteryLevel = value;
            coloredBattery = colored;
            batteryColor = textColor;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float size = Math.min(getWidth(), getHeight());
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = size / 2f;

            paint.setColor(0xFF3F4248);
            canvas.drawCircle(cx, cy, radius, paint);

            Bitmap b = bitmap;
            if (battery) {
                drawBattery(canvas, cx, cy, size);
            } else if (b != null && b.getWidth() > 0 && b.getHeight() > 0) {
                int bw = b.getWidth();
                int bh = b.getHeight();
                int crop;
                if (bw > bh) {
                    crop = bh;
                    int left = (bw - crop) / 2;
                    src.set(left, 0, left + crop, crop);
                } else {
                    crop = bw;
                    int top = (bh - crop) / 2;
                    src.set(0, top, crop, top + crop);
                }

                dst.set(cx - radius, cy - radius, cx + radius, cy + radius);
                clip.reset();
                clip.addCircle(cx, cy, radius, Path.Direction.CW);
                int save = canvas.save();
                canvas.clipPath(clip);
                canvas.drawBitmap(b, src, dst, paint);
                canvas.restoreToCount(save);
            } else {
                String value = badgeText == null ? "•" : badgeText;
                float density = getResources().getDisplayMetrics().scaledDensity;
                float sp = value.length() >= 4 ? 13f : value.length() >= 3 ? 15f : 18f;
                text.setTextSize(sp * density);
                Paint.FontMetrics fm = text.getFontMetrics();
                float baseline = cy - (fm.ascent + fm.descent) / 2f;
                canvas.drawText(value, cx, baseline, text);
            }

            canvas.drawCircle(
                    cx,
                    cy,
                    Math.max(0f, radius - border.getStrokeWidth() / 2f),
                    border);
        }

        private void drawBattery(Canvas canvas, float cx, float cy, float size) {
            float left = cx - size * 0.27f;
            float right = cx + size * 0.23f;
            float top = cy - size * 0.16f;
            float bottom = cy + size * 0.16f;
            float stroke = Math.max(1f, size * 0.025f);
            paint.setColor(batteryColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            canvas.drawRoundRect(left, top, right, bottom, size * 0.035f, size * 0.035f, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(right + stroke, cy - size * 0.07f,
                    right + size * 0.06f, cy + size * 0.07f, paint);
            if (batteryLevel >= 0) {
                float inset = stroke * 1.7f;
                float innerWidth = Math.max(0f, right - left - inset * 2f);
                paint.setColor(BatteryLevel.fillColor(batteryLevel, coloredBattery, batteryColor));
                if (batteryLevel > 0) canvas.drawRect(left + inset, top + inset,
                        left + inset + innerWidth * batteryLevel / 100f, bottom - inset, paint);
            } else {
                text.setTextSize(size * 0.24f);
                Paint.FontMetrics fm = text.getFontMetrics();
                canvas.drawText("?", (left + right) / 2f,
                        cy - (fm.ascent + fm.descent) / 2f, text);
            }
        }
    }

    private static final class ItemViews {
        final int index;
        final View root;
        final LeadingView leading;
        final TextView label;
        String picture = "";
        ItemViews(int index, View root, LeadingView leading, TextView label) {
            this.index = index;
            this.root = root;
            this.leading = leading;
            this.label = label;
        }
    }
}
