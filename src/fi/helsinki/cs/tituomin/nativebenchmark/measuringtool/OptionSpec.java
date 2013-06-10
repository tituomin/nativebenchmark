package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import java.util.Map;
import java.util.HashMap;

public enum OptionSpec {

    COMMAND_STRING ("Command run", "command_string", true),
    OUTPUT_FILEPATH ("Output path", "output_filepath", true),
    MEASURE_LENGTH ("Measuring time (sec)", "measure_length", true),
    VARIABLE ("Variable parameter in benchmark", "variable", false),
    CPUFREQ ("Fixed CPU frequency", "cpu_freq", true);

    OptionSpec(String name, String id, boolean required) {
        this.name = name;
        this.id = id;
        this.required = required;
        put(id, this);
    }

    public String id() {
        return id;
    }
    public boolean required() {
        return required;
    }

    public static OptionSpec byId(String id) {
        return specs.get(id);
    }

    private static void put(String id, OptionSpec value) {
        getSpecs().put(id, value);
    }

    public static Map<String,OptionSpec> getSpecs() {
        if (specs == null) {
            specs = new HashMap<String,OptionSpec>();
        }
        return specs;
    }

    private String name;
    private String id;
    private boolean required;

    private static Map<String,OptionSpec> specs;

}
