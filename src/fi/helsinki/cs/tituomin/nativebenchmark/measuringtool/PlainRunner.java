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

    public PlainRunner(int i, long reps) throws IOException, InterruptedException {
        super(i, reps);
    }

    protected List<MeasuringOption> defaultOptions(List<MeasuringOption> options) {
        return options;
    }

    protected List<OptionSpec> specifyAllowedOptions(List<OptionSpec> options) {
        options = super.specifyAllowedOptions(options);
        options.add(BasicOption.CPUFREQ);
        return options;
    }

    public boolean explicitGC() {
        return false;
    }

    public boolean ignore() {
        return true;
    }

    

    public List<String> initScript() {
        List<String> commands = new LinkedList<String>();
        String frequency = getOption(BasicOption.CPUFREQ);
        commands.add("echo \"userspace\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
        commands.add("echo \"" + frequency +  "\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_setspeed");
        return commands;
    }

    public long repetitions() {
        return 3000;
    }

    public void start(Runnable benchmark)
    throws InterruptedException, IOException {
        benchmark.run();
    }
}
