package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import fi.helsinki.cs.tituomin.nativebenchmark.ApplicationState;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;

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

    public CommandlineTool(int i) throws IOException, InterruptedException {
        super(i);
    }


    protected abstract String command();
    protected abstract String formatParameter(MeasuringOption option);

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

    // thanks http://muzikant-android.blogspot.fi/2011/02/how-to-get-root-access-and-execute.html
    private boolean execute(List<String> commands) throws IOException
    {
        boolean retval = false;
        DataOutputStream os = null;
        Process suProcess = null;
        try
            {
                if (null != commands && commands.size() > 0)
                    {
                        suProcess = Runtime.getRuntime().exec("su");

                        os = new DataOutputStream(suProcess.getOutputStream());

                        // Execute commands that require root access
                        for (String currCommand : commands)
                            {
                                os.writeBytes(currCommand + "\n");
                                os.flush();
                            }

                        os.writeBytes("exit\n");
                        os.flush();

                        try
                            {
                                int suProcessRetval = suProcess.waitFor();
                                if (255 != suProcessRetval)
                                    {
                                        // Root access granted
                                        retval = true;
                                    }
                                else
                                    {
                                        // Root access denied
                                        retval = false;
                                    }
                            }
                        catch (Exception ex)
                            {
                                Log.e("ROOT", "Error executing root action", ex);
                            }
                    }
            }
        catch (IOException ex)
            {
                Log.w("ROOT", "Can't get root access", ex);
            }
        catch (SecurityException ex)
            {
                Log.w("ROOT", "Can't get root access", ex);
            }
        catch (Exception ex)
            {
                Log.w("ROOT", "Error executing internal operation", ex);
            }
        finally {
            if (suProcess != null) {
                suProcess.destroy();
            }
        }
        if (os != null) {
            os.close();
        }

        return retval;
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
        if (!execute(initScript())) {
            throw new IOException("Error executing as root.");
        }
    }

    protected abstract List<String> initScript();

    public void start(Benchmark benchmark)
        throws InterruptedException, IOException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        initCommand();
        Thread benchmarkThread = new Thread(benchmark);
        Random r = new Random();
        int delay = r.nextInt(200);

        benchmark.setRepetitions(Long.MAX_VALUE);

        benchmarkThread.start();
        //Thread.sleep(delay);

        this.startDate = new Date();

        try {
            runAsCommand(this.command);
        }
        catch (InterruptedException e) {
            throw e;
        }
        finally {
            benchmarkThread.interrupt();
            benchmarkThread.join();
            benchmark.restoreRepetitions();
        }
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        putMeasurement("Started", dateFormat.format(this.startDate));
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
                result.add(formatParameter(option));
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
