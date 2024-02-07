#include <jni.h>
#include <string>
#include <mpv/client.h>
#include <android/log.h>

auto PLAYER_TAG = "mpv-android (player)";

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
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "sendCommandString : %s", cString);
  auto commandResult = mpv_command_string(handle, cString);
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "Command sent.. result = %d", commandResult);
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
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "nativeSetPropertyString invoked... (%s %s)", cKey, cValue);
  auto result = mpv_set_property_string(handle, cKey, cValue);
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "nativeSetPropertyString result = %d", result);
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
   __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "nativeSetOptionString invoked... (%s %s)", keyStr, valueStr);
  auto result = mpv_set_option_string(handle, keyStr, valueStr);
   __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "nativeSetOptionString result = %d", result);
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
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "nativeSetPropertyInt invoked... (%s %ld)", keyStr, cIntValue);
  auto result = mpv_set_property(handle, keyStr, MPV_FORMAT_INT64, &cIntValue);
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "nativeSetPropertyInt result = %d", result);
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
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "nativeSetPropertyDouble invoked... (%s %f)", keyStr, cDoubleValue);
  auto result = mpv_set_property(handle, keyStr, MPV_FORMAT_DOUBLE, &cDoubleValue);
   __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "nativeSetPropertyDouble result = %d", result);
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
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "nativeSetPropertyBoolean invoked... (%s %f)", keyStr, cBoolValue);
  auto result = mpv_set_property(handle, keyStr, MPV_FORMAT_FLAG, &cBoolValue);
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "nativeSetPropertyBoolean result = %d", result);
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
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "attachSurface invoked");
  auto cSurface = env->NewGlobalRef(java_surface);
  auto wid = (int64_t)(intptr_t) cSurface;
  auto handle = getHandle(env,thiz);
  auto result = mpv_set_option(handle, "wid", MPV_FORMAT_INT64, (void*) &wid);
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "attachSurface result = %d", result);
  auto surfaceAddress = reinterpret_cast<jlong>(cSurface);
  auto javaClazz = env->GetObjectClass(thiz);
  auto javaField = env->GetFieldID(javaClazz, "currentSurface", "J");
  env->SetLongField(thiz, javaField, surfaceAddress);
  return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_detachSurface(JNIEnv *env, jobject thiz) {
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "detachSurface invoked");
  int64_t wid = 0;
  auto handle = getHandle(env, thiz);
  auto result = mpv_set_option(handle, "wid", MPV_FORMAT_INT64, (void*) &wid);
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "detachSurface result = %d", result);
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
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "destroy invoked");
  auto handle = getHandle(env, thiz);
  mpv_wakeup(handle);
  mpv_destroy(handle);
}

extern "C"
JNIEXPORT jint JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_initialize(JNIEnv *env, jobject thiz) {
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "initialize invoked");
  auto handle = getHandle(env, thiz);
  auto result = mpv_initialize(handle);
  __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "initialize result = %d", result);
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
    auto clazz = env->FindClass("java/lang/Double");
    auto constructor = env->GetMethodID(clazz, "<init>", "(D)V");
    return env->NewObject(clazz, constructor, (jdouble) output);
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
    auto clazz = env->FindClass("java/lang/Boolean");
    auto constructor = env->GetMethodID(clazz, "<init>", "(Z)V");
    return env->NewObject(clazz, constructor, (jboolean) output);
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
    auto clazz = env->FindClass("java/lang/Integer");
    auto constructor = env->GetMethodID(clazz, "<init>", "(I)V");
    return env->NewObject(clazz, constructor, (jint) output);
  }
}

jobject createNoneValue(JNIEnv *env) {
  auto clazz = env->FindClass("fr/nextv/libmpv/LibMpv$Value$MpvNone");
  auto field = env->GetStaticFieldID(clazz, "INSTANCE", "Lfr/nextv/libmpv/LibMpv$Value$MpvNone;");
  return env->GetStaticObjectField(clazz, field);
}

jobject createValue(JNIEnv *env, mpv_format format, void *data) {
  switch (format) {
    case MPV_FORMAT_NONE: {
      return createNoneValue(env);
    }
    case MPV_FORMAT_STRING: {
      auto cString = *(const char**) data;
      auto javaString = env->NewStringUTF(cString);
      auto clazz = env->FindClass("fr/nextv/libmpv/LibMpv$Value$MpvString");
      auto constructor = env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;)V");
      return env->NewObject(clazz, constructor, javaString);
    }
    case MPV_FORMAT_FLAG: {
      auto boolValue = *(int*) data;
      auto clazz = env->FindClass("fr/nextv/libmpv/LibMpv$Value$MpvBoolean");
      auto constructor = env->GetMethodID(clazz, "<init>", "(Z)V");
      return env->NewObject(clazz, constructor, boolValue);
    }
    case MPV_FORMAT_INT64: {
      auto intValue = *(int64_t*) data;
      auto clazz = env->FindClass("fr/nextv/libmpv/LibMpv$Value$MpvLong");
      auto constructor = env->GetMethodID(clazz, "<init>", "(J)V");
      return env->NewObject(clazz, constructor, intValue);
    }
    case MPV_FORMAT_DOUBLE: {
      auto doubleValue = *(float*) data;
      auto clazz = env->FindClass("fr/nextv/libmpv/LibMpv$Value$MpvDouble");
      auto constructor = env->GetMethodID(clazz, "<init>", "(F)V");
      return env->NewObject(clazz, constructor, doubleValue);
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

jobject createJavaEventProperty(JNIEnv *env, mpv_event_property* property) {
  auto clazz = env->FindClass("fr/nextv/libmpv/LibMpv$Property");
  auto constructor = env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;Lfr/nextv/libmpv/LibMpv$Value;)V");
  auto value = createValue(env, property->format, property->data);
  auto name = env->NewStringUTF(property->name);
  return env->NewObject(clazz, constructor, name, value);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_fr_nextv_libmpv_MpvPlayerJni_awaitNextEvent(JNIEnv *env, jobject thiz) {
  auto handle = getHandle(env, thiz);
  auto nextEvent = mpv_wait_event(handle, 0);
  if (nextEvent->event_id != MPV_EVENT_NONE) {
    __android_log_print(
      ANDROID_LOG_INFO,
      PLAYER_TAG,
      "Received an mpv_event [id=%d, name=%s, error$%d]", nextEvent->event_id,  mpv_event_name(nextEvent->event_id), nextEvent->error
    );
  }

  auto playerEventClazz = env->FindClass("fr/nextv/libmpv/LibMpv$PlayerEvent");
  auto playerEventConstructor = env->GetMethodID(playerEventClazz, "<init>", "(ILfr/nextv/libmpv/LibMpv$EventData;)V");

  auto eventType = nextEvent->event_id;

  switch (eventType) {
    case MPV_EVENT_PROPERTY_CHANGE: {
      auto property = (mpv_event_property*) nextEvent->data;
      __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, "Property changed: [name=%s, format=%d]", property->name, property->format);
      auto eventData = createJavaEventProperty(env, (mpv_event_property*) property);
      return env->NewObject(playerEventClazz, playerEventConstructor, (int) eventType, eventData);
    }
    default: {
      return env->NewObject(playerEventClazz, playerEventConstructor, (int) eventType, createNoneValue(env));
    }
  }
}
