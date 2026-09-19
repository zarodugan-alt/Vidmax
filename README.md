# COMET

**Universal video downloader for Android.** Kotlin + Jetpack Compose, yt-dlp powered,
dark comet aesthetic — cyan for video, violet for audio, everywhere.

> Status: **phase 1 build** — engine, download pipeline, queue/library UI, analyze sheet,
> player, in-app browser, settings, engine updater wired. Playlist picker (full S3), custom
> player controls/mini-player/PiP (full S5), scheduling and SponsorBlock (P4) are stubbed
> for later phases.

## Modules

| Module      | Contents |
|-------------|----------|
| `:app`      | Hilt graph root, single-activity navigation (splash → disclaimer → home), share targets, browser, player |
| `:engine`   | yt-dlp wrapper (youtubedl-android), JSON/progress parsing, stable/nightly updater, failure taxonomy |
| `:download` | Room-backed queue manager, foreground `DownloadService`, boot resume, constraints (Wi-Fi/charging), MediaStore writer, filename templater |
| `:data`     | Room entities/DAOs, DataStore settings, download/engine repositories |
| `:ui`       | Compose design system: theme, glass cards, CometRing, formats catalog, home/queue/library, analyze sheet, screens |

## Build

Requirements: JDK 17, Android SDK 34.

```bash
./gradlew assembleDebug          # per-ABI split APKs (arm64-v8a, armeabi-v7a, x86_64)
./gradlew testDebugUnitTest      # engine/download/ui unit tests
./gradlew :app:lintDebug         # NewApi & friends
```

CI: GitHub Actions (`.github/workflows/android.yml`) builds all ABI splits, runs tests and
lint on every push — the project's acceptance gate.

## Architecture notes

- **Engine**: `YtDlpEngine` shells into youtubedl-android. Analyze = `-J --flat-playlist`
  (playlists), download = `--newline` stdout → `EngineEvent` flow (progress/stage/done/
  failed), cancel = `destroyProcessById`, resume = `--continue`. Two-level updater:
  yt-dlp releases (stable/nightly) + app releases via the GitHub Releases API.
- **Queue**: Room is the source of truth; `QueueManager` runs 1–3 concurrent downloads,
  respects Wi-Fi-only/charging constraints, holds a partial wake lock with a 60s grace
  period, and re-enqueues interrupted items on boot/launch.
- **Storage**: work dir in app-scoped external storage; final files published through
  MediaStore (scoped storage on API 29+, `WRITE_EXTERNAL_STORAGE` legacy path on API 28
  with runtime permission prompt).
- **Min supported device**: Android 9 (API 28). No API-31+ call without an SDK-gated
  fallback; no `foregroundServiceType` (exempt at targetSdk 28); glass effect is a
  gradient + border fake, not runtime blur (API 28 has none).

## Licensing

**This project is licensed under GPL-3.0.** The mandated engine runtime
[`youtubedl-android`](https://github.com/junkfood02/youtubedl-android) (io.github.junkfood02,
0.18.1) ships the GPL-3.0 yt-dlp core and links as a Kotlin library; the original plan of
keeping the app GPL-free while using it is not achievable, so the whole app is GPL-3.0.
See `LICENSE` and `THIRD_PARTY_NOTICES.md` (bundled font licenses live in `licenses/fonts/`).

yt-dlp itself is unlicensed/public-domain (Unlicense) — see THIRD_PARTY_NOTICES.md.

## Disclaimer

COMET is a general-purpose media downloader. Users are responsible for respecting the
terms of service of the sites they use and the copyright of the content they download.
A one-time disclaimer appears on first launch.
