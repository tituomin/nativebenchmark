
package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.PlainRunner;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.ResponseTimeRecorder;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.LinuxPerfRecordTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.CommandlineTool;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkResult;
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
import java.util.Iterator;
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

public class BenchmarkRunner {
    private static final String SEPARATOR     = ",";
    private static final String MISSING_VALUE = "-";
    private static final long WARMUP_REPS = 2000;
    private static final String CPUFREQ = "400000";
    private static BenchmarkParameter benchmarkParameter;
    private static List<MeasuringTool> measuringTools;

    public static BenchmarkParameter getBenchmarkParameter() {
        if (benchmarkParameter == null) {
            benchmarkParameter = new BenchmarkParameter();
        }
        return benchmarkParameter;
    }

    public static void initTools(File dataDir) throws IOException, InterruptedException {
        File perfDir = new File(dataDir, "perf");
        perfDir.mkdir();

        measuringTools = new ArrayList<MeasuringTool> ();

        PlainRunner p = (PlainRunner) new PlainRunner(1)
            .set(BasicOption.CPUFREQ, CPUFREQ);

        CommandlineTool.execute(p.initScript());

        measuringTools.add(p); // warmup round

        // measuringTools.add(new ResponseTimeRecorder(1)); // total response time

        // measuringTools.add(new LinuxPerfRecordTool(1) // call profile
        //     .set(BasicOption.OUTPUT_FILEPATH, perfDir.getPath())
        //     .set(BasicOption.MEASURE_LENGTH, "10")); // todo: proper val

        measuringTools.add(new ResponseTimeRecorder(1000)); // total response time
    }


    public static void runBenchmarks(
        ApplicationState mainUI, long repetitions,
        CharSequence appRevision, CharSequence appChecksum) {

        File sd = Environment.getExternalStorageDirectory();
        File dataDir = new File(sd, "results");
        dataDir.mkdir();

        try {
            BenchmarkRegistry.init(repetitions);
            initTools(dataDir);
        }
        catch (Exception e) {
            mainUI.updateState(ApplicationState.State.ERROR);
            Log.e("BenchmarkRunner", "Error initialising", e);
            return;
        }

        benchmarkParameter = getBenchmarkParameter();
        BenchmarkInitialiser.init(benchmarkParameter);

        List<Benchmark> benchmarks = BenchmarkRegistry.getBenchmarks();
        Collections.shuffle(benchmarks);

        for (MeasuringTool tool : measuringTools) {
            String measurementID;

            int max_rounds = tool.getRounds();
            for (int series = 0; series < max_rounds; series++) {
                Date start = new Date();
                long endTime = 0;

                long startTime = SystemClock.uptimeMillis();
                List<BenchmarkResult> collectedData;
                try {
                    System.gc();
                    Thread.sleep(500);
                    collectedData = runSeries(benchmarks, mainUI, tool);                    
                }
                catch (InterruptedException e) {
                    logE("Measuring thread was interrupted", e);
                    mainUI.updateState(
                        ApplicationState.State.INTERRUPTED);
                    return;
                }
                if (collectedData.isEmpty() || tool.ignore()) {
                    continue;
                }

                endTime = SystemClock.uptimeMillis();
                measurementID = Utils.getUUID();

                PrintWriter writer = null;

                try {
                    writer = makeWriter(dataDir, "benchmarks-" + measurementID + ".csv", false);

                    String[] labels = BenchmarkResult.labels();
                    for (int i = 0; i < BenchmarkResult.size(); i++) {
                        writer.print(labels[i] + SEPARATOR);
                    }
                    writer.println("");
                    String[] values = new String[labels.length];

                    // 2. print all possible values for all benchmarks
                    for (BenchmarkResult result: collectedData) {
                        for (int i = 0; i < BenchmarkResult.size(); i++) {
                            String value = result.get(i);
                            if (value == null) {
                                value = MISSING_VALUE;
                            }
                            writer.print(value + SEPARATOR);
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
                    writer.println("cpu-freq: "         + CPUFREQ);
                    writer.println("repetitions: "      + repetitions);
                    writer.println("start: "            + start);
                    writer.println("end: "              + end);
                    writer.println("duration: "         + humanTime(endTime - startTime));
                    writer.println("tool: "             + tool.getClass().getName());
                    writer.println("benchmarks: "       + benchmarks.size());
                    writer.println("code-revision: "    + appRevision);
                    writer.println("code-checksum: "    + appChecksum);
                    writer.println("");
                }
                catch (IOException e ) {
                    logE(e);
                }
                finally {
                    writer.close();
                }
                OutputStream os = null;
                List<String> filenames = tool.getFilenames();
                if (!filenames.isEmpty()) {
                    try {
                        File zip = new File(dataDir, "perfdata-" + measurementID + ".zip");
                        os = makeOutputStream(zip, false);
                        Log.v("BenchmarkRunner", "filenames " + filenames.size());
                        filenames.add(new File(dataDir, "benchmarks-" + measurementID + ".csv").getAbsolutePath());
                        writeToZipFile(os, filenames, measurementID);
                        deleteFiles(filenames);
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

    private static List<String> makeList() {
        return new ArrayList<String> (200);
    }

    private static List<BenchmarkResult> runSeries(
        List<Benchmark> benchmarks, ApplicationState mainUI, MeasuringTool tool)
        throws InterruptedException {

        final ApplicationState.State state = ApplicationState.State.MILESTONE;
        int j = 0;
        List<BenchmarkResult> compiledMetadata = new ArrayList<BenchmarkResult> (benchmarks.size());

        for (Benchmark benchmark : benchmarks) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            BenchmarkParameter bPar = getBenchmarkParameter();
            BenchmarkResult introspected;
            try {
                 introspected = inspectBenchmark(benchmark);
            }
            catch  (ClassNotFoundException e) {
                Log.e("BenchmarkRunner", "Could not find class", e);
                return compiledMetadata;
            }

            boolean onlyDefault = 
                (!introspected.get("has_reference_types").equals("1") ||
                 (Integer.parseInt(introspected.get("parameter_count")) > 1));

            Iterator<Integer> iterator = bPar.iterator();
            Integer i;
            if (iterator.hasNext()) {
                i = iterator.next();
            }
            else {
                i = null;
            }
            while (i != null) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                bPar.setUp(); // (I) needs tearDown (see II)

                try {
                    tool.startMeasuring(benchmark);
                }
                catch (IOException e) {
                    logE("Measuring caused IO exception", e);
                    if (mainUI.userWantsToRetry(e)) {
                        Log.v("BenchmarkRunner", "yes he wants to retry");
                        continue; // without incrementing i
                    }
                    else {
                        throw new InterruptedException("User wants to abort");
                    }
                }
                finally {
                    bPar.tearDown(); // (II) needs setUp (see I)
                }

                BenchmarkResult measurement = tool.getMeasurement();
                if (!measurement.isEmpty()) {
                    // todo: actual vs. requested size (objects etc.)
                    if (!onlyDefault) {
                        measurement.put("dynamic_size", "" + i);
                    }
                    measurement.put("class", benchmark.getClass().getName());
                    measurement.putAll(introspected);
                    compiledMetadata.add(measurement);
                }
                // todo: remove UI overhead?
                mainUI.updateState(state, "tool " + tool.getClass().getName() + " benchmark " + ++j);

                if (tool.explicitGC() && j % 50 == 0) {
                    System.gc();
                    Thread.sleep(350);
                }
                // if parameter size can be varied, vary it - else break with first size
                if (onlyDefault) {
                    break;
                }
                if (iterator.hasNext()) {
                    i = iterator.next();
                }
                else {
                    break;
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

    private static BenchmarkResult inspectBenchmark(Benchmark benchmark) throws ClassNotFoundException {
        BenchmarkResult bdata = new BenchmarkResult();
        int seqNo = benchmark.sequenceNo();

        Class c = Class.forName("fi.helsinki.cs.tituomin.nativebenchmark.benchmark.J2CBenchmark" + String.format("%05d", seqNo));
        
        Method[] methods = c.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {

            Method m = methods[i];
            int modifiers = m.getModifiers();

            if (Modifier.isNative(modifiers)) {

                Class [] parameters = m.getParameterTypes();
                List<Class> parameterList = new ArrayList<Class>(Arrays.asList(parameters));

                Map<String,Integer> parameterTypes = new HashMap<String,Integer> ();
                for (Class param : parameterList) {
                    Integer previousValue = null;
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

                boolean hasRefTypes = false;
                parameterList.add(returnType);
                for (Class cl : parameterList) {
                    if (Object.class.isAssignableFrom(cl)) {
                        hasRefTypes = true;
                        break;
                    }
                }

                bdata.put("has_reference_types",  hasRefTypes ? "1" : "0");
                bdata.put("parameter_type_count", parameterTypes.keySet().size() + "");
                bdata.put("parameter_count",      parameters.length + "");
                bdata.put("return_type",          returnType.getCanonicalName());
                bdata.put("native_static",        Modifier.isStatic(modifiers) ? "1" : "0");
                bdata.put("native_private",       Modifier.isPrivate(modifiers) ? "1" : "0");
                bdata.put("native_protected",     Modifier.isProtected(modifiers) ? "1" : "0");
                bdata.put("native_public",        Modifier.isPublic(modifiers) ? "1" : "0");
            }
        }
        String from = benchmark.from();
        String to   = benchmark.to();
        bdata.put("no", "" + benchmark.sequenceNo());
        bdata.put("description", benchmark.description());
        bdata.put("direction", from + " > " + to);
        return bdata;
    }
}
