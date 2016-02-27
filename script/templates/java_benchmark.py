
from templating import partial
import loop_code
import logging

t = """
package <% packagename %>;

import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkParameter;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
import fi.helsinki.cs.tituomin.nativebenchmark.MockObject;
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

    public String id() {
        return "<% _id %>";
    }

    public boolean representative() {
        return <% representative %>;
    }

    public boolean dynamicParameters() {
        return <% has_dynamic_parameters %>;
    }

    public String description() {
        return "<% description %>";
    }

    public boolean isAllocating() {
        return <% is_allocating %>;
    }

    public boolean isNonvirtual() {
        return <% is_nonvirtual %>;
    }

    <% class_fields %>

    <% native_method %>

    <% run_method %>

}

"""

native_method_t     = '<% modifiers %> native <% return_type %> <% name %> (<% parameters %>);'
dynamic_parameter_t = 'new BasicOption(BasicOption.VARIABLE, "<% variable %>")'.strip()
native_run_method_t = 'public native void runInternal();'

loop = partial(loop_code.t_java,
           benchmark_body = '<% counterpart_method_name %> (<% counterpart_method_arguments %>);')

java_run_method_t   = partial("""

    public void runInternal() {
        JavaCounterparts counterpartInstance = JavaCounterparts.INSTANCE;
        <% parameter_declarations %>;
        <% parameter_initialisations %>;

        <% loop %>
    }

""", loop = loop)

java_run_method_inline_t = """

    public void runInternal() {
        <% init %>
        <% type_init %>
        <% loop %>
    }

"""
