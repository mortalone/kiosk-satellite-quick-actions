# Screensaver Quick Actions 0.2.3

- Show battery sensors as a battery icon whose fill tracks the current remaining percentage.
- Show the percentage once beside the icon when labels are enabled. Other percentage sensors keep their circular percentage badge.
- Use red up to 20%, yellow up to 50%, and green above 50%.
- Recognize battery device class and mdi:battery icons. Unknown or unavailable levels show an outline with a question mark, not an empty battery.
- Battery icons take precedence over entity pictures; unrelated entity pictures and initials are unchanged.
- Check parsing of empty/full levels, clamping, decimal comma and unavailable/non-finite values in CI.
