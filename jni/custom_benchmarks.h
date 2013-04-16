
#ifndef CUSTOM_BENCHMARKS_H_
#define CUSTOM_BENCHMARKS_H_

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

#endif
