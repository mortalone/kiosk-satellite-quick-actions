Quick Actions & Clock 0.2.12

- Moves both dashboard Quick Actions and the dashboard clock into Activity-bound panel windows, above the hybrid-composition WebView. They no longer rely on the dashboard content view staying above Flutter's renderer.
- Reattaches both panels when Kiosk recreates its Activity, and waits for a valid window token during startup.
- Keeps the existing HA switches, clock position and shared-position spacing. Kiosk screensaver, Fotoo and Party visibility rules remain unchanged.

Update Quick Actions & Clock from https://github.com/mortalone/kiosk-satellite-quick-actions in Kiosk Satellite Plugin Manager. Clock on dashboard uses the existing HA switch; the action rail uses Show on: Dashboard + Kiosk Satellite (or another combination including Dashboard).

Validation: Android build and existing visibility, target, battery and clock-placement tests in GitHub Actions. The affected Raspberry Pi dashboard still requires device verification after installation.
