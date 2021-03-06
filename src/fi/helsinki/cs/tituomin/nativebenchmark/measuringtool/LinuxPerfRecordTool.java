package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import java.util.List;
import java.util.LinkedList;
import java.io.IOException;
import java.io.File;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool.UnsupportedOptionException;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.OptionSpec;
import fi.helsinki.cs.tituomin.nativebenchmark.Utils;

public class LinuxPerfRecordTool extends CommandlineTool {

    // public LinuxPerfRecordTool(int i, long reps, long allocreps) throws IOException, InterruptedException {
    //     super(i, reps, allocreps);
    // }

    public LinuxPerfRecordTool
        (
            int rounds,
            long repetitions,
            long allocreps,
            boolean warmup,
            boolean runAllBenchmarks) throws
                IOException, InterruptedException
    {
        super(rounds, repetitions, allocreps, warmup, runAllBenchmarks);
    }

    protected List<OptionSpec> specifyAllowedOptions(List<OptionSpec> options) {
        options = super.specifyAllowedOptions(options);
        options.add(OptionSpec.OUTPUT_FILEPATH);
        options.add(OptionSpec.MEASURE_LENGTH); // must be last
        return options;
    }

    protected List<String> initScript() {
        List<String> commands = new LinkedList<String>();
        commands.add("echo \"0\" > /proc/sys/kernel/kptr_restrict");
        commands.add("echo \"-1\" > /proc/sys/kernel/perf_event_paranoid");
        return commands;
    }

    protected List<MeasuringOption> defaultOptions(List<MeasuringOption> options) {
        options = super.defaultOptions(options);
        String uuid = Utils.getUUID();
        String filename = generateFilename(uuid);
        String basePath = getPerfDir().getPath() + "/";
        setFilename(filename, basePath);
        setUUID(uuid);
        options.add(new BasicOption(OptionSpec.OUTPUT_FILEPATH, basePath + filename));
        return options;
    }

    protected void init() throws IOException, InterruptedException {
        super.init();
        getPerfDir().mkdir();
    }

    private File getPerfDir() {
        return new File(getDataDir(), "perf");
    }

    protected String command() { 
        return "perf record -a -g";
    }

    private String generateFilename (String uuid) {
        return "perf-" + uuid + ".data";
    }

    public String formatParameter(MeasuringOption option) {
        if (option.type() == OptionSpec.OUTPUT_FILEPATH) {
            return "--output=" + option.value();
        }
        else if (option.type() == OptionSpec.MEASURE_LENGTH) {
            return "sleep " + option.value();
        }
        return super.formatParameter(option);
    }
}
