// X-ray JNI wrapper
// This file provides JNI interface to X-ray core

#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstdlib>
#include <cstring>
#include <unistd.h>
#include <sys/wait.h>

#define LOG_TAG "XrayJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// X-ray process management
static pid_t xray_pid = -1;
static int xray_status = 0; // 0 = stopped, 1 = running, 2 = error

// Path to X-ray binary (will be in assets or cache dir)
static std::string xray_binary_path = "";

// Forward declarations
jint startXray(JNIEnv* env, jobject thiz, const char* config_json);
jint stopXray(JNIEnv* env, jobject thiz);

// JNI functions
extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_XrayNative_start(
    JNIEnv* env,
    jobject thiz,
    jstring config_json
) {
    const char* config = env->GetStringUTFChars(config_json, nullptr);
    if (!config) {
        LOGE("Failed to get config string");
        return -1;
    }
    
    jint result = startXray(env, thiz, config);
    env->ReleaseStringUTFChars(config_json, config);
    return result;
}

extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_XrayNative_stop(
    JNIEnv* env,
    jobject thiz
) {
    return stopXray(env, thiz);
}

extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_XrayNative_getStatus(
    JNIEnv* env,
    jobject thiz
) {
    return xray_status;
}

extern "C" JNIEXPORT jboolean JNICALL 
Java_com_swill_vpn_jni_XrayNative_isRunning(
    JNIEnv* env,
    jobject thiz
) {
    return xray_status == 1;
}

// Implementation
jint startXray(JNIEnv* env, jobject thiz, const char* config_json) {
    LOGD("Starting X-ray with config: %s", config_json);
    
    // Check if already running
    if (xray_status == 1) {
        LOGE("X-ray is already running");
        return -1;
    }
    
    // For now, we'll use a simple approach:
    // 1. Write config to temporary file
    // 2. Execute X-ray binary
    
    // Get Android context to access cache directory
    jclass context_class = env->GetObjectClass(thiz);
    jmethodID get_cache_dir = env->GetMethodID(
        context_class, 
        "getCacheDir", 
        "()Ljava/io/File;"
    );
    
    if (!get_cache_dir) {
        LOGE("Failed to get getCacheDir method");
        return -1;
    }
    
    jobject cache_dir = env->CallObjectMethod(thiz, get_cache_dir);
    jclass file_class = env->FindClass("java/io/File");
    jmethodID get_path = env->GetMethodID(file_class, "getPath", "()Ljava/lang/String;");
    jstring cache_path = (jstring)env->CallObjectMethod(cache_dir, get_path);
    
    const char* cache_dir_str = env->GetStringUTFChars(cache_path, nullptr);
    
    // Create config file path
    std::string config_path = std::string(cache_dir_str) + "/xray_config.json";
    
    // Write config to file
    FILE* config_file = fopen(config_path.c_str(), "w");
    if (!config_file) {
        LOGE("Failed to create config file");
        env->ReleaseStringUTFChars(cache_path, cache_dir_str);
        return -1;
    }
    fwrite(config_json, 1, strlen(config_json), config_file);
    fclose(config_file);
    
    // For now, we'll set the binary path
    // In production, this will be extracted from assets or built with NDK
    xray_binary_path = std::string(cache_dir_str) + "/xray";
    
    env->ReleaseStringUTFChars(cache_path, cache_dir_str);
    
    // Check if binary exists
    if (access(xray_binary_path.c_str(), X_OK) != 0) {
        LOGE("X-ray binary not found at: %s", xray_binary_path.c_str());
        return -2;
    }
    
    // Start X-ray process
    xray_pid = fork();
    
    if (xray_pid < 0) {
        LOGE("Failed to fork process");
        return -3;
    } else if (xray_pid == 0) {
        // Child process - execute X-ray
        std::string log_path = std::string(cache_dir_str) + "/xray.log";
        
        // Redirect stdout and stderr to log file
        freopen(log_path.c_str(), "w", stdout);
        freopen(log_path.c_str(), "w", stderr);
        
        // Execute X-ray
        execl(
            xray_binary_path.c_str(),
            "xray",
            "run",
            "-c",
            config_path.c_str(),
            (char*)nullptr
        );
        
        // If we get here, exec failed
        LOGE("Failed to execute X-ray binary");
        exit(1);
    } else {
        // Parent process
        xray_status = 1;
        LOGD("X-ray started with PID: %d", xray_pid);
        return 0;
    }
}

jint stopXray(JNIEnv* env, jobject thiz) {
    LOGD("Stopping X-ray");
    
    if (xray_status != 1) {
        LOGE("X-ray is not running");
        return -1;
    }
    
    // Kill the process
    if (xray_pid > 0) {
        kill(xray_pid, SIGTERM);
        
        // Wait for process to terminate
        int status;
        waitpid(xray_pid, &status, 0);
        
        xray_pid = -1;
        xray_status = 0;
        
        LOGD("X-ray stopped");
        return 0;
    }
    
    return -1;
}
