
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

    private static final String LOGCAT_COMMAND =
        "logcat -f %s " +
        "-b main -b system -b radio -b events " +
        "-v time";

    public static void start(File dir) throws IOException {
        currentRunId = Utils.getUUID();
        mark(START, currentRunId);
        String filename = new File(dir, filename()).getAbsolutePath();
        String command = String.format(LOGCAT_COMMAND, filename);
        logcatProcess = Runtime.getRuntime().exec(command);
    }

    public static void end() {
        mark(END, currentRunId);
        logcatProcess.destroy();
        logcatProcess = null;
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
    //06-27 23:52:37.348 I/LogAccess( 2378): [End] 43ba52d0-61b0-47e4-991b-c98a3dd21f9f

    private static Pattern makeMarkerPattern(String type) {
        return Pattern.compile("[-:. [0-9]]+ I/"+ TAG + "\\([ 0-9]+\\): \\[" + type +"\\] " + currentRunId);
    }

    private static final int LOGLEVEL         = Log.INFO;
    private static final String TAG           = "LogAccess";
    private static final String START         = "Start";
    private static final String END           = "End";
    private static String currentRunId;
    private static Process logcatProcess;
}
