
package fi.helsinki.cs.tituomin.nativebenchmark;

import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.BasicOption;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringTool.RunnerException;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.PlainRunner;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.ResponseTimeRecorder;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.JavaSystemNanoResponseTimeRecorder;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.LinuxPerfRecordTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.CommandlineTool;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MockCommandlineTool;

import android.util.Pair;
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

import static fi.helsinki.cs.tituomin.nativebenchmark.Utils.colPr;
import static fi.helsinki.cs.tituomin.nativebenchmark.Utils.getUUID;
import static fi.helsinki.cs.tituomin.nativebenchmark.Utils.makeWriter;
import static fi.helsinki.cs.tituomin.nativebenchmark.Utils.makeOutputStream;

public enum BenchmarkRunner {
    INSTANCE; // singleton enum pattern

    private static final String SEPARATOR     = ",";
    private static final String MISSING_VALUE = "-";
    private static final long WARMUP_REPS = 50000;
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

    public void initTools(ToolConfig conf, long repetitions, long allocRepetitions) throws IOException, InterruptedException {

        conf.setRepetitions(this.repetitions);
        conf.setAllocRepetitions(this.allocatingRepetitions);

        measuringTools = new ArrayList<MeasuringTool> ();
        for (MeasuringTool tool : conf) {
            Log.v(TAG, "-------has tool");
            measuringTools.add(tool);
        }
    }

    private long repetitions;
    private long allocatingRepetitions;
    private CharSequence appRevision;
    private CharSequence appChecksum;
    private File cacheDir;
    private boolean runAllBenchmarks;
    private boolean runAtMaxSpeed;
    private String benchmarkSubstring;

    public enum BenchmarkSet { ALLOC, NON_ALLOC };
    private BenchmarkSet benchmarkSet;

    public BenchmarkRunner setRepetitions(long x) {
        repetitions = x;
        return this;
    }
    public BenchmarkRunner setAllocatingRepetitions(long x) {
        allocatingRepetitions = x;
        return this;
    }
    public BenchmarkRunner setAppRevision(CharSequence x) {
        appRevision = x;
        return this;
    }
    public BenchmarkRunner setAppChecksum(CharSequence x) {
        appChecksum = x;
        return this;
    }
    public BenchmarkRunner setCacheDir(File x) {
        cacheDir = x;
        return this;
    }
    public BenchmarkRunner setRunAllBenchmarks(boolean x) {
        runAllBenchmarks = x;
        return this;
    }
    public BenchmarkRunner setRunAtMaxSpeed(boolean x) {
        runAtMaxSpeed = x;
        return this;
    }
    public BenchmarkRunner setBenchmarkSubstring(String x) {
        benchmarkSubstring = x;
        return this;
    }
    public BenchmarkRunner setBenchmarkSet(BenchmarkSet x) {
        benchmarkSet = x;
        return this;
    }

    public void runBenchmarks(ApplicationState mainUI, ToolConfig config, File dataDir) {
        interrupted = false;

        try {
            BenchmarkRegistry.init(this.repetitions);
            // todo replace with config
            MeasuringTool.setDataDir(dataDir);
            Log.v(TAG, config.toString());
            initTools(config, this.repetitions, this.allocatingRepetitions);
        }
        catch (Exception e) {
            mainUI.updateState(ApplicationState.State.ERROR);
            Log.e("BenchmarkRunner", "Error initialising", e);
            return;
        }

        benchmarkParameter = getBenchmarkParameter();
        BenchmarkInitialiser.init(benchmarkParameter);

        List<Benchmark> allBenchmarks = BenchmarkRegistry.getBenchmarks();

        // todo enable
        // Collections.shuffle(benchmarks);
        try {
            Init.initEnvironment(true); // run warmup at max speed
        }
        catch (IOException e) {
            handleException(e, mainUI);
            return;
        }

        for (MeasuringTool tool : measuringTools) {
            if (interrupted) {
                return;
            }

            List<Benchmark> benchmarks = new ArrayList<Benchmark> ();

            String substringToApply = this.benchmarkSubstring;
            if (!tool.getFilter().equals("")) {
                substringToApply = tool.getFilter();
            }

            for (Benchmark b : allBenchmarks) {
                boolean selected;
                if (this.runAllBenchmarks) {
                    selected = true;
                }
                else if (!substringToApply.equals("")) {
                    selected = (
                        b.getClass().getSimpleName().toLowerCase().indexOf(
                            substringToApply) != -1);
                }
                else {
                    selected = (
                        b.representative() &&
                        ((!b.isAllocating()) && this.benchmarkSet == BenchmarkSet.NON_ALLOC) ||
                        (b.isAllocating()  && this.benchmarkSet == BenchmarkSet.ALLOC));
                }
                if (selected) {
                    benchmarks.add(b);
                }
            }
            int numOfBenchmarks = benchmarks.size();

            Log.i(TAG, tool.getClass().getSimpleName());

            if (!tool.ignore()) {
                // set the slower CPU frequency etc. after the warmup
                // round(s), taking less time
                if (!this.runAtMaxSpeed) {
                    try {
                        Init.initEnvironment(false);
                    }
                    catch (IOException e) {
                        handleException(e, mainUI);
                        return;
                    }
                }
            }

            int max_rounds = tool.getRounds();
            String measurementID = Utils.getUUID();
            File resultFile = new File(dataDir, "benchmarks-" + measurementID + ".csv");
            long startTime = SystemClock.uptimeMillis();
            Date start = new Date();
            long endTime = 0;

            int round = -1;
            boolean labelsWritten = false;

        ROUNDLOOP:
            while (++round < max_rounds) {
                benchmarkCount = 0;
                PrintWriter tempWriter = null;
                File tempFile = new File(this.cacheDir, "benchmarks-temp.csv");
                try {
                    tempWriter = Utils.makeWriter(tempFile, false);
                }
                catch (FileNotFoundException e) {
                    handleException(e, mainUI);
                    return;
                }


                List<BenchmarkResult> collectedData;

                try {
                    if (tool.explicitGC()) {
                        System.gc();
                        Thread.sleep(500);
                    }
                }
                catch (InterruptedException e) {
                    logE("Measuring thread was interrupted");
                    mainUI.updateState(
                        ApplicationState.State.INTERRUPTED);
                    interrupted = true;
                    break ROUNDLOOP;
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
                        break ROUNDLOOP;
                    }
                    catch (InterruptedException e) {
                        logE("Measuring thread was interrupted");
                        mainUI.updateState(
                            ApplicationState.State.INTERRUPTED);

                        interrupted = true;
                        break ROUNDLOOP;
                    }

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
                }
                endTime = SystemClock.uptimeMillis();
                tempWriter.close();

                if (tool.ignore()) {
                    continue; // todo test
                }

                InputStream in = null;
                OutputStream out = null;
                PrintWriter resultWriter = null;
                try {
                    resultWriter = Utils.makeWriter(resultFile, true);
                    // note: labels should be written last, after
                    // all possible keys have been created
                    if (!labelsWritten) {
                        String[] labels = BenchmarkResult.labels();
                        for (int i = 0; i < BenchmarkResult.size(); i++) {
                            resultWriter.print(labels[i] + SEPARATOR);
                        }
                        resultWriter.println("");
                        resultWriter.close();
                        labelsWritten = true;
                    }

                    in = new FileInputStream(tempFile);
                    out = Utils.makeOutputStream(resultFile, true);
                    Utils.copyStream(in, out);
                    //tempFile.delete();
                }
                catch (Exception e) {
                    mainUI.updateState(ApplicationState.State.ERROR);
                    Log.e("BenchmarkRunner", "Error writing results", e);
                    interrupted = true;
                    break ROUNDLOOP;
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
                        break ROUNDLOOP;
                    }
                }
            }

            if (round > 0 && !tool.ignore()) {
                // there has been at least one succesful round
                writeMeasurementMetadata(
                    new File(dataDir, "measurements.txt"),
                    measurementID,
                    (this.runAtMaxSpeed ? Init.CPUFREQ_MAX : Init.CPUFREQ),
                    startTime, endTime, start, tool, numOfBenchmarks, round);

                List<String> filenames = tool.getFilenames();
                if (!filenames.isEmpty()) {
                    OutputStream os = null;

                    try {
                        File zip = new File(dataDir, "perfdata-" + measurementID + ".zip");
                        os = Utils.makeOutputStream(zip, false);
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

    private void writeMeasurementMetadata (
        File catalogFile, String measurementID, int cpuFreq,
        long startTime, long endTime, Date start, MeasuringTool tool,
        int numOfBenchmarks, int rounds) {

        Date end = new Date();
        PrintWriter c = null;
        try {
            c = Utils.makeWriter(catalogFile, true);

            c.println("");
            for (Pair<String,String> pair: tool.configuration()) {
                colPr(c, pair.first, pair.second);
            }
            colPr(c, "id", measurementID);
            colPr(c, "cpu-freq", cpuFreq);
            colPr(c, "logfile", LogAccess.filename());
            colPr(c, "rounds", rounds);
            colPr(c, "start", start);
            colPr(c, "end", end);
            colPr(c, "duration", Utils.humanTime(endTime - startTime));
            colPr(c, "benchmarks", numOfBenchmarks);
            colPr(c, "code-revision", this.appRevision);
            colPr(c, "code-checksum", this.appChecksum);
            colPr(c, "benchmark-set", this.benchmarkSet);
            colPr(c, "substring-filter", this.benchmarkSubstring);
            c.println("");
        }
        catch (IOException e ) {
            logE(e);
        }
        finally {
            c.close();
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
        Log.e(TAG, msg);
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
                InputStream is = Utils.makeInputStream(filename);
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

            mainUI.updateState(state, tool.getClass().getSimpleName() + " warmup " + tool.isWarmupRound()  + " round " + roundNo  + " benchmark " + benchmarkCount);

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
        bdata.put("nonvirtual", benchmark.isNonvirtual() ? "1" : "0");

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
