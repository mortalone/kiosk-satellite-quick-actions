# Screensaver Quick Actions

Touchable Home Assistant entity/action rail for Kiosk Satellite screensavers and Fotoo.

Install in **Kiosk Satellite → Plugin Manager → Add plugin** using:

`https://github.com/mortalone/kiosk-satellite-quick-actions`

## Main features

- Up to six Home Assistant display/action items.
- Separate display entity and action entity per item.
- Uses `entity_picture` automatically for person entities.
- Touchable scripts, buttons, automations, scenes, lights, switches, input_booleans and fans.
- Vertical or horizontal rail with six anchor positions.

Example: display `person.malte`, action `script.kald_pa_malte`.


## Reordering and per-item visibility

Use **Item order & visibility** in the plugin settings. The part before `#`
is the display order. For example:

`4,1,2,3,5,6`

shows Item 4 first, followed by Items 1, 2, 3, 5 and 6. A partial order such
as `4,1,2,3` is also valid; omitted slots are appended automatically.

Optional visibility rules go after `#`. Separate rules with semicolons:

`1,2,3,4,5,6 # 1=binary_sensor.motion|Active;3=sensor.lux|Numeric below|30`

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

Kiosk Satellite allows at most 20 settings per plugin. The six independent
visibility rules are therefore encoded compactly inside **Item order &
visibility**, keeping all six display entities, all six separate action
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
