package fi.helsinki.cs.tituomin.nativebenchmark;



import android.util.Log;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkController;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

public class SocketCommunicator implements ApplicationStateListener
{
    /**
     * Thread to initialize Socket connection
     */
    private final Runnable InitializeConnection = new Thread()
    {
        @Override
        public void run()
        {
            // initialize server socket
            while (!Thread.currentThread().isInterrupted()) {
                try
                    {
                        server = new ServerSocket();
                        //server.setSoTimeout(TIMEOUT * 1000);
                        server.setReuseAddress(true);
                        server.bind(new InetSocketAddress(38300));

                        //attempt to accept a connection
                        client = server.accept();

                        SocketCommunicator.this.out = client.getOutputStream();
                        SocketCommunicator.this.printWriter = new PrintWriter(out, true);
                        SocketCommunicator.nis = client.getInputStream();
                        try
                            {
                                SocketCommunicator.this.output(helpMessage);
                                Log.v(TAG, "client >" + helpMessage);

                                byte[] bytes = new byte[1024];
                                int numRead = 0;
                                while (numRead >= 0) {
                                        SocketCommunicator.this.output("[Awaiting input]");
                                        numRead = SocketCommunicator.nis.read(bytes, 0, 1024);
                                        if (numRead < 1) {
                                            executeCommand("quit");
                                            return;
                                        }
                                        receivedCommand = new String(bytes, 0, numRead).trim();
                                        executeCommand(receivedCommand);
                                        Log.v(TAG, receivedCommand);
                                    }
                            }
                        catch (IOException ioException)
                            {
                                Log.e(SocketCommunicator.TAG, "" + ioException);
                            }
                    }
                catch (SocketTimeoutException e)
                    {
                        receivedCommand = "Connection has timed out! Please try again";
                        Log.v(TAG, receivedCommand);
                    }
                catch (IOException e)
                    {
                        Log.e(SocketCommunicator.TAG, "" + e);
                    }

                if (client != null)
                    {
                        receivedCommand = "Connection was succesful!";
                        try {
                            server.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error closing server connection.");
                        } finally {
                            server = null;
                        }
                        Log.v(TAG, receivedCommand);
                    }
            }
        }
    };

    private class StateChanger implements Runnable {
        public void run() {
        }
    }

    private void executeStart(String command) {
        String[] split = command.split(":");
        if (split.length < 2) {
            Log.e(TAG, "No configuration key provided.");
            return;
        }
        String configKey = split[1];
        // .setRepetitions           (repetitions)
        // .setAllocatingRepetitions (Long.parseLong(textValue(R.id.alloc_reps)))
        // .setBenchmarkSubstring    (textValue(R.id.benchmark_substring).toLowerCase())
        // .setRunAllBenchmarks      (isChecked(R.id.checkbox_long))
        // .setRunAtMaxSpeed         (isChecked(R.id.checkbox_max))
        // .setBenchmarkSet          (isChecked(R.id.run_alloc) ?
        //                            BenchmarkRunner.BenchmarkSet.ALLOC :
        //                            BenchmarkRunner.BenchmarkSet.NON_ALLOC);
        Map<String,ToolConfig> configurations = null;
        ToolConfig config = null;
        try {
            configurations = ToolConfig.readConfigFile();
            config = configurations.get(configKey);
        } catch (Exception e) {
            Log.e(TAG, "Error reading configuration file.", e);
        }
        if (config == null) {
            Log.e(TAG, "Could not find configuration for " + configKey);
            return;
        }
        stateThread = new Thread(
            new Runnable () {
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        ApplicationState.DetailedState detailedState = controller.getState();
                        ApplicationState.State state = detailedState.state;
                        if (state == ApplicationState.State.MILESTONE ||
                            state == ApplicationState.State.MEASURING_FINISHED ||
                            state == ApplicationState.State.MEASURING_STARTED) {
                            stateUpdated(detailedState);
                            if (state == ApplicationState.State.MEASURING_FINISHED) {
                                return;
                            }
                        }
                        try {
                            Thread.sleep(10000);
                        }
                        catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
        );

        stateThread.start();
        this.controller.startMeasuring(this.runner, config);
    }

    private void output(String message) {
        synchronized (this) {
            this.printWriter.println(message);
        }
    }

    public void executeCommand(String command) {
        boolean executed = false;
        command = command.trim();
        if (command.length() == 0) {
            return;
        }
        else if (command.startsWith("start")) {
            this.executeStart(command);
            executed = true;
        }
        else if (command.startsWith("end")) {
            this.controller.interruptMeasuring();
            executed = true;
        }
        else if (command.startsWith("quit")) {
            this.restartServer();
            executed = true;
        }
        else {
            this.output(String.format("Unkown command %s", command));
            Log.v(TAG, "" + command + " == unknown command.");
        }
        if (!executed) {
            return;
        }
        this.output(String.format(
            "[Executed %s]", command));
    }

    public void startServer(
        BenchmarkController controller,
        BenchmarkRunner runner)

    {
        this.controller = controller;
        //this.controller.addListener(this, ApplicationState.State.MILESTONE);
        this.runner = runner;
        //initialize server socket in a new separate thread
        this.serverThread = new Thread(InitializeConnection);
        this.serverThread.start();
        String msg = "Attempting to connect";
        Log.v(TAG, msg);
    }
    public boolean stopServer() {
        try
        {
            // TODO
            // Close the opened resources on activity destroy
            this.output("Stopping socket server.");
            serverThread.interrupt();
            if (SocketCommunicator.nis != null) {
                SocketCommunicator.nis.close();
            }
            if (this.out != null) {
                this.out.close();
            }
            if (server != null) {
                server.close();
            }
            return true;
        }
        catch (IOException ec)
        {
            Log.e(SocketCommunicator.TAG, "Cannot close server socket" + ec);
            return false;
        }
    }

    public void restartServer() {
        if (this.stopServer()) {
            this.startServer(this.controller, this.runner);
        }
    }

    public void stateUpdated(ApplicationState.DetailedState state) {
        this.output(state.toString());
    }

    public static final String TAG = "SocketCommunicator";
    public static final int TIMEOUT = 10;
    private ServerSocket server = null;
    private Socket client = null;
    private OutputStream out;
    private PrintWriter printWriter;
    private String receivedCommand = null;
    public static InputStream nis = null;
    private BenchmarkController controller;
    private Map<String, ToolConfig> configurations;
    private BenchmarkRunner runner;
    private Thread serverThread;
    private Thread stateThread;

    private final String helpMessage = "\n" +
        "Measuring application ready.\n" +
        "Available commands:\n" +
        "  start :CONFIG_KEY\n" +
        "    Starts measuring with the configuration\n" +
        "    loaded from nativebenchmark_setup.json file\n" +
        "    under the top level key CONFIG_KEY.\n" +
        "  end\n" +
        "    Interrupts measuring.\n";

}
