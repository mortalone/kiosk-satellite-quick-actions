# Quick Actions & Clock

Shared Home Assistant entity/action rail and optional clock for the Kiosk dashboard, screensavers and Fotoo.

Install in **Kiosk Satellite → Plugin Manager → Add plugin** using:

`https://github.com/mortalone/kiosk-satellite-quick-actions`

## Main features

- Up to six Home Assistant display/action items.
- Separate display entity and action entity per item.
- Uses `entity_picture` automatically for person entities.
- Touchable scripts, buttons, automations, scenes, lights, switches, input_booleans and fans.
- Vertical or horizontal rail with eight anchor positions, including **Top center** and **Bottom center**.

Example: display `person.malte`, action `script.kald_pa_malte`.


## Reordering and per-item visibility

Use **Item order & visibility** in the plugin settings. The part before `#`
is the display order. For example:

`4,1,2,3,5,6`

shows Item 4 first, followed by Items 1, 2, 3, 5 and 6. A partial order such
as `4,1,2,3` is also valid; omitted slots are appended automatically.

Optional visibility rules go after `#`. Separate rules with semicolons:

`1,2,3,4 # 1=binary_sensor.motion|Active;3=sensor.lux|Numeric below|30`

Each rule is:

`slot=entity|condition|value`

Examples:

- `1=binary_sensor.presence|Active`
- `2=binary_sensor.door|Inactive`
- `3=sensor.lux|Numeric below|30`
- `4=sensor.temperature|Numeric between|18..24`
- `5=media_player.stueetagen|State equals|playing`
- `6=time|Time between|22:00-06:00`

Supported conditions are Active, Inactive, State equals, State not equals,
Numeric above, Numeric below, Numeric between, and Time between.


## 0.1.2 display improvements

Entity pictures are hard-clipped to a true circle, so portrait images cannot
spill outside the pill. Percentage sensors, including battery sensors, use a
compact percentage badge when there is no entity picture. Units are appended to
the secondary state, so a battery state of `91` with unit `%` is shown as
`91 %`.

The existing **Item order** setting from 0.1.1 remains available.


## 0.2.2: manifest-limit fix

Kiosk Satellite allows at most 20 settings per plugin. The four independent
visibility rules are therefore encoded compactly inside **Item order &
visibility**, keeping all four display entities, all four separate action
entities, layout, size, spacing, opacity and labels available.


## 0.2.3: battery icons

Battery entities (HA device_class=battery or mdi:battery icon) show a battery
outline with fill proportional to their remaining level. The percentage is
printed once beside the icon when labels are enabled. The icon is red at
0–20%, yellow at 21–50% and green at 51–100%. Unknown/unavailable values show
`?` rather than pretending the battery is empty. Non-battery percentage
sensors retain their circular numeric badge.

## 0.2.5: Battery color actions

Battery outline and fill use the label text color by default. Choose
**Battery icon: red/yellow/green by level (saved)** for the former level colors,
or **Battery icon: same color as text (saved)** to return. The choice applies
to all battery items and survives app restarts. Fill amount and percentage
remain independent of color.


Quick Actions 0.2.6 yields during standalone Party Mode and retains its own visibility afterward.

Party Mode 0.1.3+ can show this rail above its full-screen view with **Show Quick Actions above Party** in Party settings. Disabled by default; each action keeps its own visibility rules.


## 0.2.8: dashboard, clock and GitHub updates

Update directly from the repository URL above in Kiosk Satellite's Plugin Manager.
ZIP-installed copies have no repository source; install once through **Add plugin**
using this URL to associate the same plugin ID with its update repository. Existing
settings are preserved by Kiosk when replacing the same plugin ID.

Run **Display & clock settings (saved)** in the plugin's Actions. It opens a native
settings window on the kiosk; select dashboard actions and independent clock
visibility for dashboard, Kiosk screensaver and Fotoo. Date, size and clock position
are configurable there. Press Save; these choices persist across app restarts.
All new display contexts start off. For a compact action row, select Horizontal
and disable Show names and states in the usual plugin settings.

Party Mode keeps hiding actions by default. Its existing Show Quick Actions option
still works; the saved display window can also opt in. The plugin clock is always
hidden in Party Mode. Leave the Kiosk screensaver clock disabled here if you want
to retain the existing built-in clock without a duplicate.

The plugin still declares exactly 20 normal settings. Battery color, order,
visibility rules and all four display/action pairs are retained.

For HA switching, expose the Open Fotoo with Quick Actions, Attach Quick Actions
to Fotoo, and Show Wall Art now commands. The ready-made HA package is at:
https://github.com/mortalone/kiosk-satellite-fotoo-overlay/blob/main/examples/screensaver-mode-package.yaml


## 0.2.9: permanent Show on combinations

**Show on** is now the permanent action visibility setting. Select dashboard,
Kiosk screensaver, Fotoo, any combination, or Hidden. The three original values
remain valid and retain their meaning. The choice survives plugin/app restarts.

The **Display & clock settings (saved)** action remains available from HA and
Kiosk. Its dashboard checkbox writes to the same Show on setting; all other
plugin settings are preserved. Existing saved dashboard preferences migrate once.
Clock/date options remain in the saved action window. Dialogs no longer control
foreground visibility through window focus; close the dialog to see the result.
Party Mode continues to be controlled separately and the clock stays hidden there.

## Center positions and clock (0.2.10)

Choose **Position → Top center** or **Bottom center** for the action rail. Both layouts support these positions; horizontal layout is usually convenient at the top/bottom. Existing saved positions remain unchanged.

The optional clock is already available through **Display & clock settings (saved)** in the plugin commands. Enable **Clock on dashboard**, **Clock on Kiosk screensaver** and/or **Clock on Fotoo**, and choose position, size and whether to show the date. The clock now also supports **Top center** and **Bottom center**, with centered time/date text. Its position is independent of the action rail; avoid selecting overlapping positions. The clock follows the device's time format and updates when the minute changes. It remains hidden during Party Mode.

This release does not modify Party Mode's EQ/DSP controls.

## Home Assistant clock controls (0.2.11)

After updating and starting the plugin, the Kiosk device in HA exposes **Ur på dashboard**, **Ur på Kiosk-screensaver**, **Ur på Fotoo**, **Ur: vis dato** switches and **Ur: placering** select with all eight positions. No helpers, YAML, extra add-on or HA integration are required. The existing Kiosk integration/connection must support SDK 1 plugin entities (the same feature used by Party Mode's controls). Clock size remains available in Display & clock settings.

HA changes and the local dialog use the same saved preferences; local saves also update the HA states. Context switches can be combined. The plugin still hides the clock during Party Mode and when Quick Actions is manually hidden.

If the action rail and clock share a position, they keep a 12 dp gap: at the top the clock goes below the rail, at the bottom it goes above, and for middle side positions it goes below. The offset follows the visible rail height as item visibility, labels and layout change. If the rail disappears, the clock returns to its configured anchor. A screen that cannot fit the full stack needs smaller items/clock or fewer visible items.

## Dashboard overlay rendering (0.2.12)

Both dashboard actions and the clock use token-bound Android application panel windows. This keeps them above Kiosk's hybrid-composition WebView, including Music Home, rather than inserting them into the Activity content tree. The panels belong to Kiosk's Activity, have no input focus, and leave touches outside their bounds to the dashboard. The clock is non-touchable. A recreated Activity receives new panels; startup waits for an attached window. Existing clock/action position settings and collision spacing still apply.

## Compact actions (0.2.13)

Button height replaces Picture size. It controls the entire button (40–132 dp), including picture, padding, text size and label width. The default 76 dp retains the previous default size; try 44–48 dp on the dashboard. Four display/action pairs are available; slots 5 and 6 are no longer used. Legacy Picture size is converted to total height when no new height value is provided.

## Smart visibility and Danish state labels (0.2.14)

Enable **Smart synlighed** in the normal plugin settings (off by default; 17 settings total).
Persons are hidden outside home, including named zones, even with a separate call script.
Unknown/unavailable persons remain visible with an honest status. Lights, switches,
fans, scripts, scenes, buttons and media players remain visible in every state.
Door/window, motion/presence, leak/smoke/gas/problem/safety/tamper binary sensors
hide only when off and used solely for status. A separate action keeps them visible.
Numeric sensors and battery percentages remain visible without guessed thresholds.
Existing explicit per-item rules take precedence; `1=person.example|Always` can
exempt an item from smart visibility. Surface visibility settings still apply.

Readable Danish labels are always enabled: Hjemme/Ude, Tændt/Slukket,
Åben/Lukket, Afspiller/På pause, Ukendt/Utilgængelig, etc. Named zones and
unrecognized states retain their original value. Scripts show Kører while active;
idle scripts and scene/button timestamps do not add a misleading state line.
Display pictures, action targets and numeric units are preserved. Hidden items
collapse automatically and return on live entity updates.

## Six action slots and Home Assistant layout controls (0.2.15)

Slots 5 and 6 are restored, offering up to six display and action entities (`item1` to `item6`).
Default order includes all six items (`1,2,3,4,5,6`), and per-item visibility rules support slots 5 and 6.

To remain strictly within Kiosk Satellite's limit of 20 manifest settings (currently 19 total settings),
the rarely adjusted `opacity` and `spacing` settings have been migrated from the manifest to native controls:
- **Home Assistant Controls**: Exposes `Handlinger: Opacitet` (`select.actions_opacity`), `Handlinger: Mellemrum` (`select.actions_spacing`), `Handlinger: Placering` (`select.actions_position`), `Handlinger: Retning` (`select.actions_layout`), and `Handlinger: Vis navne og status` (`switch.actions_show_labels`).
- **Display & clock settings (saved)**: The on-device dialog includes interactive seekbars for action rail opacity (30–100%) and spacing (0–24 dp).
- Existing saved values in plugin settings or preferences are migrated seamlessly on update.

## Direct entity entry and full domain support (0.2.16)

All display and action entity settings are converted to direct text fields (`string`).
This bypasses Kiosk Satellite's built-in entity picker modal which only queries actionable
domains and excludes generic `sensor.*` states. You can now freely enter or paste any
valid entity ID, including car battery percentages (`sensor.citroen_batteri`,
`sensor.lsjwx4098tn028453_soc`), temperature and lux sensors, persons, scripts or switches.


