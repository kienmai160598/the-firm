package com.nuvio.torrent

/**
 * The JNI shim of libmovies_engine.so (engine/src/android/movies_engine_jni.cpp), one function
 * per C ABI call. Only primitives and arrays cross: strings as UTF-8 bytes, outputs in the arrays
 * passed in. A failed call returns its status and puts the engine's message in `error[0]`.
 * Loading the class loads the library, so nothing on the JVM host touches it.
 */
internal object MoviesEngineJni {
    init {
        System.loadLibrary("movies_engine")
    }

    external fun version(): ByteArray?

    /** [budget]: store cap, ahead, behind, eviction threshold; [handle] receives the engine. */
    external fun create(
        stateDirectory: ByteArray,
        budget: LongArray,
        handle: LongArray,
        error: Array<ByteArray?>
    ): Int

    external fun destroy(handle: Long)

    /** [stream] receives the stream id. */
    external fun openStream(
        handle: Long,
        magnetUri: ByteArray,
        fileIndex: Int,
        stream: LongArray,
        error: Array<ByteArray?>
    ): Int

    /** [numbers]: state, file index, file size. [texts]: url, file name, error. */
    external fun streamInfo(
        handle: Long,
        stream: Long,
        numbers: LongArray,
        texts: Array<ByteArray?>,
        error: Array<ByteArray?>
    ): Int

    external fun closeStream(handle: Long, stream: Long, error: Array<ByteArray?>): Int
}
