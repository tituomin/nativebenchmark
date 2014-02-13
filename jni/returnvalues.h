#include "natives.h"

#ifndef RETURNVALUES_H_
#define RETURNVALUES_H_

#define RETURN_VALUE(type) type type ## Value;
//#define NULL 0

int check_interrupted(JNIEnv *);

int interrupted;
jlong CHECK_INTERRUPTED_INTERVAL;
jlong repetitions;
jlong repetitionsLeft;
jclass thread_class;
jclass java_counterparts_class;
jobject java_counterparts_object;
jmethodID current_thread_mid;
jmethodID is_interrupted_mid;

jint localPersistentValue;

#define LOCAL_FRAME_SIZE 256

RETURN_VALUE(jclass)
RETURN_VALUE(jobject)
RETURN_VALUE(jstring)
RETURN_VALUE(jthrowable)
RETURN_VALUE(jcharArray)
RETURN_VALUE(jfloatArray)
RETURN_VALUE(jlongArray)
RETURN_VALUE(jintArray)
RETURN_VALUE(jbooleanArray)
RETURN_VALUE(jobjectArray)
RETURN_VALUE(jbyteArray)
RETURN_VALUE(jshortArray)
RETURN_VALUE(jdoubleArray)
jobject directByteBufferValue;

#define CHECK_AND_CALL(variable, Type, signature)                    \
                                                                     \
    jmethodID retrieve ## Type = (*env)->GetMethodID(                \
        env, cls, "retrieve" # Type, signature);                     \
                                                                     \
    if (retrieve ## Type == NULL) {                                  \
        __android_log_write(ANDROID_LOG_ERROR,                       \
            "nativebenchmark", "missing method for " # Type);        \
       return 0;                                                     \
    }                                                                \
    local_reference = (*env)->CallObjectMethod(                      \
        env, benchmarkParameter, retrieve ## Type);                  \
                                                                     \
    if (local_reference == NULL) {                                   \
        __android_log_write(                                         \
            ANDROID_LOG_ERROR,                                       \
                "nativebenchmark", "type is null: " # Type );        \
    }                                                                \
                                                                     \
    variable ## Value = (*env)->NewGlobalRef(env, local_reference);  \
    if (variable ## Value == NULL) {                                 \
        __android_log_write(                                         \
            ANDROID_LOG_ERROR,                                       \
            "nativebenchmark",                                       \
            "can't create global reference " # Type); }  


// ---------------------------
// -- for custom benchmarks --

void set_up_custom_context(JNIEnv*, jobject, jint, jobject);
void tear_down_custom_context();
void init_strings(int);

jint __a, __b;

#define MAX_SIZE 1024
// todo set max size from par? (final static...)

//jvalue *jvalue_buf;

#define VARIABLE(_ctype, direction)                                  \
    _ctype _ctype ## __ ## direction;

#define ASSIGN_VARIABLE(_ctype, direction, val)                      \
    _ctype ## __ ## direction = val;

#define VARIABLE_IN(_ctype)                                          \
    VARIABLE(_ctype, IN)

#define VARIABLE_OUT(_ctype)                                         \
    VARIABLE(_ctype, OUT)

#define POINTER_IN(_ctype)                                           \
    _ctype * _ctype ## _ptr__IN;

#define BUFFER_IN(_ctype)                                            \
    _ctype * _ctype ## _buf__IN;

#define ASSIGN_BUFFER(_ctype)                                        \
    _ctype ## _buf__IN = (_ctype *) jvalue_buf;

#define PRIMITIVE_IN(_ctype)                                         \
    VARIABLE_IN(_ctype);                                             \
    POINTER_IN(_ctype);                                              \
    BUFFER_IN(_ctype);

#define PRIMITIVE_OUT(_ctype)                                        \
    VARIABLE_OUT(_ctype);                                            \
    POINTER_OUT(_ctype);                                             \
    BUFFER_OUT(_ctype);

#define REFERENCE_IN(_ctype)                                         \
    VARIABLE_IN(_ctype);                                             \

#define MOCK_FIELD_ID(_ctype)                                        \
    jfieldID mock_ ## _ctype ## _field_id;                           \
    jfieldID mock_ ## _ctype ## _static_field_id;

#define MOCK_FIELD_ID_DEF(_ctype, sig)                               \
    mock_ ## _ctype ## _field_id =                                   \
        (*env)->GetFieldID(env, mock_class, #_ctype "Field", sig); \
    mock_ ## _ctype ## _static_field_id =                             \
        (*env)->GetStaticFieldID(env, mock_class, #_ctype "StaticField" , sig);

// todo save memory for primitives also?
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

PRIMITIVE_IN(char);

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

VARIABLE_OUT(jboolean);
VARIABLE_OUT(jbyte);
VARIABLE_OUT(jchar);
VARIABLE_OUT(jshort);
VARIABLE_OUT(jint);
VARIABLE_OUT(jlong);
VARIABLE_OUT(jfloat);
VARIABLE_OUT(jdouble);
VARIABLE_OUT(jsize);
VARIABLE_OUT(jobject);

// ---

// par -----

// todo todo todo
//jclass jelement_class__OUT;

MOCK_FIELD_ID(jboolean);
MOCK_FIELD_ID(jbyte);
MOCK_FIELD_ID(jchar);
MOCK_FIELD_ID(jshort);
MOCK_FIELD_ID(jint);
MOCK_FIELD_ID(jlong);
MOCK_FIELD_ID(jfloat);
MOCK_FIELD_ID(jdouble);
MOCK_FIELD_ID(jsize);
MOCK_FIELD_ID(jobject);

const char* field_name__OUT;
const char* static_field_name__OUT;
const char* field_signature__OUT;
const char* static_field_signature__OUT;

const char* method_name__OUT;
const char* static_method_name__OUT;
const char* method_signature__OUT;
const char* static_method_signature__OUT;

jclass mock_class;
jobject mock_object;

// must get method/field names from class somehow
// add to par
// add to par: native init code for native structures (byteArray)
// also set current_size
// todo directBufferValue > par !!
jsize current_size; // todo set from java?
jfieldID jfieldIDValue;
jfieldID jmethodIDValue;
jmethodID objectConstructorID; 
char *classNameValue;

jvalue jvalue_buf[fi_helsinki_cs_tituomin_nativebenchmark_BenchmarkParameter_MAXSIZE];
char string_utf__OUT[fi_helsinki_cs_tituomin_nativebenchmark_BenchmarkParameter_MAXSIZE + 1];
jchar string_unicode__OUT[fi_helsinki_cs_tituomin_nativebenchmark_BenchmarkParameter_MAXSIZE];


// todo par must init (utf version)
//char * string_utf__OUT;
//jchar * string_unicode__OUT;

#endif
