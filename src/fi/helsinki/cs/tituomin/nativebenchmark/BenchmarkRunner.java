
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
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import android.os.Environment;
import android.util.Log;
import android.os.SystemClock;
import java.util.SortedSet;
import java.util.Collections;
import java.util.TreeSet;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;

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
        measuringTools.add(new PlainRunner(1));
        measuringTools.add(new ResponseTimeRecorder(rounds));
        measuringTools.add(new LinuxPerfRecordTool(1)
                        .set(BasicOption.OUTPUT_FILEPATH, perfDir.getPath())
                        .set(BasicOption.MEASURE_LENGTH, "1"));
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

        benchmarkParameter = new BenchmarkParameter();
        BenchmarkInitialiser.init(benchmarkParameter);

        List<Benchmark> benchmarks = BenchmarkRegistry.getBenchmarks();
        Map<Class, MetadataContainer> benchmarkBaseData = new HashMap<Class, MetadataContainer>();
        for (Benchmark b : benchmarks) {
            benchmarkBaseData.put(b.getClass(), inspectBenchmark(b));
        }

        Collections.shuffle(benchmarks);

        for (MeasuringTool tool : measuringTools) {
            String measurementID;

            int round_count = tool.getRounds();
            for (int series = 0; series < round_count; series++) {
                List<MetadataContainer> compiledMetadata = new ArrayList<MetadataContainer> ();

                Date start = new Date();
                long endTime = 0;

                long startTime = SystemClock.uptimeMillis();
                runSeries(benchmarks, benchmarkBaseData, mainUI, compiledMetadata, tool);
                endTime = SystemClock.uptimeMillis();

                measurementID = Utils.getUUID();

                if (compiledMetadata.isEmpty()) {
                    Log.v("runner", "compiledmetadata is empty " + tool.getClass());
                    continue;
                }

                // 1. gather all labels (!ordered!)
                SortedSet<String> labels = new TreeSet<String> ();
                for (MetadataContainer md : compiledMetadata) {
                    labels.addAll(md.keySet());
                }

                PrintWriter writer = null;

                try {
                    writer = getWriter(new File(dataDir, "benchmarks-" + measurementID + ".csv"), false);
                    for (String label : labels) {
                        writer.print(label + SEPARATOR);
                    }
                    writer.println("");
                    String[] values = new String[labels.size()];

                    // print all possible values for all benchmarks
                    for (MetadataContainer md : compiledMetadata) {
                        int i = 0;
                        for (String label : labels) {
                            String val = md.get(label);
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
                    Log.e("Nativebenchmark", "ioexception " + e);
                }
                if (writer != null) {
                    writer.close();
                }

                Date end = new Date();

                try {
                    writer = getWriter(new File(dataDir, "measurements.txt"), true);
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
                    Log.e("Nativebenchmark", "ioexception", e);
                }
                finally {
                    writer.close();
                }
            }
        }

        mainUI.updateState(
            ApplicationState.State.MEASURING_FINISHED);
        }

    private static void runSeries(
        List<Benchmark> benchmarks, Map<Class, MetadataContainer> basedata,
        ApplicationState mainUI, List<MetadataContainer> compiledMetadata,
        MeasuringTool tool) {

        final ApplicationState.State state = ApplicationState.State.MILESTONE;
        final boolean runGC = tool.explicitGC();
        int j = 0;

        for (Benchmark benchmark : benchmarks) {
            MetadataContainer md = new BenchmarkMetadata();
            tool.setBenchmark(benchmark);
            tool.run();
            MetadataContainer measurement = tool.getMeasurement();
            if (!measurement.isEmpty()) {
                md.putAll(basedata.get(benchmark.getClass()));
                md.putAll(measurement);
                compiledMetadata.add(md);
            }
            // todo: remove UI overhead?
            mainUI.updateState(state, "Benchmark " + ++j);

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
    }

    private static PrintWriter getWriter(File file, boolean append)
    throws FileNotFoundException {
        return new PrintWriter(
            new BufferedOutputStream(
                new FileOutputStream(file, append)));
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
            seconds + "s" +
            " (" + seconds_total + " s tot.)");
    }

    private static MetadataContainer lastMetadata = null;
    private static MetadataContainer inspectBenchmark(Benchmark benchmark) {
        MetadataContainer bdata = new BenchmarkMetadata();
        Class c = benchmark.getClass();
        String from = benchmark.from();
        String to   = benchmark.to();

        // reflection only works for J2C version,
        // results are cached for other versions
        if (from.equals("J") && to.equals("C")) {
            bdata.put("no", "" + benchmark.sequenceNo());
            bdata.put("description", benchmark.description());
            Method[] methods = c.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                Method m = methods[i];
                int modifiers = m.getModifiers();
                if (Modifier.isNative(modifiers)) {
                    bdata.put("native_static", Modifier.isStatic(modifiers) ? "1" : "0");
                    bdata.put("native_private", Modifier.isPrivate(modifiers) ? "1" : "0");
                    bdata.put("native_protected", Modifier.isProtected(modifiers) ? "1" : "0");
                    bdata.put("native_public", Modifier.isPublic(modifiers) ? "1" : "0");

                    Class [] parameter_arr = m.getParameterTypes();
                    List<Class> parameters = Arrays.asList(parameter_arr);
                    Map<String,Integer> parameterTypes = new HashMap<String,Integer> ();

                    Integer previousValue = null;
                    for (Class param : parameters) {
                        String param_typename = param.getCanonicalName();
                        previousValue = parameterTypes.get(param_typename);
                        parameterTypes.put(
                            param_typename,
                            (previousValue == null ? 1 : ((int)previousValue) + 1));
                    }

                    bdata.put("parameter_type_count", parameterTypes.keySet().size() + "");

                    for (String typename : parameterTypes.keySet()) {
                        bdata.put("parameter_type_" + typename + "_count", parameterTypes.get(typename) + "");
                    }

                    bdata.put("parameter_count", parameters.size() + "");
                    Class returnType = m.getReturnType();
                    bdata.put("return_type", returnType.getCanonicalName());
                }
            }
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
