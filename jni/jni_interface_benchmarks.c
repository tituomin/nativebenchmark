
#include <jni.h>
#include "returnvalues.h"
#include "custom_benchmarks.h"

// @BEGIN

void function_wrapper() {

    // @CreateObject
    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewObject(env, jclassValue));

    // -----------------------

    // @ReadUnicode vary=size

    ASSIGN_AND_CHECK(
        jchar_ptr__IN,
        (*env)->GetStringChars(env,
            stringValue,
            &jboolean__IN));

    jsize__IN = (*env)->GetStringLength(env, stringValue);

    (*env)->ReleaseStringChars(env,
        stringValue,
        jchar_ptr__IN);

    // @ReadUtf vary=sizeobjectarrayelement

    ASSIGN_AND_CHECK(
        jbyte_ptr__IN,
        (*env)->GetStringUTFChars(env,
            stringValue,
            &jboolean__IN));

    (*env)->ReleaseStringUTFChars(env,
        stringValue,
        jbyte_ptr__IN);

    // @ReadUnicodeCritical vary=size

    ASSIGN_AND_CHECK(
        jchar_ptr__IN,
        (*env)->GetStringCritical(env,
            stringValue,
            &jboolean__IN));

    (*env)->ReleaseStringCritical(env,
        string,
        jchar_ptr__IN);

    // @CopyUnicode vary=size

    jsize__IN = (*env)->GetStringLength(env, stringValue);

    (*env)->GetStringRegion(env,
        stringValue,
        0,
        jsize__IN,
        jchar_buf__IN);

    // @ReadPrimitiveArrayCritical vary=size

    ASSIGN_AND_CHECK(
        void_ptr__IN,
        (*env)->GetPrimitiveArrayCritical(env,
            jbyteArrayValue,
            &jboolean__IN));

    (*env)->ReleasePrimitiveArrayCritical(env,
        jbyteArrayValue,
        void_ptr__IN,
        0);

    // @ReadObjectArrayElement vary=size

    jobject__IN = (*env)->GetObjectArrayElement(env,
        jobjectArrayValue, 0);

    // @WriteObjectArrayElement

    (*env)->SetObjectArrayElement(env,
        jobjectArrayValue,
        0,
        jobject__OUT);

    // @AccessDirectBuffer

    ASSIGN_AND_CHECK_NNEG(
        jlong__IN,
        (*env)->GetDirectBufferCapacity(env, directBufferValue));

    ASSIGN_AND_CHECK(
        void_ptr__IN,
        (*env)->GetDirectBufferAddress(env, directBufferValue));

    // @GetObjectClass

    jclass__IN = (*env)->GetObjectClass(env, jobjectValue);

    // @FindClass

    ASSIGN_AND_CHECK(
        jclass__IN,
        (*env)->FindClass(env, classNameValue));

    // @GetFieldId vary=size

    ASSIGN_AND_CHECK(
        jfieldID__IN,
        (*env)->GetFieldID(env,
            jclassValue,
            field_name__OUT,
            field_signature__OUT));

    // @GetStaticFieldId vary=size

    ASSIGN_AND_CHECK(
        jfieldID__IN,
        (*env)->GetStaticFieldID(env,
            jclassValue,
            static_field_name__OUT,
            field_signature__OUT));

    // @GetMethodId vary=size

    ASSIGN_AND_CHECK(
        jmethodID__IN,
        (*env)->GetMethodID(env,
            jclassValue,
            method_name__OUT,
            method_signature__OUT));

 // @GetStaticMethodId vary=size

    ASSIGN_AND_CHECK(
        jmethodID__IN,
        (*env)->GetStaticMethodID(env,
            jclassValue,
            static_method_name__OUT,
            method_signature__OUT));

    // @NewStringUtf

    ASSIGN_AND_CHECK(
        jstring__IN,
        (*env)->NewStringUTF(env,
            string_utf__OUT));

    // @NewString vary=size

    ASSIGN_AND_CHECK(
        jstring__IN,
        (*env)->NewString(env,
            string_unicode__OUT, current_size));

    // @NewObjectArray vary=size

    // todo initialelement
    ASSIGN_AND_CHECK(
        jobjectArray__IN,
        (*env)->NewObjectArray(env,
            current_size,
            // todo option parametrize
            jclassValue,
            jobject__OUT));

    // @NewDirectBuffer vary=size

    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewDirectByteBuffer(env,
                                    // todo check in/out
            (void*) jchar_buf__IN, current_size));

    // @GlobalRef

    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewGlobalRef(env, jobjectValue));

    (*env)->DeleteGlobalRef(env, jobject__IN);

    // @LocalRef

    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewLocalRef(env, jobjectValue));

    (*env)->DeleteLocalRef(env, jobject__IN);

    // @WeakGlobalRef

    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewWeakGlobalRef(env, jobjectValue));

    (*env)->DeleteWeakGlobalRef(env, jobject__IN);

    // @LocalFrame

    ASSIGN_AND_CHECK_NNEG(
        jint__IN,
        (*env)->PushLocalFrame(env, 16));

    jobject__IN = (*env)->PopLocalFrame(env, NULL);

    // @END

}
