cmake_minimum_required(VERSION 3.25)

project(movies_engine VERSION 0.1.0 LANGUAGES C CXX)

option(MOVIES_ENGINE_TESTS "Build the engine tests" OFF)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

include(cmake/Dependencies.cmake)

add_library(movies_engine STATIC
    src/api/c_api.cpp
    src/api/stream_info_copy.cpp
    src/core/engine.cpp
    src/core/engine_config.cpp
    src/http/http_connection.cpp
    src/http/http_gateway.cpp
    src/http/http_request.cpp
    src/http/media_type.cpp
    src/http/socket.cpp
    src/session/events.cpp
    src/session/session_service.cpp
    src/session/session_settings.cpp
    src/storage/memory/memory_payload_store.cpp
    src/storage/payload_disk_io.cpp
    src/storage/payload_disk_io_jobs.cpp
    src/storage/payload_store.cpp
    src/storage/piece_hashing.cpp
    src/storage/written_ranges.cpp
    src/stream/sequential_window.cpp
    src/stream/stream_reader.cpp
    src/stream/stream_reading.cpp
    src/stream/stream_service.cpp
    src/torrent/file_choice.cpp
    src/torrent/torrent_service.cpp
)
target_include_directories(movies_engine
    PUBLIC include
    PRIVATE src
)
target_link_libraries(movies_engine PUBLIC torrent-rasterbar)
target_compile_definitions(movies_engine PRIVATE MOVIES_ENGINE_VERSION="${PROJECT_VERSION}")
target_compile_options(movies_engine PRIVATE -Wall -Wextra -Wpedantic -Werror)

if(ANDROID)
    # The one library the Android app loads: the C ABI, libtorrent and the JNI shim for
    # shared/torrent. Only the JNI entry points are exported.
    add_library(movies_engine_jni SHARED src/android/movies_engine_jni.cpp)
    set_target_properties(movies_engine_jni PROPERTIES
        OUTPUT_NAME movies_engine
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
    )
    target_link_libraries(movies_engine_jni PRIVATE movies_engine)
    target_compile_options(movies_engine_jni PRIVATE -Wall -Wextra -Wpedantic -Werror)
    target_link_options(movies_engine_jni PRIVATE
        -Wl,--exclude-libs,ALL -Wl,--gc-sections -Wl,--no-undefined -Wl,-z,max-page-size=16384
    )
endif()

if(MOVIES_ENGINE_TESTS)
    enable_testing()
    add_subdirectory(tests)
endif()
