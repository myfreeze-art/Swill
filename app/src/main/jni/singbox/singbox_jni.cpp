// Sing-box JNI wrapper
// This file provides JNI interface to Sing-box core

#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstdlib>
#include <cstring>
#include <unistd.h>
#include <sys/wait.h>

#define LOG_TAG "SingBoxJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Sing-box process management
static pid_t singbox_pid = -1;
static int singbox_status = 0; // 0 = stopped, 1 = running, 2 = error

// Path to Sing-box binary
static std::string singbox_binary_path = "";

// Forward declarations
jint startSingBox(JNIEnv* env, jobject thiz, const char* config_json);
jint stopSingBox(JNIEnv* env, jobject thiz);

// JNI functions
extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_SingBoxNative_start(
    JNIEnv* env,
    jobject thiz,
    jstring config_json
) {
    const char* config = env->GetStringUTFChars(config_json, nullptr);
    if (!config) {
        LOGE("Failed to get config string");
        return -1;
    }
    
    jint result = startSingBox(env, thiz, config);
    env->ReleaseStringUTFChars(config_json, config);
    return result;
}

extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_SingBoxNative_stop(
    JNIEnv* env,
    jobject thiz
) {
    return stopSingBox(env, thiz);
}

extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_SingBoxNative_getStatus(
    JNIEnv* env,
    jobject thiz
) {
    return singbox_status;
}

extern "C" JNIEXPORT jboolean JNICALL 
Java_com_swill_vpn_jni_SingBoxNative_isRunning(
    JNIEnv* env,
    jobject thiz
) {
    return singbox_status == 1;
}

// Implementation
jint startSingBox(JNIEnv* env, jobject thiz, const char* config_json) {
    LOGD("Starting Sing-box with config: %s", config_json);
    
    // Check if already running
    if (singbox_status == 1) {
        LOGE("Sing-box is already running");
        return -1;
    }
    
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
    std::string config_path = std::string(cache_dir_str) + "/singbox_config.json";
    
    // Write config to file
    FILE* config_file = fopen(config_path.c_str(), "w");
    if (!config_file) {
        LOGE("Failed to create config file");
        env->ReleaseStringUTFChars(cache_path, cache_dir_str);
        return -1;
    }
    fwrite(config_json, 1, strlen(config_json), config_file);
    fclose(config_file);
    
    // Set the binary path
    singbox_binary_path = std::string(cache_dir_str) + "/sing-box";
    
    env->ReleaseStringUTFChars(cache_path, cache_dir_str);
    
    // Check if binary exists
    if (access(singbox_binary_path.c_str(), X_OK) != 0) {
        LOGE("Sing-box binary not found at: %s", singbox_binary_path.c_str());
        return -2;
    }
    
    // Start Sing-box process
    singbox_pid = fork();
    
    if (singbox_pid < 0) {
        LOGE("Failed to fork process");
        return -3;
    } else if (singbox_pid == 0) {
        // Child process - execute Sing-box
        std::string log_path = std::string(cache_dir_str) + "/singbox.log";
        
        // Redirect stdout and stderr to log file
        freopen(log_path.c_str(), "w", stdout);
        freopen(log_path.c_str(), "w", stderr);
        
        // Execute Sing-box
        execl(
            singbox_binary_path.c_str(),
            "sing-box",
            "run",
            "-c",
            config_path.c_str(),
            (char*)nullptr
        );
        
        // If we get here, exec failed
        LOGE("Failed to execute Sing-box binary");
        exit(1);
    } else {
        // Parent process
        singbox_status = 1;
        LOGD("Sing-box started with PID: %d", singbox_pid);
        return 0;
    }
}

jint stopSingBox(JNIEnv* env, jobject thiz) {
    LOGD("Stopping Sing-box");
    
    if (singbox_status != 1) {
        LOGE("Sing-box is not running");
        return -1;
    }
    
    // Kill the process
    if (singbox_pid > 0) {
        kill(singbox_pid, SIGTERM);
        
        // Wait for process to terminate
        int status;
        waitpid(singbox_pid, &status, 0);
        
        singbox_pid = -1;
        singbox_status = 0;
        
        LOGD("Sing-box stopped");
        return 0;
    }
    
    return -1;
}
