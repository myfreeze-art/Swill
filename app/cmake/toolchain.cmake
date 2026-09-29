# Android NDK toolchain file
# This file configures the NDK toolchain for building native libraries

# Set minimum CMake version
cmake_minimum_required(VERSION 3.22.1)

# Android NDK settings
set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_SYSTEM_VERSION ${ANDROID_PLATFORM})

# Specify the NDK toolchain
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

# Set the cross-compiler
set(CMAKE_C_COMPILER ${NDK_TOOLCHAIN}/bin/${ARCH_TRIPLE}-clang)
set(CMAKE_CXX_COMPILER ${NDK_TOOLCHAIN}/bin/${ARCH_TRIPLE}-clang++)

# Set the target environment
set(CMAKE_ANDROID_STL c++_shared)
set(CMAKE_ANDROID_STL_TYPE c++_shared)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Enable position independent code
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Set find root path
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)

# Set prefix path
set(CMAKE_PREFIX_PATH ${NDK_SYSROOT})
set(CMAKE_SYSROOT ${NDK_SYSROOT})

# Additional flags for optimization
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

# ARM-specific optimizations
if(ANDROID_ABI MATCHES "arm")
    add_compile_options(
        -march=armv7-a
        -mfloat-abi=softfp
        -mfpu=vfpv3-d16
        -mthumb
    )
    add_link_options(
        -march=armv7-a
        -Wl,--fix-cortex-a8
    )
endif()

# ARM64-specific optimizations
if(ANDROID_ABI STREQUAL "arm64-v8a")
    add_compile_options(
        -march=armv8-a
    )
endif()

# x86-specific optimizations
if(ANDROID_ABI MATCHES "x86")
    add_compile_options(
        -mssse3
        -msse4.1
        -msse4.2
    )
endif()
