package fi.helsinki.cs.tituomin.nativebenchmark;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import android.util.Log;

public class SocketCommunicator
{

    public static final String TAG = "SocketCommunicator";
    public static final int TIMEOUT = 10;
    private ServerSocket server = null;
    private Socket client = null;
    private ObjectOutputStream out;
    private String receivedCommand = null;
    public static InputStream nis = null;
    private final String sendMsg = "Hello world";

    /**
     * Thread to initialize Socket connection
     */
    private final Runnable InitializeConnection = new Thread()
    {
        @Override
        public void run()
        {
            // initialize server socket
            try
            {
                server = new ServerSocket(38300);
                //server.setSoTimeout(TIMEOUT * 1000);

                //attempt to accept a connection
                client = server.accept();

                out = new ObjectOutputStream(client.getOutputStream());
                SocketCommunicator.nis = client.getInputStream();
                try
                {
                    out.writeObject(sendMsg);
                    Log.v(TAG, "client >" + sendMsg);

                    byte[] bytes = new byte[1024];
                    int numRead = 0;
                    while ((numRead = SocketCommunicator.nis.read(bytes, 0, 1024)) >= 0)
                    {
                        receivedCommand = new String(bytes, 0, numRead);
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
                Log.v(TAG, receivedCommand);
            }
        }
    };

    public void startServer()
    {
        //initialize server socket in a new separate thread
        new Thread(InitializeConnection).start();
        String msg = "Attempting to connect";
        Log.v(TAG, msg);
    }

    public void stopServer() {
        try
        {
            // TODO
            // Close the opened resources on activity destroy
            if (SocketCommunicator.nis != null) {
                SocketCommunicator.nis.close();
            }
            if (out != null) {
                out.close();
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
}
