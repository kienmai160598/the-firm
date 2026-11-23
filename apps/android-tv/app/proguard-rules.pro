# Keep rules carried over from the old app (tag android-tv-legacy, app/proguard-rules.pro),
# then the rules the bridge needs, taken from shared/core/proguard-rules.pro without the ones
# for phone screens, plugins (QuickJS) and mpv, which the bridge does not compile.

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

# Crash stack traces keep file and line numbers.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin metadata and generic signatures read by kotlinx.serialization, Ktor and Supabase.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations

# Ktor and Supabase client stack (reflective paths in serializers and plugins).
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Generated serializers of @Serializable classes.
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class com.nuvio.app.features.catalog.CatalogTargetKind { *; }

# P2P runtime around the engine's JNI bridge.
-keep class com.nuvio.app.features.p2p.** { *; }

# Optional TLS providers okhttp probes for at runtime.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
