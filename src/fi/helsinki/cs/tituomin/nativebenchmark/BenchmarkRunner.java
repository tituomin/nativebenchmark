
package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool.RunnerException;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.PlainRunner;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.ResponseTimeRecorder;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.LinuxPerfRecordTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.CommandlineTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MockCommandlineTool;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkResult;
import fi.helsinki.cs.tituomin.nativebenchmark.Utils;
import fi.helsinki.cs.tituomin.nativebenchmark.ShellEnvironment;
import fi.helsinki.cs.tituomin.nativebenchmark.Init;


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
    private static final long WARMUP_REPS = 10000;
    private static BenchmarkParameter benchmarkParameter;
    private static List<MeasuringTool> measuringTools;
    private static int benchmarkCount = 0;

    private static boolean interrupted = false;

    public static BenchmarkParameter getBenchmarkParameter() {
        if (benchmarkParameter == null) {
            benchmarkParameter = new BenchmarkParameter();
        }
        return benchmarkParameter;
    }

    public static void initTools(File dataDir, long repetitions, long allocRepetitions) throws IOException, InterruptedException {
        File perfDir = new File(dataDir, "perf");
        perfDir.mkdir();

        measuringTools = new ArrayList<MeasuringTool> ();

        // warmup round
        measuringTools.add(
            new PlainRunner(1, WARMUP_REPS, allocRepetitions));

        //measuringTools.add(
        //    new MockCommandlineTool(1, repetitions));

        // total response time
        measuringTools.add(
            new ResponseTimeRecorder(1000, repetitions, allocRepetitions));

        // call profile
        measuringTools.add(
            new LinuxPerfRecordTool(1, allocRepetitions)
            .set(BasicOption.OUTPUT_FILEPATH, perfDir.getPath())
            .set(BasicOption.MEASURE_LENGTH, "10"));

        // total response time
        //measuringTools.add(new ResponseTimeRecorder(1000, repetitions));

    }

    public static class Attributes {
        public long repetitions;
        public long allocatingRepetitions;
        public CharSequence appRevision;
        public CharSequence appChecksum;
        public File cacheDir;
        public boolean runAllBenchmarks;
        public boolean runAtMaxSpeed;
        public String benchmarkSubstring;
        public BenchmarkSelector.BenchmarkSet benchmarkSet;
    }

    private static Attributes attributes = null;
    public static void runBenchmarks(ApplicationState mainUI,
                                     Attributes requestedAttributes) {

        interrupted = false;
        attributes = requestedAttributes;

        byte[] buffer = new byte[128 * 1024];
        File sd = Environment.getExternalStorageDirectory();
        File dataDir = new File(sd, "results");
        dataDir.mkdir();
        try {
            BenchmarkRegistry.init(attributes.repetitions);
            initTools(dataDir, attributes.repetitions, attributes.allocatingRepetitions);
        }
        catch (Exception e) {
            mainUI.updateState(ApplicationState.State.ERROR);
            Log.e("BenchmarkRunner", "Error initialising", e);
            return;
        }

        benchmarkParameter = getBenchmarkParameter();
        BenchmarkInitialiser.init(benchmarkParameter);

        List<Benchmark> allBenchmarks = BenchmarkRegistry.getBenchmarks();
        List<Benchmark> benchmarks = new ArrayList<Benchmark> ();;

        boolean substringSearch = !attributes.benchmarkSubstring.equals("");
        for (Benchmark b : allBenchmarks) {
            boolean selected;
            if (attributes.runAllBenchmarks) {
                selected = true;
            }
            else if (substringSearch) {
                selected = (b.getClass().getSimpleName().toLowerCase().indexOf(attributes.benchmarkSubstring) != -1);
            }
            else {
                selected = (b.representative()
                            && ((!b.isAllocating()) && attributes.benchmarkSet == BenchmarkSelector.BenchmarkSet.NON_ALLOC)
                                || (b.isAllocating() && attributes.benchmarkSet == BenchmarkSelector.BenchmarkSet.ALLOC));
            }
            if (selected) {
                benchmarks.add(b);
            }
        }

        int numOfBenchmarks = benchmarks.size();

        //Collections.shuffle(benchmarks);
        if (attributes.runAtMaxSpeed) {
            Log.v(TAG, "maxed");
            try {
                Init.initEnvironment(true);
            }
            catch (IOException e) {
                handleException(e, mainUI);
                return;
            }
        }

        for (MeasuringTool tool : measuringTools) {

            Log.v("Runner", tool.getClass().getSimpleName());

            if (!tool.ignore()) {
                // set the slower CPU frequency etc. after the warmup
                // round(s), taking less time
                try {
                    Init.initEnvironment(attributes.runAtMaxSpeed);
                }
                catch (IOException e) {
                    handleException(e, mainUI);
                    return;
                }
            }

            int max_rounds = tool.getRounds();
            String measurementID = Utils.getUUID();
            File resultFile = new File(dataDir, "benchmarks-" + measurementID + ".csv");
            long startTime = SystemClock.uptimeMillis();
            Date start = new Date();
            long endTime = 0;

            int round = -1;
            while (++round < max_rounds) {
                benchmarkCount = 0;
                PrintWriter tempWriter = null;
                File tempFile = new File(attributes.cacheDir, "benchmarks-temp.csv");
                try {
                    tempWriter = makeWriter(tempFile, false);
                }
                catch (FileNotFoundException e) {
                    handleException(e, mainUI);
                    return;
                }


                List<BenchmarkResult> collectedData;

                try {
                    System.gc();
                    Thread.sleep(500);
                }
                catch (InterruptedException e) {
                    logE("Measuring thread was interrupted", e);
                    mainUI.updateState(
                        ApplicationState.State.INTERRUPTED);
                    interrupted = true;
                    break;
                }

                int j = 0;
                for (Benchmark benchmark : benchmarks) {
                    try {
                        collectedData = runSeries(benchmark, mainUI, tool, round);
                    }
                    catch (RunnerException e) {
                        logE("Exception was thrown", e.getCause());
                        mainUI.updateState(
                            ApplicationState.State.ERROR);
                        interrupted = true;
                        break;
                    }
                    catch (InterruptedException e) {
                        logE("Measuring thread was interrupted", e);
                        mainUI.updateState(
                            ApplicationState.State.INTERRUPTED);
                        interrupted = true;
                        break;
                    }
                    // todo: remove UI overhead?
                    //                    Log.v(TAG, benchmark.getClass().getName());

                    if (collectedData.isEmpty() || tool.ignore()) {
                        continue;
                    }

                    endTime = SystemClock.uptimeMillis();

                    // print data
                    for (BenchmarkResult result: collectedData) {
                        for (int i = 0; i < BenchmarkResult.size(); i++) {
                            String value = result.get(i);
                            if (value == null) {
                                value = MISSING_VALUE;
                            }
                            tempWriter.print(value);
                            tempWriter.print(SEPARATOR);
                        }
                        tempWriter.println("");
                        tempWriter.flush();
                    }

                } // benchmark loop
                endTime = SystemClock.uptimeMillis();
                tempWriter.close();

                if (tool.ignore()) {
                    continue; // todo test
                }

                InputStream in = null;
                OutputStream out = null;
                PrintWriter resultWriter = null;
                try {
                    resultWriter = makeWriter(resultFile, true);

                    String[] labels = BenchmarkResult.labels();
                    for (int i = 0; i < BenchmarkResult.size(); i++) {
                        resultWriter.print(labels[i] + SEPARATOR);
                    }
                    resultWriter.println("");
                    resultWriter.close();

                    in = new FileInputStream(tempFile);
                    out = makeOutputStream(resultFile, true);
                    int count;
                    while ((count = in.read(buffer, 0, 128 * 1024)) != -1) {
                        out.write(buffer, 0, count);
                    }
                    out.flush();
                    //tempFile.delete();
                }
                catch (Exception e) {
                    mainUI.updateState(ApplicationState.State.ERROR);
                    Log.e("BenchmarkRunner", "Error writing results", e);
                    interrupted = true;
                    break;
                }
                finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                        if (out != null) {
                            out.flush();
                            out.close();
                        }
                        if (resultWriter != null) {
                            resultWriter.close();
                        }
                    }
                    catch (IOException e) {
                        mainUI.updateState(ApplicationState.State.ERROR);
                        Log.e("BenchmarkRunner", "Error closing files", e);
                        interrupted = true;
                        break;
                    }
                }
            } // for round

            if (round > 0) {
                // there has been at least one succesful round
                writeMeasurementMetadata(
                    new File(dataDir, "measurements.txt"),
                    measurementID,
                    (attributes.runAtMaxSpeed ? Init.CPUFREQ_MAX : Init.CPUFREQ),
                    startTime, endTime, start, tool, numOfBenchmarks, round);

                List<String> filenames = tool.getFilenames();
                if (!filenames.isEmpty()) {
                    OutputStream os = null;

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
            if (interrupted) {
                return;
            }

        } // for tool
        mainUI.updateState(
            ApplicationState.State.MEASURING_FINISHED);
    }

    private static void writeMeasurementMetadata (
        File catalogFile, String measurementID, int cpuFreq,
        long startTime, long endTime, Date start, MeasuringTool tool, int numOfBenchmarks,
        int rounds) {

        Date end = new Date();
        PrintWriter catalogWriter = null;
        try {
            String f = "%20s: %s";
            catalogWriter = makeWriter(catalogFile, true);
            catalogWriter.println("");

            catalogWriter.println(String.format(f, "id", measurementID));
            catalogWriter.println(String.format(f, "cpu-freq", cpuFreq));
            catalogWriter.println(String.format(f, "repetitions", attributes.repetitions));
            catalogWriter.println(String.format(f, "rounds", rounds));
            catalogWriter.println(String.format(f, "start", start));
            catalogWriter.println(String.format(f, "end", end));
            catalogWriter.println(String.format(f, "duration", Utils.humanTime(endTime - startTime)));
            catalogWriter.println(String.format(f, "tool", tool.getClass().getSimpleName()));
            catalogWriter.println(String.format(f, "benchmarks", numOfBenchmarks));
            catalogWriter.println(String.format(f, "code-revision", attributes.appRevision));
            catalogWriter.println(String.format(f, "code-checksum", attributes.appChecksum));
            catalogWriter.println(String.format(f, "benchmark-set", attributes.benchmarkSet));
            catalogWriter.println(String.format(f, "substring-filter", attributes.benchmarkSubstring));
            catalogWriter.println("");
        }
        catch (IOException e ) {
            logE(e);
        }
        finally {
            catalogWriter.close();
        }
        
    }

    private final static String TAG = "BenchmarkRunner";
    private static void logE(String message, Throwable e) {
        Log.e(TAG, message, e);
    }
    private static void logE(Throwable e) {
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

    private static void handleException(Exception e, ApplicationState UI) {
        logE(e);
        UI.updateState(ApplicationState.State.ERROR);
        return;
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
        Benchmark benchmark, ApplicationState mainUI, MeasuringTool tool, int roundNo)
        throws InterruptedException, RunnerException {

        List<BenchmarkResult> compiledMetadata = new ArrayList<BenchmarkResult> ();
        final ApplicationState.State state = ApplicationState.State.MILESTONE;

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

        String refTypesString = introspected.get("has_reference_types");
        boolean hasRefTypes = (refTypesString != null) && (refTypesString.equals("1"));

        String parameterCountString = introspected.get("parameter_count");
        int parameterCount = (parameterCountString == null) ? -1 : Integer.parseInt(parameterCountString);
            
        boolean dynamicParameters =
            benchmark.dynamicParameters() ||
            (hasRefTypes && (-1 < parameterCount && parameterCount < 2));

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
            Log.v(TAG, "Benchmark " + benchmark.getClass().getSimpleName());

            bPar.setUp(); // (I) needs tearDown (see II)

            try {
                tool.startMeasuring(benchmark);
                benchmarkCount++;
            }
            catch (IOException e) {
                logE("Measuring caused IO exception", e);
                if (mainUI.userWantsToRetry(e)) {
                    continue; // without incrementing i
                }
                else {
                    throw new InterruptedException("User wants to abort");
                }
            }
            finally {
                bPar.tearDown(); // (II) needs setUp (see I)
            }
            if (tool.explicitGC() && benchmarkCount % 25 == 0) {
                System.gc();
                Thread.sleep(350);
            }

            mainUI.updateState(state, tool.getClass().getSimpleName() + " round " + roundNo  + " benchmark " + benchmarkCount);

            List<BenchmarkResult> measurements = tool.getMeasurements();
            for (BenchmarkResult measurement : measurements) {
                if (!measurement.isEmpty()) {
                    // todo: actual vs. requested size (objects etc.)
                    if (dynamicParameters) {
                        measurement.put("dynamic_size", "" + i);
                    }
                    else {
                        measurement.put("dynamic_size", MISSING_VALUE);
                    }
                    measurement.put("id", benchmark.id());
                    measurement.put("class", benchmark.getClass().getSimpleName());
                    measurement.putAll(introspected);
                    compiledMetadata.add(measurement);
                }
            }
            // if parameter size can be varied, vary it - else break with first size
            if (!dynamicParameters) {
                break;
            }
            if (iterator.hasNext()) {
                i = iterator.next();
            }
            else {
                break;
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

    private static PrintWriter
    makeWriter(File file, boolean append)
    throws FileNotFoundException
    {
        return new PrintWriter(makeOutputStream(file, append));
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

    private static BenchmarkResult inspectBenchmark(Benchmark benchmark) throws ClassNotFoundException {
        BenchmarkResult bdata = new BenchmarkResult();
        int seqNo = benchmark.sequenceNo();
        String from = benchmark.from();
        String to   = benchmark.to();
        bdata.put("no", "" + benchmark.sequenceNo());
        bdata.put("description", benchmark.description());
        bdata.put("from", from);
        bdata.put("to", to);
        bdata.put("representative", benchmark.representative() ? "1" : "0");

        if (seqNo == -1) {
            bdata.put("custom", "1");
            return bdata;
        }
        
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
                    String param_typename = param.getSimpleName();
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
        return bdata;
    }
}
