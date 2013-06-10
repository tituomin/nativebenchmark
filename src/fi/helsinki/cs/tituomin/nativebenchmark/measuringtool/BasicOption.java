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
        return new Pair<String,String> (this.type.id(), this.value);
    }

    public String toString() {
        return this.type() + " " + this.value();
    }
    
    // ----

    private OptionSpec type;
    private String value;
    
    // ----

}
