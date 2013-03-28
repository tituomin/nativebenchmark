
t = """
#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include "natives.h"
#include "native_benchmarks.h"
#include "returnvalues.h"

{initialisers}
{jni_function_templates}

"""

initialisers_t = """
static jlong CHECK_INTERRUPTED_INTERVAL;
static jlong repetitions;
static jclass java_counterparts_class;
static jmethodID mids[{amount_of_methods}];
static jclass thread_class;
static jmethodID current_thread_mid;
static jmethodID is_interrupted_mid;

static void init_methodids(JNIEnv *env) {{
    jmethodID mid;
{mid_inits}
}}

int check_interrupted(JNIEnv *env) {{
    jobject current_thread = NULL;
    current_thread = (*env)->CallStaticObjectMethod(env, thread_class, current_thread_mid);
    if (current_thread == NULL) {{
        __android_log_write(ANDROID_LOG_ERROR, "check_interrupted", "Can't get curren thread");
        return 1;
    }}
    jboolean interrupted = (*env)->CallBooleanMethod(env, current_thread, is_interrupted_mid);
    (*env)->DeleteLocalRef(env, current_thread);
    if (interrupted == JNI_TRUE) {{
        __android_log_write(ANDROID_LOG_VERBOSE, "check_interrupted", "Was interrupted");
        return 1;
    }}
    return 0;
}}

void throw_interrupted_exception(JNIEnv *env) {{
    jclass newExcCls;
    newExcCls = (*env)->FindClass(env,
    "java/lang/InterruptedException");
    if (newExcCls == NULL) {{
        /* Unable to find the exception class, give up. */
        return;
    }}
    (*env)->ThrowNew(env, newExcCls, "thrown from C code");
}}

JNIEXPORT void JNICALL
Java_fi_helsinki_cs_tituomin_nativebenchmark_BenchmarkRegistry_initNative
(JNIEnv *env, jclass cls, jlong reps, jlong interval, jclass javaCounterparts, jclass thread_cls) {{
    repetitions = reps;
    CHECK_INTERRUPTED_INTERVAL = interval;
    jclass java_counterparts_class_global_ref = NULL;
    jclass thread_class_global_ref = NULL;
    java_counterparts_class_global_ref = (*env)->NewGlobalRef(env, javaCounterparts);
    if (java_counterparts_class_global_ref == NULL) {{
        __android_log_write(ANDROID_LOG_ERROR, "initNative", "Could not create global ref.");
        return;
    }}
    java_counterparts_class = java_counterparts_class_global_ref;
    init_methodids(env);

    thread_class_global_ref = (*env)->NewGlobalRef(env, thread_cls);
    if (thread_class_global_ref == NULL) {{
        __android_log_write(ANDROID_LOG_ERROR, "initNative", "Could not create global ref.");
        return;
    }}
    thread_class = thread_class_global_ref;

    current_thread_mid = (*env)->GetStaticMethodID(env, thread_class, "currentThread", "()Ljava/lang/Thread;");
    if (current_thread_mid == NULL) {{
        __android_log_write(ANDROID_LOG_ERROR, "initNative", "Could not find currentThread");
        return;
    }}
    is_interrupted_mid = (*env)->GetMethodID(env, thread_class, "isInterrupted", "()Z");
    if (is_interrupted_mid == NULL) {{
        __android_log_write(ANDROID_LOG_ERROR, "check_interrupted", "Can't get isInterrupted method");
        return;
    }}

}}

JNIEXPORT void JNICALL
Java_fi_helsinki_cs_tituomin_nativebenchmark_BenchmarkRegistry_setRepetitions
(JNIEnv *env, jclass cls, jlong reps) {{
    repetitions = reps;
}}

"""

mid_init_t = """
    mid = (*env)->GetStaticMethodID(env, java_counterparts_class, "{method_name}", "{method_descriptor}");
    if (mid == NULL) {{
        __android_log_write(ANDROID_LOG_VERBOSE, "nativemethod", "{method_descriptor} not found.");
        return; /* method not found */
    }}
    mids[{seq_no} - 1] = mid;

"""
