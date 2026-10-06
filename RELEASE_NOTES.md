Quick Actions & Clock 0.2.9

- Expands the permanent Show on selector to all dashboard/Kiosk/Fotoo combinations and Hidden, while preserving the three existing option values.
- The Display & clock settings action remains available. Saving its dashboard checkbox updates the same Show on setting and preserves the other configured actions.
- Uses Activity resumed/paused state rather than window focus to determine whether Kiosk is foreground. Opening a dialog no longer hides the rail or causes it to change overlay hosts.
- Refreshes presentation when the settings dialog closes, without restarting the plugin.
- Migrates the saved 0.2.8 dashboard option into Show on once. Clock preferences and existing per-item/battery controls are retained.
- Adds 44 selector combination and action-preservation checks alongside visibility, battery and manifest-limit checks.

Use https://github.com/mortalone/kiosk-satellite-quick-actions to update from Kiosk Satellite.
