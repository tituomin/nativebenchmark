#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include "returnvalues.h"
#include "natives.h"

JNIEXPORT jint JNICALL
Java_fi_helsinki_cs_tituomin_nativebenchmark_BenchmarkParameter_initReturnvalues
(JNIEnv* env, jobject benchmarkParameter, jint size)
{
    jclass cls = (*env)->GetObjectClass(env, benchmarkParameter);
    jobject local_reference;

    CHECK_AND_CALL(jbooleanArray, BooleanArray, "()[Z")
    CHECK_AND_CALL(jbyteArray,    ByteArray,    "()[B")
    CHECK_AND_CALL(jcharArray,    CharArray,    "()[C")
    CHECK_AND_CALL(jdoubleArray,  DoubleArray,  "()[D")
    CHECK_AND_CALL(jfloatArray,   FloatArray,   "()[F")
    CHECK_AND_CALL(jintArray,     IntArray,     "()[I")
    CHECK_AND_CALL(jlongArray,    LongArray,    "()[J")
    CHECK_AND_CALL(jshortArray,   ShortArray,   "()[S")
    CHECK_AND_CALL(jobjectArray,  ObjectArray,  "()[Ljava/lang/Object;")
    CHECK_AND_CALL(jobject,       Object,       "()Ljava/lang/Object;")
    CHECK_AND_CALL(jclass,        Class,        "()Ljava/lang/Class;")
    CHECK_AND_CALL(jstring,       String,       "()Ljava/lang/String;")
    CHECK_AND_CALL(jthrowable,    Throwable,    "()Ljava/lang/Throwable;")

    set_up_custom_context(env, benchmarkParameter, size);
    return 1;

}

JNIEXPORT void JNICALL
Java_fi_helsinki_cs_tituomin_nativebenchmark_BenchmarkParameter_freeReturnvalues
(JNIEnv* env, jobject benchmarkParameter)
{
    (*env)->DeleteGlobalRef(env, jclassValue);
    (*env)->DeleteGlobalRef(env, jobjectValue);
    (*env)->DeleteGlobalRef(env, jstringValue);
    (*env)->DeleteGlobalRef(env, jthrowableValue);
    (*env)->DeleteGlobalRef(env, jcharArrayValue);
    (*env)->DeleteGlobalRef(env, jfloatArrayValue);
    (*env)->DeleteGlobalRef(env, jlongArrayValue);
    (*env)->DeleteGlobalRef(env, jintArrayValue);
    (*env)->DeleteGlobalRef(env, jbooleanArrayValue);
    (*env)->DeleteGlobalRef(env, jobjectArrayValue);
    (*env)->DeleteGlobalRef(env, jbyteArrayValue);
    (*env)->DeleteGlobalRef(env, jshortArrayValue);
    (*env)->DeleteGlobalRef(env, jdoubleArrayValue);
}


void set_up_custom_context(JNIEnv* env, jobject bPar, jint size) {
    __a = 2;
    __b = 3;
    jfieldIDValue = 0;
    jmethodIDValue = 0;

    classNameValue = "android/content/pm/PermissionInfo";
    field_name__OUT = "flags";
    field_signature__OUT = "I";
    static_field_name__OUT = "FLAG_COSTS_MONEY"; // todo: is final...
    static_field_signature__OUT = "I";
    method_name__OUT = "loadDescription";
    method_signature__OUT = "(Landroid/content/pm/PackageManager;)Ljava/lang/CharSequence;";
    static_method_name__OUT = "protectionToString";
    static_method_signature__OUT = "(I)Ljava/lang/String;";

    current_size = size;
    init_strings(size);
}

void init_strings(int size) {
    int i;
    for (i = 0; i < size; i++) {
        string_utf__OUT[i] = 'a';
        string_unicode__OUT[i] = 1;
    }
    string_utf__OUT[i] = 0;
}

void tear_down_custom_context() {
}
