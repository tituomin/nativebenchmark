
#include <jni.h>
#include <stdio.h>

jsize current_size = 0; // todo set from java?

jboolean jboolean__IN = NULL;
jsize jsize__IN = 0;
jlong jlong__IN = 0;


jobject jobject__IN = NULL;
jclass jclass__IN = NULL;

jchar *jchar_ptr__IN = NULL;
jbyte *jbyte_ptr__IN = NULL;
void  *void_ptr__IN = NULL;

jchar *jchar_buf__IN = jchar[MAX_SIZE]; // todo

jobject jobject__OUT = NULL; // todo
jclass jelement_class__OUT = NULL;
jobject jinitial_element__OUT = NULL;


const char* field_name__OUT = "needle";
const char* static_field_name__OUT = "sneedle";
const char* field_signature__OUT = "I;";

const char* method_name__OUT = "needle";
const char* static_method_name__OUT = "sneedle";
const char* method_signature__OUT = "I;";

jchar *jchar_buf__OUT = jchar[MAX_SIZE];

// must get method/field names from class somehow
// index must be set from runner code !! todo

// add to par: native init code for native structures (byteArray)
// also set current_size
// byteArray__OUT jcharArray__OUT

// add to par
char *classNameValue = NULL;

// jarray arr = foo .. !! todo
// todo directBufferValue > par !!

#define ASSIGN_AND_CHECK(lhs, rhs)      \
if ((lhs = rhs) == NULL) return;        

#define ASSIGN_AND_CHECK_NNEG(lhs, rhs) \
if ((lhs = rhs) < 0) return;

#define GET_STATIC_TYPE_FIELD(_ctype, _jname, _check_exceptions)                                 \
_ctype##__IN = GetStatic##_jname##Field(env, jclassOut, jfieldIDOut);                            \
if (_check_exceptions) {if (*env)->ExceptionCheck(env) return};
                         
#define SET_STATIC_TYPE_FIELD(_ctype, _jname)                                                    \
SetStatic##_jname##Field(env, jclassOut, jfieldIDOut, _ctype##Out);                              \
if (_check_exceptions) {if (*env)->ExceptionCheck(env)) return;}

#define GET_TYPE_FIELD(_ctype, _jname)                                                           \
_ctype##__IN = Get##_jname##Field(env, jobjectOut, jfieldIDOut);                                 \
if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}

#define SET_TYPE_FIELD(_ctype, _jname)                                                           \
Set##_jname##Field(env, jobjectValue, jfieldIDOut, _ctype##Out);                                 \
if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}

#define CALL_VIRTUAL(_lhs, _jname, _parameters)                                                  \
_lhs ## Call##_jname##Method(env, jobjectOut, jmethodIDOut ## _parameters);                      \ // plus parameters! (python)
if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}
// todo variations ?

#define CALL_NONVIRTUAL(_lhs, _jname, _parameters)                                               \
_lhs ## CallNonvirtual##_jname##Method(env, jobjectOut, jclassOut, jmethodIDOut ## _parameters); \
if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}
// todo void params

#define CALL_STATIC(_lhs, _jname, _parameters)                                                   \
_lhs ## CallStatic##_jname##Method(env, jclassOut, jmethodIDOut ## _parameters);                 \
if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}

// todo:

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

    // @read-unicode vary=size

    ASSIGN_AND_CHECK(jchar_ptr__IN, (*env)->GetStringChars(env, stringValue, &jboolean__IN));
    jsize__IN = (*env)->GetStringLength(env, string);
    (*env)->ReleaseStringChars(env, stringValue, jchar_ptr__IN);

    // @read-utf vary=size

    ASSIGN_AND_CHECK(jbyte_ptr__IN, (*env)->GetStringUTFChars(env, stringValue, &jboolean__IN));
    (*env)->ReleaseStringUTFChars(env, stringValue, jbyte_ptr__IN);

    // @read-unicode-critical vary=size

    ASSIGN_AND_CHECK(jchar_ptr__IN, (*env)->GetStringCritical(env, stringValue, &jboolean__IN));
    (*env)->ReleaseStringCritical(env, string, jchar_ptr__IN);

    // @copy-unicode vary=size

    jsize__IN = (*env)->GetStringLength(env, stringValue);
    (*env)->GetStringRegion(env, stringValue, 0, jsize__IN, jchar_buf__IN);

    // @read-primitive-array-critical vary=size

    ASSIGN_AND_CHECK(void_ptr__IN, (*env)->GetPrimitiveArrayCritical(env, jbyteArrayValue, &jboolean__IN));
    (*env)->ReleasePrimitiveArrayCritical(env, jbyteArrayValue, void_ptr__IN, 0);

    // @read-object-array-element vary=size

    jobject__IN = (*env)->GetObjectArrayElement(env, jobjectArrayValue, 0);

    // @write-object-array-element

    (*env)->SetObjectArrayElement(env, jobjectArrayValue, 0, jobject__OUT);

    // @access-direct-buffer

    ASSIGN_AND_CHECK_NNEG(jlong__IN, (*env)->GetDirectBufferCapacity(env, directBufferValue));
    ASSIGN_AND_CHECK(void_ptr__IN, (*env)->GetDirectBufferAddress(env, directBufferValue));

    // @get-object-class

    jclass__IN = (*env)->GetObjectClass(env, jobjectValue);

    // @find-class

    ASSIGN_AND_CHECK(jclass__IN, (*env)->FindClass(env, classNameValue));

    // todo: specify requires behaviour --- V
    // @get-field-id  requires=find-class vary=size

    ASSIGN_AND_CHECK(jfieldID__IN, (*env)->GetFieldID(env, jclassValue, field_name__OUT, field_signature__OUT));

    // @get-static-field-id vary=size

    ASSIGN_AND_CHECK(jfieldID__IN, (*env)->GetStaticFieldID(env, jclassValue, static_field_name__OUT, field_signature__OUT));

    // @get-method-id vary=size

    ASSIGN_AND_CHECK(jmethodID__IN, (*env)->GetMethodID(env, jclassValue, method_name__OUT, method_signature__OUT));

    // @get-static-method-id vary=size

    ASSIGN_AND_CHECK(jmethodID__IN, (*env)->GetStaticMethodID(env, jclassValue, static_method_name__OUT, method_signature__OUT));

    // @new-string-utf

    ASSIGN_AND_CHECK(jstring__IN, (*env)->NewStringUTF(env, byte_array__OUT));

    // @new-string vary=size

    ASSIGN_AND_CHECK(jstring__IN, (*env)->NewString(env, jchar_array__OUT, current_size));

    // @new-objectarray vary=size

    // todo initialelement 
    ASSIGN_AND_CHECK(jobjectArray__IN, (*env)->NewObjectArray(env, current_size, jelement_class__OUT, jinitial_element__OUT));

    // @new-direct-buffer vary=size

    ASSIGN_AND_CHECK(jobject__IN, (*env)->NewDirectByteBuffer(env, (void*)jchar_buf__OUT, current_size));
    
    // @global-ref

    ASSIGN_AND_CHECK(jobject__IN, (*env)->NewGlobalRef(env, jobjectValue));
    (*env)->DeleteGlobalRef(env, jobject__IN);

    // @local-ref

    ASSIGN_AND_CHECK(jobject__IN, (*env)->NewLocalRef(env, jobjectValue));
    (*env)->DeleteLocalRef(env, jobject__IN);

    // @weak-global-ref

    ASSIGN_AND_CHECK(jobject__IN, (*env)->NewWeakGlobalRef(env, jobjectValue));
    (*env)->DeleteWeakGlobalRef(env, jobject__IN);

    // @local-frame

    ASSIGN_AND_CHECK_NNEG(jint__IN, (*env)->PushLocalFrame(env, 16));
    jobject__IN = (*env)->PopLocalFrame(env, NULL);

}
