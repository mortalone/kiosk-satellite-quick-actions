# Quick Actions & Clock 0.2.21

- **Support explicit empty title (`image | | sensor`)**: Writing an empty middle pipe (e.g. `image.citroen_logo | | sensor.citroen_batteri`) explicitly omits the title, displaying only the battery percentage (`69 %`) next to the logo.
- **Auto-omit redundant image helper names**: When an image helper (`image.*`) is linked with a sensor and no custom title is specified, the plugin automatically omits the redundant image name (e.g. "citroen logo" or "mg logo"), showing only the live percentage (`69 %` / `84 %`) on the pill.

# Quick Actions & Clock 0.2.20

- **Throttled sequential polling (fixes "At most 8 KS commands may be pending")**: Implemented a sequential polling queue with at most 1 command in flight at a time. Prevents filling Kiosk Satellite's command buffer (limit 8) when polling multiple display entities, linked sensors, and visibility rules simultaneously.
- **Skip redundant entity polling**: Polling is skipped for entities already present in the local snapshot cache, reducing host load and avoiding command collisions during plugin update/install.

# Quick Actions & Clock 0.2.19

- **Multi-entity pipe syntax (`image|sensor|title`)**: Display entity fields now support flexible chaining via `|` in any order:
  - `image.citroen_logo | sensor.citroen_batteri`: automatically pairs the image avatar with the battery sensor.
  - `image.citroen_logo | sensor.citroen_batteri | Citroën`: pairs the image avatar with the battery sensor and assigns custom title "Citroën".
  - `image.citroen_logo | sensor.citroen_batteri |`: pairs image and battery sensor, but hides text completely for a clean circular pill.
- **Guaranteed two-line layout (no clipped battery percentage)**: Title is strictly kept on line 1 (truncated with ellipsis if over 14 characters), ensuring line 2 is always reserved for the live battery percentage (`69 %`). Long entity names or titles can no longer push the percentage off the screen.
- **Padded car logo PNGs**: Bundled Citroën and MG PNGs now include a 20% transparent inner margin, preventing the chevrons/octagon from touching or being clipped by the circular border or battery ring.

# Quick Actions & Clock 0.2.18

- **Car logo & picture rendering fix**: Prioritize circular bitmap rendering over battery fallback in `LeadingView.onDraw`, ensuring loaded images (e.g. car logos or person avatars) are always rendered with the colored battery ring border.
- **Immediate battery placeholder**: While an entity picture is being downloaded over the network or if image decoding fails (e.g. invalid URL), the view immediately falls back to displaying the linked battery state/icon rather than an empty dot.
- **Bundled car logo PNGs (`assets/icons/`)**: Added true raster PNG car logos for Citroën and MG in `assets/icons/` for direct, reliable use in Home Assistant image template helpers (avoiding Android `BitmapFactory` incompatibility with SVG endpoints).

# Quick Actions & Clock 0.2.17

- **Micro button heights (down to 24 dp)**: Total button height can now be scaled from 24 dp up to 132 dp (step 2 dp). Avatar circle, padding, and text scale cleanly for ultra-compact overlays.
- **Dynamic button width**: Pills now dynamically hug their content (`WRAP_CONTENT`), eliminating trailing empty space on the right of buttons.
- **Custom titles & per-item label hiding (`|`)**:
  - Append `|Custom Name` to any display entity to override the title (e.g. `sensor.citroen_batteri|Citroën` -> shows "Citroën" on line 1 and "69 %" on line 2; `person.dicte_hansen|Dicte` -> shows "Dicte" and "Hjemme").
  - Append `|` with nothing after (e.g. `person.malte|` or `image.aftensmad_lille|`) to hide the text for only that specific button, displaying a clean circular avatar.
- **Linked sensor pairing**: Action entity fields (`itemXAction`) can now be set to a sensor (e.g. `sensor.citroen_batteri` or `sensor.lsjwx4098tn028453_soc`). When paired with a display image/logo (e.g. car logo or `image.*`), the pill displays the logo image and live battery percentage/state together.
- **Enhanced battery icon mode**: When text labels are turned off globally or per-item, battery sensors without a picture display their percentage (e.g. `69%` or `84%`) boldly inside the circle with a colored level ring (green > 50%, yellow > 20%, red <= 20%).
- **Picture border coloring**: When an entity with an image/logo is paired with a battery sensor or has battery device_class, its circular avatar is bordered with a live battery level ring.
