
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
        Log.v(TAG, "starting to logcat");
        Process process = Runtime.getRuntime().exec("logcat -d -v time");
        try {
            BufferedReader bufferedReader = 
                new BufferedReader(
                    new InputStreamReader(
                        process.getInputStream()));
            // todo blocks here indefinitely
            int returnValue = process.waitFor();
            Log.v(TAG, "ending logcat waiting");
            if (returnValue != 0) {
                Log.e(TAG, "Log process failed.");
                return;
            }
            writeSegment(bufferedReader, dir);
        }
        catch (InterruptedException e) {
            Log.e(TAG, "Was interrupted.");
        }
        finally {
            process.destroy();
        }
        Log.v(TAG, "ending logcat");
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
        return "[" + type + "]";
    }

    public static String filename() {
        return "log-" + currentRunId + ".txt";
    }

    private static void writeSegment(BufferedReader reader, File dir) {
        boolean include = false;
        PrintWriter writer = null;
        Log.v(TAG, "starting to write");
        try {
            writer = Utils.makeWriter(dir, filename(), false);
            String line; 
            Matcher m = null;
            while ((line = reader.readLine()) != null) {
                if (!include) {
                    m = startPattern.matcher(line);
                    if (m.matches()) {
                        include = true;
                    }
                }
                else {
                    writer.println(line);
                    m = endPattern.matcher(line);
                    if (m.matches()) {
                        include = false;
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
            if (writer != null) {
                writer.close();
            }
        }
        Log.v(TAG, "ending write");
    }

    private static Pattern makeMarkerPattern(String type) {
        return Pattern.compile("I/"+ TAG + "\\([^)]+: \\[" + type +"\\].*");
    }

    private static final int LOGLEVEL         = Log.INFO;
    private static final String TAG           = "LogAccess";
    private static final String START         = "Start";
    private static final String END           = "End";
    private static final Pattern startPattern = makeMarkerPattern(START);
    private static final Pattern endPattern   = makeMarkerPattern(END);
    private static String currentRunId;
}
