package fi.helsinki.cs.tituomin.nativebenchmark;



import android.util.Log;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkController;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

public class SocketCommunicator
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

                        SocketCommunicator.this.out = new ObjectOutputStream(client.getOutputStream());
                        SocketCommunicator.nis = client.getInputStream();
                        try
                            {
                                SocketCommunicator.this.out.writeObject("\n" + helpMessage + "\n");
                                Log.v(TAG, "client >" + helpMessage);

                                byte[] bytes = new byte[1024];
                                int numRead = 0;
                                while ((numRead = SocketCommunicator.nis.read(bytes, 0, 1024)) >= 0)
                                    {
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
        }
        else {
            this.controller.startMeasuring(this.runner, config);
        }
    }

    public void executeCommand(String command) {
        if (command.startsWith("start")) {
            this.executeStart(command);
        }
        else if (command.startsWith("end")) {
            this.controller.interruptMeasuring();
        }
        else {
            Log.v(TAG, "" + command + " == unknown command.");
        }
    }

    public void startServer(
        BenchmarkController controller,
        BenchmarkRunner runner)

    {
        this.controller = controller;
        this.runner = runner;
        //initialize server socket in a new separate thread
        this.serverThread = new Thread(InitializeConnection);
        this.serverThread.start();
        String msg = "Attempting to connect";
        Log.v(TAG, msg);
    }

    public void stopServer() {
        try
        {
            // TODO
            // Close the opened resources on activity destroy
            serverThread.interrupt();
            if (SocketCommunicator.nis != null) {
                SocketCommunicator.nis.close();
            }
            if (this.out != null) {
                this.out.close();
            }
            if (server != null)
            {
                server.close();
            }
        }
        catch (IOException ec)
        {
            Log.e(SocketCommunicator.TAG, "Cannot close server socket" + ec);
        }
    }

    public static final String TAG = "SocketCommunicator";
    public static final int TIMEOUT = 10;
    private ServerSocket server = null;
    private Socket client = null;
    private ObjectOutputStream out;
    private String receivedCommand = null;
    public static InputStream nis = null;
    private final String sendMsg = "Hello world";
    private BenchmarkController controller;
    private Map<String, ToolConfig> configurations;
    private BenchmarkRunner runner;
    private Thread serverThread;
}
