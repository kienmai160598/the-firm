# Applies engine/cmake/patches/*.patch to the libtorrent sources, in name order. Runs in the
# libtorrent source directory when FetchContent unpacks it.
file(GLOB patches "${CMAKE_CURRENT_LIST_DIR}/patches/*.patch")
list(SORT patches)
foreach(patch IN LISTS patches)
    execute_process(
        COMMAND patch -p1 --forward --input "${patch}"
        RESULT_VARIABLE result
    )
    if(NOT result EQUAL 0)
        message(FATAL_ERROR "libtorrent patch failed: ${patch}")
    endif()
endforeach()
