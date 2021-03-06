package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import java.util.List;
import java.util.LinkedList;
import java.io.IOException;
import android.os.SystemClock;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.CommandlineTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringOption;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.OptionSpec;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import android.util.Log;

public class PlainRunner extends MeasuringTool {

    public PlainRunner(int i, long reps, long allocreps) throws IOException, InterruptedException {
        super(i, reps, allocreps, true, false);
    }

    protected List<MeasuringOption> defaultOptions(List<MeasuringOption> options) {
        return options;
    }

    protected List<OptionSpec> specifyAllowedOptions(List<OptionSpec> options) {
        options = super.specifyAllowedOptions(options);
        options.add(OptionSpec.CPUFREQ);
        return options;
    }

    public boolean explicitGC() {
        return false;
    }

    public boolean ignore() {
        return true;
    }

    public long repetitions() {
        return 10000;
    }

    public void start(Runnable benchmark)
    throws InterruptedException, IOException {
        benchmark.run();
    }
}
