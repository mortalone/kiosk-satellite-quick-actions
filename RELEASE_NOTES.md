# Quick Actions & Clock 0.2.15

- Up to six Home Assistant display/action items (Slots 1 to 6).
- Item order and visibility rules support all six slots (`1,2,3,4,5,6`).
- Manifest layout settings streamlined to 19 total settings, complying with Kiosk Satellite's 20-setting limit.
- Action rail opacity and spacing moved to native controls:
  - Configurable directly from Home Assistant via `select.actions_opacity` ("Handlinger: Opacitet") and `select.actions_spacing` ("Handlinger: Mellemrum").
  - Configurable locally on the kiosk device via **Display & clock settings (saved)** dialog seekbars.
  - Previous configurations in plugin settings or preferences are migrated seamlessly.
- Position, layout direction, and label visibility are also controllable from Home Assistant (`select.actions_position`, `select.actions_layout`, `switch.actions_show_labels`).
