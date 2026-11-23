cmake_minimum_required(VERSION 3.24)

project(movies_engine VERSION 0.1.0 LANGUAGES C CXX)

option(MOVIES_ENGINE_TESTS "Build the engine tests" OFF)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(movies_engine STATIC
    src/api/c_api.cpp
    src/core/engine.cpp
    src/core/engine_config.cpp
)
target_include_directories(movies_engine
    PUBLIC include
    PRIVATE src
)
target_compile_definitions(movies_engine PRIVATE MOVIES_ENGINE_VERSION="${PROJECT_VERSION}")
target_compile_options(movies_engine PRIVATE -Wall -Wextra -Wpedantic -Werror)

if(MOVIES_ENGINE_TESTS)
    enable_testing()
    add_subdirectory(tests)
endif()
