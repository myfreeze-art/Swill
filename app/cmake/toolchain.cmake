cmake_minimum_required(VERSION 3.22.1)

set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_SYSTEM_VERSION ${ANDROID_PLATFORM})

if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_ANDROID_ARCH_ABI arm64-v8a)
    set(ARCH_TRIPLE aarch64-linux-android)
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(CMAKE_ANDROID_ARCH_ABI armeabi-v7a)
    set(ARCH_TRIPLE armv7a-linux-androideabi)
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(CMAKE_ANDROID_ARCH_ABI x86_64)
    set(ARCH_TRIPLE x86_64-linux-android)
elseif(ANDROID_ABI STREQUAL "x86")
    set(CMAKE_ANDROID_ARCH_ABI x86)
    set(ARCH_TRIPLE i686-linux-android)
else()
    set(CMAKE_ANDROID_ARCH_ABI ${ANDROID_ABI})
    set(ARCH_TRIPLE ${ANDROID_ABI})
endif()

set(CMAKE_C_COMPILER ${NDK_TOOLCHAIN}/bin/${ARCH_TRIPLE}-clang)
set(CMAKE_CXX_COMPILER ${NDK_TOOLCHAIN}/bin/${ARCH_TRIPLE}-clang++)

set(CMAKE_ANDROID_STL c++_shared)
set(CMAKE_ANDROID_STL_TYPE c++_shared)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)

set(CMAKE_PREFIX_PATH ${NDK_SYSROOT})
set(CMAKE_SYSROOT ${NDK_SYSROOT})

add_compile_options(
    -fdata-sections
    -ffunction-sections
    -fvisibility=hidden
    -funwind-tables
    -fstack-protector-strong
    -no-canonical-prefixes
)

add_link_options(
    -Wl,--build-id
    -Wl,--warn-shared-textrel
    -Wl,--fatal-warnings
    -Wl,--no-undefined
    -Wl,-z,noexecstack
    -Wl,-z,relro
    -Wl,-z,now
)

if(ANDROID_ABI MATCHES "arm")
    add_compile_options(
        -march=armv7-a
        -mfloat-abi=softfp
        -mfpu=vfpv3-d16
        -mthumb
    )
    add_link_options(-march=armv7-a -Wl,--fix-cortex-a8)
endif()

if(ANDROID_ABI STREQUAL "arm64-v8a")
    add_compile_options(-march=armv8-a)
endif()

if(ANDROID_ABI MATCHES "x86")
    add_compile_options(-mssse3 -msse4.1 -msse4.2)
endif()
