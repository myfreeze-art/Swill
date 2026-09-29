#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstdlib>
#include <cstring>
#include <unistd.h>
#include <sys/wait.h>
#include <fcntl.h>

static pid_t xray_pid = -1;
static int xray_status = 0;

jint startXray(JNIEnv* env, jobject context, const char* config_json, const char* binary_path) {
    if (xray_status == 1) return -1;

    jclass context_class = env->GetObjectClass(context);
    jmethodID get_cache_dir = env->GetMethodID(context_class, "getCacheDir", "()Ljava/io/File;");
    if (!get_cache_dir) return -1;

    jobject cache_dir_obj = env->CallObjectMethod(context, get_cache_dir);
    jclass file_class = env->FindClass("java/io/File");
    jmethodID get_path = env->GetMethodID(file_class, "getPath", "()Ljava/lang/String;");
    jstring cache_path_jstr = (jstring)env->CallObjectMethod(cache_dir_obj, get_path);
    const char* cache_dir = env->GetStringUTFChars(cache_path_jstr, nullptr);

    std::string config_path = std::string(cache_dir) + "/xray_config.json";
    int config_fd = open(config_path.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (config_fd < 0) {
        env->ReleaseStringUTFChars(cache_path_jstr, cache_dir);
        return -1;
    }
    write(config_fd, config_json, strlen(config_json));
    close(config_fd);

    std::string log_path = std::string(cache_dir) + "/xray.log";
    int log_fd = open(log_path.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);

    xray_pid = fork();
    if (xray_pid < 0) {
        env->ReleaseStringUTFChars(cache_path_jstr, cache_dir);
        return -3;
    }

    if (xray_pid == 0) {
        dup2(log_fd, STDOUT_FILENO);
        dup2(log_fd, STDERR_FILENO);
        close(log_fd);

        execl(binary_path, "xray", "run", "-c", config_path.c_str(), (char*)nullptr);
        exit(1);
    }

    close(log_fd);
    env->ReleaseStringUTFChars(cache_path_jstr, cache_dir);
    xray_status = 1;
    return 0;
}

jint stopXray() {
    if (xray_status != 1) return -1;
    if (xray_pid > 0) {
        kill(xray_pid, SIGTERM);
        int status;
        waitpid(xray_pid, &status, 0);
        xray_pid = -1;
        xray_status = 0;
        return 0;
    }
    return -1;
}

extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_XrayNative_start(JNIEnv* env, jobject thiz, jstring config_json, jstring binary_path) {
    const char* config = env->GetStringUTFChars(config_json, nullptr);
    const char* path = env->GetStringUTFChars(binary_path, nullptr);
    if (!config || !path) {
        if (config) env->ReleaseStringUTFChars(config_json, config);
        if (path) env->ReleaseStringUTFChars(binary_path, path);
        return -1;
    }
    jint result = startXray(env, thiz, config, path);
    env->ReleaseStringUTFChars(config_json, config);
    env->ReleaseStringUTFChars(binary_path, path);
    return result;
}

extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_XrayNative_stop(JNIEnv* env, jobject thiz) {
    return stopXray();
}

extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_XrayNative_getStatus(JNIEnv* env, jobject thiz) {
    return xray_status;
}

extern "C" JNIEXPORT jboolean JNICALL 
Java_com_swill_vpn_jni_XrayNative_isRunning(JNIEnv* env, jobject thiz) {
    return xray_status == 1;
}
