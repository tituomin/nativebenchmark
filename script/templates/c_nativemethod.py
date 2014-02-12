
from templating import partial, put
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

t = partial(
    t_run_method,
    body = 'return <% return_expression %>;',
    remove = ['parameter_declarations',
              'parameter_initialisations',
              'prebody'])

# C to C
t_caller_native = partial(
    t_run_method,
    return_type = 'void',
    function = 'runInternal',
    parameters = 'jobject instance',
    prebody = '',
    body = partial(
        loop_code.t_c_jni_call,
        benchmark_body = (
            '<% counterpart_method_name %>' +
            '(<% counterpart_method_arguments %>);')))

# C to J
t_caller_java = partial(
    t_run_method,
    return_type = 'void',
    function = 'runInternal',
    parameters = 'jobject instance',
    prebody = 'jmethodID mid = mids[<% seq_no %> - 1];')

def call_java_from_c(static = True, nonvirtual = False, **parameters):
    benchmark_body = ''
    if static:
        benchmark_body = (
            '(*env)->CallStatic<% java_method_type %>Method<% call_variant %>' +
            '(env, java_counterparts_class, mid<% arguments %>);')
    elif nonvirtual:
        benchmark_body = (
            '(*env)->CallNonvirtual<% java_method_type %>Method<% call_variant %>' +
            '(env, java_counterparts_object, java_counterparts_class, mid<% arguments %>);')
    else:
        benchmark_body = (
            '(*env)->Call<% java_method_type %>Method<% call_variant %>' +
            '(env, java_counterparts_object, mid<% arguments %>);')

    parameters['body'] = put(
        loop_code.t_c_jni_call,
        benchmark_body = put(benchmark_body, **parameters))

    return partial(t_caller_java, **parameters)
