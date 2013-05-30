
package fi.helsinki.cs.tituomin.nativebenchmark;

import java.io.Reader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.util.Log;

public class LogAccess {

    public Reader readLogs() throws IOException {
        process = Runtime.getRuntime().exec("logcat -d");
        Reader bufferedReader = 
            new BufferedReader(
                new InputStreamReader(
                    process.getInputStream()));
        return bufferedReader;
    }

    public void closeLogs() throws InterruptedException, IOException {
        int returnValue = process.waitFor();
        if (returnValue != 0) {
            Log.e(TAG, "Log process failed.");
        }
        Process process = Runtime.getRuntime().exec("logcat -c");
        BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));
        returnValue = process.waitFor();
        if (returnValue != 0) {
            Log.e(TAG, "Log clearing failed.");
        }
    }

    private Process process;
    private static final String TAG = "LOGACCESS";
}
