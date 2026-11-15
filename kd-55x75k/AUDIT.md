# KD-55X75K plan audit

Desk audit of the plan in `RUNBOOK.md` against the repo at upstream commit `ddc3b2c`: the docs, the model
folder `examples/sony-bravia-vu3/`, `docs/TROUBLESHOOTING.md`, and the launcher and tools source.
Nothing here has been run on the TV yet.

## Source integrity

- The fork's base equals `Gathin23/cleanyourtv` HEAD (`git ls-remote`). The only changes on top are
  `launcher/app/src/main/res/values/config.xml` and this folder.
- Read before relying on them:
  - `tools/disable.sh`: it refuses core packages and has the Home and setupwraith guards.
  - `tools/native-adb.md` / `tools/tv.py`: the Wireless debugging path, `TV_ADB_BINARY`, never runs `adb tcpip`.
  - `HomeKeyService.java`: the Home redirect logic.
  - `tools/bench.py` and `tools/check.py`.
- Repo tests: 3,662 run. Two fail, and only because `config.xml` now differs from the shipped defaults
  (`test_key_filter`, `test_theme_key_defaults`). That's expected for a one-TV setup. It is also why this
  branch must never go upstream.
- APK not built here: the Android SDK host is blocked in the cloud session. It gets built on the Mac,
  where the size gate (< 589,824 B) is checked.

## Findings, most important first

| # | Finding | Effect | Handled by |
|---|---|---|---|
| 1 | `tools/bench.py` benchmarks the app that answers HOME. Here that stays launcherx before and after, so it would compare Google TV with itself. | A before/after launcher benchmark would be meaningless | Runbook uses device-wide `dumpsys meminfo` + `top` before/after instead; no speed claim beyond those numbers |
| 2 | Garrett's remote is unpaired (IR). Both documented VU3 units used it over Bluetooth. | The app-button and volume results come from Bluetooth mode | launcherx stays enabled, so the buttons take the stock route whatever the scancodes; every button is re-tested on the real remote |
| 3 | The remote's mic (Assistant) works only over Bluetooth. | "Hey Google" from the remote is probably already off; the phone / Google Home app still works | Recorded in the baseline as "before", so it isn't counted as a regression |
| 4 | With a key-filtering helper, the gear button opens Android Settings instead of Sony's Quick Settings ribbon (#2131, same model). | Would lose picture/sound quick settings | `key_filter` false: the helper never sees keys. Gear tested before and after |
| 5 | With `key_filter` false the helper redirects by watching windows, not keys. | Google TV's home flashes briefly before the new Home (documented on unit 2). Settings that need keys don't work: `settings_key_panel`, `app_keys`, `hold:` second apps, `home_long_press_home` | Accepted trade for keeping the volume ramp and Sony's gear; those switches are left off |
| 6 | Philips TA5: with launcherx re-enabled, its app-button launches were blocked as background starts (`BAL_BLOCK`). Not seen on the VU3. | App buttons could stop after Home moves | Runbook checks logcat for `BAL_BLOCK` and offers the documented `appops` fix, with Garrett's OK |
| 7 | `settings put secure enabled_accessibility_services X` replaces the whole list. | Would switch off Garrett's own accessibility features | Runbook saves the old value, appends HomeKeyService, and restores the saved value on undo |
| 8 | `check.py` after a firmware update expects the new launcher on Home unless told otherwise. | False "drift" every run, and `--fix` would fight launcherx | Runbook passes `com.google.android.apps.tv.launcherx` as the second argument |
| 9 | setupwraith kept | Casting survives reboots. The Sony logcat flood that a disabled setupwraith causes (VH22) does not happen | Already in the plan |
| 10 | Wireless debugging switches itself off at every reboot on this model; port 5555 opened only with USB debugging on. | Reconnect after the test reboot (new port); good for closing ADB | Runbook step 6 / 7 |
| 11 | The display already has a 1920x1080 override on the 4K panel. | Not a launcher bug if the UI looks soft | Leave it; recorded in the baseline |
| 12 | `restore_key_services` needs WRITE_SECURE_SETTINGS. | Without it, a force-stop drops the helper until it's re-enabled by hand | Install runs with `RESTORE_KEY_SERVICES=1`, which grants it |

Not applicable here:
- The Skyworth multi-user hotkey crash: different board, and launcherx stays on anyway.
- The ARC soundbar standby issue: the Ray is on optical and driven by IR.
- CVTE rules: no `com.cvte.*` on this platform; verify in the package list.

## More functionality (all offline; none on unless Garrett picks it)

Recommended for "fast and minimal":

| Switch (`config.xml`) | What it does | Recommendation |
|---|---|---|
| `show_greeting` false | Drops "Good evening" above the clock; the clock and date stay | Yes, for minimal |
| `show_network_pill` false | Hides the Wi-Fi pill (also keeps the network name out of screenshots) | Yes, unless he wants Wi-Fi status on Home |
| `inputs_connected_only` true | Inputs lists only HDMI ports with a device on; the rest fold under "More inputs" | Yes, if he uses HDMI devices; test that each shows correctly |
| `always_show_names` true | Names under every tile, dimmed until focused | Optional: easier to read from the couch |
| `app_library` true | An App library pill after the last row: Home holds only the apps he keeps there | Optional: a tidier grid |
| `show_home_settings_shortcut` true | A visible tile that opens the home picker | Optional: a remote-only way back to Google TV |
| `open_on_start_choice` true | Launcher settings gains "Open on start": e.g. YouTube TV opens at power-on, once per boot | Optional |

Already chosen: `reduced_motion` true, 4-app dock, `columns` 4, Live TV hidden.

Available but not recommended here:
- **Clock screensaver:** Garrett uses Ambient.
- **`advanced_effects`, wallpaper rotation, `show_stats`:** extra memory or clutter.
- **Private DNS ad blocking:** `PROMPT.md` offers it. It doesn't block YouTube ads, it goes around a router's DNS filtering, and the original brief excluded network changes unless asked separately.

Can be changed later without rebuilding: on the TV, hold OK on Settings → Launcher settings for apps per
row, rows below the dock, dock opacity, clock 12/24 h, animations, and Reorder apps.
