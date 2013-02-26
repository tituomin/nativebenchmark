package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.Measurement;

import java.util.List;
import java.util.LinkedList;
import java.util.Date;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.text.DateFormat;
import android.os.SystemClock;
import android.util.Log;
import java.io.File;
import android.os.Environment;

public abstract class CommandlineTool extends MeasuringTool {

    private static final DateFormat dateFormat = DateFormat.getDateTimeInstance();

    protected abstract String command();
    protected abstract String formatParameter(MeasuringOption option);


    // ----

    public CommandlineTool() {
        super();
    }

    public void initCommand() {
        List<String> commandline = new LinkedList<String> ();

        commandline.addAll(generateCommandlineArguments());

        this.builder = new ProcessBuilder()
            .directory(Environment.getExternalStorageDirectory())
            .command(commandline)
            .redirectErrorStream(true);
    }

    protected List<OptionSpec> specifyAllowedOptions(List<OptionSpec> options) {
        options = super.specifyAllowedOptions(options);
        options.add(BasicOption.COMMAND_STRING);
        return options;
    }

    protected List<MeasuringOption> defaultOptions(List<MeasuringOption> options) {
        Log.v("mt", "foo" + command());
        options.add(new BasicOption(BasicOption.COMMAND_STRING, command()));
        Log.v("mt", options.toString());
        return options;
    }

    public Measurement start(Runnable benchmark)
        throws InterruptedException, IOException {
        initCommand();

        Measurement measurement = getMeasurement();

        Thread benchmarkThread = new Thread(benchmark);
        benchmarkThread.start();
        Thread.sleep(2000); // todo hardcoded

        this.startDate = new Date();
        this.process = this.builder().start();
        // InputStream in = process.getInputStream();
        // OutputStream out = process.getOutputStream();
        // InputStream err = process.getErrorStream();
        //        Log.v("mt", 
        this.process.waitFor();

        benchmarkThread.interrupt();

        int exitValue = this.process.exitValue();
        if (exitValue == 0) {
            measurement.addData("Started", dateFormat.format(this.startDate));
        }
        return measurement;
        // }
        // catch (Exception e) {
        //     System.err.println("foo");
        // }
        // finally {
        //     process.destroy();
        // }
    }

    public Measurement stop() {
        throw new UnsupportedOperationException();
    }

    protected abstract Measurement getMeasurement();

    // -----

    public List<String> generateCommandlineArguments() {
        List<String> result = new LinkedList<String> ();
        for (OptionSpec type : allowedOptions) {
            MeasuringOption option = options.get(type);

            if (option == null) {
                Log.v("mt", type + " is null");
            }
            else {
                result.add(formatParameter(option));
            }
        }
        return result;
    }

    protected ProcessBuilder builder() {
        return builder;
    }

    // -----
    private ProcessBuilder builder;
    private Process process;

    private Date startDate;
    private Date endDate;
    private long startTime;
}
