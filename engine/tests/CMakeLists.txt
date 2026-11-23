include(FetchContent)

FetchContent_Declare(doctest
    URL https://github.com/doctest/doctest/archive/refs/tags/v2.4.12.tar.gz
    URL_HASH SHA256=73381c7aa4dee704bd935609668cf41880ea7f19fa0504a200e13b74999c2d70
    DOWNLOAD_EXTRACT_TIMESTAMP TRUE
)
FetchContent_MakeAvailable(doctest)

add_executable(movies_engine_tests
    main.cpp
    c_api_stream_test.cpp
    c_api_test.cpp
    engine_config_test.cpp
    file_choice_test.cpp
    http_request_test.cpp
    memory_payload_store_test.cpp
    sequential_window_test.cpp
    stream_integration_test.cpp
    support/http_client.cpp
    support/loopback_seeder.cpp
)
target_include_directories(movies_engine_tests PRIVATE .)
target_include_directories(movies_engine_tests PRIVATE ../src)
target_link_libraries(movies_engine_tests PRIVATE movies_engine doctest::doctest)
target_compile_definitions(movies_engine_tests PRIVATE
    MOVIES_ENGINE_TEST_VERSION="${PROJECT_VERSION}"
)
target_compile_options(movies_engine_tests PRIVATE -Wall -Wextra -Werror)

add_test(NAME movies_engine_tests COMMAND movies_engine_tests)
