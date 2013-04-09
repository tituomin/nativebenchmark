package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import  fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.LinuxPerfRecordTool;
import  fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.OptionSpec;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import android.util.Pair;
import java.util.NoSuchElementException;

public class BasicOption implements MeasuringOption {

    public BasicOption(OptionSpec type) {
        this(type, null);
    }

    public BasicOption(OptionSpec type, String value) {
        this.type = type;
        this.value = value;
    }

    // -----

    public OptionSpec id() {
        return this.type;
    }
    public void set(String value) {
        this.value = value;
    }
    public OptionSpec type() {
        return this.type;
    }
    public String value() {
        return value;
    }

    public Pair<String,String> toStringPair() {
        return new Pair<String,String> (this.type.name, this.value);
    }

    public String toString() {
        return this.type() + " " + this.value();
    }
    
    // ----

    private OptionSpec type;
    private String value;
    
    // ----

    public static final OptionSpec COMMAND_STRING  = new OptionSpec();
    public static final OptionSpec OUTPUT_FILEPATH = new OptionSpec();
    public static final OptionSpec MEASURE_LENGTH  = new OptionSpec();
    public static final OptionSpec VARIABLE        = new OptionSpec();
    public static final OptionSpec CPUFREQ         = new OptionSpec();

    static {
        COMMAND_STRING
            .name     ("Command run")
            .required (true)
            .id       ("COMMAND_STRING");

        OUTPUT_FILEPATH 
            .name     ("Output path")
            .id       ("OUTPUT_FILEPATH")
            .required (true);

        MEASURE_LENGTH
            .name    ("Measuring time (sec)")
            .id      ("MEASURE_LENGTH")
            .required (true);

        VARIABLE
            .name    ("Variable parameter in benchmark")
            .id      ("VARIABLE")
            .required (false);

        CPUFREQ
            .name    ("Fixed CPU frequency")
            .id      ("CPUFREQ")
            .required (true);

    }

}
