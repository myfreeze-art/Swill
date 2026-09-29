cmake_minimum_required(VERSION 3.22.1)

project(SwillNative)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_ANDROID_STL c++_shared)
set(CMAKE_ANDROID_STL_TYPE c++_shared)

if(ANDROID_ABI)
    set(ARCH_ABI ${ANDROID_ABI})
else()
    set(ARCH_ABI ${CMAKE_ANDROID_ARCH_ABI})
endif()

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib/${ARCH_ABI})
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib/${ARCH_ABI})
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin/${ARCH_ABI})

option(BUILD_XRAY "Build X-ray core" ON)
option(BUILD_SINGBOX "Build Sing-box core" ON)

if(BUILD_XRAY)
    add_library(xray-jni SHARED xray/xray_jni.cpp)
    target_include_directories(xray-jni PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
    target_link_libraries(xray-jni PRIVATE log android EGL GLESv2)
    set_target_properties(xray-jni PROPERTIES LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../jniLibs/${ANDROID_ABI})
endif()

if(BUILD_SINGBOX)
    add_library(singbox-jni SHARED singbox/singbox_jni.cpp)
    target_include_directories(singbox-jni PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
    target_link_libraries(singbox-jni PRIVATE log android EGL GLESv2)
    set_target_properties(singbox-jni PROPERTIES LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../jniLibs/${ANDROID_ABI})
endif()

add_library(native-lib SHARED native_lib.cpp)
target_include_directories(native-lib PRIVATE ${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_BINARY_DIR})
target_link_libraries(native-lib PRIVATE log android EGL GLESv2)

if(BUILD_XRAY)
    target_link_libraries(native-lib PRIVATE xray-jni)
endif()

if(BUILD_SINGBOX)
    target_link_libraries(native-lib PRIVATE singbox-jni)
endif()

set_target_properties(native-lib PROPERTIES LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../jniLibs/${ANDROID_ABI})

install(TARGETS native-lib LIBRARY DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/../../jniLibs/${ANDROID_ABI})
