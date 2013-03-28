

#/*    __android_log_write(ANDROID_LOG_VERBOSE, "nativemethod", "{classname} being called "); */

t = """
JNIEXPORT {return_type} JNICALL
Java_{packagename}_{classname}_{function}
(JNIEnv *env, {parameters}) {{
    return {return_expression};
}}
"""

t_caller_native = """

JNIEXPORT void JNICALL
Java_{packagename}_{classname}_run
(JNIEnv *env, jobject instance) {{
    {parameter_declarations};
    {parameter_initialisations};

    jlong division  = repetitions / CHECK_INTERRUPTED_INTERVAL;
    jlong remainder = repetitions % CHECK_INTERRUPTED_INTERVAL;
    jlong i, j;

    for (i = 0; i < division; i++) {{
        for (j = 0; j < CHECK_INTERRUPTED_INTERVAL; j++) {{
            {counterpart_method_name} ({counterpart_method_arguments});
        }}
        if (check_interrupted(env)) {{
            return;
        }}
    }}
    for (j = 0; j < remainder; j++) {{
        {counterpart_method_name} ({counterpart_method_arguments});
    }}
}}

"""

t_caller_java = """

JNIEXPORT void JNICALL
Java_{packagename}_{classname}_run
(JNIEnv *env, jobject instance) {{
    jmethodID mid = mids[{seq_no} - 1];

    {parameter_declarations};
    {parameter_initialisations};

    jlong refs = 0;

    jlong division  = repetitions / CHECK_INTERRUPTED_INTERVAL;
    jlong remainder = repetitions % CHECK_INTERRUPTED_INTERVAL;
    jlong i, j;

    for (i = 0; i < division; i++) {{
        for (j = 0; j < CHECK_INTERRUPTED_INTERVAL; j++) {{
            if (refs == 0) {{
                refs = LOCAL_FRAME_SIZE;
                if ((*env)->PushLocalFrame(env, LOCAL_FRAME_SIZE) < 0) {{
                    __android_log_write(ANDROID_LOG_ERROR,                      
                        "nativebenchmark", "can't ensure capacity");
                    return;
                }}
            }}

            (*env)->CallStatic{java_method_type}Method{call_variant}(env, java_counterparts_class, mid{arguments});

            if (--refs == 0) {{
                (*env)->PopLocalFrame(env, NULL);
            }}
        }}
        if (check_interrupted(env)) {{
            return;
        }}
    }}

    refs = 0;
    for (j = 0; j < remainder; j++) {{
        if (refs == 0) {{
            refs = LOCAL_FRAME_SIZE;
            if ((*env)->PushLocalFrame(env, LOCAL_FRAME_SIZE) < 0) {{
                __android_log_write(ANDROID_LOG_ERROR,
                    "nativebenchmark", "can't ensure capacity");
                return;
            }}
        }}
         (*env)->CallStatic{java_method_type}Method{call_variant}(env, java_counterparts_class, mid{arguments});
         if (--refs == 0) {{
            (*env)->PopLocalFrame(env, NULL);
        }}
    }}

}}

"""

# todo: refactor common loop code elsewhere?
# todo: mid caching could be done beforehand? maybe not
