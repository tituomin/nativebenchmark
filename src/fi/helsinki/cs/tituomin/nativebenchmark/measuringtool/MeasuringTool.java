package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import android.util.Log;
import java.util.Observer;
import java.util.Observable;
import fi.helsinki.cs.tituomin.nativebenchmark.ApplicationState;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.MeasuringOption;
import fi.helsinki.cs.tituomin.nativebenchmark.measuringtool.OptionSpec;
import fi.helsinki.cs.tituomin.nativebenchmark.Benchmark;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkResult;
import fi.helsinki.cs.tituomin.nativebenchmark.BenchmarkRegistry;

public abstract class MeasuringTool implements Runnable {

    // public MeasuringTool()  throws IOException, InterruptedException {
    //     this(1, 1000);
    // }

    public MeasuringTool(int rounds, long userRepetitions) throws IOException, InterruptedException {
        specifyOptions();
        this.rounds = rounds;
        this.userRepetitions = userRepetitions;
        clearMeasurements();
        init();
    }

    protected void init() throws IOException, InterruptedException { }

    protected abstract void start(Runnable benchmark)
        throws InterruptedException, IOException;

    public long repetitions() {
        return -1;
    }

    public boolean isLongRunning() {
        return false;
    }

    private Exception exceptionThrown;

    public void startMeasuring(Benchmark benchmark) throws InterruptedException, IOException {
        clearMeasurements();

        // default: user defined amount of repetitions
        benchmark.setRepetitions(this.userRepetitions);

        if (benchmark.maxRepetitions() == -1) {
            long toolOverrideReps = repetitions();
            if (toolOverrideReps != -1) {
                // the tool measurement logic requires
                // overriding the repetition amount
                Log.v("Tool", "override " + benchmark.getClass().getName());
                benchmark.setRepetitions(toolOverrideReps);
            }
            Log.v("Tool", "normal " + benchmark.getClass().getName());
            start(benchmark);
        }
        else if (benchmark.maxRepetitions() > 0) {
            // the benchmark does allocations, we have
            // to limit the amount of loops -> try to compensate
            // by repeating the loop many times
            if (isLongRunning()) {
                Log.v("Tool", "long running " + benchmark.getClass().getName());
                start(new RepetitiveRunner(benchmark));
            }
            else {
                Log.v("Tool", "not long running " + benchmark.getClass().getName());
                new RepetitiveRunner(benchmark).run();
            }
            if (exceptionThrown != null) {
                if (exceptionThrown instanceof InterruptedException) {
                    throw new InterruptedException(exceptionThrown.getMessage());
                }
                else {
                    throw new IOException(exceptionThrown);
                }
            }
        }
        else {
            Log.e("MeasuringTool", "Invalid repetition amount");
            throw new IllegalArgumentException("Invalid repetition amount.");
        }
    }

    private class RepetitiveRunner implements Runnable {
        private Benchmark benchmark;
        public RepetitiveRunner(Benchmark b) {
            benchmark = b;
        }
        public void run() {
            exceptionThrown = null;
            long totalReps = userRepetitions;
            long benchmarkReps = benchmark.maxRepetitions();
            long toolReps;
            if (!isLongRunning()) {
                toolReps = 20; // todo ? 
            }
            else {
                toolReps = Long.MAX_VALUE;
            }
            benchmark.setRepetitions(benchmarkReps);
            if (isLongRunning()) {
                putMeasurement("repetitions", benchmarkReps + "");
                putMeasurement("multiplier", toolReps + "");
                long interval = BenchmarkRegistry.CHECK_INTERRUPTED_INTERVAL;

                long division, remainder;
                long repetitions = toolReps;

                division  = repetitions / interval + 1;
                remainder = repetitions % interval + 1;

                while (--division != 0) { 
                    interval = BenchmarkRegistry.CHECK_INTERRUPTED_INTERVAL;
                    interval = interval + 1;
                    while (--interval != 0) { 
                        try {
                            benchmark.run();
                            System.gc();
                            Thread.sleep(50);
                        }
                        catch (Exception e) {
                            exceptionThrown = e;
                            return;
                        }
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                }

                while (--remainder != 0) { 
                    try {
                        benchmark.run();
                        System.gc();
                        Thread.sleep(50);
                    }
                    catch (Exception e) {
                        exceptionThrown = e;
                        return;
                    }
                }
                finishMeasurement();

            }
            else {
                while (--toolReps != 0) {
                    putMeasurement("repetitions", benchmarkReps + "");
                    putMeasurement("multiplier", toolReps + "");
                    try {
                        start(benchmark);
                        finishMeasurement();
                        System.gc();
                        Thread.sleep(50);
                    }
                    catch (Exception e) {
                        exceptionThrown = e;
                        return;
                    }
                }
            }
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
        return true;
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
            if (o.required) {
                this.requiredOptions.add(o);
            }
        }
        for (MeasuringOption op : defaultOptions(
                 new LinkedList<MeasuringOption>())) {
            setOption(op);
        };
    }

    // -----

    public MeasuringTool set(OptionSpec spec, String value) {
        // todo: not typesafe (assumes basicoption)
        setOption(new BasicOption(spec, value));
        return this;
    }

    public String getOption(OptionSpec id) {
        return this.options.get(id).value();
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

    public void addObserver(ApplicationState o) {
        if (this.observers == null) {
            this.observers = new LinkedList<ApplicationState>();
        }
        this.observers.add(o);
    }

    public void notifyObservers(ApplicationState.State state) {
        
        for (ApplicationState o : this.observers) {
            o.updateState(state);
        }
    }
    public final static Observable mockObservable = new Observable();

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

    private long userRepetitions;

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

    private Benchmark benchmark;
    private int rounds;

    public static class UnsupportedOptionException extends RuntimeException {}
}

