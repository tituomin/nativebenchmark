
#include <jni.h>
#include <stdio.h>
#include <android/log.h>
#include "returnvalues.h"
#include "custom_benchmarks.h"

// @BEGIN

void function_wrapper() {

    // @inits
    // @inits-end

    // @CreateObject alloc=true direction=cj
    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewObject(env, jclassValue, objectConstructorID));

    // @AllocObject alloc=true direction=cj
    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->AllocObject(env, jclassValue));

    // -----------------------

    // @ReadUnicode vary=size direction=cj

    ASSIGN_AND_CHECK(
        jchar_ptr__IN,
        (*env)->GetStringChars(env,
            jstringValue,
            &jboolean__IN));

    (*env)->ReleaseStringChars(env,
        jstringValue,
        jchar_ptr__IN);

    // @ReadUtf vary=sizeobjectarrayelement direction=cj

    ASSIGN_AND_CHECK(
        jbyte_ptr__IN,
        (*env)->GetStringUTFChars(env,
            jstringValue,
            &jboolean__IN));

    (*env)->ReleaseStringUTFChars(env,
        jstringValue,
        (char *)jbyte_ptr__IN);

    // @ReadUnicodeCritical vary=size direction=cj

    ASSIGN_AND_CHECK(
        jchar_ptr__IN,
        (*env)->GetStringCritical(env,
            jstringValue,
            &jboolean__IN));

    (*env)->ReleaseStringCritical(env,
        jstringValue,
        jchar_ptr__IN);

    // @StringLength vary=size direction=cj

    jsize__IN = (*env)->GetStringLength(env, jstringValue);

    // @StringLengthUTF vary=size direction=cj

    jsize__IN = (*env)->GetStringUTFLength(env, jstringValue);

    // @CopyUnicode vary=size direction=cj

    (*env)->GetStringRegion(
        env,
        jstringValue,
        0,
        current_size,
        jchar_buf__IN);

    // @CopyUTF vary=size direction=cj

    (*env)->GetStringUTFRegion(
        env,
        jstringValue,
        0,
        current_size,
        char_buf__IN);

    // @ReadPrimitiveArrayCritical vary=size direction=cj

    ASSIGN_AND_CHECK(
        void_ptr__IN,
        (*env)->GetPrimitiveArrayCritical(env,
            jbyteArrayValue,
            &jboolean__IN));

    (*env)->ReleasePrimitiveArrayCritical(env,
        jbyteArrayValue,
        void_ptr__IN,
        0);

    // @ReadObjectArrayElement direction=cj

    if (current_size > 0) {
        jobject__IN = (*env)->GetObjectArrayElement(
            env,
            jobjectArrayValue, 0);
    }

    // @WriteObjectArrayElement direction=cj

    if (current_size > 0) {
        (*env)->SetObjectArrayElement(
            env,
            jobjectArrayValue,
            0,
            jobject__OUT);
    }

    // @AccessDirectBuffer direction=cj

    ASSIGN_AND_CHECK_NNEG(
        jlong__IN,
        (*env)->GetDirectBufferCapacity(env, directByteBufferValue));

    ASSIGN_AND_CHECK(
        void_ptr__IN,
        (*env)->GetDirectBufferAddress(env, directByteBufferValue));

    // @GetObjectClass direction=cj

    jclass__IN = (*env)->GetObjectClass(env, jobjectValue);

    // @FindClass direction=cj

    ASSIGN_AND_CHECK(
        jclass__IN,
        (*env)->FindClass(env, classNameValue));

    // @GetFieldId vary=size direction=cj

    ASSIGN_AND_CHECK(
        jfieldID__IN,
        (*env)->GetFieldID(env,
            jclassValue,
            field_name__OUT,
            field_signature__OUT));

    // @GetStaticFieldId vary=size direction=cj

    ASSIGN_AND_CHECK(
        jfieldID__IN,
        (*env)->GetStaticFieldID(env,
            jclassValue,
            static_field_name__OUT,
            static_field_signature__OUT));

    // @GetMethodId vary=size direction=cj

    ASSIGN_AND_CHECK(
        jmethodID__IN,
        (*env)->GetMethodID(env,
            jclassValue,
            method_name__OUT,
            method_signature__OUT));

 // @GetStaticMethodId vary=size direction=cj

    ASSIGN_AND_CHECK(
        jmethodID__IN,
        (*env)->GetStaticMethodID(env,
            jclassValue,
            static_method_name__OUT,
            static_method_signature__OUT));

    // @NewStringUtf alloc=true direction=cj

    ASSIGN_AND_CHECK(
        jstring__IN,
        (*env)->NewStringUTF(env,
            string_utf__OUT));

    // @NewString vary=size alloc=true direction=cj

    ASSIGN_AND_CHECK(
        jstring__IN,
        (*env)->NewString(env,
            string_unicode__OUT, current_size));

    // @NewObjectArray vary=size  alloc=true direction=cj

    // todo initialelement
    ASSIGN_AND_CHECK(
        jobjectArray__IN,
        (*env)->NewObjectArray(env,
            current_size,
            // todo option parametrize
            jclassValue,
            jobjectValue));

    // @NewDirectBuffer vary=size  alloc=true direction=cj

    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewDirectByteBuffer(env,
                                    // todo check in/out
            (void*) jchar_buf__IN, current_size));

    // @GlobalRef direction=cj

    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewGlobalRef(env, jobjectValue));

    (*env)->DeleteGlobalRef(env, jobject__IN);

    // @LocalRef direction=cj

    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewLocalRef(env, jobjectValue));

    (*env)->DeleteLocalRef(env, jobject__IN);

    // ---WeakGlobalRef direction=cj

    /* ASSIGN_AND_CHECK( */
    /*     jobject__IN, */
    /*     (*env)->NewWeakGlobalRef(env, jobjectValue)); */

    /* (*env)->DeleteWeakGlobalRef(env, jobject__IN); */

    // @LocalFrame direction=cj

    ASSIGN_AND_CHECK_NNEG(
        jint__IN,
        (*env)->PushLocalFrame(env, 16));

    jobject__IN = (*env)->PopLocalFrame(env, NULL);

    // @ExceptionCheck direction=cj

    (*env)->ExceptionCheck(env);

    // @END

}
