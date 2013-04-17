
from templating import put
import loop_code

# todo: templates confusingly named

t_run_method = """
JNIEXPORT <% return_type %> JNICALL
Java_<% packagename %>_<% classname %>_<% function %>
(JNIEnv *env, <% parameters %>) {
    <% parameter_declarations %>;
    <% parameter_initialisations %>;
    <% prebody %>
    <% body %>
}

"""

t = put(
    t_run_method,
    body = 'return <% return_expression %>;',
    remove = ['parameter_declarations',
              'parameter_initialisations',
              'prebody'])

t_caller_native = put(
    t_run_method,
    return_type = 'void',
    function = 'run',
    parameters = 'jobject instance',
    prebody = '',
    body = put(
        loop_code.t_c,
        benchmark_body = (
            '<% counterpart_method_name %>' +
            '(<% counterpart_method_arguments %>);')))

t_caller_java = put(
    t_run_method,
    return_type = 'void',
    function = 'run',
    parameters = 'jobject instance',
    prebody = 'jmethodID mid = mids[<% seq_no %> - 1];',
    body = put(
        loop_code.t_c_jni_call,
        benchmark_body = (
            '(*env)->CallStatic<% java_method_type %>Method<% call_variant %>' +
            '(env, java_counterparts_class, mid<% arguments %>);')))

