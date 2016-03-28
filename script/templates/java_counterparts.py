
from templating import put

t = """
package <% packagename %>;

<% imports %>
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkParameter;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRunner;
import android.util.Log;


public enum JavaCounterparts {
    INSTANCE;

    <% return_value_declarations %>
    public int persistentValue;
    public static int staticpersistentValue = 0;

    private JavaCounterparts() {
        persistentValue = 0;
    }

    public static void initParams(BenchmarkParameter benchmarkParameter) {
        <% return_value_inits %>
    }

    <% counterpart_methods %>

}

"""

# todo     <% return_values %>


counterpart_t = """

<% privacy %> <% static %> <% return_type %> <% methodname %> (<% parameters %>) {
    <% static %>persistentValue = (<% static %>persistentValue + 1) % 10;
    return <% return_expression %>;
}

"""

return_value_t = """

private static <% actualtype %> = benchmarkParameter.retrieve<% typename %>(<% typespecs %>);

"""
