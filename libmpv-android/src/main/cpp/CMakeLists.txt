cmake_minimum_required(VERSION 3.22.1)

project("libmpv-android")

add_library(
    mpv-android
    SHARED
    main.cpp player.cpp
)

add_library(
    mpv
    SHARED
    IMPORTED
)

set_target_properties(
    mpv
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libmpv.so
)

add_library(
    avcodec
    SHARED
    IMPORTED
)

set_target_properties(
    avcodec
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../buildscripts/prefix/${ANDROID_ABI}/lib/libavcodec.so
)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../../../buildscripts/prefix/${ANDROID_ABI}/include)

target_link_libraries(mpv-android mpv avcodec log)