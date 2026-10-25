# Quick Actions & Clock 0.2.17

- **Micro button heights (down to 24 dp)**: Total button height can now be scaled from 24 dp up to 132 dp (step 2 dp). Avatar circle, padding, and text scale cleanly for ultra-compact overlays.
- **Dynamic button width**: Pills now dynamically hug their content (`WRAP_CONTENT`), eliminating trailing empty space on the right of buttons.
- **Custom titles & per-item label hiding (`|`)**:
  - Append `|Custom Name` to any display entity to override the title (e.g. `sensor.citroen_batteri|Citroën` -> shows "Citroën" on line 1 and "69 %" on line 2; `person.dicte_hansen|Dicte` -> shows "Dicte" and "Hjemme").
  - Append `|` with nothing after (e.g. `person.malte|` or `image.aftensmad_lille|`) to hide the text for only that specific button, displaying a clean circular avatar.
- **Linked sensor pairing**: Action entity fields (`itemXAction`) can now be set to a sensor (e.g. `sensor.citroen_batteri` or `sensor.lsjwx4098tn028453_soc`). When paired with a display image/logo (e.g. car logo or `image.*`), the pill displays the logo image and live battery percentage/state together.
- **Enhanced battery icon mode**: When text labels are turned off globally or per-item, battery sensors without a picture display their percentage (e.g. `69%` or `84%`) boldly inside the circle with a colored level ring (green > 50%, yellow > 20%, red <= 20%).
- **Picture border coloring**: When an entity with an image/logo is paired with a battery sensor or has battery device_class, its circular avatar is bordered with a live battery level ring.
