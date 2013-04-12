
#include <jni.h>
#include <stdio.h>

// todo MAX_SIZE
jchar *large_jchar_buf = jchar[HUGE];
jchar *jchar_buf = jchar[MAX_SIZE];
jchar *jchar_ptr = NULL;
jbyte *jbyte_ptr = NULL;
void  *void_ptrIn = NULL;
jobject jobjectIn = NULL;
jlong jlongIn = 0;
jclass jclassIn = NULL;
jobject jobj = NULL; // todo get from params
jboolean isCopy = NULL;
jsize length = 0;
jlong capacity = 0;
jint frame_capacity = 16;

jclass jelementClassOutbound = NULL;
jobject jinitialElement = NULL;

char *classNameOut = NULL;
jclass jclassOut = NULL;
// must get method/field names from class somehow

// index must be set from runner code !! todo

// jstring string = jstringValue
// jarray arr = foo .. !! todo
// todo directBufferValue

#define ASSIGN_AND_CHECK(lhs, rhs) \
 if ((lhs = rhs) == NULL) return;        \


void function_wrapper() {

    // @read-unicode
    ASSIGN_AND_CHECK(jchar_ptr, (*env)->GetStringChars(env, string, &isCopy));
    length = (*env)->GetStringLength(env, string);
    (*env)->ReleaseStringChars(env, string, jchar_ptr);

    // @read-utf
    ASSIGN_AND_CHECK(jbyte_ptr, (*env)->GetStringUTFChars(env, string, &isCopy));
    (*env)->ReleaseStringUTFChars(env, string, jbyte_ptr);

    // @copy-unicode
    length = (*env)->GetStringLength(env, string);
    (*env)->GetStringRegion(env, string, 0, length, jchar_buf);

    // @read-unicode-critical
    ASSIGN_AND_CHECK(jchar_prt, (*env)->GetStringCritical(env, string, &isCopy));
    (*env)->ReleaseStringCritical(env, string, jchar_prt);

    // @read-primitive-array-critical
    ASSIGN_AND_CHECK(void_ptrIn, (*env)->GetPrimitiveArrayCritical(env, jarr, isCopy));
    (*env)->ReleasePrimitiveArrayCritical(env, jarr, val, jint mode); // todo what is mode

    // @read-object-array-element
    jobjectIn = (*env)->GetObjectArrayElement(env, jarr, index);

    // @write-object-array-element
    (*env)->SetObjectArrayElement(env, jarrayValue, index, jobjectValue);

    // TODO HERE

    // @access-direct-buffer
    jlongIn = (*env)->GetDirectBufferCapacity(env, directBufferValue);
    void_ptrIn = (*env)->GetDirectBufferAddress(env, directBufferValue);

    // @get-object-class
    jclassIn = (*env)->GetObjectClass(env, jobjectValue);

    // @find-class
    jclassIn = (*env)->FindClass(env, classNameOut);

    // todo: specify requires behaviour
    // @get-field-id no-java requires=find-class 
    jfieldIDIn = (*env)->GetFieldID(env, jclassOut, fieldNameOut, fieldSignatureOut);

    // @get-static-field-id no-java
    jfieldIDIn = (*env)->GetStaticFieldID(env, jclassOut, fieldNameOut, fieldSignatureOut);

    // @get-method-id no-java
    jmethodIDInbound = (*env)->GetMethodID(env, jclassOut, methodNameOut, methodSignatureOut);

    // @get-static-method-id no-java
    jmethodIDInbound = (*env)->GetStaticMethodID(env, jclassOut, methodNameOut, methodSignatureOut);

    // @new-string-utf
    jstringIn = (*env)->NewStringUTF(env, byteArrayOutbound);

    // @new-string
    jstringIn = (*env)->NewString(env, jcharArrayOutbound, length);

    // @new-objectarray
    jobjectArrayIn = (*env)->NewObjectArray(env, length, jelementClass, jinitialElement); // todo

    // @new-direct-buffer
    jobjectIn = (*env)->NewDirectByteBuffer(env, (void*)large_jchar_buf, capacity);

    // @new-object
    // todo this heavily depends on object type ...
    // @new-object-v
    // @new-object-a

    // @global-ref
    jobjectIn = (*env)->NewGlobalRef(env, jobjectValue);
    (*env)->DeleteGlobalRef(env, jobjectIn);

    // @local-ref
    jobjectIn = (*env)->NewLocalRef(env, jobjectValue);
    (*env)->DeleteLocalRef(env, jobjectIn);

    // @weak-global-ref
    jobjectIn = (*env)->NewWeakGlobalRef(env, jobjectValue);
    (*env)->DeleteWeakGlobalRef(env, jobjectIn);

    // @local-frame
    jintIn = (*env)->PushLocalFrame(env, frame_capacity);
    jobjectIn = (*env)->PopLocalFrame(env, NULL);

}
