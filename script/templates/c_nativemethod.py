
from templating import put
import loop_code

#/*    __android_log_write(ANDROID_LOG_VERBOSE, "nativemethod", "<% classname %> being called "); */

t = """
JNIEXPORT <% return_type %> JNICALL
Java_<% packagename %>_<% classname %>_<% function %>
(JNIEnv *env, <% parameters %>) {
    return <% return_expression %>;
}

"""

t_caller_native = put(
"""

JNIEXPORT void JNICALL
Java_<% packagename %>_<% classname %>_run
(JNIEnv *env, jobject instance) {
    <% parameter_declarations %>;
    <% parameter_initialisations %>;
    <% loop %>

}

""", loop = put(loop_code.t_c,
        benchmark_body = '<% counterpart_method_name %> (<% counterpart_method_arguments %>);'))


t_caller_java = put(
"""

JNIEXPORT void JNICALL
Java_<% packagename %>_<% classname %>_run
(JNIEnv *env, jobject instance) {
    jmethodID mid = mids[<% seq_no %> - 1];

    <% parameter_declarations %>;
    <% parameter_initialisations %>;
    <% loop %>

}

""", loop = put(loop_code.t_c_jni_call,
        benchmark_body = '(*env)->CallStatic<% java_method_type %>Method<% call_variant %>(env, java_counterparts_class, mid<% arguments %>);'))


# todo: refactor common loop code elsewhere?
# todo: mid caching could be done beforehand? maybe not
