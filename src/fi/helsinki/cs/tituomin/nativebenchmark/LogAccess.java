
package fi.helsinki.cs.tituomin.nativebenchmark;

import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogAccess {

    public static void dumpLog(File dir) throws IOException {
        Process process = Runtime.getRuntime().exec("logcat -d -b main -b system -b radio -b events -v time");
        try {
            BufferedReader bufferedReader = 
                new BufferedReader(
                    new InputStreamReader(
                        process.getInputStream()));
            writeSegment(bufferedReader, dir);
            int returnValue = process.waitFor();
            if (returnValue != 0) {
                Log.e(TAG, "Log process failed.");
                return;
            }
        }
        catch (InterruptedException e) {
            Log.e(TAG, "Was interrupted.");
        }
        finally {
            process.destroy();
        }
    }

    public static void start() {
        currentRunId = Utils.getUUID();
        mark(START, currentRunId);
    }
    public static void end() {
        mark(END, currentRunId);
    }
    private static void mark(String type, String id) {
        Log.println(LOGLEVEL, TAG, marker(type) + id);
    }
    private static String marker(String type) {
        return "[" + type + "] ";
    }

    public static String filename() {
        return "log-" + currentRunId + ".txt";
    }

    private static void writeSegment(BufferedReader reader, File dir) {
        boolean include = false;
        PrintWriter writer = null;
        Pattern startPattern = makeMarkerPattern(START);
        Pattern endPattern   = makeMarkerPattern(END);

        try {
            writer = Utils.makeWriter(dir, filename(), false);
            String line; 
            Matcher m = null;
            while ((line = reader.readLine()) != null) {
                if (include) {
                    m = endPattern.matcher(line);
                    if (m.matches()) {
                        include = false;
                    }
                    else {
                        writer.println(line);
                    }
                }
                else {
                    m = startPattern.matcher(line);
                    if (m.matches()) {
                        include = true;
                    }
                }
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "Error writing log file. (File not found)");
        }
        catch (IOException e) {
            Log.e(TAG, "Error writing log file.");
        }
        finally {
            writer.flush();
            if (writer != null) {
                writer.close();
            }
        }
    }

    //06-27 23:52:37.348 I/LogAccess( 2378): [End] 43ba52d0-61b0-47e4-991b-c98a3dd21f9f


    private static Pattern makeMarkerPattern(String type) {
        return Pattern.compile("[-:. [0-9]]+ I/"+ TAG + "\\([ 0-9]+\\): \\[" + type +"\\] " + currentRunId);
    }

    private static final int LOGLEVEL         = Log.INFO;
    private static final String TAG           = "LogAccess";
    private static final String START         = "Start";
    private static final String END           = "End";
    private static String currentRunId;
}
