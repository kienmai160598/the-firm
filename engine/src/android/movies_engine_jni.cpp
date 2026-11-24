// JNI shim for shared/torrent's androidMain (com.nuvio.torrent.MoviesEngineJni). It maps each
// Kotlin call to one C ABI call and nothing more:
// - Strings cross as UTF-8 byte arrays, both ways: torrent file names need not be valid modified
//   UTF-8, which NewStringUTF and GetStringUTFChars expect.
// - A failed call writes movies_engine_last_error into error[0] before returning, because the
//   message is thread local and valid only until the next engine call.
// - It never calls a Java method; it only fills the arrays it is given.
// - No C++ exception leaves a function: each body runs inside `shielded`.

#include <jni.h>

#include <cstdint>
#include <cstring>
#include <string>

#include "movies_engine/movies_engine.h"

namespace {

std::string utf8_of(JNIEnv* env, jbyteArray bytes) {
    if (bytes == nullptr) return {};
    const auto length = env->GetArrayLength(bytes);
    std::string text(static_cast<std::size_t>(length), '\0');
    env->GetByteArrayRegion(bytes, 0, length, reinterpret_cast<jbyte*>(text.data()));
    return text;
}

// Stores `text` as a byte array at `slots[index]`. Leaves a pending OutOfMemoryError when the
// JVM cannot allocate it; Kotlin sees it once the call returns, and no further JNI call is made
// while it is pending.
void store(JNIEnv* env, jobjectArray slots, jsize index, const char* text) {
    if (slots == nullptr || env->ExceptionCheck()) return;
    const auto length = static_cast<jsize>(std::strlen(text));
    jbyteArray bytes = env->NewByteArray(length);
    if (bytes == nullptr) return;
    env->SetByteArrayRegion(bytes, 0, length, reinterpret_cast<const jbyte*>(text));
    env->SetObjectArrayElement(slots, index, bytes);
    env->DeleteLocalRef(bytes);
}

movies_engine_status reported(JNIEnv* env, movies_engine_status status, jobjectArray error) {
    if (status != MOVIES_ENGINE_OK) store(env, error, 0, movies_engine_last_error());
    return status;
}

template <typename Body>
jint shielded(JNIEnv* env, jobjectArray error, Body&& body) noexcept {
    try {
        return body();
    } catch (...) {
        store(env, error, 0, "JNI shim failed");
        return MOVIES_ENGINE_INTERNAL_ERROR;
    }
}

movies_engine* engine_of(jlong handle) {
    return reinterpret_cast<movies_engine*>(static_cast<std::intptr_t>(handle));
}

}

extern "C" {

JNIEXPORT jbyteArray JNICALL Java_com_nuvio_torrent_MoviesEngineJni_version(JNIEnv* env, jobject) {
    const char* version = movies_engine_version();
    const auto length = static_cast<jsize>(std::strlen(version));
    jbyteArray bytes = env->NewByteArray(length);
    if (bytes != nullptr) {
        env->SetByteArrayRegion(bytes, 0, length, reinterpret_cast<const jbyte*>(version));
    }
    return bytes;
}

// budget: store cap, ahead, behind, eviction threshold (0 keeps the default). handle[0] receives
// the engine.
JNIEXPORT jint JNICALL Java_com_nuvio_torrent_MoviesEngineJni_create(
    JNIEnv* env, jobject, jbyteArray state_directory, jlongArray budget, jlongArray handle,
    jobjectArray error
) {
    return shielded(env, error, [&]() -> jint {
        const auto directory = utf8_of(env, state_directory);
        jlong values[4] = {0, 0, 0, 0};
        if (budget != nullptr && env->GetArrayLength(budget) == 4) {
            env->GetLongArrayRegion(budget, 0, 4, values);
        }
        const movies_engine_config config{
            .state_directory = directory.c_str(),
            .store_cap_bytes = values[0],
            .ahead_bytes = values[1],
            .behind_bytes = values[2],
            .eviction_threshold_bytes = values[3],
        };
        movies_engine* engine = nullptr;
        const auto status = movies_engine_create(&config, &engine);
        const jlong address = static_cast<jlong>(reinterpret_cast<std::intptr_t>(engine));
        env->SetLongArrayRegion(handle, 0, 1, &address);
        return reported(env, status, error);
    });
}

JNIEXPORT void JNICALL Java_com_nuvio_torrent_MoviesEngineJni_destroy(JNIEnv*, jobject, jlong handle) {
    movies_engine_destroy(engine_of(handle));
}

// stream[0] receives the stream id.
JNIEXPORT jint JNICALL Java_com_nuvio_torrent_MoviesEngineJni_openStream(
    JNIEnv* env, jobject, jlong handle, jbyteArray magnet_uri, jint file_index, jlongArray stream,
    jobjectArray error
) {
    return shielded(env, error, [&]() -> jint {
        const auto magnet = utf8_of(env, magnet_uri);
        std::uint64_t id = 0;
        const auto status = movies_engine_open_stream(engine_of(handle), magnet.c_str(), file_index, &id);
        const jlong value = static_cast<jlong>(id);
        env->SetLongArrayRegion(stream, 0, 1, &value);
        return reported(env, status, error);
    });
}

// numbers: state, file index, file size. texts: url, file name, error of a FAILED stream.
JNIEXPORT jint JNICALL Java_com_nuvio_torrent_MoviesEngineJni_streamInfo(
    JNIEnv* env, jobject, jlong handle, jlong stream, jlongArray numbers, jobjectArray texts,
    jobjectArray error
) {
    return shielded(env, error, [&]() -> jint {
        movies_engine_stream_info info{};
        const auto status = movies_engine_get_stream_info(
            engine_of(handle), static_cast<std::uint64_t>(stream), &info
        );
        if (status != MOVIES_ENGINE_OK) return reported(env, status, error);
        const jlong values[3] = {info.state, info.file_index, info.file_size};
        env->SetLongArrayRegion(numbers, 0, 3, values);
        store(env, texts, 0, info.url);
        store(env, texts, 1, info.file_name);
        store(env, texts, 2, info.error);
        return status;
    });
}

JNIEXPORT jint JNICALL Java_com_nuvio_torrent_MoviesEngineJni_closeStream(
    JNIEnv* env, jobject, jlong handle, jlong stream, jobjectArray error
) {
    return shielded(env, error, [&]() -> jint {
        const auto status = movies_engine_close_stream(engine_of(handle), static_cast<std::uint64_t>(stream));
        return reported(env, status, error);
    });
}

}
