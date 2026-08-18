# mpv-tv

Android TV-optimized fork of [mpv-android](https://github.com/mpv-android/mpv-android).

General-purpose video player that stays in sync with upstream while adding TV-focused improvements.

## What's different from upstream

- **External config directory** — config files at `/sdcard/Android/data/com.qutaiba.mpvtv/files/`, writable via ADB without root
- **Built-in recording toggle** — stream recording with timestamped filenames and OSD feedback, no Lua needed
- **Android TV defaults** — ships with optimized `mpv.conf` and `input.conf` for Android TV (correct `vo`, `hwdec`, buffering, D-pad bindings)
- **TV remote keybindings** — D-pad, PLAY/PAUSE, FORWARD/REWIND, number keys mapped out of the box

## Upstream sync

This fork tracks `mpv-android/mpv-android` master. All our changes live on the `dev` branch.

### How to merge upstream updates

```bash
git fetch upstream
git checkout master
git merge upstream/master
git checkout dev
git merge master
# resolve any conflicts in our changed files
git push origin dev master
```

Our changes are isolated to a small set of files (see below), so merge conflicts should be rare.

### Files we modify (keep minimal for clean merges)

- `app/src/main/java/is/xyz/mpv/MPVActivity.kt` — config dir change (one line)
- `app/build.gradle` — package name, version
- `app/src/main/assets/mpv.conf` — default TV config (NEW file, no conflict)
- `app/src/main/assets/input.conf` — default TV keybindings (NEW file, no conflict)
- `app/src/main/java/com/qutaiba/mpvtv/RecordManager.kt` — recording toggle (NEW file)
- `README.md` — this file

## Config files

Place config files at:
```
/sdcard/Android/data/com.qutaiba.mpvtv/files/mpv.conf
/sdcard/Android/data/com.qutaiba.mpvtv/files/input.conf
/sdcard/Android/data/com.qutaiba.mpvtv/files/scripts/  (Lua scripts)
```

Push via ADB:
```bash
adb push mpv.conf /sdcard/Android/data/com.qutaiba.mpvtv/files/mpv.conf
```

## Building

See [buildscripts/README.md](buildscripts/README.md) for native library build instructions.

Based on [mpv-android](https://github.com/mpv-android/mpv-android) by the mpv team.
