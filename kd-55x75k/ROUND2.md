# KD-55X75K round 2: what the living-room BRAVIA got (2026-11-17)

Same setup as `RUNBOOK.md` (launcherx stays Home, HomeKeyService redirect, key_filter false). Bring over the extras
proven on `bravia-atv2-uc/`. Each TV-changing step needs Garrett's OK at that moment.

## 1. Connect (Wireless debugging; it turns itself off at every reboot on this model)

TV: Settings > System > Developer options > Wireless debugging on > Pair device with pairing code.
```bash
adb pair <tv-ip>:<pair-port>; adb connect <tv-ip>:<connect-port>
export TV_ADB_BINARY="$(which adb)" TV_HOST=<tv-ip>:<connect-port>
T(){ tools/venv/bin/python -I tools/tv.py "$@"; }
T 'getprop ro.build.fingerprint'; T 'pm list packages -d' | sort > backup/kd-55x75k/disabled-round2-before.txt
T "settings get secure enabled_accessibility_services"   # must still hold HomeKeyService after step 2
T 'dumpsys meminfo | grep -E "Free RAM|Used RAM"'
```

## 2. Launcher with the weather line (rebuild from main; config.xml already holds this TV's dock)

```bash
STOCK_LAUNCHER=none RESTORE_KEY_SERVICES=1 launcher/build-and-install.sh   # Home check reports launcherx: expected
stat -f %z launcher/app/build/outputs/apk/release/app-release.apk          # < 589,824
```
Then Home from an app must still land on `com.gathin.tvhome` (HomeKeyService redirect).

## 3. Weather add-on (Fahrenheit) + greeting

```bash
(cd addons/weather && ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ./gradlew -q assembleRelease)
T --push addons/weather/app/build/outputs/apk/release/app-release.apk /data/local/tmp/addon.apk
T 'pm install -r /data/local/tmp/addon.apk; rm -f /data/local/tmp/addon.apk'
tools/theme.sh kd-55x75k        # greeting_name Garrett, game_day = Weather
T 'am start -n io.github.sumyabazar1.weather/.WeatherActivity'   # Garrett types the city; answer "Show card" on Home
T 'content query --uri content://io.github.sumyabazar1.weather.home/turns'   # the line under the clock
```

## 4. Privacy pick-up and ad-blocking DNS

```bash
tools/venv/bin/python -I tools/privacy.py > backup/kd-55x75k/privacy-round2.txt   # read-only; review with Garrett
T 'settings put global private_dns_specifier dns.adguard-dns.com'; T 'settings put global private_dns_mode hostname'
T "dumpsys connectivity | grep -o 'PrivateDnsServerName: [^ ]*'"; T 'ping -c1 -W3 doubleclick.net | head -1'
```
Then open YouTube TV and AirPlay from an iPhone: both must still work (undo: `private_dns_mode opportunistic`).

## 5. Speed

```bash
T 'pm trim-caches 999G'
for s in window_animation_scale transition_animation_scale animator_duration_scale; do T "settings put global $s 0.5"; done
for p in com.disney.disneyplus com.wbd.stream com.netflix.ninja com.google.android.youtube.tvunplugged \
         com.amazon.amazonvideo.livingroom com.apple.atve.androidtv.appletv com.gathin.tvhome; do
  T "cmd package compile -m speed-profile $p"; done
T 'dumpsys meminfo | grep -E "Free RAM|Used RAM"'
```

## 6. Reboot check, then close ADB (RUNBOOK.md steps 6 and 7)

`T sync`, wait 15 s, reboot; disabled list unchanged, power-on lands on the new Home, HomeKeyService still bound.
Wireless debugging off, revoke authorisations, `tools/adb-closed.py <tv-ip> <port>` and then 5555: both closed.
