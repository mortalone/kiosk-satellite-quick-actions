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


## Reordering items

Use **Item order** in the plugin settings. The values are the configured slot
numbers. For example:

`4,1,2,3,5,6`

shows Item 4 first, followed by Items 1, 2, 3, 5 and 6. You may also enter a
partial order such as `4,1,2,3`; any omitted slots are appended automatically.
