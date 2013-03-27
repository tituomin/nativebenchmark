
package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.PlainRunner;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.ResponseTimeRecorder;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.LinuxPerfRecordTool;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;
import fi.helsinki.cs.tituomin.nativebenchmark.Utils;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.SortedSet;
import java.util.Collections;
import java.util.TreeSet;

import android.os.Environment;
import android.util.Log;
import android.os.SystemClock;
import android.content.res.Resources;

public class BenchmarkRunner {
    private static final String SEPARATOR     = ",";
    private static final String MISSING_VALUE = "-";
    private static final long WARMUP_REPS = 2000;
    private static BenchmarkParameter benchmarkParameter;
    private static List<MeasuringTool> measuringTools;

    public static BenchmarkParameter getBenchmarkParameter() {
        if (benchmarkParameter == null) {
            benchmarkParameter = new BenchmarkParameter();
        }
        return benchmarkParameter;
    }

    public static void initTools(File dataDir, int rounds) {
        File perfDir = new File(dataDir, "perf");
        perfDir.mkdir();

        measuringTools = new ArrayList<MeasuringTool> ();
        measuringTools.add(new PlainRunner(1)); // warmup round
        measuringTools.add(new ResponseTimeRecorder(rounds)); // total response time
        measuringTools.add(new LinuxPerfRecordTool(1) // call profile
                        .set(BasicOption.OUTPUT_FILEPATH, perfDir.getPath())
                        .set(BasicOption.MEASURE_LENGTH, "0.1"));
    }


    public static void runBenchmarks(
        ApplicationState mainUI, int rounds, long repetitions,
        Resources resources) {

        File sd = Environment.getExternalStorageDirectory();
        File dataDir = new File(sd, "results");
        dataDir.mkdir();

        initTools(dataDir, rounds);

        try {
            BenchmarkRegistry.init(repetitions);
        }
        catch (ClassNotFoundException e) {
            mainUI.updateState(ApplicationState.State.ERROR);
            Log.e("BenchmarkRunner", "Class not found.", e);
            return;
        }

        benchmarkParameter = getBenchmarkParameter();
        BenchmarkInitialiser.init(benchmarkParameter);

        List<Benchmark> benchmarks = BenchmarkRegistry.getBenchmarks();
        Map<Class, Map<String,String>> benchmarkBaseData = new HashMap<Class, Map<String,String>>();
        for (Benchmark b : benchmarks) {
            benchmarkBaseData.put(b.getClass(), inspectBenchmark(b));
        }

        Collections.shuffle(benchmarks);

        for (MeasuringTool tool : measuringTools) {
            String measurementID;

            int round_count = tool.getRounds();
            for (int series = 0; series < round_count; series++) {
                Date start = new Date();
                long endTime = 0;

                long startTime = SystemClock.uptimeMillis();
                List<Map<String,String>> collectedData = runSeries(benchmarks, benchmarkBaseData, mainUI, tool);
                endTime = SystemClock.uptimeMillis();

                measurementID = Utils.getUUID();

                if (collectedData.isEmpty()) {
                    continue;
                }

                // 1. gather all labels (!ordered!)
                SortedSet<String> labels = new TreeSet<String> ();
                for (Map<String,String> measurementData : collectedData) {
                    labels.addAll(measurementData.keySet());
                }

                PrintWriter writer = null;

                try {
                    writer = makeWriter(dataDir, "benchmarks-" + measurementID + ".csv", false);
                    for (String label : labels) {
                        writer.print(label + SEPARATOR);
                    }
                    writer.println("");
                    String[] values = new String[labels.size()];

                    // 2. print all possible values for all benchmarks
                    for (Map<String,String> measuredData : collectedData) {
                        int i = 0;
                        for (String label : labels) {
                            String val = measuredData.get(label);
                            values[i++] = (val == null ? MISSING_VALUE : val);
                        }
                        for (i = 0; i < values.length; i++) {
                            writer.print(values[i] + SEPARATOR);
                        }
                        writer.println("");
                    }
                    writer.flush();
                    writer.close();

                }
                catch (IOException e) {
                    logE(e);
                }
                if (writer != null) {
                    writer.close();
                }

                Date end = new Date();

                try {
                    writer = makeWriter(dataDir, "measurements.txt", true);
                    writer.println("");
                    writer.println("id: "               + measurementID);
                    writer.println("repetitions: "      + repetitions);
                    writer.println("rounds: "           + rounds);
                    writer.println("start: "            + start);
                    writer.println("end: "              + end);
                    writer.println("duration: "         + humanTime(endTime - startTime));
                    writer.println("tool: "             + tool.getClass().getName());
                    writer.println("benchmarks: "       + benchmarks.size());
                    writer.println("code-revision: "    + resources.getText(R.string.app_revision));
                    writer.println("code-checksum: "    + resources.getText(R.string.app_checksum));
                    writer.println("");
                }
                catch (IOException e ) {
                    logE(e);
                }
                finally {
                    writer.close();
                }
                OutputStream os = null;
                try {
                    File zip = new File(dataDir, "perfdata-" + measurementID + ".zip");
                    os = makeOutputStream(zip, false);
                    List<String> filenames = tool.getFilenames();
                    Log.v("BenchmarkRunner", "filenames " + filenames.size());
                    filenames.add(new File(dataDir, "benchmarks-" + measurementID + ".csv").getAbsolutePath());
                    writeToZipFile(os, filenames, measurementID);
                    // deleteFiles(filenames);
                }
                catch (FileNotFoundException e) {
                    logE(e);
                }
                catch (IOException e) {
                    logE("Error writing zip file.",  e);
                }
                try {
                    if (os != null) {
                        os.close();
                    }
                }
                catch (IOException e) {
                    logE("Error closing file.", e);
                }
            }
        }
        mainUI.updateState(
            ApplicationState.State.MEASURING_FINISHED);
    }

    private final static String TAG = "BenchmarkRunner";
    private static void logE(String message, Exception e) {
        Log.e(TAG, message, e);
    }
    private static void logE(Exception e) {
        Log.e(TAG, "exception", e);
    }
    private static void logE(String msg) {
        Log.e(TAG, "msg");
    }

    private static void deleteFiles(List<String> filenames) {
        for (String filename: filenames) {
            boolean success = new File(filename).delete();
            if (!success) {
                logE("Error deleting file " + filename);
            }
        }
    }

    private static void
    writeToZipFile(OutputStream os, List<String> filenames, String mID)
    throws IOException
    {
        ZipOutputStream zos = new ZipOutputStream(os);
        final int byteCount = 512 * 1024;
        byte[] bytes = new byte[byteCount];
        for (String filename : filenames) {
            try {
                InputStream is = makeInputStream(filename);
                ZipEntry entry = new ZipEntry(mID + "/" + new File(filename).getName());
                int bytesRead = -1;
                zos.putNextEntry(entry);
                while ((bytesRead = is.read(bytes, 0, byteCount)) != -1) {
                    zos.write(bytes, 0, bytesRead);
                }
                zos.closeEntry();
            } finally {
                zos.flush();
            }                
        }
        try {
            zos.close();
        }
        catch (IOException e) {
            logE(e);
        }
    }

    private static Map<String,String> makeMap() {
        return new HashMap<String,String> ();
    }

    private static List<Map<String,String>> runSeries(
        List<Benchmark> benchmarks, Map<Class, Map<String,String>> basedata,
        ApplicationState mainUI, MeasuringTool tool) {

        final ApplicationState.State state = ApplicationState.State.MILESTONE;
        boolean runGC = tool.explicitGC();
        int j = 0;
        List<Map<String,String>> compiledMetadata = new ArrayList<Map<String,String>> ();

        for (Benchmark benchmark : benchmarks) {
            Map<String,String> md = makeMap();
            tool.setBenchmark(benchmark);
            tool.run();
            Map<String,String> measurement = tool.getMeasurement();
            if (!measurement.isEmpty()) {
                md.putAll(basedata.get(benchmark.getClass()));
                md.putAll(measurement);
                compiledMetadata.add(md);
            }
            // todo: remove UI overhead?
            mainUI.updateState(state, "tool " + tool.getClass().getName() + " benchmark " + ++j);

            if (runGC && j % 50 == 0) {
                System.gc();
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException e) {
                    Log.e("BenchmarkRunner", "Measuring thread was interrupted", e);
                    mainUI.updateState(
                        ApplicationState.State.ERROR,
                        "Measuring was interrupted");
                }
            }
        }
        return compiledMetadata;
    }

    private static PrintWriter
    makeWriter(File dir, String filename, boolean append)
    throws FileNotFoundException
    {
        return new PrintWriter(makeOutputStream(dir, filename, append));
    }

    private static OutputStream
    makeOutputStream(File dir, String filename, boolean append)
    throws FileNotFoundException
    {
        return makeOutputStream(new File(dir, filename), append);
    }

    private static OutputStream
    makeOutputStream(File file, boolean append)
    throws FileNotFoundException
    {
        return new BufferedOutputStream(
            new FileOutputStream(
                file, append));
    }

    private static InputStream
    makeInputStream(String filename)
    throws FileNotFoundException
    {
        return new BufferedInputStream(new FileInputStream(new File(filename)));
    }

    private static String humanTime(long millis) {
        String time;
        long seconds = millis  / 1000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;
        long seconds_total = seconds;
        seconds %= 60;
        minutes %= 60;
        return (
            hours   + "h " +
            minutes + "m " +
            seconds + "s " +
            "(" + seconds_total + " s tot.)");
    }

    private static Map<String,String> lastMetadata = null;
    private static Map<String,String> inspectBenchmark(Benchmark benchmark) {
        Map<String,String> bdata = new HashMap<String,String>();
        Class c = benchmark.getClass();
        String from = benchmark.from();
        String to   = benchmark.to();

        // reflection only works for J2C version,
        // results are cached for other versions
        if (from.equals("J") && to.equals("C")) {
            Method[] methods = c.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {

                Method m = methods[i];
                int modifiers = m.getModifiers();

                if (Modifier.isNative(modifiers)) {

                    Map<String,Integer> parameterTypes = new HashMap<String,Integer> ();
                    Class [] parameters = m.getParameterTypes();
                    Integer previousValue = null;
                    for (Class param : Arrays.asList(parameters)) {
                        String param_typename = param.getCanonicalName();
                        previousValue = parameterTypes.get(param_typename);
                        parameterTypes.put(
                            param_typename,
                            (previousValue == null ? 1 : ((int)previousValue) + 1));
                    }
                    for (String typename : parameterTypes.keySet()) {
                        bdata.put("parameter_type_" + typename + "_count", parameterTypes.get(typename) + "");
                    }
                    Class returnType = m.getReturnType();

                    bdata.put("parameter_type_count", parameterTypes.keySet().size() + "");
                    bdata.put("parameter_count",      parameters.length + "");
                    bdata.put("return_type",          returnType.getCanonicalName());
                    bdata.put("native_static",        Modifier.isStatic(modifiers) ? "1" : "0");
                    bdata.put("native_private",       Modifier.isPrivate(modifiers) ? "1" : "0");
                    bdata.put("native_protected",     Modifier.isProtected(modifiers) ? "1" : "0");
                    bdata.put("native_public",        Modifier.isPublic(modifiers) ? "1" : "0");
                }
            }
            bdata.put("no", "" + benchmark.sequenceNo());
            bdata.put("description", benchmark.description());

            lastMetadata = bdata;
        }
        else {
            // other than J2C
            if (lastMetadata == null) {
                Log.e("BenchmarkRunner", "Could not retrieve inspected metadata.");
                return null;
            }
            bdata.putAll(lastMetadata);
            if (!("" + benchmark.sequenceNo()).equals(bdata.get("no"))) {
                Log.e("BenchmarkRunner", "Retrieved metadata has wrong number.");
                return null;
            }
        }

        bdata.put("direction", from + " > " + to);

        return bdata;
    }
}
