package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import fi.helsinki.cs.tituomin.nativebenchmark.ApplicationState;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import fi.helsinki.cs.tituomin.nativebenchmark.ShellEnvironment;

import java.util.List;
import java.util.LinkedList;
import java.util.Date;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import android.os.SystemClock;
import android.util.Log;
import java.io.File;
import android.os.Environment;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Random;
import java.util.ArrayList;


public abstract class CommandlineTool extends MeasuringTool {

    public CommandlineTool(int i, long reps) throws IOException, InterruptedException {
        super(i, reps);
    }


    protected abstract String command();
    protected String formatParameter(MeasuringOption option) {
        throw new UnsupportedOptionException();
    }

    protected String formatDefaultParameter(MeasuringOption option) {
        if (option.type() == BasicOption.COMMAND_STRING) {
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
        options.add(BasicOption.COMMAND_STRING);
        return options;
    }

    protected List<MeasuringOption> defaultOptions(List<MeasuringOption> options) {
        options.add(new BasicOption(BasicOption.COMMAND_STRING, command()));
        return options;
    }

    private void runAsCommand(String command)
        throws IOException, InterruptedException {

        Process process = null;
        InputStream err = null;
        try {
            process = Runtime.getRuntime().exec(command);
            err = process.getErrorStream();
            process.waitFor();
        
            if (process.exitValue() != 0) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(err));
                StringBuilder sb = new StringBuilder("Command failed.\n");
                while ((line = br.readLine()) != null) {
                    Log.e("tm", line);
                    sb.append(line);
                    sb.append("\n");
                }
                process.destroy();
                br.close();
                throw new IOException(sb.toString());
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (InterruptedException e) {
            throw e;
        }
        finally {
            if (err != null) err.close();
            if (process != null) process.destroy();
        }
    }

    protected void init() throws IOException, InterruptedException {
        if (!ShellEnvironment.execute(initScript())) {
            throw new IOException("Error executing as root.");
        }
    }

    protected abstract List<String> initScript();

    public boolean isLongRunning() {
        return true;
    }

    public long repetitions() {
        return Long.MAX_VALUE;
    }

    public void start(Runnable benchmark)
        throws InterruptedException, IOException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        initCommand();
        Thread benchmarkThread = new Thread(benchmark);
        Random r = new Random();
        int delay = r.nextInt(20);

        benchmarkThread.start();
        Thread.sleep(delay);

        try {
            runAsCommand(this.command);
        }
        catch (InterruptedException e) {
            throw e;
        }
        finally {
            benchmarkThread.interrupt();
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

    private static List<String> filenames = new ArrayList<String> ();
    private static final DateFormat dateFormat = DateFormat.getDateTimeInstance();

}
