
from templating import put, partial

t = """
    <% declare_counters %>
    <% additional_declaration %>

    <% init_counters %>
    division  = repetitions / interval + 1;
    remainder = repetitions % interval + 1;

    <% debug %>
    <% additional_init %>
    while (--division != 0) { 
        <% init_counters %>
        interval = interval + 1;
        while (--interval != 0) { 
            <% pre_body %>
            <% extra_debug %>
            <% benchmark_body %>
            <% post_body %>
        }
        if (<% test_interrupted %>) {
            <% debug_interrupted %>
            return;
        }
    }

    <% additional_init %>

    while (--remainder != 0) { 
        <% pre_body %>
        <% benchmark_body %>
        <% post_body %>
    }

    <% removal_prevention %>
    <% finished %>

"""

jni_push_frame = """
if (refs == 0) {
    refs = LOCAL_FRAME_SIZE;
    if ((*env)->PushLocalFrame(env, LOCAL_FRAME_SIZE) < 0) {
        return;
    }
}
"""

jni_pop_frame  = """
if (--refs == 0) {
    (*env)->PopLocalFrame(env, NULL);
}

"""


t_c_base = partial(
    t,
    declare_counters = 'jlong interval, division, remainder;',
    init_counters    = 'interval = CHECK_INTERRUPTED_INTERVAL;'
    ,#;\n__android_log_print(ANDROID_LOG_DEBUG, "nativebenchmark", "interval is %lld", interval);',
    test_interrupted = 'interrupted')

t_c_jni_call = partial(
    t_c_base,
    additional_declaration = 'jlong refs;',
    additional_init        = 'refs = 0;',
    remove = ['extra_debug', 'debug', 'debug_interrupted', 'removal_prevention'],
    pre_body               = jni_push_frame,
    post_body              = jni_pop_frame)

t_c = partial(
    t_c_base,
    remove = ['extra_debug', 'debug', 'debug_interrupted',
              'additional_declaration', 'additional_init',
              'pre_body', 'post_body', 'removal_prevention'])

t_java = partial(
    t,
    test_interrupted = 'Thread.currentThread().isInterrupted()',
    extra_debug = '',#,'Log.v("Benchmark", division + " " + interval);',
    declare_counters = 'long interval, division, remainder;',
    init_counters = 'interval = BenchmarkRegistry.CHECK_INTERRUPTED_INTERVAL;',
    removal_prevention = 'repetitionsLeft = division * interval + remainder;',
    remove = ['additional_declaration', 'additional_init', 'pre_body', 'post_body'])
