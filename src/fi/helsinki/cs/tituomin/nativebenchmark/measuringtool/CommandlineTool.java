package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import fi.helsinki.cs.tituomin.nativebenchmark.ApplicationState;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;
import fi.helsinki.cs.tituomin.nativebenchmark.ShellEnvironment;

import java.util.List;
import java.util.LinkedList;
import java.util.Date;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import android.os.SystemClock;
import android.util.Log;
import java.io.File;
import android.os.Environment;
import java.util.Random;
import java.util.ArrayList;


public abstract class CommandlineTool extends MeasuringTool {

    // public CommandlineTool(int i, long reps, long allocreps) throws IOException, InterruptedException {
    //     super(i, reps, allocreps);
    // }

    private static final long REPETITIONS = Long.MAX_VALUE;

    public CommandlineTool(int rounds, long repetitions, long allocreps, boolean warmup, boolean x) throws IOException, InterruptedException {
        super(rounds, REPETITIONS, allocreps, false, x);
        this.filenames = new ArrayList<String> ();
    }

    protected abstract String command();
    protected String formatParameter(MeasuringOption option) {
        throw new UnsupportedOptionException();
    }

    protected String formatDefaultParameter(MeasuringOption option) {
        if (option.type() == OptionSpec.COMMAND_STRING) {
            return option.value();
        }
        else {
            return formatParameter(option);
        }
    }

    public void initCommand() {
        List<String> commandline = new LinkedList<String> ();
        commandline.addAll(generateCommandlineArguments());
        this.command = "";
        for (String s : commandline) {
            this.command += s + " ";
        }
    }

    protected List<OptionSpec> specifyAllowedOptions(List<OptionSpec> options) {
        options = super.specifyAllowedOptions(options);
        options.add(OptionSpec.COMMAND_STRING);
        return options;
    }

    protected List<MeasuringOption> defaultOptions(List<MeasuringOption> options) {
        options.add(new BasicOption(OptionSpec.COMMAND_STRING, command()));
        return options;
    }

    protected void init() throws IOException, InterruptedException {
        super.init();
        if (!ShellEnvironment.runAsRoot(initScript())) {
            throw new IOException("Error executing as root.");
        }
    }

    protected abstract List<String> initScript();

    public boolean isLongRunning() {
        return true;
    }

    public void start(Runnable benchmark)
        throws InterruptedException, IOException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        initCommand();
        BenchmarkRegistry.resetInterruptFlag();
        Thread benchmarkThread = new Thread(benchmark);
        Random r = new Random();
        int delay = r.nextInt(20);

        benchmarkThread.start();
        Thread.sleep(delay);

        try {
            ShellEnvironment.run(this.command);
        }
        catch (InterruptedException e) {
            throw e;
        }
        finally {
            benchmarkThread.interrupt();
            BenchmarkRegistry.interruptNative();
            benchmarkThread.join();
        }
    }

    public void setFilename(String name, String path) {
        filenames.add(path + "/" + name);
        putMeasurement("Filename", name);
    }

    public List<String> getFilenames() {
        return filenames;
    }
        

    public void setUUID(String uuid) {
        putMeasurement("UUID", uuid);
    }

    // -----

    public List<String> generateCommandlineArguments() {
        List<String> result = new LinkedList<String> ();
        for (OptionSpec type : allowedOptions) {
            MeasuringOption option = options.get(type);

            if (option == null) {
                Log.v("mt", type + " is null");
            }
            else {
                result.add(formatDefaultParameter(option));
            }
        }
        return result;
    }

    // -----

    private Date startDate;
    private Date endDate;
    private long startTime;
    private String command;

    private List<String> filenames;
    private static final DateFormat dateFormat = DateFormat.getDateTimeInstance();

}
