package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.OptionSpec;

import java.util.Iterator;
import android.util.Pair;

public interface MeasuringOption {

    public void set(String value);
    public OptionSpec id();
    public String value();
    public OptionSpec type();
    public Pair<String,String> toStringPair();
}
