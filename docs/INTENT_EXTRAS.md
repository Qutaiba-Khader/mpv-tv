# mpv-tv Intent Extras API

External apps can control mpv-tv playback by passing intent extras when launching it via `ACTION_VIEW`.

## Stock mpv-android extras (unchanged)

| Extra | Type | Description |
|-------|------|-------------|
| `position` | Int (ms) | Start at position |
| `title` | String | Force media title |
| `decode_mode` | Byte (2) | Force software decode |
| `subs` | Uri[] | Subtitle file URIs |
| `subs.enable` | Uri[] | Auto-select these subs |

## mpv-tv extended extras

### Video

| Extra | Type | Example | Description |
|-------|------|---------|-------------|
| `aspect` | String | `"16:9"`, `"4:3"`, `"2.35:1"` | Override aspect ratio |
| `panscan` | String | `"0.42"` | Panscan (0.0 - 1.0) |
| `hwdec` | String | `"mediacodec"`, `"no"` | Hardware decoder |
| `vo` | String | `"gpu"`, `"mediacodec_embed"` | Video output |

### Audio

| Extra | Type | Example | Description |
|-------|------|---------|-------------|
| `audio-delay` | String | `"0.2"`, `"-0.1"` | Audio delay in seconds |
| `volume` | String | `"80"` | Volume (0-100) |
| `audio-spdif` | String | `"ac3,dts"` | Passthrough codecs |

### Playback control

| Extra | Type | Example | Description |
|-------|------|---------|-------------|
| `profile` | String | `"low-latency"` | Apply an mpv profile |
| `no-resume` | Boolean | `true` | Don't resume from saved position |
| `pause` | Boolean | `true` | Start paused |
| `no-pause` | Boolean | `true` | Force unpause (live streams) |
| `loop` | Boolean | `true` | Loop the file |

### Buffering / demuxer

| Extra | Type | Example | Description |
|-------|------|---------|-------------|
| `demuxer-max-bytes` | String | `"64MiB"` | Demuxer buffer size |
| `demuxer-readahead-secs` | String | `"30"` | Read-ahead seconds |
| `cache` | String | `"yes"`, `"no"` | Enable/disable cache |

### OSD

| Extra | Type | Example | Description |
|-------|------|---------|-------------|
| `osd-message` | String | `"Live: Channel 5"` | Show OSD text at start |
| `osd-duration` | Int (ms) | `5000` | Duration for osd-message |

### Arbitrary mpv options

| Extra | Type | Example | Description |
|-------|------|---------|-------------|
| `mpv-options` | String | `"deband=yes\|speed=1.5"` | Pipe-separated key=value pairs |

The `mpv-options` extra is the escape hatch — any valid mpv option can be passed this way. Options are set as `file-local-options` so they only affect the current file.

## Usage examples

### Kotlin (from StreamRecorder TV)

```kotlin
val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(Uri.parse(streamUrl), "video/mp4")
    setPackage("com.qutaiba.mpvtv")
    putExtra("title", "TikTok Live: @username")
    putExtra("profile", "low-latency")
    putExtra("no-resume", true)
    putExtra("osd-message", "LIVE: @username")
}
startActivity(intent)
```

### ADB (testing)

```bash
adb shell am start -a android.intent.action.VIEW \
  -d "https://example.com/stream.m3u8" \
  -t "video/*" \
  -n com.qutaiba.mpvtv/is.xyz.mpv.MPVActivity \
  --es title "Test Stream" \
  --es profile "low-latency" \
  --ez no-resume true \
  --es osd-message "Testing live"
```

### Via bridge APK (is.xyz.mpv)

If the bridge APK is installed, apps targeting `is.xyz.mpv` automatically forward to mpv-tv with all extras preserved:

```kotlin
val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(Uri.parse(url), "video/mp4")
    setPackage("is.xyz.mpv")  // bridge forwards to com.qutaiba.mpvtv
    putExtra("profile", "low-latency")
}
startActivity(intent)
```

## Return extras

On playback exit, mpv-tv returns:

| Extra | Type | Description |
|-------|------|-------------|
| `position` | Int (ms) | Last playback position |
| `duration` | Int (ms) | Total duration |

Result code is `RESULT_OK` if playback started, `RESULT_CANCELED` otherwise.
