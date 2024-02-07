#include <jni.h>
#include <string>
#include <android/log.h>
#include <mpv/client.h>
extern "C" {
    #include <libavcodec/jni.h>
}

auto TAG = "mpv-android";

JavaVM* javaVmUniqueReference = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    __android_log_print(ANDROID_LOG_INFO, TAG, "JNI_OnLoad invoked");
    javaVmUniqueReference = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
  __android_log_print(ANDROID_LOG_INFO, TAG, "JNI_OnUnload invoked");
    // Perform cleanup when the library is unloaded (optional)
}

extern "C"
JNIEXPORT void JNICALL
Java_fr_nextv_libmpv_LibMpv_initializeMpvEngine(JNIEnv *env, jobject thiz) {
  __android_log_print(ANDROID_LOG_INFO, TAG, "initializeMpvEngine invoked");
  auto initializationResult = av_jni_set_java_vm(javaVmUniqueReference, nullptr);
  __android_log_print(ANDROID_LOG_INFO, TAG, "av_jni_set_java_vm result code: %d", initializationResult);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_fr_nextv_libmpv_LibMpv_createMpvHandle(JNIEnv *env, jobject thiz) {
    auto newMpvHandle = mpv_create();
    return reinterpret_cast<jlong>(newMpvHandle);
}
