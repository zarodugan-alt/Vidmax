# COMET release rules (R8 wiring lands with the release hardening phase).
# youtubedl-android keeps its native payloads in resources — nothing to strip yet.
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**
