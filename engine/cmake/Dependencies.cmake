# libtorrent and Boost, pinned by URL and SHA-256. Sources are unpacked once into engine/.deps and
# shared by every build directory (host tests, tvOS device, tvOS simulator); each build directory
# compiles its own copy.
include(FetchContent)

set(MOVIES_ENGINE_DEPS "${CMAKE_CURRENT_LIST_DIR}/../.deps")

FetchContent_Declare(boost
    URL https://archives.boost.io/release/1.86.0/source/boost_1_86_0.tar.bz2
    URL_HASH SHA256=1bed88e40401b2cb7a1f76d4bab499e352fa4d0c5f31c0dbae64e24d34d7513b
    DOWNLOAD_DIR "${MOVIES_ENGINE_DEPS}/downloads"
    SOURCE_DIR "${MOVIES_ENGINE_DEPS}/boost"
    DOWNLOAD_EXTRACT_TIMESTAMP TRUE
)
FetchContent_Declare(libtorrent
    URL https://github.com/arvidn/libtorrent/releases/download/v2.0.12/libtorrent-rasterbar-2.0.12.tar.gz
    URL_HASH SHA256=25b898d02e02e43ee9a8ea5480c20007f129091b5754d0283f94e4d51d11a19e
    DOWNLOAD_DIR "${MOVIES_ENGINE_DEPS}/downloads"
    SOURCE_DIR "${MOVIES_ENGINE_DEPS}/libtorrent"
    DOWNLOAD_EXTRACT_TIMESTAMP TRUE
    SYSTEM
    PATCH_COMMAND "${CMAKE_COMMAND}" -P "${CMAKE_CURRENT_LIST_DIR}/apply-patches.cmake"
)

# Boost is used for its headers only (Asio and friends), so it is never built.
FetchContent_Populate(boost)
set(BOOST_ROOT "${boost_SOURCE_DIR}")
set(Boost_NO_SYSTEM_PATHS ON)
# Cross builds search only inside the SDK, so point FindBoost at the headers directly.
set(Boost_INCLUDE_DIR "${boost_SOURCE_DIR}" CACHE PATH "" FORCE)

# No TLS until decision D3 says otherwise: keep OpenSSL and GnuTLS out even when the host has them,
# so the host tests run the same libtorrent as the tvOS build.
set(CMAKE_DISABLE_FIND_PACKAGE_OpenSSL ON)
set(CMAKE_DISABLE_FIND_PACKAGE_GnuTLS ON)
set(CMAKE_DISABLE_FIND_PACKAGE_LibGcrypt ON)
set(BUILD_SHARED_LIBS OFF)
set(deprecated-functions OFF CACHE BOOL "" FORCE)
set(i2p OFF CACHE BOOL "" FORCE)
set(logging OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(libtorrent)
# Upstream code: its warnings are not ours to fix.
target_compile_options(torrent-rasterbar PRIVATE -w)
