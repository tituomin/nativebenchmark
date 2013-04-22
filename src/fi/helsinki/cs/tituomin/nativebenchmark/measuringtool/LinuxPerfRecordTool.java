package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import java.util.List;
import java.util.LinkedList;
import java.io.IOException;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool.UnsupportedOptionException;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.OptionSpec;
import fi.helsinki.cs.tituomin.nativebenchmark.Utils;

public class LinuxPerfRecordTool extends CommandlineTool {

    public LinuxPerfRecordTool(int i, long reps) throws IOException, InterruptedException {
        super(i, reps);
    }

    protected List<OptionSpec> specifyAllowedOptions(List<OptionSpec> options) {
        options = super.specifyAllowedOptions(options);
        options.add(BasicOption.OUTPUT_FILEPATH);
        options.add(BasicOption.MEASURE_LENGTH); // must be last
        return options;
    }

    protected String command() { 
        return "perf record -a -g";
    }

    protected List<String> initScript() {
        List<String> commands = new LinkedList<String>();
        commands.add("echo \"0\" > /proc/sys/kernel/kptr_restrict");
        commands.add("echo \"-1\" > /proc/sys/kernel/perf_event_paranoid");
        return commands;
    }

    private String generateFilename (String uuid) {
        return "perf-" + uuid + ".data";
    }

    public String formatParameter(MeasuringOption option) {
        if (option.type() == BasicOption.OUTPUT_FILEPATH) {
            String prefix = "--output=";
            String uuid = Utils.getUUID();
            String filename = generateFilename(uuid);
            setFilename(filename, option.value());
            setUUID(uuid);
            return prefix + option.value() + "/" + filename;
        }
        else if (option.type() == BasicOption.MEASURE_LENGTH) {
            return "sleep " + option.value();
        }
        else if (option.type() == BasicOption.COMMAND_STRING) {
            return option.value();
        }
        else {
            throw new UnsupportedOptionException();
        }
            
    }

}
