# Screensaver Quick Actions 0.2.2

- Fixes installation/update failure: **Too many settings or commands**.
- Keeps the manifest at Kiosk Satellite's hard limit of 20 settings.
- Preserves all six display entities and all six separate action entities.
- Preserves position, layout, picture size, spacing, opacity and labels.
- Keeps independent visibility per Quick Action using compact rules in **Item order & visibility**.
- Rule format after `#`: `slot=entity|condition|value`, separated by semicolons.
- Supports Home Assistant state, active/inactive presence-style states, numeric thresholds/ranges and local time windows.
- Keeps the percentage/battery fix so values are not printed twice.
