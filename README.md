# mpv-tv

Android TV-optimized fork of [mpv-android](https://github.com/mpv-android/mpv-android). General-purpose video player that stays in sync with upstream while adding TV-focused improvements.

## Features

### From upstream (unchanged)
- Hardware and software video decoding
- Gesture-based seeking, volume/brightness control
- libass subtitles with secondary subtitle support
- High-quality rendering (scalers, debanding, interpolation)
- Background playback, Picture-in-Picture
- Network stream playback (HTTP, RTMP, RTSP, etc.)

### New in mpv-tv

- **External config directory** — ADB-writable without root
- **Stream recording** — toggle from QuickPanel, timestamped filenames, persistent REC indicator
- **Quick Settings Panel** — long-press D-pad UP: speed, aspect, deband, deinterlace, record
- **D-pad long-press actions** — 60s seek on long-press LEFT/RIGHT, QuickPanel on long-press UP
- **Config presets** — bundled + GitHub Pages, device-recommended on first launch
- **Key remapping UI** — capture remote buttons, export to input.conf
- **Device profile** — hardware detection with Shizuku-enhanced mode
- **URL input** — with history and M3U playlist support
- **Watch history** — resume past sessions with position tracking
- **Screenshot gallery** — browse and delete captured screenshots
- **Bridge APK** — optional intent forwarder from `is.xyz.mpv` to `com.qutaiba.mpvtv`
- **Extended intent extras** — external apps can control playback per-launch

## Package & Compatibility

| | Stock mpv-android | mpv-tv |
|---|---|---|
| Package | `is.xyz.mpv` | `com.qutaiba.mpvtv` |
| Coexist | — | Yes, both install side-by-side |
| Config path | `/data/data/is.xyz.mpv/files/` (internal, needs root) | `/sdcard/Android/data/com.qutaiba.mpvtv/files/` (ADB-writable) |
| Config editor | In-app only | ADB push + in-app |

### Bridge APK

Apps targeting `is.xyz.mpv` (like StreamRecorder) won't find mpv-tv automatically. The optional bridge APK (`is.xyz.mpv` package) forwards all intents to `com.qutaiba.mpvtv`. Install via Settings > Bridge APK.

## Config Files

ADB-push to the external config directory:
```bash
adb push mpv.conf /sdcard/Android/data/com.qutaiba.mpvtv/files/mpv.conf
adb push input.conf /sdcard/Android/data/com.qutaiba.mpvtv/files/input.conf
adb push script.lua /sdcard/Android/data/com.qutaiba.mpvtv/files/scripts/script.lua
```

### Custom options (mpvtv-* prefix)

Add these to `mpv.conf` to control mpv-tv features:
```ini
mpvtv-seekbar=normal       # normal | mini (thin line at bottom)
mpvtv-seekbar-height=3     # dp (mini mode)
mpvtv-seekbar-opacity=0.4  # 0.0-1.0 (mini mode)
mpvtv-seekbar-color=#FF4444
mpvtv-seekbar-mode=always  # always | on-seek | on-pause
mpvtv-quickpanel=yes       # enable quick settings panel
mpvtv-history=yes          # enable watch history
mpvtv-history-max=100      # max history entries
mpvtv-longpress-seek=60    # seconds for long-press seek
mpvtv-record-position=top-right  # top-left | top-right
mpvtv-record-style=text    # text | emoji
```

## Intent Extras API

External apps can control mpv-tv playback by passing extras with `ACTION_VIEW`.

### Stock mpv-android extras (unchanged)

| Extra | Type | Description |
|-------|------|-------------|
| `position` | Int (ms) | Start at position |
| `title` | String | Force media title |
| `decode_mode` | Byte (2) | Force software decode |
| `subs` | Uri[] | Subtitle URIs |
| `subs.enable` | Uri[] | Auto-select these subs |

### mpv-tv extended extras (NEW)

| Extra | Type | Example | Description |
|-------|------|---------|-------------|
| `profile` | String | `"low-latency"` | Apply an mpv profile |
| `aspect` | String | `"16:9"` | Override aspect ratio |
| `panscan` | String | `"0.42"` | Panscan value |
| `hwdec` | String | `"mediacodec"` | Hardware decoder |
| `vo` | String | `"gpu"` | Video output |
| `audio-delay` | String | `"0.2"` | Audio delay in seconds |
| `volume` | String | `"80"` | Volume level |
| `audio-spdif` | String | `"ac3,dts"` | Passthrough codecs |
| `no-resume` | Boolean | `true` | Don't resume saved position |
| `pause` | Boolean | `true` | Start paused |
| `no-pause` | Boolean | `true` | Force unpause (live) |
| `loop` | Boolean | `true` | Loop file |
| `demuxer-max-bytes` | String | `"64MiB"` | Buffer size |
| `cache` | String | `"yes"` | Enable cache |
| `osd-message` | String | `"LIVE"` | Show OSD text at start |
| `osd-duration` | Int (ms) | `5000` | Duration for OSD message |
| `mpv-options` | String | `"deband=yes\|speed=1.5"` | Pipe-separated key=value |

### Example (Kotlin)
```kotlin
val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(Uri.parse(url), "video/mp4")
    setPackage("com.qutaiba.mpvtv")  // or "is.xyz.mpv" via bridge
    putExtra("title", "Live Stream")
    putExtra("profile", "low-latency")
    putExtra("no-resume", true)
}
startActivity(intent)
```

### Return extras
| Extra | Type | Description |
|-------|------|-------------|
| `position` | Int (ms) | Last playback position |
| `duration` | Int (ms) | Total duration |

## Default Keybindings

| Key | Action |
|-----|--------|
| D-pad LEFT/RIGHT | Seek ±10s |
| Long-press LEFT/RIGHT | Seek ±60s |
| Long-press UP | Quick Settings Panel |
| FORWARD/REWIND | Seek ±30s |
| PLAYPAUSE | Toggle pause |
| 1 / 4 | Audio delay ±0.1s |
| 7 | Reset audio delay |
| 3 | Cycle aspect ratio |
| 2 / 5 | Panscan ±0.01 |
| 8 | Panscan preset (0.42) |
| 0 | Reset zoom/pan |
| 6 | Screenshot |
| BS (Back) | Show media info |

## Settings

Access via menu (gear icon) > Settings. mpv-tv adds these screens:

- **Presets** — choose config presets (bundled + online)
- **Key Mapping** — remap remote buttons with capture dialog
- **Device Profile** — hardware info + Shizuku-enhanced detection
- **Open URL** — URL input with clipboard paste + history
- **Watch History** — resume previous sessions
- **Screenshots** — browse captured screenshots
- **Bridge APK** — install/manage the intent bridge

## Upstream Sync

This fork tracks `mpv-android/mpv-android` master. See [FORK.md](FORK.md) for the 10 rules governing changes and the complete change registry.

```bash
./scripts/sync-upstream.sh  # fetch upstream, merge into master, merge into dev
```

## Building

```bash
# Native libs: extract from stock mpv-android APK or build via buildscripts/
# Then:
./gradlew :app:assembleDefaultRelease
./gradlew :bridge:assembleRelease  # bridge APK (optional)
```

See [buildscripts/README.md](buildscripts/README.md) for native library build instructions.

Based on [mpv-android](https://github.com/mpv-android/mpv-android) by the mpv team.
