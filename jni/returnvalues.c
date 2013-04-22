#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include "returnvalues.h"
#include "natives.h"

JNIEXPORT jint JNICALL
Java_fi_helsinki_cs_tituomin_nativebenchmark_BenchmarkParameter_initReturnvalues
(JNIEnv* env, jobject benchmarkParameter, jint size, jobject mockObject)
{
    jclass cls = (*env)->GetObjectClass(env, benchmarkParameter);
    jobject local_reference;

    CHECK_AND_CALL(jbooleanArray, BooleanArray, "()[Z");
    CHECK_AND_CALL(jbyteArray,    ByteArray,    "()[B");
    CHECK_AND_CALL(jcharArray,    CharArray,    "()[C");
    CHECK_AND_CALL(jdoubleArray,  DoubleArray,  "()[D");
    CHECK_AND_CALL(jfloatArray,   FloatArray,   "()[F");
    CHECK_AND_CALL(jintArray,     IntArray,     "()[I");
    CHECK_AND_CALL(jlongArray,    LongArray,    "()[J");
    CHECK_AND_CALL(jshortArray,   ShortArray,   "()[S");
    CHECK_AND_CALL(jobjectArray,  ObjectArray,  "()[Ljava/lang/Object;");
    CHECK_AND_CALL(jobject,       Object,       "()Ljava/lang/Object;");
    CHECK_AND_CALL(jclass,        Class,        "()Ljava/lang/Class;");
    CHECK_AND_CALL(jstring,       String,       "()Ljava/lang/String;");
    CHECK_AND_CALL(jthrowable,    Throwable,    "()Ljava/lang/Throwable;");
    CHECK_AND_CALL(directByteBuffer, DirectByteBuffer, "()Ljava/nio/ByteBuffer;");

    set_up_custom_context(env, benchmarkParameter, size, mockObject);
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
    (*env)->DeleteGlobalRef(env, mock_object);
    (*env)->DeleteGlobalRef(env, mock_class);
}


void set_up_custom_context(JNIEnv* env, jobject bPar, jint size, jobject mockObject) {
    __a = 2;
    __b = 3;
    jfieldIDValue = 0;
    jmethodIDValue = 0;

    classNameValue = "android/content/pm/PermissionInfo";
    field_name__OUT = "descriptionRes";
    field_signature__OUT = "I";
    static_field_name__OUT = "PROTECTION_NORMAL"; // todo: is final...
    static_field_signature__OUT = "I";
    method_name__OUT = "loadDescription";
    method_signature__OUT = "(Landroid/content/pm/PackageManager;)Ljava/lang/CharSequence;";
    static_method_name__OUT = "protectionToString";
    static_method_signature__OUT = "(I)Ljava/lang/String;";

    current_size = size;
    init_strings(size);

    ASSIGN_BUFFER(jboolean);
    ASSIGN_BUFFER(jbyte);
    ASSIGN_BUFFER(jchar);
    ASSIGN_BUFFER(jshort);
    ASSIGN_BUFFER(jint);
    ASSIGN_BUFFER(jlong);
    ASSIGN_BUFFER(jfloat);
    ASSIGN_BUFFER(jdouble);
    ASSIGN_BUFFER(jsize);

    jobject mock_object_l = mockObject;
    jclass mock_class_l = (*env)->GetObjectClass(env, mock_object_l);

    mock_object = (*env)->NewGlobalRef(env, mock_object_l);
    if (mock_object == NULL) {
        __android_log_write(                                         
            ANDROID_LOG_ERROR,                                       
            "nativebenchmark",                                       
            "can't create global reference mock_object");
    }  
    mock_class  = (*env)->NewGlobalRef(env, mock_class_l);
    if (mock_class == NULL) {
        __android_log_write(                                         
            ANDROID_LOG_ERROR,                                       
            "nativebenchmark",                                       
            "can't create global reference mock_class");
    }  

    MOCK_FIELD_ID_DEF(jboolean, "Z");
    MOCK_FIELD_ID_DEF(jbyte, "B");
    MOCK_FIELD_ID_DEF(jchar, "C");
    MOCK_FIELD_ID_DEF(jshort, "S");
    MOCK_FIELD_ID_DEF(jint, "I");
    MOCK_FIELD_ID_DEF(jlong, "J");
    MOCK_FIELD_ID_DEF(jfloat, "F");
    MOCK_FIELD_ID_DEF(jdouble, "D");
    MOCK_FIELD_ID_DEF(jobject, "Ljava/lang/Object;");

    objectConstructorID = (*env)->GetMethodID(env, jclassValue, "<init>", "()V");
    if (objectConstructorID == 0) {
        __android_log_write(ANDROID_LOG_VERBOSE, "returnvalues.c", "Object constructor not found.");
        return; /* method not found */
    }
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
