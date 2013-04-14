
#include <jni.h>
#include <stdio.h>

// todo set max size from par?
jvalue * jvalue_buf = jvalue[MAX_SIZE];

// todo save memory for primitives also?

#define VARIABLE(_ctype, direction) \
_ctype _ctype ## __ ## direction = 0;

#define VARIABLE_IN(_ctype)   \
    VARIABLE(_ctype, IN)

#define VARIABLE_OUT(_ctype) \
    VARIABLE(_ctype, OUT)
    
#define POINTER_IN(_ctype)    \
    _ctype * _ctype ## _ptr__IN = 0;

#define BUFFER_IN(_ctype)     \
    _ctype * _ctype ## _buf__IN = (_ctype *) jvalue_buf;

#define PRIMITIVE__IN(_ctype) \
    VARIABLE_IN(_ctype);          \
    POINTER_IN(_ctype);           \
    BUFFER_IN(_ctype);

#define REFERENCE__IN(_ctype) \
    VARIABLE_IN(_ctype);          \

// primitives

PRIMITIVE_IN(jboolean);       
PRIMITIVE_IN(jbyte);          
PRIMITIVE_IN(jchar);          
PRIMITIVE_IN(jshort);         
PRIMITIVE_IN(jint);           
PRIMITIVE_IN(jlong);          
PRIMITIVE_IN(jfloat);         
PRIMITIVE_IN(jdouble);        
PRIMITIVE_IN(jsize);

POINTER_IN(void);

VARIABLE_IN(jfieldID);
VARIABLE_IN(jmethodID);

// references

REFERENCE_IN(jobject);
REFERENCE_IN(jclass);
REFERENCE_IN(jstring);
REFERENCE_IN(jweak);

// arrays

REFERENCE_IN(jarray);
REFERENCE_IN(jobjectArray);
REFERENCE_IN(jbooleanArray);
REFERENCE_IN(jbyteArray);
REFERENCE_IN(jcharArray);
REFERENCE_IN(jshortArray);
REFERENCE_IN(jintArray);
REFERENCE_IN(jlongArray);
REFERENCE_IN(jfloatArray);
REFERENCE_IN(jdoubleArray);
REFERENCE_IN(jthrowable);

PRIMITIVE_OUT(jboolean);       
PRIMITIVE_OUT(jbyte);          
PRIMITIVE_OUT(jchar);          
PRIMITIVE_OUT(jshort);         
PRIMITIVE_OUT(jint);           
PRIMITIVE_OUT(jlong);          
PRIMITIVE_OUT(jfloat);         
PRIMITIVE_OUT(jdouble);        
PRIMITIVE_OUT(jsize);
PRIMITIVE_OUT(jobject);       

// par -----

jclass jelement_class__OUT = NULL;

const char* field_name__OUT = "needle";
const char* static_field_name__OUT = "sneedle";
const char* field_signature__OUT = "I;";

const char* method_name__OUT = "needle";
const char* static_method_name__OUT = "sneedle";
const char* method_signature__OUT = "I;";

// must get method/field names from class somehow
// add to par
// add to par: native init code for native structures (byteArray)
// also set current_size
// todo directBufferValue > par !!
jsize current_size = 0; // todo set from java?
jfieldID jfieldIDValue = 0;
jfieldID jmethodIDValue = 0;
char *classNameValue = NULL;

// todo par must init (utf version)
char * string_utf__OUT = char[MAX_SIZE];
jchar * string_unicode__OUT = jchar[MAX_SIZE];


#define ASSIGN_AND_CHECK(lhs, rhs)      \
    if ((lhs = rhs) == NULL) return;        

#define ASSIGN_AND_CHECK_NNEG(lhs, rhs) \
    if ((lhs = rhs) < 0) return;

#define GET_STATIC_TYPE_FIELD(_ctype, _jname, _check_exceptions)                                           \
    _ctype##__IN = GetStatic##_jname##Field(env, jclassValue, jfieldIDValue);                              \
    if (_check_exceptions) {if (*env)->ExceptionCheck(env) return};
                         
#define SET_STATIC_TYPE_FIELD(_ctype, _jname, _check_exceptions)                                           \
    SetStatic##_jname##Field(env, jclassValue, jfieldIDValue, _ctype##__OUT);                              \
    if (_check_exceptions) {if (*env)->ExceptionCheck(env)) return;}

#define GET_TYPE_FIELD(_ctype, _jname, _check_exceptions)                                                  \
    _ctype##__IN = Get##_jname##Field(env, jobjectValue, jfieldIDValue);                                   \
    if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}

#define SET_TYPE_FIELD(_ctype, _jname, _check_exceptions)                                                  \
    Set##_jname##Field(env, jobjectValue, jfieldIDValue, _ctype##__OUT);                                   \
    if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}

#define CALL_VIRTUAL(_lhs, _jname, _parameters, _check_exceptions)                                         \
    _lhs ## Call##_jname##Method(env, jobjectValue, jmethodIDValue ## _parameters);                        \ // plus parameters! (python)
    if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}
    // todo variations ?

#define CALL_NONVIRTUAL(_lhs, _jname, _parameters, _check_exceptions)                                      \
    _lhs ## CallNonvirtual##_jname##Method(env, jobjectValue, jclassValue, jmethodIDValue ## _parameters); \
    if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}
    // todo void params

#define CALL_STATIC(_lhs, _jname, _parameters, _check_exceptions)                                          \
    _lhs ## CallStatic##_jname##Method(env, jclassValue, jmethodIDValue ## _parameters);                   \
    if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}

#define NEW_PRIMITIVE_ARRAY(_artype, _jname)                                                               \
    ASSIGN_AND_CHECK(_artype ## __IN, New##_jname##Array(env, current_size));

#define GET_PRIMITIVE_ARRAY_ELEMENTS(_ctype, _jname)                                                       \
    ASSIGN_AND_CHECK(_ctype ##_ptr__IN, Get##_jname##ArrayElements(env,                                    \
        _ctype##ArrayValue, &jboolean__IN));                                                               

#define RELEASE_PRIMITIVE_ARRAY_ELEMENTS(_ctype, _jname)                                                   \
    Release##_jname##ArrayElements(env, _ctype##ArrayValue, _ctype ## _ptr__IN, 0)                         \

#define GET_PRIMITIVE_ARRAY_REGION(_ctype, _jname)                                                         \
    Get##_jname##ArrayRegion(JNIEnv* env,                                                                  \
        _ctype##Array jarr, 0, current_size, _ctype ## _buf__IN)                                           

#define SET_PRIMITIVE_ARRAY_REGION(_ctype, _jname)                                                         \
Set##_jname##ArrayRegion(JNIEnv* env,                                                                      \
    _ctype##ArrayValue, 0, current_size, _ctype ## _buf__IN)                                               

/* PRIMITIVE_ARRAY_FUNCTIONS(_ctype, _jname) */


void function_wrapper() {

    // todo
    // @new-object
    // todo this heavily depends on object type ...
    // @new-object-v
    // @new-object-a

    // -----------------------

    // @read-unicode vary=size

    ASSIGN_AND_CHECK(
        jchar_ptr__IN,
        (*env)->GetStringChars(env,
            stringValue,
            &jboolean__IN));

    jsize__IN = (*env)->GetStringLength(env, stringValue);

    (*env)->ReleaseStringChars(env,
        stringValue,
        jchar_ptr__IN);

    // @read-utf vary=size

    ASSIGN_AND_CHECK(
        jbyte_ptr__IN,
        (*env)->GetStringUTFChars(env,
            stringValue,
            &jboolean__IN));

    (*env)->ReleaseStringUTFChars(env,
        stringValue,
        jbyte_ptr__IN);

    // @read-unicode-critical vary=size

    ASSIGN_AND_CHECK(
        jchar_ptr__IN,
        (*env)->GetStringCritical(env,
            stringValue,
            &jboolean__IN));

    (*env)->ReleaseStringCritical(env,
        string,
        jchar_ptr__IN);

    // @copy-unicode vary=size

    jsize__IN = (*env)->GetStringLength(env, stringValue);

    (*env)->GetStringRegion(env,
        stringValue,
        0,
        jsize__IN,
        jchar_buf__IN);

    // @read-primitive-array-critical vary=size

    ASSIGN_AND_CHECK(
        void_ptr__IN,
        (*env)->GetPrimitiveArrayCritical(env,
            jbyteArrayValue,
            &jboolean__IN));

    (*env)->ReleasePrimitiveArrayCritical(env,
        jbyteArrayValue,
        void_ptr__IN,
        0);

    // @read-object-array-element vary=size

    jobject__IN = (*env)->GetObjectArrayElement(env,
        jobjectArrayValue, 0);

    // @write-object-array-element

    (*env)->SetObjectArrayElement(env,
        jobjectArrayValue,
        0,
        jobject__OUT);

    // @access-direct-buffer

    ASSIGN_AND_CHECK_NNEG(
        jlong__IN,
        (*env)->GetDirectBufferCapacity(env, directBufferValue));

    ASSIGN_AND_CHECK(
        void_ptr__IN,
        (*env)->GetDirectBufferAddress(env, directBufferValue));

    // @get-object-class

    jclass__IN = (*env)->GetObjectClass(env, jobjectValue);

    // @find-class

    ASSIGN_AND_CHECK(
        jclass__IN,
        (*env)->FindClass(env, classNameValue));

    // todo: specify requires behaviour --- V
    // @get-field-id  requires=find-class vary=size

    ASSIGN_AND_CHECK(
        jfieldID__IN,
        (*env)->GetFieldID(env,
            jclassValue,
            field_name__OUT,
            field_signature__OUT));

    // @get-static-field-id vary=size

    ASSIGN_AND_CHECK(
        jfieldID__IN,
        (*env)->GetStaticFieldID(env,
            jclassValue,
            static_field_name__OUT,
            field_signature__OUT));

    // @get-method-id vary=size

    ASSIGN_AND_CHECK(
        jmethodID__IN,
        (*env)->GetMethodID(env,
            jclassValue,
            method_name__OUT,
            method_signature__OUT));

 // @get-static-method-id vary=size

    ASSIGN_AND_CHECK(
        jmethodID__IN,
        (*env)->GetStaticMethodID(env,
            jclassValue,
            static_method_name__OUT,
            method_signature__OUT));

    // @new-string-utf

    ASSIGN_AND_CHECK(
        jstring__IN,
        (*env)->NewStringUTF(env,
            string_utf__OUT));

    // @new-string vary=size

    ASSIGN_AND_CHECK(
        jstring__IN,
        (*env)->NewString(env,
            string_unicode__OUT, current_size));

    // @new-objectarray vary=size

    // todo initialelement 
    ASSIGN_AND_CHECK(
        jobjectArray__IN,
        (*env)->NewObjectArray(env,
            current_size,
            jelement_class__OUT,
            jobject__OUT));

    // @new-direct-buffer vary=size

    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewDirectByteBuffer(env,
            (void*) jchar_buf__OUT, current_size));
    
    // @global-ref

    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewGlobalRef(env, jobjectValue));

    (*env)->DeleteGlobalRef(env, jobject__IN);

    // @local-ref

    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewLocalRef(env, jobjectValue));

    (*env)->DeleteLocalRef(env, jobject__IN);

    // @weak-global-ref

    ASSIGN_AND_CHECK(
        jobject__IN,
        (*env)->NewWeakGlobalRef(env, jobjectValue));

    (*env)->DeleteWeakGlobalRef(env, jobject__IN);

    // @local-frame

    ASSIGN_AND_CHECK_NNEG(
        jint__IN,
        (*env)->PushLocalFrame(env, 16));

    jobject__IN = (*env)->PopLocalFrame(env, NULL);

}
