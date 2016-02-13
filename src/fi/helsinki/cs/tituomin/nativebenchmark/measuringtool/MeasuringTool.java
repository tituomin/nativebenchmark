package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;


import android.util.Log;
import android.util.Pair;
import fi.helsinki.cs.tituomin.nativebenchmark.ApplicationState;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkResult;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.AllocatingBenchmarkLongRunningWrapper;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.AllocatingBenchmarkShortRunningWrapper;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringOption;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.OptionSpec;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.RunningWrapper;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

public abstract class MeasuringTool implements Runnable {

    public MeasuringTool
        (
            int rounds,
            long repetitions,
            long allocRepetitions,
            boolean warmup,
            boolean runAllBenchmarks
         ) throws
            IOException, InterruptedException
    {
        clearMeasurements();
        specifyOptions();
        this.rounds = rounds;
        this.repetitions = repetitions;
        this.allocRepetitions = allocRepetitions;
        this.warmup = warmup;
        this.explicitGC = !warmup;
        this.runAllBenchmarks = runAllBenchmarks;
        init();
    }

    public static synchronized void userInterrupt() {
        userInterrupted = true;
        BenchmarkRegistry.interruptNative();
    }

    public static synchronized boolean userInterrupted() {
        if (userInterrupted == true) {
            userInterrupted = false;
            return true;
        }
        return false;
    }

    protected void init() throws IOException, InterruptedException { }

    protected abstract void start(Runnable benchmark)
        throws InterruptedException, IOException;

    public boolean isLongRunning() {
        return false;
    }

    public RunningWrapper wrap(Benchmark benchmark) {
        if (!benchmark.isAllocating()) {
            // Default running algorithm
            return new RunningWrapper(benchmark);
        }
        else {
            // the benchmark does allocations, we have
            // to limit the amount of loops -> compensate
            // by repeating the loop many times
            if (isLongRunning()) {
                return new AllocatingBenchmarkLongRunningWrapper(benchmark, allocRepetitions);
            }
            else {
                return new AllocatingBenchmarkShortRunningWrapper(benchmark, allocRepetitions);
            }
        }
    }

    public void startMeasuring(Benchmark benchmark) throws InterruptedException, IOException, RunnerException {
        String benchmarkName = benchmark.getClass().getSimpleName();
        clearMeasurements();
        setDefaultOptions();
        benchmark.setRepetitions(this.repetitions);
        RunningWrapper wrapper = wrap(benchmark);
        Date start = null, end = null;

        start = new Date();
        wrapper.begin(this);
        end = new Date();
        
        putMeasurement("start", DATEFORMAT.format(start));
        putMeasurement("end", DATEFORMAT.format(end));

        if (wrapper.wasInterrupted() && userInterrupted()) {
            throw new InterruptedException("Interrupted by user");
        }
        if (wrapper.exceptionWasThrown()) {
            throw new RunnerException(wrapper.getException());
        }
    }

    public class RunnerException extends Exception { 
        public RunnerException(Throwable t) {
            super(t);
        }
    }


    public void run() {
        try {
            start(benchmark);
        }
        catch (InterruptedException e) {
            Log.e("BenchmarkRunner", "Measuring was interrupted ", e);
        }
        catch (IOException e) {
            Log.e("BenchmarkRunner", "IOException", e);
        }
    }

    public boolean explicitGC() {
        return this.explicitGC;
    }

    public boolean runAllBenchmarks() {
        return this.runAllBenchmarks;
    }

    public void setExplicitGC(boolean e) {
        this.explicitGC = e;
    }

    protected abstract List<MeasuringOption>
        defaultOptions(List<MeasuringOption> container);

    protected List<OptionSpec> specifyAllowedOptions(List<OptionSpec> container) {
        return container;
    }

    protected void specifyOptions() {
        this.allowedOptions = specifyAllowedOptions(
            new LinkedList<OptionSpec>());

        if (this.requiredOptions == null) {
            this.requiredOptions = new HashSet<OptionSpec>();
        }
        for (OptionSpec o : this.allowedOptions) {
            if (o.required()) {
                this.requiredOptions.add(o);
            }
        }
    }

    protected void setDefaultOptions() {
        for (MeasuringOption op : defaultOptions(
                 new LinkedList<MeasuringOption>())) {
            setOption(op);
        };
    }

    // -----

    public MeasuringTool set(String id, String value) {
        setOption(new BasicOption(OptionSpec.byId(id), value));
        return this;
    }

    public MeasuringTool set(OptionSpec spec, String value) {
        // todo: not typesafe (assumes basicoption)
        setOption(new BasicOption(spec, value));
        return this;
    }

    public String getOption(OptionSpec id) {
        return this.options.get(id).value();
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public void setOption(MeasuringOption option) {
        if (this.allowedOptions == null) {
            specifyOptions();
        }
        if (!allowedOptions.contains(option.type())) {
            throw new UnsupportedOptionException();
        }
        else {
            this.options =
                options != null ?
                options :
                new HashMap<OptionSpec,MeasuringOption> ();
            
            this.options.put(option.type(), option);
        }
    }

    public void setBenchmark(Benchmark b) {
        benchmark = b;
    }

    public boolean ignore() {
        return false;
    }

    private boolean hasRequiredOptions() {
        return options.keySet().containsAll(requiredOptions);
    }

    protected List<OptionSpec> allowedOptions;
    protected Set<OptionSpec> requiredOptions;
    // currently support one value per option, change?
    protected Map<OptionSpec,MeasuringOption> options;

    private List<ApplicationState> observers;

    private BenchmarkResult currentMeasurement;
    private List<BenchmarkResult> measurements;

    protected long repetitions;

    protected void putMeasurement(String key, String value) {
        currentMeasurement.put(key, value);
    }

    protected void finishMeasurement() {
        currentMeasurement = new BenchmarkResult();
        measurements.add(currentMeasurement);
    }

    public List<BenchmarkResult> getMeasurements() {
        for (BenchmarkResult measurement : measurements) {
            if (this.options != null) {
                for (MeasuringOption op : options.values()) {
                    measurement.put(
                        op.toStringPair().first,
                        op.toStringPair().second);
                }
            }
        }
        return this.measurements;
    }

    public void clearMeasurements() {
        this.currentMeasurement = new BenchmarkResult();
        this.measurements = new LinkedList<BenchmarkResult> ();
        measurements.add(currentMeasurement);
    }

    private static final List<String> emptyList = new ArrayList<String> ();
    public List<String> getFilenames() {
        return emptyList;
    }

    public int getRounds() {
        return rounds;
    }

    public static void setDataDir(File dir) {
        dataDir = dir;
    }

    public static File getDataDir() {
        return dataDir;
    }

    public void setFilter(String substring) {
        filterSubstring = substring;
    }
    public String getFilter() {
        return filterSubstring;
    }
    public List<Pair<String,String>> configuration() {
        List<Pair<String,String>> pairs = new ArrayList<Pair<String,String>> ();
        pairs.add(new Pair<String,String>("tool", this.getClass().getSimpleName()));
        pairs.add(new Pair<String,String>("repetitions", "" + this.repetitions));
        pairs.add(new Pair<String,String>("description", this.description));
        pairs.add(new Pair<String,String>("warmup", "" + this.warmup));
        if (options != null) {
            for (MeasuringOption opt : options.values()) {
                pairs.add(new Pair<String,String>(opt.type().id(), opt.value()));
            }
        }
        return pairs;
    }

    public boolean isWarmupRound() {
        return warmup;
    }

    private static File dataDir;

    private Benchmark benchmark;
    private int rounds;
    private long allocRepetitions;
    private String description;
    private String filterSubstring;
    protected boolean warmup;
    private boolean explicitGC;
    private boolean runAllBenchmarks;
    private static boolean userInterrupted = false;
    private final static String TAG = "MeasuringTool";
    public static class UnsupportedOptionException extends RuntimeException {}

    private static SimpleDateFormat DATEFORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);

}

