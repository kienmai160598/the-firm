#ifndef MOVIES_ENGINE_H
#define MOVIES_ENGINE_H

/*
 * C ABI of the Movies torrent engine. Rules every call follows:
 * - It returns a status and never throws or aborts; a failure leaves a message for
 *   movies_engine_last_error.
 * - Fixed width types only: the Android TV box is 32-bit.
 * - The engine never calls back into the host. Hosts poll for events.
 * The app and the engine are built from the same commit, so structs carry no size field.
 */

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef int32_t movies_engine_status;

#define MOVIES_ENGINE_OK 0
#define MOVIES_ENGINE_INVALID_ARGUMENT 1
#define MOVIES_ENGINE_INTERNAL_ERROR 2
#define MOVIES_ENGINE_NOT_FOUND 3

typedef struct movies_engine movies_engine;

typedef struct movies_engine_config {
    /* Absolute path of a directory the engine may own. Must not be NULL or empty. */
    const char* state_directory;
} movies_engine_config;

/* Semantic version of the engine, a static string. */
const char* movies_engine_version(void);

/* Message of the last failed call on the calling thread, or "" when it succeeded. Valid until
 * the next engine call on the same thread. */
const char* movies_engine_last_error(void);

movies_engine_status movies_engine_create(
    const movies_engine_config* config,
    movies_engine** engine
);

/* Accepts NULL. */
void movies_engine_destroy(movies_engine* engine);

/* Streams. The engine keeps the payload in memory and serves the file over HTTP on 127.0.0.1.
 * A stream resolves once the torrent's metadata arrives; poll movies_engine_get_stream_info until
 * it is READY (play its url) or FAILED (show its error). */

typedef int32_t movies_engine_stream_state;

#define MOVIES_ENGINE_STREAM_RESOLVING 0
#define MOVIES_ENGINE_STREAM_READY 1
#define MOVIES_ENGINE_STREAM_FAILED 2

/* file_index for the largest video file of the torrent. */
#define MOVIES_ENGINE_ANY_FILE (-1)

typedef struct movies_engine_stream_info {
    movies_engine_stream_state state;
    /* Set when READY. */
    int32_t file_index;
    int64_t file_size;
    char url[128];
    char file_name[256];
    /* Set when FAILED. */
    char error[256];
} movies_engine_stream_info;

/* Returns at once with a stream id; INVALID_ARGUMENT when the magnet link does not parse. */
movies_engine_status movies_engine_open_stream(
    movies_engine* engine,
    const char* magnet_uri,
    int32_t file_index,
    uint64_t* stream
);

/* NOT_FOUND for a stream that was closed or never opened. */
movies_engine_status movies_engine_get_stream_info(
    movies_engine* engine,
    uint64_t stream,
    movies_engine_stream_info* info
);

/* Ends the stream's HTTP responses and frees its memory once no other stream plays the torrent.
 * NOT_FOUND for an unknown stream. */
movies_engine_status movies_engine_close_stream(movies_engine* engine, uint64_t stream);

#ifdef __cplusplus
}
#endif

#endif
