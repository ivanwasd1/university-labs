cmake_minimum_required(VERSION 3.16)
project(z2_project LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

enable_testing()

add_library(z2_lib
    src/z2.cpp
)

find_package(GTest REQUIRED)

add_executable(z2_tests
    tests/test_z2.cpp
)

target_link_libraries(z2_tests
    PRIVATE
        z2_lib
        GTest::gtest
        GTest::gtest_main
)

include(GoogleTest)
gtest_discover_tests(z2_tests)
