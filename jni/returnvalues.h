
#ifndef RETURNVALUES_H_
#define RETURNVALUES_H_

#define RETURN_VALUE(type) type type ## Value;
//#define NULL 0

#define LOCAL_FRAME_SIZE 100

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

#define CHECK_AND_CALL(variable, Type, signature)                   \
                                                                    \
    jmethodID retrieve ## Type = (*env)->GetMethodID(               \
        env, cls, "retrieve" # Type, signature);                    \
                                                                    \
    if (retrieve ## Type == NULL) {                                 \
        __android_log_write(ANDROID_LOG_ERROR,                      \
            "nativebenchmark", "missing method for " # Type);       \
       return 0;                                                    \
    }                                                               \
    local_reference = (*env)->CallObjectMethod(                     \
        env, benchmarkParameter, retrieve ## Type);                 \
                                                                    \
    if (local_reference == NULL) {                                  \
        __android_log_write(                                        \
            ANDROID_LOG_ERROR,                                      \
                "nativebenchmark", "type is null: " # Type );       \
    }                                                               \
                                                                    \
    variable ## Value = (*env)->NewGlobalRef(env, local_reference); \
    if (variable ## Value == NULL) {                                \
        __android_log_write(                                        \
            ANDROID_LOG_ERROR,                                      \
            "nativebenchmark",                                      \
            "can't create global reference " # Type); }  


// ---------------------------
// -- for custom benchmarks --

void set_up_custom_context(JNIEnv*, jobject, jint);
void tear_down_custom_context();
void init_strings(int);

jint __a, __b;

#define MAX_SIZE 1024
// todo set max size from par? (final static...)

jvalue jvalue_buf[MAX_SIZE];

#define VARIABLE(_ctype, direction) \
    _ctype _ctype ## __ ## direction;

#define ASSIGN_VARIABLE(_ctype, direction, val)     \
    _ctype ## __ ## direction = val;

#define VARIABLE_IN(_ctype)         \
    VARIABLE(_ctype, IN)

#define VARIABLE_OUT(_ctype)        \
    VARIABLE(_ctype, OUT)

#define POINTER_IN(_ctype)          \
    _ctype * _ctype ## _ptr__IN;

#define BUFFER_IN(_ctype)           \
    _ctype * _ctype ## _buf__IN;
//todo = (_ctype *) jvalue_buf;

#define PRIMITIVE_IN(_ctype)       \
    VARIABLE_IN(_ctype);            \
    POINTER_IN(_ctype);             \
    BUFFER_IN(_ctype);

#define PRIMITIVE_OUT(_ctype)       \
    VARIABLE_OUT(_ctype);            \
    POINTER_OUT(_ctype);             \
    BUFFER_OUT(_ctype);

#define REFERENCE_IN(_ctype)       \
    VARIABLE_IN(_ctype);            \

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

const char* field_name__OUT;
const char* static_field_name__OUT;
const char* field_signature__OUT;
const char* static_field_signature__OUT;

const char* method_name__OUT;
const char* static_method_name__OUT;
const char* method_signature__OUT;
const char* static_method_signature__OUT;

// must get method/field names from class somehow
// add to par
// add to par: native init code for native structures (byteArray)
// also set current_size
// todo directBufferValue > par !!
jsize current_size; // todo set from java?
jfieldID jfieldIDValue;
jfieldID jmethodIDValue;
char *classNameValue;

// todo par must init (utf version)
char string_utf__OUT[MAX_SIZE + 1];
jchar string_unicode__OUT[MAX_SIZE];


#endif
