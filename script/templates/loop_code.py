
from templating import put

t = """
    <% declare_counters %>
    <% additional_declaration %>

    <% init_counters %>
    division  = repetitions / interval + 1;
    remainder = repetitions % interval + 1;
    interval = interval + 1;

    <% additional_init %>
    while (--division != 0) { 
        while (--interval != 0) { 
            <% pre_body %>
            <% benchmark_body %>
            <% post_body %>
        }
        if (<% test_interrupted %>) {
            return;
        }
    }

    <% additional_init %>

    while (--remainder != 0) { 
        <% pre_body %>
        <% benchmark_body %>
        <% post_body %>
    }

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


t_c_base = put(
    t,
    declare_counters = 'jlong interval, division, remainder;',
    init_counters    = 'interval = CHECK_INTERRUPTED_INTERVAL;',#;\n__android_log_print(ANDROID_LOG_DEBUG, "nativebenchmark", "interval is %lld", interval);',
    test_interrupted = 'check_interrupted(env)')

t_c_jni_call = put(
    t_c_base,
    additional_declaration = 'jlong refs;',
    additional_init        = 'refs = 0;',
    pre_body               = jni_push_frame,
    post_body              = jni_pop_frame)

t_c = put(
    t_c_base,
    remove = ['additional_declaration', 'additional_init', 'pre_body', 'post_body'])

t_java = put(
    t,
    test_interrupted = 'Thread.currentThread().isInterrupted()',
    declare_counters = ('long interval = BenchmarkRegistry.CHECK_INTERRUPTED_INTERVAL;' +
                        'long division, remainder;'),
    remove = ['init_counters', 'additional_declaration', 'additional_init', 'pre_body', 'post_body'])
