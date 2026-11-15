# Sony KD-55X75K runbook (Garrett's TV), for Claude Code on the Mac

Prepared in a cloud session that could not reach the TV. Nothing has touched the TV yet.
Follow `AGENTS.md` and `PROMPT.md`; this file only adds Garrett's answers and the order of work.
Every step that changes the TV needs Garrett's OK at that moment.

## Garrett's answers (already given; do not ask again)

| Question | Answer |
|---|---|
| Dock | Disney+, HBO Max, Netflix, YouTube TV (the live-TV service app, `com.google.android.youtube.tvunplugged`); everything else in the grid |
| Assistant / voice | uses it |
| Phone casting (Chromecast) | uses it |
| AirPlay | uses it |
| Live TV / antenna | does NOT use it |
| HDMI-CEC | uses it |
| Sony Input menu | uses it |
| Ambient / photo screensaver | uses it |
| Accessibility features | uses them |
| Netflix / YouTube / Prime Video / YouTube Music buttons | uses them |
| Clock on Home | yes |
| Scope | new launcher first, then the reviewed cleanup |
| Wireless debugging, temporarily | yes, close it at the end |
| HomeKeyService (accessibility helper, key filtering off) | yes |
| Audio | Sonos Ray on optical; VRC-001 remote UNPAIRED, IR volume/mute drives the Ray. Never pair it. |

Still open, check on the TV: HBO Max is `com.wbd.stream` (Max) or the older `com.hbo.hbonow`. `config.xml`
lists both and skips the missing one; if both are installed, remove the one he doesn't use before building.
Optional look-and-feel switches are in `AUDIT.md` ("More functionality"); apply only the ones he picks.

## Already done in the repo (branch `claude/optimistic-maxwell-316d4k`)

`launcher/app/src/main/res/values/config.xml`:
- `dock_apps`: Disney+, HBO Max, Netflix, YouTube TV; `columns` 4
- `hidden_apps`: `com.android.tv` (Live TV unused); `live_tv_package` empty
- `stock_home_redirect` = `com.google.android.apps.tv.launcherx`, `key_filter` false, `restore_key_services` true
  (the route in `examples/sony-bravia-vu3/README.md`, "Install with launcherx kept")
- `reduced_motion` true

Cleanup lists, beside this file (`kd-55x75k/`): `batch1-sony-promos.txt`, `batch2-ads-reporting.txt`,
`batch3-optional-ask.txt`. All are lines from `examples/sony-bravia-vu3/debloat.txt`. Left out on purpose:
dreamx (screensaver), setupwraith (casting), launcherx (app buttons + Home redirect).

## 0. Mac setup

```bash
git clone -b claude/optimistic-maxwell-316d4k https://github.com/garrettawolfe/cleanyourtv && cd cleanyourtv
git remote add upstream https://github.com/Gathin23/cleanyourtv && git fetch upstream
git log --oneline upstream/main -1; git diff upstream/main --stat   # only config.xml + kd-55x75k/ should differ
brew install openjdk@17 android-commandlinetools android-platform-tools
sdkmanager "platforms;android-34" "build-tools;34.0.0"
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
```

## 1. Connect (Wireless debugging, no port 5555)

TV: Settings > System > About > Build x7 > Developer options > Wireless debugging on > Pair device with pairing code.

```bash
adb pair <tv-ip>:<pairing-port>          # code from the TV
adb connect <tv-ip>:<connection-port>    # port from the main Wireless debugging screen; note it
export TV_ADB_BINARY="$(which adb)" TV_HOST=<tv-ip>:<connection-port>
tools/venv/bin/python -I tools/tv.py --probe
```

Do not run `adb tcpip 5555`. If macOS says "No route to host", see `tools/native-adb.md` (macOS section).

## 2. Read-only baseline (save under backup/, which git ignores)

```bash
T="tools/venv/bin/python -I tools/tv.py"
mkdir -p backup shots
$T 'getprop ro.product.model; getprop ro.product.name; getprop ro.build.fingerprint; getprop ro.build.version.sdk'
$T 'pm list packages'      > backup/packages.txt
$T 'pm list packages -d'   > backup/disabled.txt
$T 'getprop'               > backup/getprop.txt
for ns in global secure system; do $T "settings list $ns" > backup/settings-$ns.txt; done
$T 'cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME'
$T 'settings get secure enabled_accessibility_services'   # write this down: the exact value to put back
$T 'settings get secure accessibility_enabled'
$T 'wm size; wm density'                                    # expect an override 1920x1080; leave it
$T 'pm list users'
$T "dumpsys wifi | grep -o 'cname=[^ ]*'"
$T --screenshot shots/00-before.png
# Resource baseline. Not tools/bench.py: it benchmarks whatever answers HOME, and with this setup that
# stays launcherx before AND after, so it would compare Google TV with itself (see AUDIT.md).
$T 'dumpsys meminfo | grep -E "Total RAM|Free RAM|Used RAM"' > backup/mem-before.txt
$T 'top -b -n 1 | head -25'                                   > backup/top-before.txt
$T 'dumpsys window policy | grep -A30 GlobalKey'               > backup/globalkeys.txt   # app-button routing
# Read-only privacy report (changes nothing); compare its candidates with the batches, add none without asking.
tools/venv/bin/python -I tools/privacy.py > backup/privacy-report.txt
```

Check: build is `UKR1.240726.001` (else say so and slow down). Grep `backup/packages.txt` for the dock
packages and the batch lists. Garrett's accessibility features: whatever `enabled_accessibility_services`
holds now must still be there after step 4 (append HomeKeyService, never overwrite).

Baseline with Garrett on the real remote, and write down what each does NOW: Home, the 4 app buttons, Input,
the Settings/gear button (on this model it opens Sony's Quick Settings ribbon), Ray volume up/down/mute and
HOLD volume up (ramp), cast from phone, AirPlay, "Hey Google" (the mic may not work with the remote unpaired;
then it was already off before we started), the screensaver, CEC (a device on HDMI powers on/off with the TV).

## 3. Launcher (stock launcher stays enabled). Needs Garrett's OK.

```bash
STOCK_LAUNCHER=none RESTORE_KEY_SERVICES=1 launcher/build-and-install.sh
```
Its Home check reports launcherx and exits 1: expected with launcherx kept.
APK must be < 589,824 B: `stat -f %z launcher/app/build/outputs/apk/release/app-release.apk`.

## 4. HomeKeyService. Needs Garrett's OK.

Append, don't overwrite (keep his existing accessibility services, `:`-separated):
```bash
PREV="$($T 'settings get secure enabled_accessibility_services')"
NEW="com.gathin.tvhome/.HomeKeyService"; [ -n "$PREV" ] && [ "$PREV" != "null" ] && NEW="$PREV:$NEW"
$T "settings put secure enabled_accessibility_services '$NEW'"
$T 'settings put secure accessibility_enabled 1'
```
Then test with Garrett: Home from an app lands on `com.gathin.tvhome/.MainActivity`
(`$T 'dumpsys activity activities | grep mFocusedApp'`; Google TV home may flash first), D-pad from every
edge, Inputs pill (each HDMI input shows a picture), Settings pill, the gear button still opens Sony's
ribbon, each app button on the REAL remote, Ray volume tap + hold, mute.
If an app button now does nothing: `$T 'logcat -d | grep BAL_BLOCK'`; if launcherx is blocked, the Philips fix
in `docs/TROUBLESHOOTING.md` (`appops set com.google.android.apps.tv.launcherx SYSTEM_ALERT_WINDOW allow`) - ask first.
Don't force-stop the launcher from Settings (unbinds the helper until Home starts again).

## 5. Cleanup, one batch at a time. Each batch needs Garrett's OK.

```bash
tools/disable.sh kd-55x75k/batch1-sony-promos.txt     # then retest, screenshot shots/01.png
tools/disable.sh kd-55x75k/batch2-ads-reporting.txt   # retest + cast, AirPlay, Assistant, playback
# batch3: go through each line with Garrett, comment out what he keeps, then apply
```
First regression: `tools/restore.sh <that batch>` and stop.

## 6. Reboot once (Garrett's OK), then re-verify

`$T sync`, wait 15 s, reboot. Reconnect (Wireless debugging may switch itself off at reboot on this model:
turn it back on, connect port changes). Check `pm list packages -d` against the batches; power-on lands on
the new Home (`$T 'logcat -b events -d | grep wm_set_resumed_activity'`); HomeKeyService still in
`enabled_accessibility_services`; repeat the step 4 tests including casting (setupwraith was kept, so it
should survive) and the screensaver. Then the after-numbers: same `dumpsys meminfo` and `top` as step 2
into `backup/*-after.txt`, compared like for like (same app state, a few minutes after boot).

## 7. Close ADB and hand over

1. Note the current connection port. TV: Wireless debugging OFF; Developer options > Revoke USB debugging
   authorisations; Paired devices > forget the Mac. `adb disconnect`.
2. `python3 tools/adb-closed.py <tv-ip> <connection-port>` must say closed.
3. Reboot, confirm Wireless debugging still off, `python3 tools/adb-closed.py <tv-ip>` (5555) must say closed.

## After a Sony firmware update (handover note for Garrett)

An update can re-enable packages or reset Home. With the Mac connected the same way:
```bash
for b in kd-55x75k/batch*.txt; do tools/venv/bin/python -I tools/check.py "$b" com.google.android.apps.tv.launcherx; done
```
The second argument matters: without it check.py expects the new launcher to own Home and reports drift
every time (docs/firmware-updates.md, "When the stock launcher stays Home on purpose"). Close ADB again after.

## Rollback

| Undo | How |
|---|---|
| A batch | `TV_HOST=... tools/restore.sh kd-55x75k/<batch>.txt` |
| The helper | put back the exact value saved in step 2 with `settings put secure enabled_accessibility_services` |
| Home | Launcher settings > Choose default home > Google TV, or Settings > Apps > CleanYourTV > Uninstall |
| No computer | Settings > Apps > show system apps > the package > Enable |
| Before a factory reset | nothing to do: setupwraith and launcherx were never disabled |
