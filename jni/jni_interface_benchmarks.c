
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

// jarray arr = foo .. !! todo
// todo directBufferValue > par

#define ASSIGN_AND_CHECK(lhs, rhs)      \
if ((lhs = rhs) == NULL) return;        

#define ASSIGN_AND_CHECK_NEGATIVE(lhs, rhs) \
if ((lhs = rhs) < 0) return;

#define GET_STATIC_TYPE_FIELD(_ctype, _jname, _check_exceptions) \
_ctype##In = GetStatic##_jname##Field(env, jclassOut, jfieldIDOut);
if (_check_exceptions && (*env)->ExceptionCheck(env)) return;
                         
#define SET_STATIC_TYPE_FIELD(_ctype, _jname) \
SetStatic##_jname##Field(env, jclassOut, jfieldIDOut, _ctype##Out);
if (_check_exceptions && (*env)->ExceptionCheck(env)) return;

#define GET_TYPE_FIELD(_ctype, _jname) \
_ctype##In = Get##_jname##Field(env, jobjectOut, jfieldIDOut);
if (_check_exceptions && (*env)->ExceptionCheck(env)) return;

#define SET_TYPE_FIELD(_ctype, _jname) \
Set##_jname##Field(env, jobjectValue, jfieldIDOut, _ctype##Out);
if (_check_exceptions && (*env)->ExceptionCheck(env)) return;

#define CALL_VIRTUAL(_lhs, _jname, _parameters) \
_lhs ## Call##_jname##Method(env, jobjectOut, jmethodIDOut ## _parameters); // plus parameters! (python)
if (_check_exceptions && (*env)->ExceptionCheck(env)) return;
// todo variations ?

#define CALL_NONVIRTUAL(_lhs, _jname, _parameters) \
_lhs ## CallNonvirtual##_jname##Method(env, jobjectOut, jclassOut, jmethodIDOut ## _parameters);
if (_check_exceptions && (*env)->ExceptionCheck(env)) return;
// todo void params

#define CALL_STATIC(_lhs, _jname, _parameters) \
_lhs ## CallStatic##_jname##Method(env, jclassOut, jmethodIDOut ## _parameters);
if (_check_exceptions && (*env)->ExceptionCheck(env)) return;

/* NEW_PRIMITIVE_ARRAY(_artype, _jname, _typechar) */

/* GET_PRIMITIVE_ARRAY_ELEMENTS(_ctype, _jname) */
/* RELEASE_PRIMITIVE_ARRAY_ELEMENTS(_ctype, _jname) */
/* GET_PRIMITIVE_ARRAY_REGION(_ctype, _jname) */
/* SET_PRIMITIVE_ARRAY_REGION(_ctype, _jname) */

/* PRIMITIVE_ARRAY_FUNCTIONS(_ctype, _jname) */


void function_wrapper() {

    // todo
    // @new-object
    // todo this heavily depends on object type ...
    // @new-object-v
    // @new-object-a

    // -----------------------

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

    // @access-direct-buffer

    ASSIGN_AND_CHECK_NEGATIVE(jlongIn, (*env)->GetDirectBufferCapacity(env, directBufferValue), -1);
    ASSIGN_AND_CHECK(void_ptrIn, (*env)->GetDirectBufferAddress(env, directBufferValue));

    // @get-object-class

    jclassIn = (*env)->GetObjectClass(env, jobjectValue);

    // @find-class

    ASSIGN_AND_CHECK(jclassIn, (*env)->FindClass(env, classNameOut));

    // todo: specify requires behaviour --- V
    // @get-field-id no-java requires=find-class 

    ASSIGN_AND_CHECK(jfieldIDIn, (*env)->GetFieldID(env, jclassOut, fieldNameOut, fieldSignatureOut));

    // @get-static-field-id no-java

    ASSIGN_AND_CHECK(jfieldIDIn, (*env)->GetStaticFieldID(env, jclassOut, fieldNameOut, fieldSignatureOut));

    // @get-method-id no-java

    ASSIGN_AND_CHECK(jmethodIDInbound, (*env)->GetMethodID(env, jclassOut, methodNameOut, methodSignatureOut));

    // @get-static-method-id no-java

    ASSIGN_AND_CHECK(jmethodIDInbound, (*env)->GetStaticMethodID(env, jclassOut, methodNameOut, methodSignatureOut));

    // @new-string-utf

    ASSIGN_AND_CHECK(jstringIn, (*env)->NewStringUTF(env, byteArrayOutbound));

    // @new-string

    ASSIGN_AND_CHECK(jstringIn, (*env)->NewString(env, jcharArrayOutbound, length));

    // @new-objectarray

    // todo initialelement
    ASSIGN_AND_CHECK(jobjectArrayIn, (*env)->NewObjectArray(env, length, jelementClass, jinitialElement));

    // @new-direct-buffer

    ASSIGN_AND_CHECK(jobjectIn, (*env)->NewDirectByteBuffer(env, (void*)large_jchar_buf, capacity));
    
    // @global-ref

    ASSIGN_AND_CHECK(jobjectIn, (*env)->NewGlobalRef(env, jobjectValue));
    (*env)->DeleteGlobalRef(env, jobjectIn);

    // @local-ref

    ASSIGN_AND_CHECK(jobjectIn, (*env)->NewLocalRef(env, jobjectValue));
    (*env)->DeleteLocalRef(env, jobjectIn);

    // @weak-global-ref

    ASSIGN_AND_CHECK(jobjectIn, (*env)->NewWeakGlobalRef(env, jobjectValue));
    (*env)->DeleteWeakGlobalRef(env, jobjectIn);

    // @local-frame

    ASSIGN_AND_CHECK_NEGATIVE(jintIn, (*env)->PushLocalFrame(env, frame_capacity));
    jobjectIn = (*env)->PopLocalFrame(env, NULL);

}
