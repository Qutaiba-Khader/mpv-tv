# mpv-tv Fork Rules

This document governs how we modify this fork. Every contributor (including AI agents) must follow these rules.

## Principle: Minimum Contact

Every change to an upstream file is a future merge conflict. We treat upstream files as read-only unless there is no alternative.

## Rule 1: Change Registry

Every modification to an upstream file MUST be registered in the table below. If it's not in this table, it doesn't ship.

### Modified Upstream Files

| File | What changed | Why it can't be a new file | Merge risk |
|------|-------------|---------------------------|------------|
| `app/build.gradle` | applicationId, versionName | Gradle requires it here | LOW — end of file, rarely conflicts |
| `app/src/main/java/is/xyz/mpv/MPVActivity.kt` | config dir: `filesDir` → `getExternalFilesDir` | Init call is in this file | LOW — one line change |
| `app/src/main/AndroidManifest.xml` | package name, any new permissions | Android requires it here | LOW — additive changes |
| `app/src/main/res/values/strings.xml` | app_name | Branding | LOW — one line |

### New Files (zero merge risk)

| File | Purpose |
|------|---------|
| `FORK.md` | This document |
| `scripts/sync-upstream.sh` | Upstream merge automation |
| `app/src/main/assets/mpv.conf` | Default TV config (shipped in APK) |
| `app/src/main/assets/input.conf` | Default TV keybindings (shipped in APK) |
| `app/src/main/java/is/xyz/mpv/RecordManager.kt` | Recording toggle with timestamped filenames |
| `app/src/main/java/is/xyz/mpv/TvDefaults.kt` | TV preset loader (applies defaults if no user config) |
| `.github/workflows/build-release.yml` | Our CI/CD pipeline |
| `.github/workflows/sync-upstream.yml` | Automated upstream sync check |

## Rule 2: New Files Over Edits

If a feature can be implemented as a NEW file that the existing code calls, do that. Don't inline logic into upstream files.

**Good:** Create `RecordManager.kt`, add one line in `MPVActivity.kt` to instantiate it.
**Bad:** Add 50 lines of recording logic directly into `MPVActivity.kt`.

## Rule 3: One Line Per Upstream Touch

When you must modify an upstream file, the change should ideally be ONE line — a function call, a variable swap, or an import. The actual logic lives in our new files.

## Rule 4: Mark Every Upstream Edit

Every line we change in an upstream file gets a comment:

```kotlin
// mpv-tv: external config dir (was: filesDir.path)
player.initialize(getExternalFilesDir(null)!!.path, cacheDir.path)
```

This makes merge conflicts obvious and grep-able: `grep -r "mpv-tv:" app/src/`

## Rule 5: Upstream Sync Process

1. `git fetch upstream`
2. `git checkout master && git merge upstream/master`
3. `git checkout dev && git merge master`
4. If conflicts: check this file's Change Registry. Our marked lines make conflicts trivial to resolve.
5. Run `grep -r "// mpv-tv:" app/src/` — every hit should match the registry above.
6. Build and test.

## Rule 6: No Upstream Cosmetic Changes

Never reformat, rename, refactor, or "clean up" upstream code. Every diff line is a future conflict. If upstream code is ugly, it stays ugly in our fork.

## Rule 7: Version Tracking

| Field | Value |
|-------|-------|
| Upstream repo | `mpv-android/mpv-android` |
| Last synced commit | `7cc841e` (buildscripts: update harfbuzz) |
| Last sync date | 2026-08-18 |
| Our package | `com.qutaiba.mpvtv` |
| Our branch | `dev` |

Update this table on every upstream sync.

## Rule 8: Build Must Pass on Clean Clone

A fresh `git clone` + build must work. No local-only dependencies, no manual steps beyond what `buildscripts/README.md` documents.

## Rule 9: Config Layering

Priority (highest wins):
1. User's `mpv.conf` / `input.conf` in external files dir
2. Our default `assets/mpv.conf` / `assets/input.conf`
3. mpv's built-in defaults

User config OVERRIDES our defaults. Our defaults OVERRIDE mpv defaults. User never loses their customizations on app update.

## Rule 10: No Feature Creep

This fork adds:
- External config directory (ADB-writable)
- Recording toggle with timestamps
- TV-optimized defaults
- TV remote keybindings

That's the scope. New features require updating this document first.
