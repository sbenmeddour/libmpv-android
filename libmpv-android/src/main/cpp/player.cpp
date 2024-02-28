#include <jni.h>
#include <string>
#include <mpv/client.h>
#include <android/log.h>
extern "C" {
  #include <libavcodec/jni.h>
}

auto PLAYER_TAG = "mpv-android";

JavaVM* javaVmUniqueReference = nullptr;

jclass mpvStringClazz;
jmethodID mpvStringConstructor;

jclass mpvDoubleClazz;
jmethodID mpvDoubleConstructor;

jclass mpvIntClazz;
jmethodID mpvIntConstructor;

jclass mpvBoolClazz;
jmethodID mpvBoolConstructor;

jclass mpvPropertyChangeClazz;
jmethodID mpvPropertyChangeConstructor;

jclass mpvSimpleEventClazz;
jmethodID mpvSimpleEventCreator;

jclass mpvLogMessageClazz;
jmethodID mpvLogMessageConstructor;

jclass javaIntegerClazz;
jmethodID javaIntegerConstructor;

jclass javaBooleanClazz;
jmethodID javaBooleanConstructor;

jclass javaDoubleClazz;
jmethodID javaDoubleConstructor;

jobject MPV_NONE_SINGLETON;

jclass newJavaClassGlobalRef(jclass javaClazz, JNIEnv* env) {
  return reinterpret_cast<jclass>(env->NewGlobalRef(javaClazz));
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
  javaVmUniqueReference = vm;
  av_jni_set_java_vm(javaVmUniqueReference, nullptr);

  JNIEnv* env = nullptr;
  vm->GetEnv((void**) &env, JNI_VERSION_1_6);

  mpvStringClazz = newJavaClassGlobalRef(env->FindClass("fr/nextv/libmpv/LibMpv$Value$MpvString"), env);
  mpvStringConstructor = env->GetMethodID(mpvStringClazz, "<init>", "(Ljava/lang/String;)V");

  mpvDoubleClazz = newJavaClassGlobalRef(env->FindClass("fr/nextv/libmpv/LibMpv$Value$MpvDouble"), env);
  mpvDoubleConstructor = env->GetMethodID(mpvDoubleClazz, "<init>", "(F)V");

  mpvIntClazz = newJavaClassGlobalRef(env->FindClass("fr/nextv/libmpv/LibMpv$Value$MpvLong"), env);
  mpvIntConstructor = env->GetMethodID(mpvIntClazz, "<init>", "(J)V");

  mpvBoolClazz = newJavaClassGlobalRef(env->FindClass("fr/nextv/libmpv/LibMpv$Value$MpvBoolean"), env);
  mpvBoolConstructor = env->GetMethodID(mpvBoolClazz, "<init>", "(Z)V");

  mpvPropertyChangeClazz = newJavaClassGlobalRef(env->FindClass("fr/nextv/libmpv/LibMpv$Event$PropertyChange"), env);
  mpvPropertyChangeConstructor = env->GetMethodID(mpvPropertyChangeClazz, "<init>", "(Ljava/lang/String;Lfr/nextv/libmpv/LibMpv$Value;)V");

  mpvSimpleEventClazz = newJavaClassGlobalRef(env->FindClass("fr/nextv/libmpv/LibMpv$Event$SimpleEvent"), env);
  mpvSimpleEventCreator = env->GetStaticMethodID(mpvSimpleEventClazz, "fromNativeCode", "(I)Lfr/nextv/libmpv/LibMpv$Event$SimpleEvent;");

  mpvLogMessageClazz = newJavaClassGlobalRef(env->FindClass("fr/nextv/libmpv/LibMpv$Event$LogMessage"), env);
  mpvLogMessageConstructor = env->GetMethodID(mpvLogMessageClazz, "<init>", "(ILjava/lang/String;Ljava/lang/String;)V");

  javaIntegerClazz = newJavaClassGlobalRef(env->FindClass("java/lang/Integer"), env);
  javaIntegerConstructor = env->GetMethodID(javaIntegerClazz, "<init>", "(I)V");

  javaBooleanClazz = newJavaClassGlobalRef(env->FindClass("java/lang/Boolean"), env);
  javaBooleanConstructor = env->GetMethodID(javaBooleanClazz, "<init>", "(Z)V");

  javaDoubleClazz = newJavaClassGlobalRef(env->FindClass("java/lang/Double"), env);
  javaDoubleConstructor = env->GetMethodID(javaDoubleClazz, "<init>", "(D)V");

  auto mpvNoneClazz = env->FindClass("fr/nextv/libmpv/LibMpv$Value$MpvNone");
  auto mpvNoneInstanceField = env->GetStaticFieldID(mpvNoneClazz, "INSTANCE", "Lfr/nextv/libmpv/LibMpv$Value$MpvNone;");
  MPV_NONE_SINGLETON = env->NewGlobalRef(env->GetStaticObjectField(mpvNoneClazz, mpvNoneInstanceField));

  return JNI_VERSION_1_6;
}

jobject createNoneValue() {
  return MPV_NONE_SINGLETON;
}

jobject createValue(JNIEnv *env, mpv_format format, void *data) {
  switch (format) {
    case MPV_FORMAT_NONE: {
      return createNoneValue();
    }
    case MPV_FORMAT_STRING: {
      auto cString = *(const char**) data;
      auto javaString = env->NewStringUTF(cString);
      return env->NewObject(mpvStringClazz, mpvStringConstructor, javaString);
    }
    case MPV_FORMAT_FLAG: {
      auto boolValue = *(int*) data;
      return env->NewObject(mpvBoolClazz, mpvBoolConstructor, boolValue);
    }
    case MPV_FORMAT_INT64: {
      auto intValue = *(int64_t*) data;
      return env->NewObject(mpvIntClazz, mpvIntConstructor, intValue);
    }
    case MPV_FORMAT_DOUBLE: {
      auto doubleValue = *(float*) data;
      return env->NewObject(mpvDoubleClazz, mpvDoubleConstructor, doubleValue);
    }
    case MPV_FORMAT_NODE:
    case MPV_FORMAT_NODE_ARRAY:
    case MPV_FORMAT_NODE_MAP:
    case MPV_FORMAT_BYTE_ARRAY:
    case MPV_FORMAT_OSD_STRING: {
      auto message = "FORMAT " + std::to_string(format) + " is not supported";
      auto exception = env->FindClass("java/lang/NullPointerException");
      env->ThrowNew(exception, message.c_str());
      return NULL;
    }
  }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_fr_nextv_libmpv_LibMpv_createMpvHandle(JNIEnv *env, jobject thiz) {
  auto newMpvHandle = mpv_create();
  return reinterpret_cast<jlong>(newMpvHandle);
}

mpv_handle *getHandle(JNIEnv *env, jobject mpvPlayerJni) {
  auto classObject = env->GetObjectClass(mpvPlayerJni);
  auto field = env->GetFieldID(classObject, "handle", "J");
  auto handle = env->GetLongField(mpvPlayerJni, field);
  return reinterpret_cast<mpv_handle *>(static_cast<uintptr_t>(handle));
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_sendCommandString(
    JNIEnv *env,
    jobject thiz,
    jstring javaString
) {
  auto handle = getHandle(env, thiz);
  auto cString = env->GetStringUTFChars(javaString, nullptr);
  auto commandResult = mpv_command_string(handle, cString);
  env->ReleaseStringUTFChars(javaString, cString);
  return commandResult;
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_sendCommand(JNIEnv *env, jobject thiz, jobjectArray command) {
  auto handle = getHandle(env, thiz);
  const char *arguments[128] = { 0 };
  int length = env->GetArrayLength(command);
  for (int i = 0; i < length; ++i) {
    arguments[i] = env->GetStringUTFChars(
      (jstring) env->GetObjectArrayElement(command, i),
      NULL
    );
  }
  auto commandResult = mpv_command(handle, arguments);
  for (int i = 0; i < length; ++i) {
    env->ReleaseStringUTFChars(
        (jstring) env->GetObjectArrayElement(command, i),
        arguments[i]
    );
  }
  return commandResult;
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_setPropertyString(
    JNIEnv *env,
    jobject thiz,
    jstring key,
    jstring value
) {
  auto handle = getHandle(env, thiz);
  const char* cKey = env->GetStringUTFChars(key, nullptr);
  auto cValue = env->GetStringUTFChars(value, nullptr);
  auto result = mpv_set_property_string(handle, cKey, cValue);
  env->ReleaseStringUTFChars(key, cKey);
  env->ReleaseStringUTFChars(value, cValue);
  return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_setOptionString(
    JNIEnv *env,
    jobject thiz,
    jstring key,
    jstring value
) {
  auto handle = getHandle(env, thiz);
  const char* keyStr = env->GetStringUTFChars(key, nullptr);
  const char* valueStr = env->GetStringUTFChars(value, nullptr);
  auto result = mpv_set_option_string(handle, keyStr, valueStr);
  env->ReleaseStringUTFChars(key, keyStr);
  env->ReleaseStringUTFChars(value, valueStr);
  return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_setPropertyInt(
    JNIEnv *env,
    jobject thiz,
    jstring key,
    jint value
) {
  auto handle = getHandle(env, thiz);
  const char* keyStr = env->GetStringUTFChars(key, nullptr);
  auto cIntValue = static_cast<int64_t>(value);
  auto result = mpv_set_property(handle, keyStr, MPV_FORMAT_INT64, &cIntValue);
  env->ReleaseStringUTFChars(key, keyStr);
  return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_setPropertyDouble(
    JNIEnv *env,
    jobject thiz,
    jstring key,
    jdouble value
) {
  auto handle = getHandle(env, thiz);
  const char* keyStr = env->GetStringUTFChars(key, nullptr);
  double cDoubleValue = value;
  auto result = mpv_set_property(handle, keyStr, MPV_FORMAT_DOUBLE, &cDoubleValue);
  env->ReleaseStringUTFChars(key, keyStr);
  return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_setPropertyBoolean(
    JNIEnv *env,
    jobject thiz,
    jstring key,
    jboolean value
) {
  auto handle = getHandle(env, thiz);
  const char* keyStr = env->GetStringUTFChars(key, nullptr);
  double cBoolValue = (value == JNI_TRUE);
  auto result = mpv_set_property(handle, keyStr, MPV_FORMAT_FLAG, &cBoolValue);
  env->ReleaseStringUTFChars(key, keyStr);
  return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_observeProperty(
    JNIEnv *env,
    jobject thiz,
    jstring key,
    jint format
) {
  auto handle = getHandle(env, thiz);
  auto keyString = env->GetStringUTFChars(key, nullptr);
  auto result = mpv_observe_property(handle, 0, keyString, (mpv_format) format);
  env->ReleaseStringUTFChars(key, keyString);
  return result;
}


extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_unObserveProperties(
    JNIEnv *env,
    jobject thiz
) {
  auto handle = getHandle(env, thiz);
  auto result = mpv_unobserve_property(handle, 0);
  if (result < 0) {
    return result;
  }
  return (int) MPV_ERROR_SUCCESS;
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_attachSurface(JNIEnv *env, jobject thiz, jobject java_surface) {
  auto cSurface = env->NewGlobalRef(java_surface);
  auto wid = (int64_t)(intptr_t) cSurface;
  auto handle = getHandle(env,thiz);
  auto result = mpv_set_option(handle, "wid", MPV_FORMAT_INT64, (void*) &wid);
  auto surfaceAddress = reinterpret_cast<jlong>(cSurface);
  auto javaClazz = env->GetObjectClass(thiz);
  auto javaField = env->GetFieldID(javaClazz, "currentSurface", "J");
  env->SetLongField(thiz, javaField, surfaceAddress);
  return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_detachSurface(JNIEnv *env, jobject thiz) {
  int64_t wid = 0;
  auto handle = getHandle(env, thiz);
  auto result = mpv_set_option(handle, "wid", MPV_FORMAT_INT64, (void*) &wid);
  auto javaClazz = env->GetObjectClass(thiz);
  auto javaField = env->GetFieldID(javaClazz, "currentSurface", "J");
  auto javaAddress = env->GetLongField(thiz, javaField);
  auto javaSurfaceRef = reinterpret_cast<jobject>(javaAddress);
  if (javaSurfaceRef != 0) {
    env->DeleteGlobalRef(javaSurfaceRef);
    env->SetLongField(thiz, javaField, (jlong) 0);
  }
  return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_destroy(JNIEnv *env, jobject thiz) {
  __android_log_print(ANDROID_LOG_DEBUG, PLAYER_TAG, "Destroying mpv handle...");
  auto handle = getHandle(env, thiz);
  mpv_wakeup(handle);
  mpv_destroy(handle);
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_initialize(JNIEnv *env, jobject thiz) {
  auto handle = getHandle(env, thiz);
  auto result = mpv_initialize(handle);
  if (result != 0) {
    mpv_free(handle);
  }
  return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_getPropertyString(JNIEnv *env, jobject thiz, jstring key) {
  auto handle = getHandle(env, thiz);
  auto cString = env->GetStringUTFChars(key, NULL);
  auto result = mpv_get_property_string(handle, cString);
  env->ReleaseStringUTFChars(key, cString);
  if (result == NULL) {
    return NULL;
  } else {
    auto javaResult = env->NewStringUTF(result);
    mpv_free(result);
    return javaResult;
  }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_getPropertyDouble(JNIEnv *env, jobject thiz, jstring key) {
  auto handle = getHandle(env, thiz);
  auto cString = env->GetStringUTFChars(key, NULL);
  double output = 0;
  auto result = mpv_get_property(handle, cString, MPV_FORMAT_DOUBLE, &output);
  env->ReleaseStringUTFChars(key, cString);
  if (result >= 0) {
    return NULL;
  } else {
    return env->NewObject(javaDoubleClazz, javaDoubleConstructor, (jdouble) output);
  }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_getPropertyBoolean(JNIEnv *env, jobject thiz, jstring key) {
  auto handle = getHandle(env, thiz);
  auto cString = env->GetStringUTFChars(key, NULL);
  int output = 0;
  auto result = mpv_get_property(handle, cString, MPV_FORMAT_FLAG, &output);
  env->ReleaseStringUTFChars(key, cString);
  if (result >= 0) {
    return NULL;
  } else {
    return env->NewObject(javaBooleanClazz, javaBooleanConstructor, (jboolean) output);
  }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_getPropertyInt(JNIEnv *env, jobject thiz, jstring key) {
  auto handle = getHandle(env, thiz);
  auto cString = env->GetStringUTFChars(key, NULL);
  int64_t output = 0;
  auto result = mpv_get_property(handle, cString, MPV_FORMAT_INT64, &output);
  env->ReleaseStringUTFChars(key, cString);
  if (result >= 0) {
    return NULL;
  } else {
    return env->NewObject(javaIntegerClazz, javaIntegerConstructor, (jint) output);
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_cancelCurrentAwaitNextEvent(JNIEnv *env, jobject thiz) {
  auto handle = getHandle(env, thiz);
  mpv_wakeup(handle);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_awaitNextEvent(JNIEnv *env, jobject thiz) {
  auto handle = getHandle(env, thiz);
  auto nextEvent = mpv_wait_event(handle, 0);
  auto eventType = nextEvent->event_id;

  switch (eventType) {
    case MPV_EVENT_PROPERTY_CHANGE: {
      auto property = (mpv_event_property*) nextEvent->data;
      auto propertyName = env->NewStringUTF(property->name);
      auto value = createValue(env, property->format, property->data);
      return env->NewObject(mpvPropertyChangeClazz, mpvPropertyChangeConstructor, propertyName, value);
    }
    case MPV_EVENT_LOG_MESSAGE: {
      auto data = (mpv_event_log_message*) nextEvent->data;
      auto prefix = env->NewStringUTF(data->prefix);
      auto text = env->NewStringUTF(data->text);
      auto result =  env->NewObject(
          mpvLogMessageClazz,
          mpvLogMessageConstructor,
          (jint) data->log_level, prefix, text
      );
      return result;
    }
    case MPV_EVENT_START_FILE: {
      auto data = (mpv_event_start_file*) nextEvent->data;
      auto clazz = env->FindClass("fr/nextv/libmpv/LibMpv$Event$StartFile");
      auto constructor = env->GetMethodID(clazz, "<init>", "(I)V");
      return env->NewObject(
          clazz,
          constructor,
          (jint) data->playlist_entry_id
      );
    }
    case MPV_EVENT_END_FILE: {
      auto data = (mpv_event_end_file*) nextEvent->data;
      auto clazz = env->FindClass("fr/nextv/libmpv/LibMpv$Event$EndFile");
      auto constructor = env->GetMethodID(clazz, "<init>", "(IIII)V");
      return env->NewObject(
          clazz,
          constructor,
          (jint) data->reason, (jint) data->error, (jint) data->playlist_entry_id, (jint) data->playlist_insert_id
      );
    }
    case MPV_EVENT_CLIENT_MESSAGE:
    case MPV_EVENT_COMMAND_REPLY:
    case MPV_EVENT_GET_PROPERTY_REPLY:
    case MPV_EVENT_SET_PROPERTY_REPLY:
    case MPV_EVENT_NONE:
    case MPV_EVENT_SHUTDOWN:
    case MPV_EVENT_FILE_LOADED:
    case MPV_EVENT_IDLE:
    case MPV_EVENT_TICK:
    case MPV_EVENT_SEEK:
    case MPV_EVENT_VIDEO_RECONFIG:
    case MPV_EVENT_AUDIO_RECONFIG:
    case MPV_EVENT_PLAYBACK_RESTART:
    case MPV_EVENT_QUEUE_OVERFLOW:
    case MPV_EVENT_HOOK: {
      return env->CallStaticObjectMethod(mpvSimpleEventClazz, mpvSimpleEventCreator, (jint ) nextEvent->event_id);
    }

  }
}
