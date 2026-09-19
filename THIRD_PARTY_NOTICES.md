# Third-party notices

Bundled or linked components and their licenses:

| Component | Coordinates | License |
|-----------|-------------|---------|
| youtubedl-android | `io.github.junkfood02.youtubedl-android:{library,ffmpeg,aria2c}:0.18.1` | **GPL-3.0** (ships yt-dlp) — reason this app is GPL-3.0 |
| yt-dlp (via above) | bundled binary | Unlicense (public domain) |
| Kotlin / Coroutines / Serialization | org.jetbrains.kotlin:* | Apache-2.0 |
| Jetpack Compose + Material3 | androidx.compose / androidx.compose.material3 | Apache-2.0 |
| AndroidX (core, activity, lifecycle, navigation-compose, datastore, room, work) | androidx.* | Apache-2.0 |
| Media3 / ExoPlayer | androidx.media3:* | Apache-2.0 |
| Coil | `io.coil-kt:coil-compose:2.7.0` | Apache-2.0 |
| Hilt / Dagger | com.google.dagger:* | Apache-2.0 |
| Timber | com.jakewharton.timber:timber | Apache-2.0 |
| kotlinx-datetime (engine dates) | org.jetbrains.kotlinx:kotlinx-datetime | Apache-2.0 |
| Space Grotesk (bundled font) | `ui/src/main/res/font/space_grotesk_medium.ttf` | OFL-1.1 — `licenses/fonts/OFL-SpaceGrotesk.txt` |
| Inter (bundled font) | `ui/src/main/res/font/inter_{regular,medium}.ttf` | OFL-1.1 — `licenses/fonts/OFL-Inter.txt` |
| JetBrains Mono (bundled font) | `ui/src/main/res/font/jetbrains_mono_regular.ttf` | OFL-1.1 — `licenses/fonts/OFL-JetBrainsMono.txt` |

## GPL deviation note

The build specification's acceptance criterion "no GPL-linked dependency on the `:app`
classpath" cannot be met while using the mandated engine library: youtubedl-android 0.18.1
is GPL-3.0 and links as a Kotlin library (not a separate process). Resolution adopted here:
the entire app ships as GPL-3.0.
