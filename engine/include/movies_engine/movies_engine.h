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

#ifdef __cplusplus
}
#endif

#endif
