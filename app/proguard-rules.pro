# Default ProGuard rules for the TV APK Android application.
# Keep ExoPlayer extensions reachable via reflection.
-keep class androidx.media3.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepattributes *Annotation*, InnerClasses
