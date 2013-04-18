
#ifndef CUSTOM_BENCHMARKS_H_
#define CUSTOM_BENCHMARKS_H_

#define ASSIGN_AND_CHECK(lhs, rhs)      \
    if ((lhs = rhs) == NULL) return;

#define ASSIGN_AND_CHECK_NNEG(lhs, rhs) \
    if ((lhs = rhs) < 0) return;

#define GET_STATIC_TYPE_FIELD(_ctype, _jname)                                           \
    _ctype##__IN = (*env)->GetStatic##_jname##Field(env, mock_class, mock_ ## _ctype ## _static_field_id);                              

#define SET_STATIC_TYPE_FIELD(_ctype, _jname)                                           \
    (*env)->SetStatic##_jname##Field(env, mock_class, mock_ ## _ctype ## _static_field_id, _ctype##__OUT);                              

#define GET_TYPE_FIELD(_ctype, _jname)                                                  \
    _ctype##__IN = (*env)->Get##_jname##Field(env, mock_object, mock_ ## _ctype ## _field_id);                                   

#define SET_TYPE_FIELD(_ctype, _jname)                                                  \
    (*env)->Set##_jname##Field(env, mock_object, mock_ ## _ctype ## _field_id, _ctype##__OUT); \

#define NEW_PRIMITIVE_ARRAY(_c_el_type, _jname)                                                            \
    ASSIGN_AND_CHECK(_c_el_type ## Array ## __IN, (*env)->New##_jname##Array(env, current_size));

#define GET_PRIMITIVE_ARRAY_ELEMENTS(_ctype, _jname)                                                       \
    ASSIGN_AND_CHECK(_ctype ##_ptr__IN, (*env)->Get##_jname##ArrayElements(env, \
        _ctype##ArrayValue, &jboolean__IN));

#define RELEASE_PRIMITIVE_ARRAY_ELEMENTS(_ctype, _jname)                                                   \
    (*env)->Release##_jname##ArrayElements(env, _ctype##ArrayValue, _ctype ## _ptr__IN, 0) \

#define GET_PRIMITIVE_ARRAY_REGION(_ctype, _jname)                                                         \
    (*env)->Get##_jname##ArrayRegion(env,                              \
        _ctype##ArrayValue, 0, current_size, _ctype ## _buf__IN)

#define SET_PRIMITIVE_ARRAY_REGION(_ctype, _jname)                                                         \
    (*env)->Set##_jname##ArrayRegion(env,                              \
        _ctype##ArrayValue, 0, current_size, _ctype ## _buf__IN)

// ----- ----- ----- ---- method calls ... ... 

#define CALL_VIRTUAL(_lhs, _jname, _parameters, _check_exceptions)                                         \
    _lhs ## (*env)->Call##_jname##Method(env, jobjectValue, jmethodIDValue ## _parameters); \
    if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}
    // todo variations ?

#define CALL_NONVIRTUAL(_lhs, _jname, _parameters, _check_exceptions)                                      \
    _lhs ## (*env)->CallNonvirtual##_jname##Method(env, jobjectValue, jclassValue, jmethodIDValue ## _parameters); \
    if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}
    // todo void params

#define CALL_STATIC(_lhs, _jname, _parameters, _check_exceptions)                                          \
    _lhs ## (*env)->CallStatic##_jname##Method(env, jclassValue, jmethodIDValue ## _parameters); \
    if (_check_exceptions) { if (*env)->ExceptionCheck(env)) return;}

/* PRIMITIVE_ARRAY_FUNCTIONS(_ctype, _jname) */

#endif
