
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

t_run_method = """
JNIEXPORT void JNICALL
Java_<% packagename %>_<% classname %>_run
(JNIEnv *env, jobject instance) {
    <% parameter_declarations %>;
    <% parameter_initialisations %>;
    <% prebody %>
    <% body %>
}

"""

t_caller_native = put(
    t_run_method,
    body = put(
        loop_code.t_c,
        benchmark_body = (
            '<% counterpart_method_name %> (<% counterpart_method_arguments %>);')),
    prebody = '')

t_caller_java = put(
    t_run_method,
    prebody = 'jmethodID mid = mids[<% seq_no %> - 1];',
    body = put(
        loop_code.t_c,
        benchmark_body = (
        '(*env)->CallStatic<% java_method_type %>Method<% call_variant %>(env, java_counterparts_class, mid<% arguments %>);')))


# todo: refactor common loop code elsewhere?
# todo: mid caching could be done beforehand? maybe not
