# Keep rules carried over from the old app (tag android-tv-legacy, app/proguard-rules.pro).
# The bridge's own rules (Ktor, kotlinx.serialization, Supabase) come with slice A2:
# start from shared/core/proguard-rules.pro.

# Torrent engine (lib-nuvio-engine-android), called through JNI
-keep class com.nuvio.engine.** { *; }
-keep interface com.nuvio.engine.** { *; }

# Media3 fork AARs and the FFmpeg decoder
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keep class androidx.media.** { *; }

# Native allocator classes of the media3 fork, used by the engine's JNI code
-keep class androidx.media3.exoplayer.upstream.DefaultAllocatorNative {
    native <methods>;
}
-keep class androidx.media3.exoplayer.source.SampleDataQueueNative {
    native <methods>;
}
-keep class androidx.media3.exoplayer.upstream.Allocation {
    <init>(java.nio.ByteBuffer, int, long);
    public long nativeHandle;
}
