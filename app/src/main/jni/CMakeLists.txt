cmake_minimum_required(VERSION 3.22.1)

# Project name
project(SwillNative)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Android NDK settings
set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_ANDROID_STL c++_shared)
set(CMAKE_ANDROID_STL_TYPE c++_shared)

# Enable position independent code
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Get Android ABI
if(ANDROID_ABI)
    set(ARCH_ABI ${ANDROID_ABI})
else()
    set(ARCH_ABI ${CMAKE_ANDROID_ARCH_ABI})
endif()

message(STATUS "Building for ABI: ${ARCH_ABI}")

# Set output directories
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib/${ARCH_ABI})
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib/${ARCH_ABI})
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin/${ARCH_ABI})

# Option to build X-ray
option(BUILD_XRAY "Build X-ray core" ON)

# Option to build Sing-box
option(BUILD_SINGBOX "Build Sing-box core" ON)

# ============================================================
# X-ray Core
# ============================================================
if(BUILD_XRAY)
    message(STATUS "Configuring X-ray core...")
    
    # For now, we'll create a simple JNI wrapper
    # In production, X-ray will be built from source or included as pre-built binaries
    
    add_library(xray-jni SHARED
        xray/xray_jni.cpp
    )
    
    target_include_directories(xray-jni PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_BINARY_DIR}
    )
    
    target_link_libraries(xray-jni PRIVATE
        log
        android
        EGL
        GLESv2
    )
    
    # Set output to jniLibs
    set_target_properties(xray-jni PROPERTIES
        LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../jniLibs/${ANDROID_ABI}
    )
    
    message(STATUS "X-ray JNI library configured")
endif()

# ============================================================
# Sing-box Core
# ============================================================
if(BUILD_SINGBOX)
    message(STATUS "Configuring Sing-box core...")
    
    add_library(singbox-jni SHARED
        singbox/singbox_jni.cpp
    )
    
    target_include_directories(singbox-jni PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_BINARY_DIR}
    )
    
    target_link_libraries(singbox-jni PRIVATE
        log
        android
        EGL
        GLESv2
    )
    
    # Set output to jniLibs
    set_target_properties(singbox-jni PROPERTIES
        LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../jniLibs/${ANDROID_ABI}
    )
    
    message(STATUS "Sing-box JNI library configured")
endif()

# ============================================================
# Main Native Library
# ============================================================

# Create main native library that will be loaded by Android
add_library(native-lib SHARED
    native_lib.cpp
)

# Include directories
target_include_directories(native-lib PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_BINARY_DIR}
)

# Link libraries
target_link_libraries(native-lib PRIVATE
    log
    android
    EGL
    GLESv2
)

if(BUILD_XRAY)
    target_link_libraries(native-lib PRIVATE xray-jni)
endif()

if(BUILD_SINGBOX)
    target_link_libraries(native-lib PRIVATE singbox-jni)
endif()

# Set output directory for native-lib
set_target_properties(native-lib PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../jniLibs/${ANDROID_ABI}
)

# Install command
install(TARGETS native-lib
    LIBRARY DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/../../jniLibs/${ANDROID_ABI}
)

message(STATUS "Native library configured")

# ============================================================
# Go-based builds (for future implementation)
# ============================================================

# These would be used when building Go-based cores from source
# For now, they're placeholders

if(BUILD_XRAY)
    # Placeholder for Go-based X-ray build
    message(STATUS "Note: For full X-ray build, use Go mobile or CGO")
endif()

if(BUILD_SINGBOX)
    # Placeholder for Go-based Sing-box build
    message(STATUS "Note: For full Sing-box build, use Go mobile or CGO")
endif()
