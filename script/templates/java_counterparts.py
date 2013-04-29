
from templating import put

t = """
package <% packagename %>;

<% imports %>
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkParameter;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRunner;
import android.util.Log;


public class JavaCounterparts {

    <% return_value_declarations %>

    public static void initParams(BenchmarkParameter benchmarkParameter) {
        <% return_value_inits %>
    }

    <% counterpart_methods %>

}

"""

# todo     <% return_values %>


counterpart_t = """

public static <% return_type %> <% methodname %> (<% parameters %>) {
    return <% return_expression %>;
}

"""

return_value_t = """

private static <% actualtype %> = benchmarkParameter.retrieve<% typename %>(<% typespecs %>);

"""
