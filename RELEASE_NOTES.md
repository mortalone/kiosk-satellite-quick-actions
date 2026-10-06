Quick Actions & Clock 0.2.8

- Publishes the dashboard/clock and Wall Art/Fotoo changes in the dedicated GitHub update repository.
- Preserves battery rendering/color controls, item order, and per-item entity/time visibility rules from 0.2.7.
- Adds Display & clock settings (saved): dashboard actions, optional Party actions, independent clock contexts, date, size and position. These persist across restarts without exceeding Kiosk's 20-setting limit.
- Keeps the existing Party Mode opt-in handshake. The plugin clock is always hidden in Party Mode.
- Adds explicit Fotoo launch/attach and Show Wall Art now commands for HA scripts.
- Adds foreground/Party visibility checks and manifest budget validation, alongside existing battery tests.

Install/update using https://github.com/mortalone/kiosk-satellite-quick-actions in Kiosk Satellite's Plugin Manager. Approve host.control for the Wall Art command if prompted. Existing settings keep their keys.
