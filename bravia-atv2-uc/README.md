# Sony BRAVIA 4K GB (BRAVIA_ATV2_UC), Living Room

Android TV 9, `PTT1.190515.001.S54 672201`. Same platform and build as `examples/sony-bravia-4k-gb-us`.
Cleaned 2026-11-17, checked across a reboot (disabled list held, boots to the new Home).

Household keeps: Spotify, Disney+, HBO Max (`com.wbd.stream`), YouTube, YouTube TV, Netflix, Chromecast,
Google Assistant, the ambient screensaver. No antenna. HDMI 1 Apple TV, HDMI 2 Xbox.

| File | What |
|---|---|
| `config.json` | Launcher theme: 6-app dock in that order, Live TV and old HBO app hidden |
| `batch1-ads-tracking.txt` | Samba ACR, Sony promos/Select/Discover, recommendations, demo, device agents, feedback |
| `batch2-preloads.txt` | Help/manual, Play Movies/Games/Music, YT Music, DIRECTV RVU, Alexa setup, partner apps not on the list |
| `batch3-optional.txt` | Miracast, phone remote, DLNA, USB players (Chromecast and screensaver kept, commented) |

The stock launcher `com.google.android.tvlauncher` is disabled by `launcher/build-and-install.sh`.

## Undo / change

```bash
export TV_HOST=192.168.1.183      # TV needs Developer options > ADB debugging on
tools/restore.sh bravia-atv2-uc/batch2-preloads.txt   # re-enable a whole batch
tools/venv/bin/python -I tools/tv.py 'pm enable com.hulu.livingroomplus'   # or one app
tools/theme.sh bravia-atv2-uc     # after editing config.json (dock order, columns, wallpaper)
```
No computer: Settings > Apps > See all apps > Show system apps > the app > Enable.
Old Home back: enable `com.google.android.tvlauncher`, or uninstall CleanYourTV in Settings > Apps.
After a Sony firmware update: `tools/venv/bin/python -I tools/check.py bravia-atv2-uc/batch1-ads-tracking.txt` (and each batch).

## Speed tweaks (2026-11-17)

Prime Video and Apple TV re-enabled (in the grid under the dock). Cleared app caches (~0.5 GB back),
animation scales 0.5 (undo: `settings put global window_animation_scale 1`, same for
`transition_animation_scale` and `animator_duration_scale`), Japanese keyboard disabled, the household's
apps compiled with `cmd package compile -m speed-profile`. Free RAM went from ~375 MB to ~770 MB.
||||||| parent of 43f227c9 (bravia-atv2-uc: greeting, Weather (Fahrenheit) and Digital Rain cards)
## Extras (2026-11-17)

Greeting "Good evening, Garrett" (`greeting_name` in `config.json`). Add-ons installed as Home cards:
Weather (built with Fahrenheit as the default, `addons/weather/.../options.xml`) and Digital Rain.
Digital Rain as the system screensaver instead of the ambient one:
`tools/venv/bin/python -I tools/tv.py 'settings put secure screensaver_components io.github.dennisoleary.digitalrain/.RainDream'`
