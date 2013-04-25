
from templating import put
import loop_code
import logging

t = """
package <% packagename %>;

import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkParameter;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
<% imports %>
import android.util.Log;

public class <% classname %> <% class_relations %> extends Benchmark {

    public <% classname %> (BenchmarkParameter bp) {
        init(bp);
    }

    public String from() {
        return "<% from_language %>";
    }

    public String to() {
        return "<% to_language %>";
    }

    public int sequenceNo() {
        return <% seq_no %>;
    }

    public boolean dynamicParameters() {
        return <% has_dynamic_parameters %>;
    }

    public String description() {
        return "<% description %>";
    }

    public long maxRepetitions() {
        return <% max_repetitions %>;
    }

    <% native_method %>

    <% run_method %>

}

"""

native_method_t     = '<% modifiers %> native <% return_type %> <% name %> (<% parameters %>);'
dynamic_parameter_t = 'new BasicOption(BasicOption.VARIABLE, "<% variable %>")'.strip()
native_run_method_t = 'public native void run();'

loop = put(loop_code.t_java,
    benchmark_body = '<% counterpart_method_name %> (<% counterpart_method_arguments %>);')

java_run_method_t   = put("""

    public void run() {
        <% parameter_declarations %>;
        <% parameter_initialisations %>;

        <% loop %>
    }

""", loop = loop)

java_run_method_inline_t = """

    public void run() {
        <% init %>
        <% loop %>
    }

"""



def native_method(modifiers=None, return_type=None, name=None, parameters=None):
    return put(native_method_t,
        modifiers=modifiers, return_type=return_type,
        name=name, parameters=parameters)

def native_run_method():
    return native_run_method_t

def java_run_method(parameter_declarations=None, parameter_initialisations=None,
                    counterpart_method_name=None, counterpart_method_arguments=None):

    return put(java_run_method_t,
        parameter_declarations        = parameter_declarations,
        parameter_initialisations     = parameter_initialisations,
        counterpart_method_name       = counterpart_method_name,
        counterpart_method_arguments  = counterpart_method_arguments)
        





    
